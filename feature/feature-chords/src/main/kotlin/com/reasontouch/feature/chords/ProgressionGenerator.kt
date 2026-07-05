package com.reasontouch.feature.chords

import com.reasontouch.feature.chords.CompositionIntent
import com.reasontouch.feature.chords.generation.strategies.ContinueStrategy

/**
 * Delegates chord progression generation to strategy-specific implementations.
 *
 * Takes a ProgressionGenerationRequest (with intent, key, source progression)
 * and returns candidate GeneratedProgressions (purely harmonic suggestions).
 *
 * Each intent (CONTINUE, RESOLVE, LIFT, etc.) has its own strategy.
 */
object ProgressionGenerator {

    fun generate(
        request: ProgressionGenerationRequest
    ): List<GeneratedProgression> {

        return when (request.primaryIntent) {

            CompositionIntent.CONTINUE ->
                ContinueStrategy.generate(request)

            CompositionIntent.RESOLVE ->
                emptyList()  // TODO: ResolveStrategy

            CompositionIntent.LIFT ->
                emptyList()  // TODO: LiftStrategy

            CompositionIntent.CONTRAST ->
                emptyList()  // TODO: ContrastStrategy

            else ->
                emptyList()
        }
    }
}