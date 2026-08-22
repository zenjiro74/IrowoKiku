package com.example.zenjiro74.irowokiku.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TWO_PI = 2.0 * PI

/**
 * 位相連続なサイン波を生成する純 DSP。Android API に依存しないので JVM 単体テストできる。
 *
 * 周波数はサンプル単位の一次遅れ (ポルタメント) で目標値に追従させ、位相は [fill] を跨いで
 * 保持する。周波数が飛んでも波形自体は連続なので、プチッというクリックが出ない。
 *
 * [targetFrequencyHz] と [targetAmplitude] は解析スレッドから、[fill] はオーディオスレッドから
 * 呼ばれる想定。値の受け渡しは volatile な単一フィールドのみでロックを持たない。
 */
class SineOscillator(
    private val sampleRate: Int,
    portamentoSeconds: Double = DEFAULT_PORTAMENTO_SECONDS,
    amplitudeRampSeconds: Double = DEFAULT_AMPLITUDE_RAMP_SECONDS,
    initialFrequencyHz: Float = 440f,
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive but was $sampleRate" }
    }

    /** 一次遅れの係数。1 サンプル進むごとに目標との差をこの割合だけ詰める。 */
    private val portamentoCoeff: Double =
        1.0 - exp(-1.0 / (max(portamentoSeconds, MIN_TIME_CONSTANT) * sampleRate))

    /** 振幅は線形ランプ。目標に厳密に到達させたいので一次遅れにはしない。 */
    private val amplitudeStep: Double =
        1.0 / (max(amplitudeRampSeconds, MIN_TIME_CONSTANT) * sampleRate)

    @Volatile
    var targetFrequencyHz: Float = initialFrequencyHz

    @Volatile
    var targetAmplitude: Float = 0f

    private var currentFrequencyHz: Double = initialFrequencyHz.toDouble()
    private var currentAmplitude: Double = 0.0
    private var phase: Double = 0.0

    /** フェードアウトが完了して無音になったか。再生ループの終了判定に使う。 */
    val isSilent: Boolean
        get() = currentAmplitude <= 0.0 && targetAmplitude <= 0f

    /** 現在鳴っている周波数。ポルタメント中は目標値と一致しない。 */
    val currentFrequency: Float
        get() = currentFrequencyHz.toFloat()

    /** ポルタメントを挟まずに [hz] へ飛ばす。再生開始時の初期化用。 */
    fun snapFrequency(hz: Float) {
        targetFrequencyHz = hz
        currentFrequencyHz = hz.toDouble()
    }

    /** [out] の先頭 [count] サンプルを埋める。位相は呼び出しを跨いで継続する。 */
    fun fill(out: FloatArray, count: Int = out.size) {
        val frequencyTarget = targetFrequencyHz.toDouble()
        val amplitudeTarget = targetAmplitude.toDouble()
        for (i in 0 until count) {
            currentFrequencyHz += (frequencyTarget - currentFrequencyHz) * portamentoCoeff
            currentAmplitude = approach(currentAmplitude, amplitudeTarget, amplitudeStep)
            phase += TWO_PI * currentFrequencyHz / sampleRate
            if (phase >= TWO_PI) phase -= TWO_PI
            out[i] = (sin(phase) * currentAmplitude).toFloat()
        }
    }

    private fun approach(current: Double, target: Double, step: Double): Double = when {
        current < target -> min(current + step, target)
        current > target -> max(current - step, target)
        else -> target
    }

    companion object {
        const val DEFAULT_PORTAMENTO_SECONDS = 0.06
        const val DEFAULT_AMPLITUDE_RAMP_SECONDS = 0.02
        private const val MIN_TIME_CONSTANT = 1e-6
    }
}
