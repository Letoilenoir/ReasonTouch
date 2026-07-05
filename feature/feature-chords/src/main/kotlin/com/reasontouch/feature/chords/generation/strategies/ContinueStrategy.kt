package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.ProgressionGenerationRequest

/**
 * Implements the CONTINUE compositional intent.
 *
 * Initially this strategy simply preserves the user's seed progression.
 *
 * Future iterations will extend the progression using
 * harmonic grammar and voice-leading.
 */
object ContinueStrategy {

    fun generate(
        request: ProgressionGenerationRequest
    ): List<GeneratedProgression> {

        val explanation = when (request.sourceAnalysis.endingFunction) {

            com.reasontouch.feature.chords.HarmonicFunction.TONIC ->
                "The progression feels complete. Continuing reinforces the established musical idea."

            com.reasontouch.feature.chords.HarmonicFunction.PREDOMINANT ->
                "The progression remains open, allowing further harmonic development."

            com.reasontouch.feature.chords.HarmonicFunction.DOMINANT ->
                "The progression naturally invites continuation towards resolution."
        }

        return listOf(

            GeneratedProgression(

                chords = request.sourceProgression,

                confidence = 0.80f,

                explanation = explanation
            )
        )
    }
}