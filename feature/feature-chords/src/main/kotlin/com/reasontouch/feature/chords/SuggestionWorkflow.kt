package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent

/**
 * SuggestionWorkflow: single source of truth for the SUGGEST NEXT flow
 * (key detection -> analysis -> pairing decision -> phrase generation).
 *
 * Consolidates what were previously two independently-maintained copies in
 * ChordViewModel and PianoRollViewModel, which had drifted out of sync
 * (ChordViewModel hardcoded preferredLength = 4; PianoRollViewModel correctly
 * used pairingDecision.suggestedBars). See Task_List.md, "Consolidate
 * duplicated suggestNextSection() logic", confirmed 2026-08-02.
 *
 * Deliberately synchronous / non-suspend: callers are responsible for
 * supplying the progression (e.g. a StateFlow's .value, or a repository
 * fetch) so this object has no opinion on where the data comes from.
 */
object SuggestionWorkflow {

    /**
     * Returns the top-ranked PairingDecision for the given progression.
     * Fallback rationale/confidence distinguishes "no progression yet" from
     * "progression present but key couldn't be detected" -- these are
     * different situations and shouldn't share a message.
     */
    fun suggestNextSection(progression: List<ChordEvent>): PairingDecision {
        if (progression.isEmpty()) {
            return PairingDecision(
                type = PairingType.CONTINUE,
                suggestedBars = 4,
                confidence = 0.5f,
                rationale = "No progression yet - start with any 4-bar section"
            )
        }

        val chordNames = progression.map { it.chordName.substringBefore(" ") }
        val detectedKey = KeyDetector.detect(chordNames).firstOrNull()
            ?: return PairingDecision(
                type = PairingType.CONTINUE,
                suggestedBars = 4,
                confidence = 0.3f,
                rationale = "Could not detect key"
            )

        val analysis = ProgressionAnalyzer.analyze(progression, detectedKey)
        return PairingEngine.suggestNext(analysis)
    }

    /**
     * Generates phrase candidates for the given progression, using
     * PairingEngine's top-ranked decision to drive both intent and bar
     * count. Returns an empty list if the progression is empty or key
     * detection fails -- callers already handle the empty-result case via
     * their existing "pathway not yet implemented" fallback UI.
     */
    fun suggestNextPhrases(progression: List<ChordEvent>): List<GeneratedProgression> {
        if (progression.isEmpty()) return emptyList()

        val chordNames = progression.map { it.chordName.substringBefore(" ") }
        val detectedKey = KeyDetector.detect(chordNames).firstOrNull() ?: return emptyList()

        val analysis = ProgressionAnalyzer.analyze(progression, detectedKey)
        val decision = PairingEngine.suggestNext(analysis)

        val request = ProgressionGenerationRequest(
            sourceAnalysis = analysis,
            sourceProgression = progression,
            primaryIntent = decision.type.toCompositionIntent(),
            targetSection = null,
            targetEnergy = null,
            preferredLength = decision.suggestedBars
        )

        return ProgressionGenerator.generate(request)
    }

    private fun PairingType.toCompositionIntent(): CompositionIntent = when (this) {
        PairingType.CONTINUE  -> CompositionIntent.CONTINUE
        PairingType.LIFT      -> CompositionIntent.LIFT
        PairingType.CONTRAST  -> CompositionIntent.CONTRAST
        PairingType.RESOLVE   -> CompositionIntent.RESOLVE
        PairingType.EXPAND    -> CompositionIntent.EXPAND
        PairingType.SURPRISE  -> CompositionIntent.SURPRISE
        PairingType.SIMPLIFY  -> CompositionIntent.SIMPLIFY
        PairingType.MODULATE  -> CompositionIntent.DEVELOP  // closest existing intent; no direct equivalent yet
    }

/**
 * One ranked intent paired with its generated phrase candidates. Used by the multi-option
 * SUGGEST NEXT dialog -- IntentOption alone (from PairingEngine) doesn't carry phrases,
 * since phrase generation is a SuggestionWorkflow-level concern, not a PairingEngine one.
 */
data class SuggestionOption(
    val option: IntentOption,
    val phrases: List<GeneratedProgression>
)

/**
 * Multi-option entry point for the SUGGEST NEXT dialog. Computes the analysis once, then
 * generates phrases for every ranked intent (not just the top one) so the dialog can offer
 * intent-level choice per the 2026-08-03 design note ("if we impose a decision on the user,
 * we are effectively building the composition for them").
 *
 * Falls back to a single-entry list (empty phrases) for the same two edge cases
 * suggestNextSection() handles -- empty progression / undetected key -- so callers can
 * treat the dialog's input uniformly rather than special-casing "no options yet".
 */
fun suggestNextOptionsWithPhrases(progression: List<ChordEvent>): List<SuggestionOption> {
    if (progression.isEmpty()) {
        val fallback = IntentOption(
            type = PairingType.CONTINUE,
            suggestedBars = 4,
            confidence = 0.5f,
            rationale = "No progression yet - start with any 4-bar section",
            isAboveThreshold = true
        )
        return listOf(SuggestionOption(fallback, emptyList()))
    }

    val chordNames = progression.map { it.chordName.substringBefore(" ") }
    val detectedKey = KeyDetector.detect(chordNames).firstOrNull()
    if (detectedKey == null) {
        val fallback = IntentOption(
            type = PairingType.CONTINUE,
            suggestedBars = 4,
            confidence = 0.3f,
            rationale = "Could not detect key",
            isAboveThreshold = false
        )
        return listOf(SuggestionOption(fallback, emptyList()))
    }

    val analysis = ProgressionAnalyzer.analyze(progression, detectedKey)
    val decisionSet = PairingEngine.suggestNextMulti(analysis)

    return decisionSet.options.map { option ->
        val request = ProgressionGenerationRequest(
            sourceAnalysis = analysis,
            sourceProgression = progression,
            primaryIntent = option.type.toCompositionIntent(),
            targetSection = null,
            targetEnergy = null,
            preferredLength = option.suggestedBars
        )
        SuggestionOption(option = option, phrases = ProgressionGenerator.generate(request))
    }
}
}