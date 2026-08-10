package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.MusicTheory
import com.reasontouch.feature.chords.ProgressionAnalysis
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.TheoryChord
import com.reasontouch.feature.chords.guitarLabel

object ContinueStrategy {

    private val VARIANT_LABELS = listOf(
        "Closest Fit", "Alternate Voicing", "Brighter Option", "Contrasting Option"
    )

    fun generate(request: ProgressionGenerationRequest): List<GeneratedProgression> {
        val source = request.sourceProgression
        if (source.isEmpty()) return emptyList()

        val trajectory = ContinueTargeting.deriveTrajectory(
            analysis = request.sourceAnalysis,
            phraseLength = request.preferredLength
        )

        val results = mutableListOf<GeneratedProgression>()

        for (variantIndex in VARIANT_LABELS.indices) {
            val continuation = mutableListOf<TheoryChord>()
            val usedChords = mutableSetOf<TheoryChord>()
            var currentChord = source.last().chordName
            var confidenceSum = 0f
            repeat(request.preferredLength) { position ->
                val suggestions = ChordSuggestionEngine.suggest(
                    key = request.sourceAnalysis.key,
                    lastChordName = currentChord,
                    maxResults = ChordSuggestionEngine.FULL_DIATONIC_POOL
                )
                if (suggestions.isEmpty()) return@repeat
                val targetFunction = trajectory.getOrNull(position)
                val chosen = ChordCandidateSelector.select(
                    suggestions = suggestions,
                    targetFunction = targetFunction,
                    continuation = continuation,
                    usedChords = usedChords,
                    variantIndex = variantIndex
                ) ?: return@repeat
                continuation += chosen.chord
                confidenceSum += chosen.confidence
                currentChord = chosen.chord.guitarLabel()
                usedChords += chosen.chord
            }

            if (continuation.isNotEmpty()) {
                results.add(
                    GeneratedProgression(
                        chords = continuation,
                        confidence = confidenceSum / continuation.size,
                        explanation = "${VARIANT_LABELS[variantIndex]}: ${explanationFor(request.sourceAnalysis)}"
                    )
                )
            }
        }

        return results.distinctBy { it.chords.map { c -> c.label } }
    }

    private fun explanationFor(analysis: ProgressionAnalysis): String {
        return when {
            analysis.endsOnDominant() ->
                "resolves the preceding dominant into a settled continuation."
            analysis.isClosed() ->
                "reopens harmonic movement after a resolved phrase."
            analysis.isOscillatingProgression() ->
                "breaks the oscillating pattern with directional movement."
            analysis.isDescendingProgression() ->
                "continues the descending harmonic motion."
            else ->
                "continues naturally from the preceding phrase."
        }
    }
}