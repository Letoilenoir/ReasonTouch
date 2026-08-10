package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestion
import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.TheoryChord

/**
 * Shared candidate-selection logic for all phrase-generation strategies
 * (Continue/Contrast/Resolve/Lift). Centralizes the function-filter ->
 * exclusion -> fallback pipeline so fixes to it live in exactly one place,
 * rather than four independently-copied blocks that already drifted out
 * of sync once -- see the exhaustion-rotation bug found and fixed
 * 2026-08-04 (docs/handoffs/ReasonTouch_Handoff_2026-08-04_LiftStrategy_ExhaustionFix.md),
 * which was present identically in all four strategies before this
 * extraction.
 */
object ChordCandidateSelector {

    /**
     * @param suggestions the full candidate pool for this position (from
     *   ChordSuggestionEngine.suggest(), typically with maxResults =
     *   FULL_DIATONIC_POOL)
     * @param targetFunction the harmonic function this position's
     *   trajectory calls for; null falls through to the unfiltered pool
     * @param continuation chords already picked earlier in THIS variant's
     *   continuation (used to count exhaustion-rotation occurrences --
     *   must be the growing List, not a Set, or the rotation freezes;
     *   see handoff doc above for the failed first attempt that used
     *   usedChords instead)
     * @param usedChords set of chords already used in this continuation
     *   (used for the initial not-yet-exhausted exclusion pass)
     * @param variantIndex which of the ranked variants this is (0-3),
     *   used both as the initial rank and as the rotation offset once
     *   a function's pool is exhausted
     * @return the chosen candidate, or null if suggestions was empty
     */
    fun select(
        suggestions: List<ChordSuggestion>,
        targetFunction: HarmonicFunction?,
        continuation: List<TheoryChord>,
        usedChords: Set<TheoryChord>,
        variantIndex: Int
    ): ChordSuggestion? {
        if (suggestions.isEmpty()) return null

        val matching = suggestions.filter { it.function == targetFunction }
        val pool = matching.ifEmpty { suggestions }

        val unusedPool = pool.filterNot { it.chord in usedChords }
        val finalPool = unusedPool.ifEmpty { pool }

        val rank = if (unusedPool.isEmpty()) {
            val timesRevisited = continuation.count { c -> pool.any { it.chord == c } }
            (variantIndex + timesRevisited) % finalPool.size
        } else {
            variantIndex.coerceAtMost(finalPool.size - 1)
        }

        return finalPool[rank]
    }
}