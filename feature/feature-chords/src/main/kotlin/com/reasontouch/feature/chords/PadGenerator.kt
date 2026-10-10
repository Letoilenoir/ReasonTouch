package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.NoteEvent
import java.util.UUID

object PadGenerator {

    private const val PAD_MIN_MIDI = 48 // C3
    private const val PAD_MAX_MIDI = 72 // C5

    fun generate(
        chords: List<ChordEvent>,
        targetTrackId: String,
        beatsPerBar: Float = 4f,
        appendOffset: Float = 0f
    ): List<NoteEvent> {
        if (chords.isEmpty()) return emptyList()

        val notes = mutableListOf<NoteEvent>()

        chords.forEachIndexed { barIdx, chord ->
            val barStart = appendOffset + (barIdx * beatsPerBar)
            val chordTones = chord.midiNotes
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .map { padRegister(it) }
                .distinct()
                .sorted()

            if (chordTones.isEmpty()) return@forEachIndexed

            chordTones.forEach { tone ->
                notes.add(
                    NoteEvent(
                        id = UUID.randomUUID().toString(),
                        trackId = targetTrackId,
                        pitch = (108 - tone).coerceIn(0, 87),
                        beat = barStart,
                        duration = beatsPerBar * 0.98f,
                        velocity = 70
                    )
                )
            }
        }
        return notes
    }

    private fun padRegister(midiNote: Int): Int {
        var n = midiNote % 12
        n += 48 // C3 range
        while (n < PAD_MIN_MIDI) n += 12
        while (n > PAD_MAX_MIDI) n -= 12
        return n.coerceIn(PAD_MIN_MIDI, PAD_MAX_MIDI)
    }
}
