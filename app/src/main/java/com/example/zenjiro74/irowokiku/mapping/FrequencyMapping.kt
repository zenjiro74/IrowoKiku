package com.example.zenjiro74.irowokiku.mapping

import kotlin.math.pow

/**
 * colorfulness を周波数へ写す。音程は対数で知覚されるので、指標を線形に取って
 * 周波数を指数で動かす。既定では 110Hz(A2) から 1760Hz(A6) までのちょうど 4 オクターブ。
 */
class FrequencyMapping(
    val minFrequencyHz: Float = DEFAULT_MIN_FREQUENCY_HZ,
    val maxFrequencyHz: Float = DEFAULT_MAX_FREQUENCY_HZ,
    val minColorfulness: Float = DEFAULT_MIN_COLORFULNESS,
    val maxColorfulness: Float = DEFAULT_MAX_COLORFULNESS,
) {
    init {
        require(minFrequencyHz > 0f && maxFrequencyHz > minFrequencyHz) {
            "invalid frequency range: $minFrequencyHz..$maxFrequencyHz"
        }
        require(maxColorfulness > minColorfulness) {
            "invalid colorfulness range: $minColorfulness..$maxColorfulness"
        }
    }

    private val ratio = (maxFrequencyHz / minFrequencyHz).toDouble()

    /** [colorfulness] に対応する周波数(Hz)。レンジ外はクランプする。 */
    fun toFrequency(colorfulness: Float): Float {
        val t = ((colorfulness - minColorfulness) / (maxColorfulness - minColorfulness))
            .coerceIn(0f, 1f)
        return (minFrequencyHz * ratio.pow(t.toDouble())).toFloat()
    }

    companion object {
        const val DEFAULT_MIN_FREQUENCY_HZ = 110f
        const val DEFAULT_MAX_FREQUENCY_HZ = 1760f

        // 実測に基づく初期値。ステップ5で調整する。
        const val DEFAULT_MIN_COLORFULNESS = 5f
        const val DEFAULT_MAX_COLORFULNESS = 80f
    }
}

/**
 * フレーム単位の指数移動平均。手ぶれや AE の揺れで指標がバタつくのを均す。
 * ポルタメントが音の連続性を担うのに対し、こちらは指標そのものの平滑化。
 */
class ExponentialSmoother(private val alpha: Float = DEFAULT_ALPHA) {
    init {
        require(alpha > 0f && alpha <= 1f) { "alpha must be in (0, 1] but was $alpha" }
    }

    private var current: Float? = null

    /** 平滑後の値を返す。最初のサンプルはそのまま通す。 */
    fun update(sample: Float): Float {
        val next = current?.let { it + (sample - it) * alpha } ?: sample
        current = next
        return next
    }

    fun reset() {
        current = null
    }

    companion object {
        /** 30fps でおよそ 150ms の時定数。 */
        const val DEFAULT_ALPHA = 0.2f
    }
}
