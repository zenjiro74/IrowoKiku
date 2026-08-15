package com.example.zenjiro74.irowokiku.camera

import kotlin.math.max
import kotlin.math.sqrt

/** 1 フレームから取り出した統計量。 */
data class FrameStats(
    /** Hasler–Süsstrunk colorfulness。0-255 スケールで無彩色なら 0 付近。 */
    val colorfulness: Float,
    /** 平均輝度 (0-255)。暗所でノイズが指標を押し上げるのを弾くために使う。 */
    val meanLuma: Float,
) {
    companion object {
        val EMPTY = FrameStats(colorfulness = 0f, meanLuma = 0f)
    }
}

/**
 * Hasler–Süsstrunk の colorfulness metric。
 *
 *   rg = R - G
 *   yb = 0.5 * (R + G) - B
 *   M  = sqrt(var(rg) + var(yb)) + 0.3 * sqrt(mean(rg)^2 + mean(yb)^2)
 *
 * Android のフレームワークに依存しないので JVM 単体テストできる。
 */
object ColorfulnessMetric {

    /** サブサンプルのブロック辺。4 なら 640x480 が 160x120 = 19,200 サンプルになる。 */
    const val DEFAULT_BLOCK_SIZE = 4

    private const val MEAN_WEIGHT = 0.3

    /**
     * RGBA_8888 のバイト列 [rgba] から統計量を求める。
     *
     * 全画素は舐めず [blockSize] 四方のボックス平均でサブサンプルする。平均化は速度のためだけで
     * なく、暗所のショットノイズを 1/blockSize^2 に落として偽の「カラフルさ」を減らす効果もある。
     *
     * [rowStride] は行あたりのバイト数。端末によっては width * pixelStride より大きい
     * (行末にパディングが入る) ので、必ず呼び出し側の実値を渡すこと。
     */
    fun analyze(
        rgba: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int = 4,
        blockSize: Int = DEFAULT_BLOCK_SIZE,
    ): FrameStats {
        require(blockSize >= 1) { "blockSize must be >= 1 but was $blockSize" }

        val blocksX = width / blockSize
        val blocksY = height / blockSize
        if (blocksX <= 0 || blocksY <= 0) return FrameStats.EMPTY

        var sumRg = 0.0
        var sumRgSq = 0.0
        var sumYb = 0.0
        var sumYbSq = 0.0
        var sumLuma = 0.0

        val pixelsPerBlock = blockSize * blockSize
        val invPixelsPerBlock = 1.0 / pixelsPerBlock

        for (blockY in 0 until blocksY) {
            for (blockX in 0 until blocksX) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                for (dy in 0 until blockSize) {
                    val rowOffset = (blockY * blockSize + dy) * rowStride
                    var index = rowOffset + blockX * blockSize * pixelStride
                    for (dx in 0 until blockSize) {
                        sumR += rgba[index].toInt() and 0xFF
                        sumG += rgba[index + 1].toInt() and 0xFF
                        sumB += rgba[index + 2].toInt() and 0xFF
                        index += pixelStride
                    }
                }

                val r = sumR * invPixelsPerBlock
                val g = sumG * invPixelsPerBlock
                val b = sumB * invPixelsPerBlock

                val rg = r - g
                val yb = 0.5 * (r + g) - b

                sumRg += rg
                sumRgSq += rg * rg
                sumYb += yb
                sumYbSq += yb * yb
                sumLuma += LUMA_R * r + LUMA_G * g + LUMA_B * b
            }
        }

        val count = blocksX.toLong() * blocksY
        val invCount = 1.0 / count

        val meanRg = sumRg * invCount
        val meanYb = sumYb * invCount
        // 浮動小数の丸めで僅かに負になりうるので 0 で下げ止める。
        val varRg = max(0.0, sumRgSq * invCount - meanRg * meanRg)
        val varYb = max(0.0, sumYbSq * invCount - meanYb * meanYb)

        val colorfulness =
            sqrt(varRg + varYb) + MEAN_WEIGHT * sqrt(meanRg * meanRg + meanYb * meanYb)

        return FrameStats(
            colorfulness = colorfulness.toFloat(),
            meanLuma = (sumLuma * invCount).toFloat(),
        )
    }

    private const val LUMA_R = 0.299
    private const val LUMA_G = 0.587
    private const val LUMA_B = 0.114
}
