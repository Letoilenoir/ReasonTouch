package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.TheoryChord
import com.reasontouch.feature.chords.guitarLabel

object SurpriseStrategy {

    fun generate(request: ProgressionGenerationRequest): List<GeneratedProgression> {
        val source = request.sourceProgression
        if (source.isEmpty()) return emptyList()

        val plan = SurpriseTargeting.derivePlan(
            analysis = request.sourceAnalysis,
            phraseLength = request.preferredLength
        )
        if (plan.trajectory.isEmpty()) return emptyList()

        val borrowed = ChordSuggestionEngine.borrowedChords(request.sourceAnalysis.key)
        if (borrowed.isEmpty()) return emptyList()

        val results = mutableListOf<GeneratedProgression>()

        for (variantIndex in borrowed.indices) {
            val continuation = mutableListOf<TheoryChord>()
            val usedChords = mutableSetOf<TheoryChord>()
            var currentChord = source.last().chordName
            var confidenceSum = 0f
            var stepCount = 0
            var variantSymbol = ""

            for ((position, targetFunction) in plan.trajectory.withIndex()) {
                val useBorrowedHere = position == plan.borrowedChordPosition
                if (useBorrowedHere) {
                    val borrowedIndex = variantIndex.coerceAtMost(borrowed.size - 1)
                    val chosen = borrowed[borrowedIndex]
                    continuation += chosen.chord
                    confidenceSum += 0.6f
                    currentChord = chosen.chord.guitarLabel()
                    stepCount++
                    variantSymbol = chosen.symbol
                } else {
                    val suggestions = ChordSuggestionEngine.suggest(
                        key = request.sourceAnalysis.key,
                        lastChordName = currentChord,
                        maxResults = ChordSuggestionEngine.FULL_DIATONIC_POOL
                    )
                    if (suggestions.isEmpty()) continue
                    val chosen = ChordCandidateSelector.select(
                        suggestions = suggestions,
                        targetFunction = targetFunction,
                        continuation = continuation,
                        usedChords = usedChords,
                        variantIndex = variantIndex
                    ) ?: continue
                    continuation += chosen.chord
                    confidenceSum += chosen.confidence
                    currentChord = chosen.chord.guitarLabel()
                    stepCount++
                    usedChords += chosen.chord
                }
            }

            if (continuation.isNotEmpty()) {
                results.add(
                    GeneratedProgression(
                        chords = continuation,
                        confidence = confidenceSum / stepCount.coerceAtLeast(1),
                        explanation = "Deceptive $variantSymbol: builds toward an expected " +
                                "resolution, then substitutes a borrowed chord at the last moment."
                    )
                )
            }
        }

        return results.distinctBy { it.chords.map { c -> c.label } }
    }
}