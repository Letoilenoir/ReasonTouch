package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.TheoryChord
import com.reasontouch.feature.chords.guitarLabel

object LiftStrategy {
    private val VARIANT_LABELS = listOf(
        "Driving Alternation", "Alternate Voicing", "Extended Approach", "Contrasting Approach"
    )

    fun generate(request: ProgressionGenerationRequest): List<GeneratedProgression> {
        val source = request.sourceProgression
        if (source.isEmpty()) return emptyList()

        val trajectory = LiftTargeting.deriveTrajectory(
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
                val matching = suggestions.filter { it.function == targetFunction }
                val pool = matching.ifEmpty { suggestions }

                // Prefer chords not already used earlier in this continuation,
                // falling back to the full function-matched pool if excluding
                // used chords would leave nothing -- same pattern as
                // Continue/Contrast/Resolve. Lift is the strategy most likely
                // to hit genuine diatonic exhaustion (only 2 candidates exist
                // per non-tonic function in most keys, alternating for a full
                // 8-bar phrase), so this fallback matters here more than
                // anywhere else -- see docs/testing/Strategy_Test_Log.md,
                // 2026-08-03 tonic-exhaustion finding, for the related but
                // distinct TONIC-side version of this same limit.
                val unusedPool = pool.filterNot { it.chord in usedChords }
                val finalPool = unusedPool.ifEmpty { pool }
                val rankForThisVariant = variantIndex.coerceAtMost(finalPool.size - 1)
                val chosen = finalPool[rankForThisVariant]

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
                        explanation = "${VARIANT_LABELS[variantIndex]}: sustains the phrase's " +
                                "existing energy with continuous harmonic motion, avoiding a tonic landing."
                    )
                )
            }
        }

        return results.distinctBy { it.chords.map { c -> c.label } }
    }
}