package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent
import kotlin.collections.firstOrNull

/**
 * Converts ranked composition intents into a generation request.
 *
 * This separates musical analysis from music generation.
 */
object GenerationPlanner {

    fun createRequest(
        sourceProgression: List<ChordEvent>,
        analysis: ProgressionAnalysis,
        rankedIntents: List<IntentRanking>,
        targetSection: SectionType? = null,
        targetEnergy: EnergyLevel? = null,
        preferredLength: Int = 4
    ): ProgressionGenerationRequest {

        val primaryIntent =
            rankedIntents.firstOrNull()?.intent
                ?: CompositionIntent.CONTINUE

        return ProgressionGenerationRequest(
            sourceAnalysis = analysis,
            sourceProgression = sourceProgression,
            primaryIntent = primaryIntent,
            targetSection = targetSection,
            targetEnergy = targetEnergy,
            preferredLength = preferredLength
        )
    }
}