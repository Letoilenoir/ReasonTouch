package com.reasontouch.feature.chords.generation

import com.reasontouch.feature.chords.HarmonicFunction

/**
 * Shared harmonic movement rules.
 *
 * These rules describe common functional movement.
 *
 * They are intentionally simple initially and will
 * become richer over time.
 */
object ProgressionGrammar {

    private val transitions = mapOf(

        HarmonicFunction.TONIC to listOf(
            HarmonicFunction.PREDOMINANT,
            HarmonicFunction.DOMINANT
        ),

        HarmonicFunction.PREDOMINANT to listOf(
            HarmonicFunction.DOMINANT
        ),

        HarmonicFunction.DOMINANT to listOf(
            HarmonicFunction.TONIC
        )
    )

    fun nextFunctions(
        current: HarmonicFunction
    ): List<HarmonicFunction> =
        transitions[current].orEmpty()
}