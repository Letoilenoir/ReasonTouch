package com.reasontouch.feature.chords.generation

import com.reasontouch.feature.chords.ProgressionGenerationRequest

/**
 * Converts a ProgressionGenerationRequest into a shared
 * GenerationContext.
 *
 * Every generation strategy uses this same context.
 */
object GenerationContextBuilder {

    fun from(
        request: ProgressionGenerationRequest
    ): GenerationContext {

        return GenerationContext(
            seedProgression = request.sourceProgression,
            analysis = request.sourceAnalysis
        )
    }
}
