package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.feature.chords.CompositionIntent
import com.reasontouch.feature.chords.generation.strategies.ContinueStrategy

/**
 * Creates candidate chord progressions that fulfil a musical intention.
 *
 * This is currently a scaffold.
 * Musical generation will be implemented incrementally.
 */
object ProgressionGenerator {

    fun generate(
        request: ProgressionGenerationRequest
    ): List<GeneratedProgression> {

        return when (request.primaryIntent) {

            CompositionIntent.CONTINUE ->
                ContinueStrategy.generate(request)

            else ->
                emptyList()
        }
    }

    private fun generateContinue(
        request: ProgressionGenerationRequest
    ): List<GeneratedProgression> {

        // First implementation:
        // simply repeat the existing progression.

        return listOf(

            GeneratedProgression(

                chords = request.sourceProgression,

                confidence = 0.80f,

                explanation =
                    "Repeats the established harmonic pattern to reinforce continuity."

            )

        )

    }
}
