package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.CadenceType
import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * ResolveTargeting: Interprets a ProgressionAnalysis into a harmonic
 * trajectory that guarantees a cadential resolution onto the tonic.
 *
 * Unlike ContinueTargeting (which biases toward a plausible next move
 * without guaranteeing where it lands), ResolveTargeting's contract is
 * stronger: the final entry in the returned trajectory is always
 * HarmonicFunction.TONIC, and ResolveStrategy enforces this at the
 * chord-selection step too. This is what makes "Resolve" meaningfully
 * different from a strongly-biased Continue.
 */
object ResolveTargeting {

    /**
     * Derives a target sequence of harmonic functions that resolves
     * the preceding phrase, based on how it ended.
     *
     * @param analysis the completed analysis of the preceding progression
     * @param phraseLength number of chords to plan for (typically 4)
     */
    fun deriveTrajectory(
        analysis: ProgressionAnalysis,
        phraseLength: Int
    ): List<HarmonicFunction> {
        if (phraseLength <= 0) return emptyList()

        return when {
            // Half cadence — already sitting on V. Resolve immediately,
            // then reinforce with a plagal "amen" tag for a firm landing.
            analysis.endsOnDominant() -> cadentialConfirmationTrajectory(phraseLength)

            // Deceptive cadence — V resolved to vi instead of I last time.
            // Acknowledge the tonic-function chord we're actually on, then
            // run a full predominant-dominant-tonic cadence to land where
            // the ear was originally led to expect.
            analysis.cadenceType == CadenceType.DECEPTIVE -> deceptiveRecoveryTrajectory(phraseLength)

            // Phrase already closed (authentic/plagal, high stability) —
            // briefly reopen harmonic motion before resolving again, so
            // this reads as a deliberate new cadence rather than simply
            // repeating the same closed phrase.
            analysis.isClosed() -> reopenThenResolveTrajectory(phraseLength)

            // Open/interrupted/default — standard cadential run home,
            // shaped by wherever the phrase actually left off.
            else -> defaultTrajectory(analysis.endingFunction, phraseLength)
        }
    }

    private fun cadentialConfirmationTrajectory(length: Int): List<HarmonicFunction> =
        fitLength(
            listOf(
                HarmonicFunction.DOMINANT,
                HarmonicFunction.TONIC,
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.TONIC
            ),
            length
        )

    private fun deceptiveRecoveryTrajectory(length: Int): List<HarmonicFunction> =
        fitLength(
            listOf(
                HarmonicFunction.TONIC,
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.DOMINANT,
                HarmonicFunction.TONIC
            ),
            length
        )

    private fun reopenThenResolveTrajectory(length: Int): List<HarmonicFunction> =
        fitLength(
            listOf(
                HarmonicFunction.DOMINANT,
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.DOMINANT,
                HarmonicFunction.TONIC
            ),
            length
        )

    private fun defaultTrajectory(
        endingFunction: HarmonicFunction,
        length: Int
    ): List<HarmonicFunction> {
        val base = when (endingFunction) {
            HarmonicFunction.TONIC -> listOf(
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.DOMINANT,
                HarmonicFunction.TONIC,
                HarmonicFunction.TONIC
            )
            HarmonicFunction.PREDOMINANT -> listOf(
                HarmonicFunction.DOMINANT,
                HarmonicFunction.TONIC,
                HarmonicFunction.DOMINANT,
                HarmonicFunction.TONIC
            )
            HarmonicFunction.DOMINANT -> listOf(
                HarmonicFunction.TONIC,
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.DOMINANT,
                HarmonicFunction.TONIC
            )
        }
        return fitLength(base, length)
    }

    /**
     * Pads or trims a base trajectory to the requested phrase length.
     *
     * Unlike ContinueTargeting's fitLength, truncation here uses
     * takeLast() rather than take() — Resolve's bases are written so
     * the LAST entry is always TONIC, and we need to preserve that
     * cadential tail even when trimming to a shorter length, not just
     * keep the front of the sequence.
     */
    private fun fitLength(
        base: List<HarmonicFunction>,
        length: Int
    ): List<HarmonicFunction> {
        if (base.size == length) return base
        if (base.size > length) return base.takeLast(length)
        val padding = List(length - base.size) { base.last() }
        return base + padding
    }
}