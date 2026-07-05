package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.TheoryChord

/**
 * Generates candidate continuations that preserve the
 * musical character of the source progression.
 *
 * Uses ChordSuggestionEngine (existing music theory logic)
 * to determine what naturally follows the last chord.
 *
 * Returns purely harmonic suggestions (TheoryChord objects).
 * No session/persistence data is created here.
 */
object ContinueStrategy {

    fun generate(
        request: ProgressionGenerationRequest
    ): List<GeneratedProgression> {

        val source = request.sourceProgression

        if (source.isEmpty()) {
            return emptyList()
        }

        // Get the last chord in the progression
        val lastChord = source.lastOrNull() ?: return emptyList()
        
        // Ask ChordSuggestionEngine what naturally follows
        val suggestions = ChordSuggestionEngine.suggest(
            key = request.sourceAnalysis.key,
            lastChordName = lastChord.chordName
        )

        if (suggestions.isEmpty()) {
            return emptyList()
        }

        // Build multiple candidate continuations
        val candidates = mutableListOf<GeneratedProgression>()

        // Candidate 1: Most natural suggestion (highest confidence)
        val topSuggestion = suggestions.first()
        candidates.add(
            GeneratedProgression(
                chords = listOf(topSuggestion.chord),
                confidence = topSuggestion.confidence,
                explanation = "Most natural continuation: ${topSuggestion.description}"
            )
        )

        // Candidate 2: Second choice (if available)
        if (suggestions.size > 1) {
            val secondSuggestion = suggestions[1]
            candidates.add(
                GeneratedProgression(
                    chords = listOf(secondSuggestion.chord),
                    confidence = secondSuggestion.confidence,
                    explanation = "Alternative continuation: ${secondSuggestion.description}"
                )
            )
        }

        // Candidate 3: Third choice (if available)
        if (suggestions.size > 2) {
            val thirdSuggestion = suggestions[2]
            candidates.add(
                GeneratedProgression(
                    chords = listOf(thirdSuggestion.chord),
                    confidence = thirdSuggestion.confidence,
                    explanation = "Another option: ${thirdSuggestion.description}"
                )
            )
        }

        return candidates.sortedByDescending { it.confidence }
    }
}