package com.reasontouch.core.midi

/**
 * Runtime model passed to MidiFileWriter.
 * Decoupled from the Room entities — assembled by the ViewModel before export or playback.
 */
data class MidiProgressionBar(
    val chordName: String,
    val notes: Array<Int?>,         // 6 MIDI notes, null = muted string
    val steps: List<StepState>,     // 16 steps
    val durationBeats: Double,
    val tempoBpm: Int,
    val strumSpeed: Double,         // beats per string offset (0 = instant)
    val gmProgram: Int              // GM program number 0-indexed
)
