package com.reasontouch.feature.chords.generation.planning

import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * Describes the broad musical role of the generated answer.
 */
enum class AnswerType {

    /**
     * Continues the musical conversation without
     * creating a strong cadence.
     */
    BALANCED,

    /**
     * Moves toward harmonic closure.
     */
    RESOLUTION,

    /**
     * Builds expectation by increasing tension.
     */
    DEVELOPMENT
}

/**
 * Chooses the most appropriate answer type from
 * the harmonic analysis.
 *
 * Version 1 philosophy:
 *
 * • Open phrases should normally resolve.
 * • Closed phrases should remain balanced.
 * • High tension encourages further development.
 */
object AnswerTypeStrategy {

    fun determine(
        analysis: ProgressionAnalysis
    ): AnswerType {

        return when {

            analysis.isOpen() ->
                AnswerType.RESOLUTION

            analysis.tension > 0.70f ->
                AnswerType.DEVELOPMENT

            else ->
                AnswerType.BALANCED
        }
    }
}