package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.TheoryChord
import com.reasontouch.feature.chords.guitarLabel

object ContrastStrategy {

    fun generate(request: ProgressionGenerationRequest): List<GeneratedProgression> {
        val source = request.sourceProgression
        if (source.isEmpty()) return emptyList()

        val plan = ContrastTargeting.derivePlan(
            analysis = request.sourceAnalysis,
            phraseLength = request.preferredLength
        )
        if (plan.trajectory.isEmpty()) return emptyList()

        val borrowed = ChordSuggestionEngine.borrowedChords(request.sourceAnalysis.key)

        val results = mutableListOf<GeneratedProgression>()

        for (variantIndex in borrowed.indices) {
            val continuation = mutableListOf<TheoryChord>()
            val usedChords = mutableSetOf<TheoryChord>()
            var currentChord = source.last().chordName
            var confidenceSum = 0f
            var stepCount = 0
            var variantSymbol = ""
            for ((position, targetFunction) in plan.trajectory.withIndex()) {
                val useBorrowedHere = position == plan.borrowedChordPosition && borrowed.isNotEmpty()
                if (useBorrowedHere) {
                    val borrowedIndex = variantIndex.coerceAtMost(borrowed.size - 1)
                    val chosen = borrowed[borrowedIndex]
                    continuation += chosen.chord
                    confidenceSum += 0.6f
                    currentChord = chosen.chord.guitarLabel()
                    stepCount++
                    variantSymbol = chosen.symbol
                    usedChords += chosen.chord
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
                val openingChordLabel = continuation.firstOrNull()?.guitarLabel() ?: ""
                results.add(
                    GeneratedProgression(
                        chords = continuation,
                        confidence = confidenceSum / stepCount.coerceAtLeast(1),
                        explanation = "Borrowed $variantSymbol: opens on $openingChordLabel, " +
                                "introducing a borrowed chord for genuine character change."
                    )
                )
            }
        }
        return results.distinctBy { it.chords.map { c -> c.label } }
    }
}

