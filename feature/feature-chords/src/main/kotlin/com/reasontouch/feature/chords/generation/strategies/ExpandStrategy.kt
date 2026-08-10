package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.TheoryChord
import com.reasontouch.feature.chords.guitarLabel

object ExpandStrategy {
    private val VARIANT_LABELS = listOf(
        "Harmonic Tour", "Alternate Voicing", "Extended Approach", "Contrasting Approach"
    )

    fun generate(request: ProgressionGenerationRequest): List<GeneratedProgression> {
        val source = request.sourceProgression
        if (source.isEmpty()) return emptyList()

        val trajectory = ExpandTargeting.deriveTrajectory(
            analysis = request.sourceAnalysis,
            phraseLength = request.preferredLength
        )
        if (trajectory.isEmpty()) return emptyList()

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
                        explanation = "${VARIANT_LABELS[variantIndex]}: tours the diatonic " +
                                "harmonic territory rather than settling on any single functional goal."
                    )
                )
            }
        }

        return results.distinctBy { it.chords.map { c -> c.label } }
    }
}