package com.reasontouch.feature.chords

import com.reasontouch.core.data.NoteEvent
import com.reasontouch.core.midi.StepState
import java.util.UUID

/**
 * Shared, pure strum-to-NoteEvent generation, extracted from
 * ChordViewModel.sendToPianoRoll() (Roadmap: Strum Persistence & Continuation
 * Inheritance, Phase 3). Used by sendToPianoRoll(), sendProgressionToPianoRoll(),
 * and addPhrase(), so all three produce identical note timing from the same
 * chord + pattern + speed inputs.
 *
 * Generates the notes for a SINGLE bar. Callers loop over their own bars,
 * computing beatStart per bar, and accumulate the results.
 *
 * strumSpeedSeconds is the per-string delay in seconds (e.g. 0.02 = 20ms,
 * per STRUM_SPEED_PRESETS). This function converts that seconds value into
 * beats internally using bpm -- fixing the unit mismatch confirmed in
 * Sprint 0 Decision 2 (Option A): the previous code added the raw seconds
 * value directly onto a beats quantity with no tempo conversion.
 */
fun generateStrumNotes(
    midiNotes: List<Int>,
    pattern: StepPattern?,
    useStrum: Boolean,
    strumSpeedSeconds: Double,
    bpm: Int,
    beatStart: Float,
    beatsPerBar: Float,
    trackId: String,
    velocity: Int = 80
): List<NoteEvent> {
    val notes = mutableListOf<NoteEvent>()

    val activeSteps = if (useStrum && pattern != null) {
        pattern.steps.mapIndexedNotNull { index, state ->
            if (state != StepState.OFF) Pair(index, state) else null
        }
    } else {
        emptyList()
    }

    if (activeSteps.isEmpty()) {
        // Block mode: all notes together, full bar duration
        midiNotes.forEach { midiNote ->
            notes.add(
                NoteEvent(
                    id = UUID.randomUUID().toString(),
                    trackId = trackId,
                    pitch = 108 - midiNote,
                    beat = beatStart,
                    duration = beatsPerBar,
                    velocity = velocity
                )
            )
        }
    } else {
        // Strum mode: spread notes across active pattern steps
        val beatsPerStep = beatsPerBar / 16f
        val beatsPerSecond = bpm / 60f

        activeSteps.forEach { (stepIndex, stepState) ->
            val stepBeat = beatStart + (stepIndex * beatsPerStep)
            val orderedNotes = if (stepState == StepState.DOWN) midiNotes else midiNotes.reversed()

            orderedNotes.forEachIndexed { noteIndex, midiNote ->
                // seconds -> beats conversion (the unit-mismatch fix)
                val offsetBeats = (noteIndex * strumSpeedSeconds).toFloat() * beatsPerSecond

                notes.add(
                    NoteEvent(
                        id = UUID.randomUUID().toString(),
                        trackId = trackId,
                        pitch = 108 - midiNote,
                        beat = stepBeat + offsetBeats,
                        duration = (beatsPerStep * 0.95f).coerceAtLeast(0.0625f),
                        velocity = velocity
                    )
                )
            }
        }
    }

    return notes
}