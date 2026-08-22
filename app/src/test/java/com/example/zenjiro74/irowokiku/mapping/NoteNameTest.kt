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
        assertEquals(0.0, mapping.normalizedPosition(mapping.minColorfulness).toDouble(), 1e-5)
        assertEquals(1.0, mapping.normalizedPosition(mapping.maxColorfulness).toDouble(), 1e-5)
    }

    @Test
    fun `レンジの中央が 0_5 になる`() {
        val middle = (mapping.minColorfulness + mapping.maxColorfulness) / 2f
        assertEquals(0.5, mapping.normalizedPosition(middle).toDouble(), 1e-5)
    }

    @Test
    fun `レンジ外はクランプされる`() {
        assertEquals(0.0, mapping.normalizedPosition(0f).toDouble(), 1e-5)
        assertEquals(0.0, mapping.normalizedPosition(-10f).toDouble(), 1e-5)
        assertEquals(1.0, mapping.normalizedPosition(1000f).toDouble(), 1e-5)
    }

    @Test
    fun `バー位置と周波数が同じ指標から決まる`() {
        // 位置 0.5 のとき周波数はレンジの幾何平均 (440Hz) になる。
        // 両者が同じ t を起点にしている限りこの関係は崩れない。
        val middle = (mapping.minColorfulness + mapping.maxColorfulness) / 2f
        assertEquals(0.5, mapping.normalizedPosition(middle).toDouble(), 1e-5)
        assertEquals(440.0, mapping.toFrequency(middle).toDouble(), 0.01)
    }
}
