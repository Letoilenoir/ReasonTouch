package com.reasontouch.feature.chords

/**
 * Converts ranked composition intents into a generation request.
 *
 * This separates musical analysis from music generation.
 */
object GenerationPlanner {

    fun createRequest(
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
            primaryIntent = primaryIntent,
            targetSection = targetSection,
            targetEnergy = targetEnergy,
            preferredLength = preferredLength
        )
    }
}