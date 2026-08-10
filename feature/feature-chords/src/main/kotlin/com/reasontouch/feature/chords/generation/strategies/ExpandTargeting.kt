package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * ExpandTargeting: Interprets a ProgressionAnalysis into a trajectory
 * that tours all three harmonic functions in sequence, rather than
 * resolving (Resolve), sustaining tension via two-function alternation
 * (Lift), or departing via borrowed chords (Contrast).
 *
 * Expand fires (via PairingEngine) in two related cases -- a HALF
 * cadence ("Ends on V -> explore harmonically", i.e. don't resolve,
 * explore instead) and the medium-stability default case ("Medium
 * stability -> harmonic exploration"). Both point the same direction:
 * this is the strategy for touring diatonic harmonic territory broadly,
 * not pursuing any single functional goal.
 *
 * Trajectory: TONIC -> PREDOMINANT -> DOMINANT, cycling, starting one
 * step after wherever the source progression's ending function left
 * off -- so the first generated chord is itself a function change,
 * same continuity principle as LiftTargeting, extended to a full
 * 3-function cycle rather than a 2-function alternation.
 */
object ExpandTargeting {

    private val CYCLE = listOf(
        HarmonicFunction.TONIC,
        HarmonicFunction.PREDOMINANT,
        HarmonicFunction.DOMINANT
    )

    fun deriveTrajectory(
        analysis: ProgressionAnalysis,
        phraseLength: Int
    ): List<HarmonicFunction> {
        if (phraseLength <= 0) return emptyList()

        val startIndex = when (analysis.endingFunction) {
            HarmonicFunction.TONIC -> 1       // start on PREDOMINANT
            HarmonicFunction.PREDOMINANT -> 2 // start on DOMINANT
            HarmonicFunction.DOMINANT -> 0     // start on TONIC
        }

        return List(phraseLength) { i -> CYCLE[(startIndex + i) % CYCLE.size] }
    }
}