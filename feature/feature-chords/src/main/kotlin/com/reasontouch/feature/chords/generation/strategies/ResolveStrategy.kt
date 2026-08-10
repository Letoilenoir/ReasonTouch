package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.CadenceType
import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.ProgressionAnalysis
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.TheoryChord
import com.reasontouch.feature.chords.guitarLabel

object ResolveStrategy {
    private val VARIANT_LABELS = listOf(
        "Direct Cadence", "Alternate Voicing", "Extended Approach", "Contrasting Approach"
    )

    fun generate(request: ProgressionGenerationRequest): List<GeneratedProgression> {
        val source = request.sourceProgression
        if (source.isEmpty()) return emptyList()

        val trajectory = ResolveTargeting.deriveTrajectory(
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

                val isFinalStep = position == request.preferredLength - 1
                // On the final step, insist on TONIC regardless of what the
                // trajectory said — this is Resolve's actual guarantee.
                // Every other step follows the planned trajectory as usual.
                val targetFunction = if (isFinalStep) {
                    HarmonicFunction.TONIC
                } else {
                    trajectory.getOrNull(position)
                }

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
                "resolves the pending dominant with a confirmed cadence home."
            analysis.cadenceType == CadenceType.DECEPTIVE ->
                "completes the resolution the deceptive cadence deferred."
            analysis.isClosed() ->
                "reopens briefly before landing on a firm, deliberate cadence."
            else ->
                "brings the open phrase home with a full cadential run."
        }
    }
}