package com.example.zenjiro74.irowokiku.mapping

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteNameTest {

    @Test
    fun `レンジの両端が A2 と A6 になる`() {
        assertEquals("A2", noteNameOf(110f))
        assertEquals("A6", noteNameOf(1760f))
    }

    @Test
    fun `基準音と主要なオクターブを正しく名付ける`() {
        assertEquals("A4", noteNameOf(440f))
        assertEquals("A3", noteNameOf(220f))
        assertEquals("A5", noteNameOf(880f))
        assertEquals("C4", noteNameOf(261.63f))
        assertEquals("C5", noteNameOf(523.25f))
    }

    @Test
    fun `半音のずれは最も近い音名に丸める`() {
        // A4(440) と A#4(466.16) の間。どちら寄りかで名前が変わる。
        assertEquals("A4", noteNameOf(445f))
        assertEquals("A#4", noteNameOf(465f))
    }

    @Test
    fun `0以下の周波数はプレースホルダを返す`() {
        assertEquals("-", noteNameOf(0f))
        assertEquals("-", noteNameOf(-100f))
    }
}

class NormalizedPositionTest {

    private val mapping = FrequencyMapping()

    @Test
    fun `レンジの下端と上端が 0 と 1 になる`() {
        assertEquals(0.0, mapping.normalizedPosition(110f).toDouble(), 1e-5)
        assertEquals(1.0, mapping.normalizedPosition(1760f).toDouble(), 1e-5)
    }

    @Test
    fun `対数スケールなので中央は 440Hz`() {
        assertEquals(0.5, mapping.normalizedPosition(440f).toDouble(), 1e-5)
    }

    @Test
    fun `レンジ外はクランプされる`() {
        assertEquals(0.0, mapping.normalizedPosition(50f).toDouble(), 1e-5)
        assertEquals(0.0, mapping.normalizedPosition(0f).toDouble(), 1e-5)
        assertEquals(1.0, mapping.normalizedPosition(5000f).toDouble(), 1e-5)
    }
}
