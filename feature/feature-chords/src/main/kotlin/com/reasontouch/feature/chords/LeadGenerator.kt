package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.NoteEvent
import java.util.UUID

object LeadGenerator {

    private const val LEAD_MIN_MIDI = 60 // C4
    private const val LEAD_MAX_MIDI = 84 // C6

    fun generate(
        chords: List<ChordEvent>,
        key: KeyCandidate?,
        pattern: LeadPattern,
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
                .map { leadRegister(it + 12) }
                .distinct()
                .sorted()

            if (chordTones.isEmpty()) return@forEachIndexed

            val isLastInBarOrPhrase = barIdx == chords.size - 1

            pattern.targetBeats.forEachIndexed { stepIdx, beatOffset ->
                if (beatOffset >= beatsPerBar) return@forEachIndexed

                val beat = barStart + beatOffset
                val dur = (beatsPerBar / pattern.targetBeats.size) * 0.85f

                val pitch = if (beatOffset % 2f == 0f || stepIdx == 0) {
                    chordTones[stepIdx % chordTones.size]
                } else {
                    val base = chordTones.first()
                    leadRegister(base + (stepIdx * 2))
                }

                val resolvedPitch = if (isLastInBarOrPhrase && beatOffset >= beatsPerBar - 1f) {
                    chordTones.first()
                } else {
                    pitch
                }

                notes.add(
                    NoteEvent(
                        id = UUID.randomUUID().toString(),
                        trackId = targetTrackId,
                        pitch = (108 - resolvedPitch).coerceIn(0, 87),
                        beat = beat,
                        duration = dur.coerceAtLeast(0.125f),
                        velocity = if (beatOffset == 0f) 100 else 85
                    )
                )
            }
        }
        return notes
    }

    private fun leadRegister(midiNote: Int): Int {
        var n = midiNote % 12
        n += 60 // C4 range
        while (n < LEAD_MIN_MIDI) n += 12
        while (n > LEAD_MAX_MIDI) n -= 12
        return n.coerceIn(LEAD_MIN_MIDI, LEAD_MAX_MIDI)
    }
}
