package com.reasontouch.feature.chords

import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.KeyCandidate

/**
 * CadenceType: Classification of how a progression ends.
 */
enum class CadenceType {
    AUTHENTIC,      // V → I (strongest resolution)
    PLAGAL,         // IV → I (weaker but complete)
    DECEPTIVE,      // V → vi (V without resolution, creates surprise)
    HALF,           // → V (ends on dominant, unresolved)
    INTERRUPTED,    // Other patterns
    OPEN            // Ends mid-phrase (no clear cadence)
}
data class IntentRanking(
    val intent: CompositionIntent,
    val confidence: Float,
    val rationale: String
)
/**
 * ProgressionAnalysis: Complete harmonic profile of a single progression.
 * Output of ProgressionAnalyzer.
 */
data class ProgressionAnalysis(
    val key: KeyCandidate,
    val cadenceType: CadenceType,
    val harmonicStability: Float,  // 0.0 (unresolved) → 1.0 (fully resolved)
    val energy: Float,              // Low → High
    val tension: Float,             // Consonant → Dissonant
    val endingFunction: HarmonicFunction,
    val rootMovementIntervals: List<Int>,
    val barCount: Int,
    val confidence: Float = 1.0f
) {
    fun isClosed(): Boolean = harmonicStability > 0.75f
    fun isOpen(): Boolean = harmonicStability < 0.4f
    fun endsOnDominant(): Boolean = endingFunction == HarmonicFunction.DOMINANT
}
