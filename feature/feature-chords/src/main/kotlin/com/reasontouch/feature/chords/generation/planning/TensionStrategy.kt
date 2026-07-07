package com.reasontouch.feature.chords.generation.planning

import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * Describes how the continuation should treat harmonic tension.
 */
enum class TensionDirection {

    /**
     * Maintain approximately the same harmonic intensity.
     */
    MAINTAIN,

    /**
     * Increase harmonic expectation.
     */
    BUILD,

    /**
     * Reduce tension and move toward cadence.
     */
    RELEASE
}

/**
 * Chooses the desired tension trajectory for the
 * generated continuation.
 *
 * Version 1 philosophy:
 *
 * • Closed phrases maintain stability.
 * • Open phrases release into a cadence.
 * • High tension is allowed to build further.
 */
object TensionStrategy {

    fun determine(
        analysis: ProgressionAnalysis
    ): TensionDirection {

        return when {

            analysis.isClosed() ->
                TensionDirection.MAINTAIN

            analysis.tension > 0.70f ->
                TensionDirection.BUILD

            else ->
                TensionDirection.RELEASE
        }
    }
}