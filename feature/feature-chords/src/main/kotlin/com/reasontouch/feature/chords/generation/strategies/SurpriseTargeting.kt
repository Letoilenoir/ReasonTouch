package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * SurpriseTargeting: Interprets a ProgressionAnalysis into a trajectory
 * that primes an expected resolution, then denies it -- generalizing
 * the deceptive cadence pattern (V, expecting I, landing on vi instead)
 * to the whole generated phrase, rather than just a single V->vi move.
 *
 * Surprise fires (via PairingEngine) specifically on CadenceType.DECEPTIVE
 * ("Deceptive cadence -> unexpected turn"). Unlike ContrastStrategy,
 * which places its one borrowed (non-diatonic) chord at position 0 as
 * an opening declaration of difference, SurpriseStrategy places its
 * borrowed chord at the FINAL position -- after a trajectory that
 * otherwise looks like an ordinary cadential run (PREDOMINANT ->
 * DOMINANT, priming an expected TONIC landing), the expected resolution
 * is substituted for a borrowed chord instead. Same building block as
 * Contrast (ChordSuggestionEngine.borrowedChords() -- the only lever
 * available for genuine harmonic color, since diatonic chords are
 * quality-locked to MAJ/MIN/DIM), repositioned to match what "surprise"
 * actually means structurally: subversion at the moment resolution
 * seemed certain, not departure at the start.
 */
object SurpriseTargeting {

    data class SurprisePlan(
        val trajectory: List<HarmonicFunction>,
        val borrowedChordPosition: Int
    )

    fun derivePlan(analysis: ProgressionAnalysis, phraseLength: Int): SurprisePlan {
        if (phraseLength <= 0) return SurprisePlan(emptyList(), -1)

        // Base trajectory always primes toward an expected TONIC landing
        // (PREDOMINANT -> DOMINANT -> TONIC shape), regardless of how
        // the source progression itself ended -- the whole point is to
        // build a conventional-sounding expectation before subverting it.
        val base = listOf(
            HarmonicFunction.PREDOMINANT,
            HarmonicFunction.DOMINANT,
            HarmonicFunction.PREDOMINANT,
            HarmonicFunction.DOMINANT
        )
        val trajectory = fitLength(base, phraseLength)

        // The borrowed-chord substitution always lands on the final
        // position -- the moment a listener would expect resolution.
        val borrowedChordPosition = phraseLength - 1

        return SurprisePlan(trajectory, borrowedChordPosition)
    }

    private fun fitLength(base: List<HarmonicFunction>, length: Int): List<HarmonicFunction> {
        if (base.size == length) return base
        if (base.size > length) return base.takeLast(length)
        val padding = List(length - base.size) { base.last() }
        return base + padding
    }
}