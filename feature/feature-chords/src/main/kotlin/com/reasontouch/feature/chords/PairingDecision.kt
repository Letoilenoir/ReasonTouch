package com.reasontouch.feature.chords

/**
 * PairingType: Compositional intent for what comes next.
 */
enum class PairingType {
    CONTINUE,    // Stay in groove (low stability → keep driving)
    LIFT,        // Build energy (high stability → change texture)
    CONTRAST,    // Change character (high stability → provide variation)
    RESOLVE,     // Complete phrase (low stability, dominant → resolve)
    EXPAND,      // Extend range (medium stability → harmonic exploration)
    MODULATE,    // Change key (deceptive cadence → surprise)
    SURPRISE,    // Break expectation (deceptive cadence → unexpected turn)
    SIMPLIFY     // Reduce complexity (high energy → pull back)
}

/**
 * PairingDecision: Output of PairingEngine.suggestNext() — single top-ranked intent.
 * Answers: "What type of section should follow this progression?"
 */
data class PairingDecision(
    val type: PairingType,
    val suggestedBars: Int,      // How long should the next section be?
    val confidence: Float,        // 0.0 (unsure) → 1.0 (certain)
    val rationale: String         // Why this suggestion (for UI/debug)
)

/**
 * IntentOption: One possible next-section intent, as part of a ranked multi-intent result.
 * Used by the multi-choice Suggest Next UI.
 *
 * isAboveThreshold is informational only (e.g. "primary" vs "more options" styling in the UI) —
 * it does NOT mean this option was excluded. Per the multi-intent design doc: scoring ranks
 * intents, it never filters out ones with a working strategy. See PairingEngine.suggestNextMulti().
 */
data class IntentOption(
    val type: PairingType,
    val suggestedBars: Int,
    val confidence: Float,
    val rationale: String,
    val isAboveThreshold: Boolean
)

/**
 * PairingDecisionSet: Multi-option output of PairingEngine.suggestNextMulti().
 * options is sorted descending by confidence. By default it contains every intent that has a
 * working strategy behind it (see PairingEngine.IMPLEMENTED_INTENTS) — nothing is dropped unless
 * you explicitly opt in via suggestNextMulti(filterByThreshold = true).
 */
data class PairingDecisionSet(
    val options: List<IntentOption>,
    val threshold: Float = 0.65f
) {
    val topOption: IntentOption? get() = options.firstOrNull()
}