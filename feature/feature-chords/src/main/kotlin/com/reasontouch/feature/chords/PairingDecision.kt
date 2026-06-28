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
 * PairingDecision: Output of PairingEngine.
 * Answers: "What type of section should follow this progression?"
 */
data class PairingDecision(
    val type: PairingType,
    val suggestedBars: Int,      // How long should the next section be?
    val confidence: Float,        // 0.0 (unsure) → 1.0 (certain)
    val rationale: String         // Why this suggestion (for UI/debug)
)