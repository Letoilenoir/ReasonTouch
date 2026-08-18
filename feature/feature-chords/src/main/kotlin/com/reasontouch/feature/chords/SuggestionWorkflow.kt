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
}