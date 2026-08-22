package com.example.zenjiro74.irowokiku.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencyMappingTest {

    private val mapping = FrequencyMapping(
        minColorfulness = 0f,
        maxColorfulness = 100f,
    )

    @Test
    fun `レンジの下端と上端がそれぞれ 110Hz と 1760Hz になる`() {
        assertEquals(110.0, mapping.toFrequency(0f).toDouble(), 0.01)
        assertEquals(1760.0, mapping.toFrequency(100f).toDouble(), 0.01)
    }

    @Test
    fun `レンジ外はクランプされる`() {
        assertEquals(110.0, mapping.toFrequency(-50f).toDouble(), 0.01)
        assertEquals(110.0, mapping.toFrequency(Float.NEGATIVE_INFINITY).toDouble(), 0.01)
        assertEquals(1760.0, mapping.toFrequency(1000f).toDouble(), 0.01)
        assertEquals(1760.0, mapping.toFrequency(Float.POSITIVE_INFINITY).toDouble(), 0.01)
    }

    @Test
    fun `中点は対数マッピングなので幾何平均の 440Hz になる`() {
        // 110 * 2^(4 * 0.5) = 440。線形マッピングなら 935Hz になってしまう。
        assertEquals(440.0, mapping.toFrequency(50f).toDouble(), 0.01)
    }

    @Test
    fun `colorfulness が増えると周波数は単調に増える`() {
        var previous = mapping.toFrequency(0f)
        for (value in 1..100) {
            val current = mapping.toFrequency(value.toFloat())
            assertTrue("$value で単調増加が崩れた: $previous -> $current", current > previous)
            previous = current
        }
    }

    @Test
    fun `既定レンジは Hasler-Susstrunk の官能スケールに沿う`() {
        val default = FrequencyMapping()

        // "slightly colorful" 以下は最低音、"highly colorful" 以上は最高音。
        assertEquals(110.0, default.toFrequency(15f).toDouble(), 0.01)
        assertEquals(1760.0, default.toFrequency(82f).toDouble(), 0.01)

        // "moderately"(33) と "quite"(59) はその間に単調に収まる。
        val moderately = default.toFrequency(33f)
        val quite = default.toFrequency(59f)
        assertTrue("$moderately < $quite が成り立たない", moderately < quite)
        assertTrue(moderately > 110f && quite < 1760f)
    }

    @Test
    fun `等間隔の colorfulness は等しい音程差になる`() {
        // 対数マッピングなので、指標が 25 増えるごとに周波数比は一定 (1 オクターブ)。
        val ratios = listOf(0f, 25f, 50f, 75f, 100f)
            .map { mapping.toFrequency(it) }
            .zipWithNext { low, high -> high / low }

        ratios.forEach { assertEquals(2.0, it.toDouble(), 0.001) }
    }
}

class ExponentialSmootherTest {

    @Test
    fun `最初のサンプルはそのまま通す`() {
        val smoother = ExponentialSmoother(alpha = 0.2f)
        assertEquals(42.0, smoother.update(42f).toDouble(), 1e-6)
    }

    @Test
    fun `ステップ入力に徐々に追従する`() {
        val smoother = ExponentialSmoother(alpha = 0.2f)
        smoother.update(0f)

        val first = smoother.update(100f)
        assertEquals(20.0, first.toDouble(), 1e-4)

        repeat(50) { smoother.update(100f) }
        assertEquals(100.0, smoother.update(100f).toDouble(), 0.1)
    }

    @Test
    fun `reset すると次のサンプルがそのまま通る`() {
        val smoother = ExponentialSmoother(alpha = 0.2f)
        repeat(10) { smoother.update(0f) }

        smoother.reset()

        assertEquals(100.0, smoother.update(100f).toDouble(), 1e-6)
    }
}
