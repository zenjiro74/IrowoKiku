package com.example.zenjiro74.irowokiku.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 解析に流す映像サイズ。色の統計量が欲しいだけなので高解像度は不要。 */
private val ANALYSIS_RESOLUTION = Size(640, 480)

/** AE/AWB がひとまず収束するまでの待ち時間。ロック要求はこれを待ってから出す。 */
private const val AUTO_EXPOSURE_WARM_UP_MILLIS = 1_000L

/**
 * 背面カメラのプレビューを表示しつつ、同じカメラのフレームを [analyzer] に流す。
 *
 * 解析は専用の単一スレッドで回し、バックプレッシャは KEEP_ONLY_LATEST。
 * 解析が遅れてもフレームがキューに溜まらず、常に最新フレームだけを見る。
 *
 * [lockExposureAndWhiteBalance] が true のときは AE/AWB をロックする。
 */
@Composable
fun CameraViewfinder(
    analyzer: ImageAnalysis.Analyzer,
    modifier: Modifier = Modifier,
    lockExposureAndWhiteBalance: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentAnalyzer by rememberUpdatedState(analyzer)

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(analysisExecutor) {
        onDispose { analysisExecutor.shutdown() }
    }

    LaunchedEffect(lifecycleOwner) {
        val provider = context.awaitCameraProvider()

        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            ANALYSIS_RESOLUTION,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .build()
            .apply {
                // rememberUpdatedState 越しに呼ぶことで、analyzer が差し替わっても
                // カメラを bind し直さずに済む。
                setAnalyzer(analysisExecutor) { image -> currentAnalyzer.analyze(image) }
            }

        provider.unbindAll()
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis,
        )

        try {
            awaitCancellation()
        } finally {
            camera = null
            imageAnalysis.clearAnalyzer()
            provider.unbindAll()
        }
    }

    LaunchedEffect(camera, lockExposureAndWhiteBalance) {
        val boundCamera = camera ?: return@LaunchedEffect
        // 露出/WB が動いている最中にロックすると中途半端な状態で固まるので、収束を待つ。
        if (lockExposureAndWhiteBalance) delay(AUTO_EXPOSURE_WARM_UP_MILLIS)
        boundCamera.setAutoExposureAndWhiteBalanceLocked(lockExposureAndWhiteBalance)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * [ProcessCameraProvider.getInstance] の ListenableFuture を suspend で待つ。
 * CameraX 1.6 には suspend 版の API が無いので、Guava 連携を足さずに自前で橋渡しする。
 */
private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
        continuation.invokeOnCancellation { future.cancel(false) }
    }
