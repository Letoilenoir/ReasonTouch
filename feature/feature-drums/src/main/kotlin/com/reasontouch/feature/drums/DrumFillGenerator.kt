package com.reasontouch.feature.drums

import com.reasontouch.core.data.NoteEvent
import java.util.UUID

object DrumFillPatterns {
    val FILLS = mapOf(
        "8th-note" to DrumFillPattern(
            "8th-Note Roll",
            "Steady 8th notes leading to a crash",
            FillDuration.FULL_BAR,
            listOf(
                38 to 0.0f, 42 to 0.5f, 38 to 1.0f, 42 to 1.5f,
                38 to 2.0f, 42 to 2.5f, 38 to 3.0f, 42 to 3.5f,
                49 to 0.0f // Crash (GM 49)
            )
        ),
        "16th-note" to DrumFillPattern(
            "16th-Note Roll",
            "Busy 16th-note snare roll ending in crash",
            FillDuration.HALF_BAR,
            listOf(
                38 to 2.0f, 38 to 2.25f, 38 to 2.5f, 38 to 2.75f,
                38 to 3.0f, 38 to 3.25f, 38 to 3.5f, 38 to 3.75f,
                49 to 0.0f // Crash
            )
        ),
        "quarter-note" to DrumFillPattern(
            "Quarter-Note Fill",
            "Simple, spacious hits on each beat",
            FillDuration.FULL_BAR,
            listOf(
                38 to 0.0f, 38 to 1.0f, 38 to 2.0f, 38 to 3.0f,
                49 to 0.0f
            )
        ),
        "triplet" to DrumFillPattern(
            "Triplet Fill",
            "Swinging triplet feel across the bar",
            FillDuration.FULL_BAR,
            listOf(
                38 to 0.0f, 38 to 0.33f, 38 to 0.66f,
                38 to 1.0f, 38 to 1.33f, 38 to 1.66f,
                38 to 2.0f, 38 to 2.33f, 38 to 2.66f,
                38 to 3.0f, 38 to 3.33f, 38 to 3.66f,
                49 to 0.0f
            )
        ),
        "single-stroke" to DrumFillPattern(
            "Single Stroke Roll",
            "Rapid alternating single hits",
            FillDuration.HALF_BAR,
            listOf(
                38 to 2.0f, 38 to 2.2f, 38 to 2.4f, 38 to 2.6f, 38 to 2.8f,
                38 to 3.0f, 38 to 3.2f, 38 to 3.4f, 38 to 3.6f, 38 to 3.8f,
                49 to 0.0f
            )
        ),
        "double-stroke" to DrumFillPattern(
            "Double Stroke Roll",
            "Classic double-hit pattern",
            FillDuration.HALF_BAR,
            listOf(
                38 to 2.0f, 38 to 2.15f, 38 to 2.5f, 38 to 2.65f,
                38 to 3.0f, 38 to 3.15f, 38 to 3.5f, 38 to 3.65f,
                49 to 0.0f
            )
        ),
        "paradiddle" to DrumFillPattern(
            "Paradiddle Fill",
            "Sticking rhythm translated to drum accents",
            FillDuration.FULL_BAR,
            listOf(
                38 to 0.0f, 39 to 0.25f, 38 to 0.5f, 38 to 0.75f,
                38 to 1.0f, 39 to 1.25f, 38 to 1.5f, 38 to 1.75f,
                49 to 0.0f
            )
        ),
        "splat-boom" to DrumFillPattern(
            "Splat-Boom",
            "Flammed snare rush ending in heavy kick and crash",
            FillDuration.HALF_BAR,
            listOf(
                38 to 2.0f, 38 to 2.25f, 38 to 2.5f, 38 to 2.75f,
                36 to 3.0f, 49 to 3.0f
            )
        ),
        "motown" to DrumFillPattern(
            "Motown Roll",
            "Classic 16th pickup into downbeat crash",
            FillDuration.HALF_BAR,
            listOf(
                38 to 2.0f, 38 to 2.33f, 38 to 2.66f, 38 to 3.0f, 38 to 3.33f, 38 to 3.66f,
                36 to 0.0f, 49 to 0.0f
            )
        ),
        "tom-descent" to DrumFillPattern(
            "Tom Descent",
            "Descending tom roll (High Tom -> Mid Tom -> Floor Tom -> Crash)",
            FillDuration.FULL_BAR,
            listOf(
                50 to 2.0f, 48 to 2.5f, 43 to 3.0f, 43 to 3.5f, 49 to 0.0f
            )
        )
    )
}

object DrumFillGenerator {
    fun generate(
        fillTypeName: String,
        trackId: String,
        barStart: Float,
        beatsPerBar: Float = 4f
    ): List<NoteEvent> {
        val pattern = DrumFillPatterns.FILLS[fillTypeName] ?: DrumFillPatterns.FILLS["8th-note"]!!
        val notes = mutableListOf<NoteEvent>()

        pattern.beats.forEach { (gmNote, offset) ->
            val beat = barStart + offset
            notes.add(
                NoteEvent(
                    id = UUID.randomUUID().toString(),
                    trackId = trackId,
                    pitch = 108 - gmNote, // Genuine GM coding for MIDI export
                    beat = beat,
                    duration = 0.25f,
                    velocity = if (gmNote == 49) 110 else 95
                )
            )
        }
        return notes
    }
}
