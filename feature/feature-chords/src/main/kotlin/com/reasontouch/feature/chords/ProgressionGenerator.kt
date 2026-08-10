package com.reasontouch.feature.chords
import com.reasontouch.feature.chords.CompositionIntent
import com.reasontouch.feature.chords.generation.strategies.ContinueStrategy
import com.reasontouch.feature.chords.generation.strategies.ContrastStrategy
import com.reasontouch.feature.chords.generation.strategies.ExpandStrategy
import com.reasontouch.feature.chords.generation.strategies.LiftStrategy
import com.reasontouch.feature.chords.generation.strategies.ResolveStrategy

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
                ResolveStrategy.generate(request)

            CompositionIntent.LIFT ->
                LiftStrategy.generate(request)

            CompositionIntent.CONTRAST ->
                ContrastStrategy.generate(request)
            CompositionIntent.EXPAND ->
                ExpandStrategy.generate(request)

            else ->
                emptyList()
        }
    }
}