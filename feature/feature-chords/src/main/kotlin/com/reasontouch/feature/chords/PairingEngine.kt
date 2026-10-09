package com.reasontouch.feature.chords

/**
 * PairingEngine: Suggests what section type should follow a progression.
 * Input: ONE ProgressionAnalysis
 * Output: PairingDecision (single top-ranked intent) or PairingDecisionSet (ranked list)
 *
 * 2026-08 update: suggestNext() is now a thin wrapper over the multi-intent scoring layer
 * (suggestNextMulti() / rankIntents() / scoreIntent()) rather than its own hand-branched logic.
 * See ReasonTouch_Handoff_2026-08-01.md ("Multi-Intent Pairing") for the design rationale.
 *
 * The old suggestAfterHighStability/MediumStability/LowStability() functions are kept below,
 * commented out of the live path, purely as a reference for the conditions that used to route
 * to EXPAND/SIMPLIFY/MODULATE — none of which have a real strategy today. Suggesting those
 * intents previously meant the user could pick something that generated nothing — the ranking
 * layer below only ever surfaces intents with a working strategy behind them (currently Resolve,
 * Contrast, Continue, Lift, Surprise — see IMPLEMENTED_INTENTS).
 */
object PairingEngine {

    /**
     * Intents with a real chord-generation strategy behind them today: Resolve, Contrast,
     * Continue (per the 2026-08-01 exclusion-fix handoff), plus Lift and Surprise (confirmed via
     * LiftStrategy.kt / SurpriseStrategy.kt — both call ChordSuggestionEngine.suggest(...) and
     * return real GeneratedProgression lists).
     *
     * EXPAND, SIMPLIFY, MODULATE remain excluded — no strategy exists for any of them yet. Add an
     * intent here the moment its strategy lands; scoreIntent() already has branches ready for all
     * eight PairingType values.
     */
    private val IMPLEMENTED_INTENTS: Set<PairingType> = setOf(
        PairingType.RESOLVE,
        PairingType.CONTRAST,
        PairingType.CONTINUE,
        PairingType.LIFT,
        PairingType.SURPRISE,
        PairingType.REPEAT
    )

    /**
     * Scores how typical/plausible a given intent is in the current harmonic context.
     * Continuous 0..1 plausibility, not a hard gate — ranking happens in rankIntents(), never
     * exclusion here. EXPAND/SIMPLIFY/MODULATE fall through to 0.0f since they have no live
     * strategy and are excluded from IMPLEMENTED_INTENTS regardless of score.
     */
    fun scoreIntent(type: PairingType, analysis: ProgressionAnalysis): Float {
        val s = analysis.harmonicStability
        val cadence = analysis.cadenceType
        val ending = analysis.endingFunction
        val borrowed = ChordSuggestionEngine.borrowedChords(analysis.key).isNotEmpty()

        return when (type) {

            PairingType.RESOLVE ->
                when {
                    ending == HarmonicFunction.DOMINANT -> 0.95f
                    cadence == CadenceType.DECEPTIVE -> 0.90f
                    s < 0.4f -> 0.85f
                    analysis.isOpen() -> 0.75f
                    analysis.isClosed() -> 0.55f
                    else -> 0.40f
                }

            PairingType.CONTRAST ->
                when {
                    s >= 0.8f -> 0.90f
                    s >= 0.4f -> 0.75f
                    borrowed -> 0.50f
                    else -> 0.40f
                }

            PairingType.CONTINUE ->
                when {
                    analysis.isOpen() -> 0.85f
                    s >= 0.4f && s < 0.8f -> 0.75f
                    s < 0.4f -> 0.65f
                    s >= 0.8f -> 0.55f
                    else -> 0.45f
                }

            PairingType.LIFT ->
                when {
                    s >= 0.8f -> 0.90f
                    s >= 0.4f -> 0.80f
                    s < 0.4f -> 0.50f
                    else -> 0.45f
                }

            PairingType.SURPRISE ->
                when {
                    cadence == CadenceType.DECEPTIVE -> 0.90f
                    ending == HarmonicFunction.DOMINANT -> 0.80f
                    s >= 0.4f && s < 0.8f -> 0.70f
                    s >= 0.8f -> 0.60f
                    s < 0.4f -> 0.50f
                    else -> 0.40f
                }

            PairingType.REPEAT -> 0.80f

            PairingType.EXPAND, PairingType.SIMPLIFY, PairingType.MODULATE -> 0.0f
        }
    }

    /**
     * UI-facing explanation for a type's score, mirroring the same conditions scoreIntent() uses
     * so the displayed rationale always matches the number that drove it.
     */
    private fun rationaleFor(type: PairingType, analysis: ProgressionAnalysis): String {
        val s = analysis.harmonicStability
        val cadence = analysis.cadenceType
        val ending = analysis.endingFunction

        return when (type) {
            PairingType.RESOLVE ->
                when {
                    ending == HarmonicFunction.DOMINANT -> "Ends on V → resolution needed"
                    cadence == CadenceType.DECEPTIVE -> "Deceptive cadence → resolution overdue"
                    s < 0.4f -> "Low stability → resolution needed"
                    analysis.isOpen() -> "Open phrase → resolution would land well here"
                    analysis.isClosed() -> "Already closed → resolution optional"
                    else -> "Resolution possible, though not strongly indicated"
                }

            PairingType.CONTRAST ->
                when {
                    s >= 0.8f -> "High stability → natural point for variation"
                    s >= 0.4f -> "Medium stability → variation plausible"
                    else -> "Low stability → contrast is less typical here"
                }

            PairingType.CONTINUE ->
                when {
                    analysis.isOpen() -> "Open phrase → natural to keep going"
                    s >= 0.4f && s < 0.8f -> "Medium stability → continuation fits"
                    s < 0.4f -> "Low stability → maintain momentum"
                    else -> "High stability → continuation is less typical here"
                }

            PairingType.SURPRISE ->
                when {
                    cadence == CadenceType.DECEPTIVE -> "Deceptive cadence → unexpected turn fits"
                    ending == HarmonicFunction.DOMINANT -> "Ends on V → an unresolved twist is possible"
                    s >= 0.4f && s < 0.8f -> "Medium stability → room for an unexpected turn"
                    else -> "Context allows a rule-breaking choice here"
                }

            PairingType.LIFT ->
                when {
                    s >= 0.8f -> "High stability → ready for an energy lift"
                    s >= 0.4f -> "Medium stability → lift is plausible"
                    else -> "Low stability → lift is less typical here"
                }

            PairingType.REPEAT ->
                "Reuse an existing sequence to establish structure and familiarity"

            PairingType.EXPAND, PairingType.SIMPLIFY, PairingType.MODULATE ->
                "No live strategy implementation yet"
        }
    }

    /**
     * Ranks every intent with a working strategy against the current harmonic context.
     * Sorted descending by confidence. Nothing is excluded here — that's the whole point.
     */
    fun rankIntents(analysis: ProgressionAnalysis, threshold: Float = 0.65f): List<IntentOption> =
        IMPLEMENTED_INTENTS
            .map { type ->
                val score = scoreIntent(type, analysis)
                IntentOption(
                    type = type,
                    suggestedBars = if (type == PairingType.RESOLVE) 4 else 8,
                    confidence = score,
                    rationale = rationaleFor(type, analysis),
                    isAboveThreshold = score >= threshold
                )
            }
            .sortedByDescending { it.confidence }

    /**
     * Multi-intent entry point. By default returns every implemented intent, ranked — matching
     * the design doc's "rank, don't filter" principle. Pass filterByThreshold = true to restore
     * the old cut-at-threshold behavior if you want it for a specific UI surface.
     */
    fun suggestNextMulti(
        currentAnalysis: ProgressionAnalysis,
        threshold: Float = 0.65f,
        filterByThreshold: Boolean = false
    ): PairingDecisionSet {
        val ranked = rankIntents(currentAnalysis, threshold)
        val options = if (filterByThreshold) ranked.filter { it.confidence >= threshold } else ranked
        return PairingDecisionSet(options = options, threshold = threshold)
    }

    /**
     * Single-intent entry point — existing call sites (ChordViewModel, PianoRollViewModel) are
     * unaffected. Now just the top of the ranked list rather than its own branch logic.
     *
     * history is accepted for source compatibility but currently unused by the ranking itself —
     * both known call sites already pass emptyList() (per 2026-08-01 handoff, confirmed
     * primaryCount is always 0). Left as a hook for future work (e.g. nudging the ranking away
     * from repeating the same intent back-to-back), not implemented here.
     */
    fun suggestNext(
        currentAnalysis: ProgressionAnalysis,
        @Suppress("UNUSED_PARAMETER") history: List<PairingType> = emptyList()
    ): PairingDecision {
        val top = suggestNextMulti(currentAnalysis).options.first()
        return PairingDecision(
            type = top.type,
            suggestedBars = top.suggestedBars,
            confidence = top.confidence,
            rationale = top.rationale
        )
    }

    // ---------------------------------------------------------------------------------------
    // Reference only — not called from the live path anymore. Kept so the reasoning behind the
    // old branch structure isn't lost (e.g. why HALF cadence used to route to EXPAND). Safe to
    // delete once you're confident the ranked version covers everything you need.
    // ---------------------------------------------------------------------------------------
    /*
    private fun suggestAfterHighStability(...) { ... }
    private fun suggestAfterMediumStability(...) { ... }
    private fun suggestAfterLowStability(...) { ... }
    */
}