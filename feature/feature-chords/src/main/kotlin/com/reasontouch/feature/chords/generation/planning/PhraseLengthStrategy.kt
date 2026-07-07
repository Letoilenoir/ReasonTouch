package com.reasontouch.feature.chords.generation.planning

import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * Determines the length of the continuation phrase.
 *
 * Version 1.0 philosophy:
 * A continuation answers the existing phrase with another
 * phrase of the same number of bars.
 *
 * Future versions may take into account:
 * - composition intent
 * - section type
 * - user preferences
 * - phrase expansion/compression
 */
object PhraseLengthStrategy {

    fun determineLength(
        analysis: ProgressionAnalysis
    ): Int {

        return analysis.barCount
    }
}