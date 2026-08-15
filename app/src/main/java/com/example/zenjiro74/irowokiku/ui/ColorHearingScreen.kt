package com.example.zenjiro74.irowokiku.ui

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.zenjiro74.irowokiku.camera.CameraViewfinder

private const val TAG = "IrowoKiku"

/**
 * メイン画面。ステップ3以降でサイン波エンジンと HUD を載せる。
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

    Box(modifier = modifier.fillMaxSize()) {
        CameraViewfinder(analyzer = analyzer, modifier = Modifier.fillMaxSize())
    }
}
