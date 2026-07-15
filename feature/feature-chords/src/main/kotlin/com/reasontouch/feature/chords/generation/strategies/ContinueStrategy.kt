package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.MusicTheory
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.TheoryChord
import com.reasontouch.feature.chords.guitarLabel

object ContinueStrategy {

    fun generate(
        request: ProgressionGenerationRequest
    ): List<GeneratedProgression> {

        val source = request.sourceProgression
        if (source.isEmpty()) return emptyList()

        val continuation = mutableListOf<TheoryChord>()

        var currentChord =
            source.last().chordName

        var confidenceSum = 0f

        repeat(request.preferredLength) {

            val suggestions = ChordSuggestionEngine.suggest(
                key = request.sourceAnalysis.key,
                lastChordName = currentChord
            )

            if (suggestions.isEmpty())
                return@repeat

            val chosen = suggestions.first()

            continuation += chosen.chord
            confidenceSum += chosen.confidence

            currentChord = chosen.chord.guitarLabel()
        }

        if (continuation.isEmpty())
            return emptyList()

        return listOf(
            GeneratedProgression(
                chords = continuation,
                confidence = confidenceSum / continuation.size,
                explanation = "Natural continuation"
            )
        )
    }
}