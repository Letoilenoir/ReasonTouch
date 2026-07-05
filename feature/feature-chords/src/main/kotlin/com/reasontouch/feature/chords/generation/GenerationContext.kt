package com.reasontouch.feature.chords.generation

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.feature.chords.ProgressionAnalysis

/**
 * Shared musical context passed to every generation strategy.
 *
 * The context is always derived from a user-created seed progression.
 */
data class GenerationContext(

    /**
     * The original progression supplied by the user.
     */
    val seedProgression: List<ChordEvent>,

    /**
     * Harmonic analysis of the seed.
     */
    val analysis: ProgressionAnalysis,

    /**
     * Convenience reference to the final chord.
     */
    val lastChord: ChordEvent? =
        seedProgression.lastOrNull()
)