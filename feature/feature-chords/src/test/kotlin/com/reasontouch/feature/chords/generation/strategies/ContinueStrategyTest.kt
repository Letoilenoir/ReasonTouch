package com.reasontouch.feature.chords.generation.strategies

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.feature.chords.ChordSuggestionEngine
import com.reasontouch.feature.chords.CompositionIntent
import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.KeyDetector
import com.reasontouch.feature.chords.ProgressionAnalyzer
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.ProgressionGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueStrategyTest {

    private fun buildProgression(vararg chordNames: String): List<ChordEvent> =
        chordNames.mapIndexed { index, name ->
            ChordEvent(
                id = "test-$index",
                sessionId = "test-session",
                barIndex = index,
                chordName = name,
                rootMidi = 60,
                midiNotes = "60,64,67",
                voicing = "root",
                strumPatternId = null
            )
        }

    @Test
    fun `dominant can transition to predominant (deceptive cadence)`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()

        val suggestions = ChordSuggestionEngine.suggest(key = key, lastChordName = "G")

        assertTrue(
            "Expected at least one PREDOMINANT suggestion after a DOMINANT chord",
            suggestions.any { it.function == HarmonicFunction.PREDOMINANT }
        )
    }

    @Test
    fun `tonic can transition to tonic (relative motion)`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()

        val suggestions = ChordSuggestionEngine.suggest(key = key, lastChordName = "C")

        assertTrue(
            "Expected at least one TONIC suggestion after a TONIC chord",
            suggestions.any { it.function == HarmonicFunction.TONIC }
        )
    }

    @Test
    fun `degree 1 does not exclusively dominate tonic suggestions`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()

        val suggestions = ChordSuggestionEngine.suggest(key = key, lastChordName = "G")
        val tonicDegrees = suggestions
            .filter { it.function == HarmonicFunction.TONIC }
            .map { it.degree }
            .toSet()

        assertTrue(
            "Expected more than just degree 1 among TONIC suggestions, got degrees: $tonicDegrees",
            tonicDegrees.size > 1
        )
    }

    @Test
    fun `oscillating progression yields a directional trajectory, not a repeat of the same shape`() {
        val progression = buildProgression("C", "G", "C", "G")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()
        val analysis = ProgressionAnalyzer.analyze(progression, key)

        assertTrue(
            "Expected the seed progression to be detected as oscillating",
            analysis.isOscillatingProgression()
        )

        val trajectory = ContinueTargeting.deriveTrajectory(analysis, phraseLength = 4)

        val isPlainAlternation = trajectory.zipWithNext().all { (a, b) -> a != b } &&
            trajectory.distinct().size <= 2 &&
            trajectory[0] == trajectory[2] && trajectory[1] == trajectory[3]

        assertTrue(
            "Expected trajectory to break the oscillation, got: $trajectory",
            !isPlainAlternation
        )
    }

    @Test
    fun `closed progression yields a reopening trajectory starting away from tonic`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()
        val analysis = ProgressionAnalyzer.analyze(progression, key)

        assertTrue(
            "Expected the seed progression to be detected as closed",
            analysis.isClosed()
        )

        val trajectory = ContinueTargeting.deriveTrajectory(analysis, phraseLength = 4)

        assertEquals(
            "Expected reopening trajectory to not start on TONIC",
            false,
            trajectory.firstOrNull() == HarmonicFunction.TONIC
        )
    }

    @Test
    fun `trajectory length matches requested phrase length`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()
        val analysis = ProgressionAnalyzer.analyze(progression, key)

        val trajectory = ContinueTargeting.deriveTrajectory(analysis, phraseLength = 4)

        assertEquals(4, trajectory.size)
    }

    @Test
    fun `zero phrase length yields an empty trajectory`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()
        val analysis = ProgressionAnalyzer.analyze(progression, key)

        val trajectory = ContinueTargeting.deriveTrajectory(analysis, phraseLength = 0)

        assertTrue(trajectory.isEmpty())
    }

    @Test
    fun `F G C C seed does not collapse into a two-chord GCGC loop`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()
        val analysis = ProgressionAnalyzer.analyze(progression, key)

        val request = ProgressionGenerationRequest(
            sourceAnalysis = analysis,
            sourceProgression = progression,
            primaryIntent = CompositionIntent.CONTINUE,
            targetSection = null,
            targetEnergy = null,
            preferredLength = 4
        )

        val candidates = ProgressionGenerator.generate(request)

        assertTrue("Expected at least one candidate", candidates.isNotEmpty())

        val labels = candidates.first().chords.map { it.label }
        val isGCGCPattern = labels.size == 4 &&
            labels[0] == labels[2] && labels[1] == labels[3] && labels[0] != labels[1]

        assertTrue(
            "First candidate collapsed into the known GCGC bug pattern: $labels",
            !isGCGCPattern
        )
    }

    @Test
    fun `continue generates multiple distinct candidates when the harmony supports it`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()
        val analysis = ProgressionAnalyzer.analyze(progression, key)

        val request = ProgressionGenerationRequest(
            sourceAnalysis = analysis,
            sourceProgression = progression,
            primaryIntent = CompositionIntent.CONTINUE,
            targetSection = null,
            targetEnergy = null,
            preferredLength = 4
        )

        val candidates = ProgressionGenerator.generate(request)

        assertTrue(
            "Expected more than one distinct candidate, got: ${candidates.size}",
            candidates.size > 1
        )
    }

    @Test
    fun `empty source progression yields no candidates`() {
        val request = ProgressionGenerationRequest(
            sourceAnalysis = ProgressionAnalyzer.analyze(
                emptyList(),
                KeyDetector.detect(listOf("C")).first()
            ),
            sourceProgression = emptyList(),
            primaryIntent = CompositionIntent.CONTINUE,
            targetSection = null,
            targetEnergy = null,
            preferredLength = 4
        )

        val candidates = ProgressionGenerator.generate(request)

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `contrast opens with a borrowed chord and generates exactly the requested length`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()
        val analysis = ProgressionAnalyzer.analyze(progression, key)

        val request = ProgressionGenerationRequest(
            sourceAnalysis = analysis,
            sourceProgression = progression,
            primaryIntent = CompositionIntent.CONTRAST,
            targetSection = null,
            targetEnergy = null,
            preferredLength = 4
        )

        val candidates = ProgressionGenerator.generate(request)

        assertTrue("Expected at least one CONTRAST candidate", candidates.isNotEmpty())
        candidates.forEach { candidate ->
            assertEquals("Expected exactly 4 chords", 4, candidate.chords.size)
        }
    }

    @Test
    fun `contrast produces three distinct borrowed-chord variants`() {
        val progression = buildProgression("F", "G", "C", "C")
        val key = KeyDetector.detect(progression.map { it.chordName }).first()
        val analysis = ProgressionAnalyzer.analyze(progression, key)

        val request = ProgressionGenerationRequest(
            sourceAnalysis = analysis,
            sourceProgression = progression,
            primaryIntent = CompositionIntent.CONTRAST,
            targetSection = null,
            targetEnergy = null,
            preferredLength = 4
        )

        val candidates = ProgressionGenerator.generate(request)

        assertEquals(
            "Expected exactly 3 distinct borrowed-chord variants (bVII, iv, bVI)",
            3,
            candidates.size
        )
    }

}
