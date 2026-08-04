package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * LiftTargeting: Interprets a ProgressionAnalysis into a trajectory that
 * sustains and extends energy, rather than resolving or departing from it.
 *
 * Lift fires (via PairingEngine) when the preceding phrase is already
 * stable AND tense (harmonicStability >= 0.8f, tension > 0.6f) -- so the
 * goal here is not to build energy from nothing, but to keep the
 * existing energetic quality going forward.
 *
 * Per ProgressionAnalyzer's own formulas, tension is highest on DOMINANT
 * (0.8) and lowest on TONIC (0.2), and energy is literally the ratio of
 * how often the harmonic function changes between adjacent chords. So
 * unlike Continue/Contrast/Resolve, Lift's trajectory is not branched by
 * cadence type or ending function -- it's a single, deliberately
 * maximal-tension, maximal-transition shape: alternate DOMINANT and
 * PREDOMINANT (the two highest-tension functions), never repeat a
 * function twice in a row, never touch TONIC.
 *
 * Note: diatonic chords in this codebase are restricted to MAJ/MIN/DIM
 * quality (see MusicTheory.MAJOR_QUALITIES/MINOR_QUALITIES), and borrowed
 * chords are the same. Lift has no access to 7ths/sus/aug as an
 * additional tension lever -- it can only manipulate trajectory shape,
 * not chord color. This is a real ceiling on expressiveness, not a bug.
 */
object LiftTargeting {

    /**
     * Derives a trajectory that sustains energy: alternating DOMINANT
     * and PREDOMINANT, always starting with whichever of the two the
     * source progression did NOT just end on (so the very first
     * generated step is itself a function change, continuing the
     * energy rather than pausing on a repeat).
     *
     * @param analysis the completed analysis of the preceding progression
     * @param phraseLength number of chords to plan for (typically 4 or 8)
     */
    fun deriveTrajectory(
        analysis: ProgressionAnalysis,
        phraseLength: Int
    ): List<HarmonicFunction> {
        if (phraseLength <= 0) return emptyList()

        // Start on whichever high-tension function differs from the
        // source's ending function, so the trajectory's first step is
        // itself a change -- maximizing the transition-energy metric
        // from the very first generated chord.
        val startsOn = if (analysis.endingFunction == HarmonicFunction.DOMINANT) {
            HarmonicFunction.PREDOMINANT
        } else {
            HarmonicFunction.DOMINANT
        }
        val other = if (startsOn == HarmonicFunction.DOMINANT) {
            HarmonicFunction.PREDOMINANT
        } else {
            HarmonicFunction.DOMINANT
        }

        return List(phraseLength) { position ->
            if (position % 2 == 0) startsOn else other
        }
    }
}