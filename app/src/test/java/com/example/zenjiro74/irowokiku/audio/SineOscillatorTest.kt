package com.example.zenjiro74.irowokiku.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

private const val SAMPLE_RATE = 44_100

class SineOscillatorTest {

    /** 振幅ランプを完了させ、定常状態の波形だけを見られるようにする。 */
    private fun SineOscillator.warmUp() {
        targetAmplitude = 1f
        fill(FloatArray(SAMPLE_RATE / 10))
    }

    private fun countPositiveZeroCrossings(samples: FloatArray): Int {
        var count = 0
        for (i in 1 until samples.size) {
            if (samples[i - 1] <= 0f && samples[i] > 0f) count++
        }
        return count
    }

    private fun maxAdjacentDelta(samples: FloatArray): Float {
        var maxDelta = 0f
        for (i in 1 until samples.size) {
            maxDelta = maxOf(maxDelta, abs(samples[i] - samples[i - 1]))
        }
        return maxDelta
    }

    @Test
    fun `固定周波数では1秒あたりのゼロ交差数が周波数と一致する`() {
        val oscillator = SineOscillator(SAMPLE_RATE)
        oscillator.snapFrequency(440f)
        oscillator.warmUp()

        val samples = FloatArray(SAMPLE_RATE)
        oscillator.fill(samples)

        assertEquals(440.0, countPositiveZeroCrossings(samples).toDouble(), 1.0)
    }

    @Test
    fun `fill をまたいでも位相が連続する`() {
        val oscillator = SineOscillator(SAMPLE_RATE)
        oscillator.snapFrequency(440f)
        oscillator.warmUp()

        val first = FloatArray(1024)
        val second = FloatArray(1024)
        oscillator.fill(first)
        oscillator.fill(second)

        // 1 サンプル分の増分の上限は amplitude * 2pi * f / sr。
        val perSampleBound = (2 * PI * 440 / SAMPLE_RATE).toFloat()
        val boundaryDelta = abs(second[0] - first[first.lastIndex])
        assertTrue(
            "境界の段差 $boundaryDelta が1サンプル分の増分 $perSampleBound を超えている",
            boundaryDelta <= perSampleBound * 1.1f,
        )
    }

    @Test
    fun `周波数を急変させても波形が不連続にならない`() {
        val oscillator = SineOscillator(SAMPLE_RATE)
        oscillator.snapFrequency(110f)
        oscillator.warmUp()

        oscillator.targetFrequencyHz = 1760f
        val samples = FloatArray(SAMPLE_RATE / 2)
        oscillator.fill(samples)

        // 位相不連続が起きると最大 2.0 の段差が出る。上限は最高周波数での1サンプル増分。
        val bound = (2 * PI * 1760 / SAMPLE_RATE).toFloat() * 1.1f
        val actual = maxAdjacentDelta(samples)
        assertTrue("隣接サンプル差 $actual が上限 $bound を超えている", actual <= bound)
    }

    @Test
    fun `ポルタメントは目標周波数へ徐々に近づく`() {
        val portamentoSeconds = 0.06
        val oscillator = SineOscillator(SAMPLE_RATE, portamentoSeconds = portamentoSeconds)
        oscillator.snapFrequency(110f)
        oscillator.targetFrequencyHz = 1760f

        // 1 サンプルでは目標にほとんど動かない。
        oscillator.fill(FloatArray(1))
        assertTrue(
            "1サンプルで ${oscillator.currentFrequency}Hz まで飛んでいる",
            oscillator.currentFrequency < 115f,
        )

        // 時定数の 5 倍も進めばほぼ到達している。
        oscillator.fill(FloatArray((portamentoSeconds * 5 * SAMPLE_RATE).toInt()))
        assertEquals(1760.0, oscillator.currentFrequency.toDouble(), 1760.0 * 0.01)
    }

    @Test
    fun `振幅ランプで無音から立ち上がり無音に戻る`() {
        val rampSeconds = 0.02
        val oscillator = SineOscillator(SAMPLE_RATE, amplitudeRampSeconds = rampSeconds)
        oscillator.snapFrequency(440f)

        // 立ち上がり中の先頭は必ず 0 付近から始まる。
        val rising = FloatArray(16)
        oscillator.targetAmplitude = 1f
        oscillator.fill(rising)
        assertTrue("開始直後の振幅が大きすぎる: ${rising[0]}", abs(rising[0]) < 0.05f)
        assertTrue(!oscillator.isSilent)

        oscillator.fill(FloatArray(SAMPLE_RATE / 10))

        // フェードアウトはランプ時間で必ず 0 に到達する。
        oscillator.targetAmplitude = 0f
        oscillator.fill(FloatArray((rampSeconds * SAMPLE_RATE).toInt() + 1))
        assertTrue("フェードアウトが完了していない", oscillator.isSilent)

        val silence = FloatArray(256)
        oscillator.fill(silence)
        assertEquals(0f, silence.max(), 0f)
        assertEquals(0f, silence.min(), 0f)
    }
}
