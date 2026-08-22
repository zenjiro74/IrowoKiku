package com.example.zenjiro74.irowokiku.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val WIDTH = 64
private const val HEIGHT = 48
private const val PIXEL_STRIDE = 4

/**
 * RGBA_8888 のバイト列を組み立てる。[rowPadding] は行末に足すバイト数で、
 * rowStride > width * pixelStride になる端末を再現する。
 */
private fun rgbaImage(
    width: Int = WIDTH,
    height: Int = HEIGHT,
    rowPadding: Int = 0,
    pixel: (x: Int, y: Int) -> Triple<Int, Int, Int>,
): Pair<ByteArray, Int> {
    val rowStride = width * PIXEL_STRIDE + rowPadding
    val bytes = ByteArray(rowStride * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val (r, g, b) = pixel(x, y)
            val index = y * rowStride + x * PIXEL_STRIDE
            bytes[index] = r.toByte()
            bytes[index + 1] = g.toByte()
            bytes[index + 2] = b.toByte()
            bytes[index + 3] = 0xFF.toByte()
        }
    }
    return bytes to rowStride
}

private fun analyze(image: Pair<ByteArray, Int>, width: Int = WIDTH, height: Int = HEIGHT) =
    ColorfulnessMetric.analyze(
        rgba = image.first,
        width = width,
        height = height,
        rowStride = image.second,
        pixelStride = PIXEL_STRIDE,
    )

class ColorfulnessMetricTest {

    @Test
    fun `一様なグレーは colorfulness が 0 になる`() {
        val stats = analyze(rgbaImage { _, _ -> Triple(128, 128, 128) })

        assertEquals(0.0, stats.colorfulness.toDouble(), 1e-3)
        assertEquals(128.0, stats.meanLuma.toDouble(), 0.5)
    }

    @Test
    fun `明るさが違っても無彩色なら colorfulness は上がらない`() {
        // 縦じまのグレースケール。輝度は大きく振れるが色は付いていない。
        val stats = analyze(rgbaImage { x, _ -> if (x < WIDTH / 2) Triple(20, 20, 20) else Triple(230, 230, 230) })

        assertEquals(0.0, stats.colorfulness.toDouble(), 1e-3)
    }

    @Test
    fun `赤と青の二色画像は colorfulness が大きくなる`() {
        val gray = analyze(rgbaImage { _, _ -> Triple(128, 128, 128) })
        val vivid = analyze(
            rgbaImage { x, _ -> if (x < WIDTH / 2) Triple(255, 0, 0) else Triple(0, 0, 255) },
        )

        assertTrue(
            "二色画像 ${vivid.colorfulness} がグレー ${gray.colorfulness} を上回っていない",
            vivid.colorfulness > gray.colorfulness + 100f,
        )
    }

    @Test
    fun `彩度が高いほど colorfulness が大きくなる`() {
        fun splitImage(saturation: Int) = analyze(
            rgbaImage { x, _ ->
                val low = 128 - saturation / 2
                val high = 128 + saturation / 2
                if (x < WIDTH / 2) Triple(high, low, low) else Triple(low, low, high)
            },
        ).colorfulness

        val weak = splitImage(40)
        val medium = splitImage(120)
        val strong = splitImage(240)

        assertTrue("$weak < $medium が成り立たない", weak < medium)
        assertTrue("$medium < $strong が成り立たない", medium < strong)
    }

    @Test
    fun `行末パディングがあっても結果が変わらない`() {
        val pixel: (Int, Int) -> Triple<Int, Int, Int> = { x, y ->
            Triple((x * 4) % 256, (y * 5) % 256, (x + y) % 256)
        }

        val packed = analyze(rgbaImage(rowPadding = 0, pixel = pixel))
        val padded = analyze(rgbaImage(rowPadding = 64, pixel = pixel))

        assertEquals(packed.colorfulness.toDouble(), padded.colorfulness.toDouble(), 1e-4)
        assertEquals(packed.meanLuma.toDouble(), padded.meanLuma.toDouble(), 1e-4)
    }

    @Test
    fun `ブロックより小さい画像は空の結果を返す`() {
        val stats = ColorfulnessMetric.analyze(
            rgba = ByteArray(3 * 3 * PIXEL_STRIDE),
            width = 3,
            height = 3,
            rowStride = 3 * PIXEL_STRIDE,
            blockSize = 4,
        )

        assertEquals(FrameStats.EMPTY, stats)
    }
}
