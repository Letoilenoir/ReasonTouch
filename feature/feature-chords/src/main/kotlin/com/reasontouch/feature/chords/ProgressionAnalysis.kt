package com.reasontouch.feature.chords

import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.KeyCandidate

/**
 * CadenceType: Classification of how a progression ends.
 */
enum class CadenceType {
    AUTHENTIC,      // V -> I (strongest resolution)
    PLAGAL,         // IV -> I (weaker but complete)
    DECEPTIVE,      // V -> vi (V without resolution, creates surprise)
    HALF,           // -> V (ends on dominant, unresolved)
    INTERRUPTED,    // Other patterns
    OPEN            // Ends mid-phrase (no clear cadence)
}

/**
 * IntentRanking: A ranked compositional intention.
 * Output of IntentEngine.rank()
 */
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
    val harmonicStability: Float,  // 0.0 (unresolved) -> 1.0 (fully resolved)
    val energy: Float,              // Low -> High
    val tension: Float,             // Consonant -> Dissonant
    val endingFunction: HarmonicFunction,
    val rootMovementIntervals: List<Int>,
    val barCount: Int,
    val confidence: Float = 1.0f,
    val functionalSequence: List<HarmonicFunction>  // [I, vi, ii, IV] -- the harmonic shape
) {
    fun isClosed(): Boolean = harmonicStability > 0.75f
    fun isOpen(): Boolean = harmonicStability < 0.4f
    fun endsOnDominant(): Boolean = endingFunction == HarmonicFunction.DOMINANT

    fun startsOnTonic(): Boolean = functionalSequence.firstOrNull() == HarmonicFunction.TONIC

    fun isDescendingProgression(): Boolean {
        // Check if harmonic functions generally descend (I->V)
        if (functionalSequence.size < 2) return false
        // V(5) > IV(4) > iii(3) > ii(2) > I(1) in Roman numeral order
        // Descending: majority of transitions go down
        var descendingCount = 0
        for (i in 0 until functionalSequence.size - 1) {
            val current = functionDegree(functionalSequence[i])
            val next = functionDegree(functionalSequence[i + 1])
            if (next < current) descendingCount++
        }
        return descendingCount > functionalSequence.size / 2
    }

    fun isOscillatingProgression(): Boolean {
        // Check if harmonic functions oscillate (V->I or similar)
        if (functionalSequence.size < 3) return false
        // Count how many direction changes occur
        var directionChanges = 0
        for (i in 1 until functionalSequence.size - 1) {
            val prev = functionDegree(functionalSequence[i - 1])
            val curr = functionDegree(functionalSequence[i])
            val next = functionDegree(functionalSequence[i + 1])

            val goingUp = curr > prev
            val nextGoingUp = next > curr
            if (goingUp != nextGoingUp) directionChanges++
        }
        return directionChanges >= 2
    }

    private fun functionDegree(function: HarmonicFunction): Int {
        return when (function) {
            HarmonicFunction.TONIC -> 1
            HarmonicFunction.PREDOMINANT -> 4
            HarmonicFunction.DOMINANT -> 5
        }
    }
}