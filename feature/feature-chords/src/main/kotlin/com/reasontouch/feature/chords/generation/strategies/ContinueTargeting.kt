package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * ContinueTargeting: Interprets a ProgressionAnalysis into a desired
 * harmonic trajectory for a continuation phrase.
 *
 * This answers "where should the music go?" — it does not select
 * chords itself. ContinueStrategy consumes the trajectory it returns
 * to realise actual chords via ChordSuggestionEngine.
 */
object ContinueTargeting {

    /**
     * Derives a target sequence of harmonic functions for the next
     * phrase, based on how the preceding phrase behaved as a whole.
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
            // Phrase ended unresolved on the dominant — the natural
            // continuation is to resolve toward the tonic.
            analysis.endsOnDominant() -> resolvingTrajectory(phraseLength)

            // Phrase already closed strongly (authentic/plagal cadence,
            // high stability) — reopen harmonic motion rather than
            // repeating a safe, static continuation.
            analysis.isClosed() -> reopeningTrajectory(phraseLength)

            // Phrase oscillated between two functions (e.g. V-I-V-I) —
            // deliberately break the oscillation instead of extending it.
            analysis.isOscillatingProgression() -> directionalTrajectory(phraseLength)

            // Phrase was already moving with clear direction — continue it.
            analysis.isDescendingProgression() -> directionalTrajectory(phraseLength)

            // Default: mild continuation from wherever the phrase left off.
            else -> defaultTrajectory(analysis.endingFunction, phraseLength)
        }
    }

    private fun resolvingTrajectory(length: Int): List<HarmonicFunction> =
        fitLength(
            listOf(
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.DOMINANT,
                HarmonicFunction.TONIC,
                HarmonicFunction.TONIC
            ),
            length
        )

    private fun reopeningTrajectory(length: Int): List<HarmonicFunction> =
        fitLength(
            listOf(
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.DOMINANT,
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.TONIC
            ),
            length
        )

    private fun directionalTrajectory(length: Int): List<HarmonicFunction> =
        fitLength(
            listOf(
                HarmonicFunction.DOMINANT,
                HarmonicFunction.PREDOMINANT,
                HarmonicFunction.PREDOMINANT,
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
                HarmonicFunction.PREDOMINANT,
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

    /** Pads or trims a base trajectory to the requested phrase length. */
    private fun fitLength(
        base: List<HarmonicFunction>,
        length: Int
    ): List<HarmonicFunction> {
        if (base.size == length) return base
        if (base.size > length) return base.take(length)
        val padding = List(length - base.size) { base.last() }
        return base + padding
    }
}