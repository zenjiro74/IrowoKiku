package com.example.zenjiro74.irowokiku.mapping

import kotlin.math.ln
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

    /** [frequencyHz] がレンジのどこにあるか (0..1)。HUD のバー表示用。 */
    fun normalizedPosition(frequencyHz: Float): Float {
        if (frequencyHz <= 0f) return 0f
        return (ln(frequencyHz / minFrequencyHz.toDouble()) / ln(ratio))
            .toFloat()
            .coerceIn(0f, 1f)
    }

    companion object {
        const val DEFAULT_MIN_FREQUENCY_HZ = 110f
        const val DEFAULT_MAX_FREQUENCY_HZ = 1760f

        // Hasler–Süsstrunk 論文の官能評価スケールに合わせる。
        //   0 not colorful / 15 slightly / 33 moderately / 45 average
        //   59 quite / 82 highly / 109 extremely
        // 実際にカメラを向けるのはほとんど "slightly"〜"highly" の帯なので、
        // その範囲を 4 オクターブいっぱいに使う。
        const val DEFAULT_MIN_COLORFULNESS = 15f
        const val DEFAULT_MAX_COLORFULNESS = 82f
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
