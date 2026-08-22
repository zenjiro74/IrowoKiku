package com.example.zenjiro74.irowokiku.mapping

import kotlin.math.log2
import kotlin.math.roundToInt

private val NOTE_NAMES = arrayOf(
    "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B",
)

private const val MIDI_A4 = 69
private const val A4_HZ = 440.0
private const val SEMITONES_PER_OCTAVE = 12

/**
 * 周波数を最も近い音名 (例: "A2", "C#5") に丸める。
 * 生の Hz より音名のほうが「今どのくらいの高さか」を掴みやすいので HUD に併記する。
 */
fun noteNameOf(frequencyHz: Float): String {
    if (frequencyHz <= 0f) return "-"

    val midi = MIDI_A4 + (SEMITONES_PER_OCTAVE * log2(frequencyHz / A4_HZ)).roundToInt()
    val name = NOTE_NAMES[midi.mod(SEMITONES_PER_OCTAVE)]
    // MIDI 60 が C4。オクターブ番号は C を境に上がる。
    val octave = midi.floorDiv(SEMITONES_PER_OCTAVE) - 1
    return "$name$octave"
}
