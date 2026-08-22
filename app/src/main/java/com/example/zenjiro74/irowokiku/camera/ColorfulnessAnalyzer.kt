package com.example.zenjiro74.irowokiku.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * ImageAnalysis のフレームを [ColorfulnessMetric] に通し、[onResult] へ渡す。
 *
 * ImageProxy の入出力とバッファ管理だけを持ち、指標の計算そのものは純関数側にある。
 * [onResult] は CameraX の解析用スレッドから呼ばれる。
 */
class ColorfulnessAnalyzer(
    private val blockSize: Int = ColorfulnessMetric.DEFAULT_BLOCK_SIZE,
    private val onResult: (FrameStats) -> Unit,
) : ImageAnalysis.Analyzer {

    // 解析は単一スレッドで直列に走るので、フレーム間で使い回して割り当てを避ける。
    private var pixels = ByteArray(0)

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer.apply { rewind() }
            val size = buffer.remaining()
            if (pixels.size < size) pixels = ByteArray(size)
            buffer.get(pixels, 0, size)

            onResult(
                ColorfulnessMetric.analyze(
                    rgba = pixels,
                    width = image.width,
                    height = image.height,
                    rowStride = plane.rowStride,
                    pixelStride = plane.pixelStride,
                    blockSize = blockSize,
                ),
            )
        } finally {
            // 閉じ忘れるとフレーム供給が止まる。
            image.close()
        }
    }
}
