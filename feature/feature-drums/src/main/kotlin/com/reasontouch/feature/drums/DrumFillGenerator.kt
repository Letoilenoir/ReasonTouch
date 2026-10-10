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
                1 to 0.0f, 3 to 0.5f, 1 to 1.0f, 3 to 1.5f,
                1 to 2.0f, 3 to 2.5f, 1 to 3.0f, 3 to 3.5f,
                5 to 0.0f // Crash
            )
        ),
        "16th-note" to DrumFillPattern(
            "16th-Note Roll",
            "Busy 16th-note snare roll ending in crash",
            FillDuration.HALF_BAR,
            listOf(
                1 to 2.0f, 1 to 2.25f, 1 to 2.5f, 1 to 2.75f,
                1 to 3.0f, 1 to 3.25f, 1 to 3.5f, 1 to 3.75f,
                5 to 0.0f // Crash
            )
        ),
        "quarter-note" to DrumFillPattern(
            "Quarter-Note Fill",
            "Simple, spacious hits on each beat",
            FillDuration.FULL_BAR,
            listOf(
                1 to 0.0f, 1 to 1.0f, 1 to 2.0f, 1 to 3.0f,
                5 to 0.0f
            )
        ),
        "triplet" to DrumFillPattern(
            "Triplet Fill",
            "Swinging triplet feel across the bar",
            FillDuration.FULL_BAR,
            listOf(
                1 to 0.0f, 1 to 0.33f, 1 to 0.66f,
                1 to 1.0f, 1 to 1.33f, 1 to 1.66f,
                1 to 2.0f, 1 to 2.33f, 1 to 2.66f,
                1 to 3.0f, 1 to 3.33f, 1 to 3.66f,
                5 to 0.0f
            )
        ),
        "single-stroke" to DrumFillPattern(
            "Single Stroke Roll",
            "Rapid alternating single hits",
            FillDuration.HALF_BAR,
            listOf(
                1 to 2.0f, 1 to 2.2f, 1 to 2.4f, 1 to 2.6f, 1 to 2.8f,
                1 to 3.0f, 1 to 3.2f, 1 to 3.4f, 1 to 3.6f, 1 to 3.8f,
                5 to 0.0f
            )
        ),
        "double-stroke" to DrumFillPattern(
            "Double Stroke Roll",
            "Classic double-hit (RR-DD) pattern",
            FillDuration.HALF_BAR,
            listOf(
                1 to 2.0f, 1 to 2.15f, 1 to 2.5f, 1 to 2.65f,
                1 to 3.0f, 1 to 3.15f, 1 to 3.5f, 1 to 3.65f,
                5 to 0.0f
            )
        ),
        "paradiddle" to DrumFillPattern(
            "Paradiddle Fill",
            "RLRR LRLL sticking rhythm translated to drum accents",
            FillDuration.FULL_BAR,
            listOf(
                1 to 0.0f, 1 to 0.25f, 1 to 0.5f, 1 to 0.75f,
                1 to 1.0f, 1 to 1.25f, 1 to 1.5f, 1 to 1.75f,
                5 to 0.0f
            )
        ),
        "splat-boom" to DrumFillPattern(
            "Splat-Boom",
            "Flammed snare rush ending in a heavy kick and crash",
            FillDuration.HALF_BAR,
            listOf(
                1 to 2.0f, 1 to 2.25f, 1 to 2.5f, 1 to 2.75f,
                0 to 3.0f, 5 to 3.0f
            )
        ),
        "motown" to DrumFillPattern(
            "Motown Roll",
            "Classic 16th pickup into downbeat crash",
            FillDuration.HALF_BAR,
            listOf(
                1 to 2.0f, 1 to 2.33f, 1 to 2.66f, 1 to 3.0f, 1 to 3.33f, 1 to 3.66f,
                0 to 0.0f, 5 to 0.0f
            )
        ),
        "tom-descent" to DrumFillPattern(
            "Tom Descent",
            "Descending tom roll (High Tom -> Mid Tom -> Floor Tom -> Crash)",
            FillDuration.FULL_BAR,
            listOf(
                1 to 2.0f, 2 to 2.5f, 4 to 3.0f, 4 to 3.5f, 5 to 0.0f
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

        pattern.beats.forEach { (laneIdx, offset) ->
            val lane = DrumKit.lanes.getOrNull(laneIdx) ?: DrumKit.lanes[1]
            val beat = barStart + offset
            notes.add(
                NoteEvent(
                    id = UUID.randomUUID().toString(),
                    trackId = trackId,
                    pitch = lane.pitch,
                    beat = beat,
                    duration = 0.25f,
                    velocity = if (laneIdx == 5) 110 else 95
                )
            )
        }
        return notes
    }
}
