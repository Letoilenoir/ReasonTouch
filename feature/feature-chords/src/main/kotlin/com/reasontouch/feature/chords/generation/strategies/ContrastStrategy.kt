package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.ProgressionAnalysis
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
                } else {
                    val suggestions = ChordSuggestionEngine.suggest(
                        key = request.sourceAnalysis.key,
                        lastChordName = currentChord
                    )
                    if (suggestions.isEmpty()) continue

                    val matching = suggestions.filter { it.function == targetFunction }
                    val pool = matching.ifEmpty { suggestions }
                    val rank = variantIndex.coerceAtMost(pool.size - 1)
                    val chosen = pool[rank]

                    continuation += chosen.chord
                    confidenceSum += chosen.confidence
                    currentChord = chosen.chord.guitarLabel()
                    stepCount++
                }
            }

            if (continuation.isNotEmpty()) {
                val openingChordLabel = continuation.firstOrNull()?.label ?: ""
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

    private fun explanationFor(analysis: ProgressionAnalysis): String {
        return "introduces a borrowed chord for genuine character change after a resolved, stable phrase."
    }
}