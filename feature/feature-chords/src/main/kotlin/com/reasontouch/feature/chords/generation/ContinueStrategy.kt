package com.reasontouch.feature.chords.generation

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.ProgressionGenerationRequest

/**
 * Generates candidate continuations that preserve the
 * musical character of the source progression.
 *
 * This first implementation is intentionally conservative.
 * It favours continuity over surprise.
 */
object ContinueStrategy {

    fun generate(
        request: ProgressionGenerationRequest
    ): List<GeneratedProgression> {

        val source = request.sourceProgression

        if (source.isEmpty()) {
            return emptyList()
        }

        return listOf(

            GeneratedProgression(
                chords = source,
                confidence = 0.85f,
                explanation = "Continue the established harmonic pattern."
            )

        )
    }
}