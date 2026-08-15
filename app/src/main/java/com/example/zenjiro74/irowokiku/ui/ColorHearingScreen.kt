package com.example.zenjiro74.irowokiku.ui

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.zenjiro74.irowokiku.audio.SineEngine
import com.example.zenjiro74.irowokiku.camera.CameraViewfinder

private const val TAG = "IrowoKiku"

/** ステップ3の動作確認用。ステップ4で colorfulness から決まる値に置き換える。 */
private const val TEST_FREQUENCY_HZ = 440f

/**
 * メイン画面。ステップ4以降で解析結果をエンジンに流し込む。
 */
@Composable
fun ColorHearingScreen(modifier: Modifier = Modifier) {
    // 結線確認用の暫定 analyzer。フレームが実際に届いているかを Logcat で見る。
    val analyzer = remember {
        var frames = 0L
        ImageAnalysis.Analyzer { image ->
            try {
                if (frames % 30L == 0L) {
                    val plane = image.planes[0]
                    Log.d(
                        TAG,
                        "frame #$frames ${image.width}x${image.height} " +
                            "format=${image.format} rowStride=${plane.rowStride} " +
                            "pixelStride=${plane.pixelStride} rotation=${image.imageInfo.rotationDegrees}",
                    )
                }
                frames++
            } finally {
                // 閉じ忘れるとフレーム供給が止まる。
                image.close()
            }
        }
    }

    val engine = remember { SineEngine() }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(engine) {
        onDispose { engine.stop() }
    }
    // バックグラウンドに回ったら鳴らし続けない。
    LifecycleResumeEffect(engine) {
        onPauseOrDispose {
            engine.stop()
            isPlaying = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CameraViewfinder(analyzer = analyzer, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    if (isPlaying) {
                        engine.stop()
                    } else {
                        engine.start(TEST_FREQUENCY_HZ)
                    }
                    isPlaying = engine.isRunning
                },
            ) {
                Text(if (isPlaying) "停止" else "開始")
            }
        }
    }
}
