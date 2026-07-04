package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent

/**
 * Describes the musical goal for generating a new progression.
 *
 * This object contains no chord data.
 * It simply describes the desired musical outcome.
 */
data class ProgressionGenerationRequest(

    val sourceAnalysis: ProgressionAnalysis,

    val sourceProgression: List<ChordEvent>,

    val primaryIntent: CompositionIntent,

    val targetSection: SectionType? = null,

    val targetEnergy: EnergyLevel? = null,

    val preferredLength: Int = 4
)