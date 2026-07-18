package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.MusicTheory
import com.reasontouch.feature.chords.ProgressionAnalysis
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.TheoryChord
import com.reasontouch.feature.chords.guitarLabel

object ContinueStrategy {
    fun generate(
        request: ProgressionGenerationRequest
    ): List<GeneratedProgression> {
        val source = request.sourceProgression
        if (source.isEmpty()) return emptyList()

        val trajectory = ContinueTargeting.deriveTrajectory(
            analysis = request.sourceAnalysis,
            phraseLength = request.preferredLength
        )
        android.util.Log.d("ContinueDebug", "Trajectory: $trajectory")
        val continuation = mutableListOf<TheoryChord>()
        var currentChord = source.last().chordName
        var confidenceSum = 0f

        repeat(request.preferredLength) { position ->
            val suggestions = ChordSuggestionEngine.suggest(
                key = request.sourceAnalysis.key,
                lastChordName = currentChord
            )
            if (suggestions.isEmpty()) return@repeat

            val targetFunction = trajectory.getOrNull(position)
            val chosen = suggestions.firstOrNull { it.function == targetFunction }
                ?: suggestions.first()
            android.util.Log.d("ContinueDebug", "Position $position target=$targetFunction chosen=${chosen.chord.label} (${chosen.function}, degree=${chosen.degree})")


            continuation += chosen.chord
            confidenceSum += chosen.confidence
            currentChord = chosen.chord.guitarLabel()
        }

        if (continuation.isEmpty()) return emptyList()

        return listOf(
            GeneratedProgression(
                chords = continuation,
                confidence = confidenceSum / continuation.size,
                explanation = explanationFor(request.sourceAnalysis)
            )
        )
    }

    private fun explanationFor(analysis: ProgressionAnalysis): String {
        return when {
            analysis.endsOnDominant() ->
                "Resolves the preceding dominant into a settled continuation."
            analysis.isClosed() ->
                "Reopens harmonic movement after a resolved phrase."
            analysis.isOscillatingProgression() ->
                "Breaks the oscillating pattern with directional movement."
            analysis.isDescendingProgression() ->
                "Continues the descending harmonic motion."
            else ->
                "Continues naturally from the preceding phrase."
        }
    }
}