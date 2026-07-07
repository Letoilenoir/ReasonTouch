package com.reasontouch.feature.chords.generation.phrase

import com.reasontouch.feature.chords.ProgressionAnalysis
import com.reasontouch.feature.chords.generation.planning.AnswerType
import com.reasontouch.feature.chords.generation.planning.AnswerTypeStrategy
import com.reasontouch.feature.chords.generation.planning.PhraseLengthStrategy
import com.reasontouch.feature.chords.generation.planning.TensionDirection
import com.reasontouch.feature.chords.generation.planning.TensionStrategy

/**
 * Complete planning information for a continuation phrase.
 *
 * This class describes WHAT should be generated,
 * not HOW it is generated.
 */
data class ContinuationPhrasePlan(

    /**
     * Desired phrase length in bars.
     */
    val phraseLength: Int,

    /**
     * Overall musical role.
     */
    val answerType: AnswerType,

    /**
     * Desired tension trajectory.
     */
    val tensionDirection: TensionDirection,

    /**
     * Whether the harmonic contour should broadly
     * mirror the source progression.
     */
    val mirrorShape: Boolean
)

/**
 * Builds a complete phrase-generation plan from the
 * analysed source progression.
 */
object ContinuationPhraseGenerator {

    fun buildPlan(
        analysis: ProgressionAnalysis
    ): ContinuationPhrasePlan {

        val phraseLength =
            PhraseLengthStrategy.determineLength(analysis)

        val answerType =
            AnswerTypeStrategy.determine(analysis)

        val tension =
            TensionStrategy.determine(analysis)

        val mirrorShape =
            analysis.isDescendingProgression()

        return ContinuationPhrasePlan(
            phraseLength = phraseLength,
            answerType = answerType,
            tensionDirection = tension,
            mirrorShape = mirrorShape
        )
    }
}