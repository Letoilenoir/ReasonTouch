package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * ContrastTargeting: Interprets a ProgressionAnalysis into a trajectory
 * intended to change character rather than continue or resolve.
 *
 * Contrast fires (via PairingEngine) when the preceding phrase is already
 * stable/resolved -- so the goal here is deliberate departure, not more
 * of the same harmonic territory.
 */
object ContrastTargeting {

    data class ContrastPlan(
        val trajectory: List<HarmonicFunction>,
        val borrowedChordPosition: Int?
    )

    fun derivePlan(analysis: ProgressionAnalysis, phraseLength: Int): ContrastPlan {
        if (phraseLength <= 0) return ContrastPlan(emptyList(), null)

        val trajectory = when (analysis.endingFunction) {
            HarmonicFunction.TONIC -> fitLength(
                listOf(
                    HarmonicFunction.DOMINANT,
                    HarmonicFunction.PREDOMINANT,
                    HarmonicFunction.DOMINANT,
                    HarmonicFunction.TONIC
                ),
                phraseLength
            )
            HarmonicFunction.PREDOMINANT -> fitLength(
                listOf(
                    HarmonicFunction.DOMINANT,
                    HarmonicFunction.TONIC,
                    HarmonicFunction.DOMINANT,
                    HarmonicFunction.PREDOMINANT
                ),
                phraseLength
            )
            HarmonicFunction.DOMINANT -> fitLength(
                listOf(
                    HarmonicFunction.PREDOMINANT,
                    HarmonicFunction.TONIC,
                    HarmonicFunction.PREDOMINANT,
                    HarmonicFunction.DOMINANT
                ),
                phraseLength
            )
        }

        val borrowedChordPosition = 0

        return ContrastPlan(trajectory, borrowedChordPosition)
    }

    private fun fitLength(base: List<HarmonicFunction>, length: Int): List<HarmonicFunction> {
        if (base.size == length) return base
        if (base.size > length) return base.take(length)
        val padding = List(length - base.size) { base.last() }
        return base + padding
    }
}