package com.reasontouch.feature.chords.generation

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.feature.chords.CompositionIntent
import com.reasontouch.feature.chords.KeyDetector
import com.reasontouch.feature.chords.ProgressionAnalyzer
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.ProgressionGenerator

object GenerationTestHarness {

    fun testSeedProgression(vararg chordNames: String) {
        println("\n" + "=".repeat(60))
        println("TEST: Seed Progression")
        println("=".repeat(60))

        println("\nSeed: ${chordNames.joinToString(" > ")}")

        val sourceProgression = chordNames.mapIndexed { index, name ->
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

        val detectedKeys = KeyDetector.detect(chordNames.toList())
        val detectedKey = detectedKeys.firstOrNull() ?: run {
            println("Could not detect key")
            return
        }

        println("Detected Key: ${detectedKey.root} ${if (detectedKey.isMinor) "minor" else "major"}")

        val sourceAnalysis = ProgressionAnalyzer.analyze(sourceProgression, detectedKey)

        println("\nSeed Analysis:")
        println("  Stability: ${(sourceAnalysis.harmonicStability * 100).toInt()}%")
        println("  Functions: ${sourceAnalysis.functionalSequence.joinToString(" > ")}")
        println("  Cadence: ${sourceAnalysis.cadenceType}")

        val request = ProgressionGenerationRequest(
            sourceAnalysis = sourceAnalysis,
            sourceProgression = sourceProgression,
            primaryIntent = CompositionIntent.CONTINUE,
            targetSection = null,
            targetEnergy = null,
            preferredLength = 4
        )

        println("\nGenerating CONTINUE candidates...")
        val candidates = ProgressionGenerator.generate(request)

        if (candidates.isEmpty()) {
            println("No candidates generated")
            return
        }

        println("Generated ${candidates.size} candidate(s)\n")

        candidates.forEachIndexed { idx, candidate ->
            val chordString = candidate.chords.joinToString(" > ") { it.label }
            val fullProgression = chordNames.joinToString(" > ") + " > " + chordString

            println("Candidate ${idx + 1}: $fullProgression")
            println("  Chords: $chordString")
            println("  Confidence: ${(candidate.confidence * 100).toInt()}%")
            println("  Reason: ${candidate.explanation}")
            println()
        }
    }

    fun runAllTests() {
        testSeedProgression("C", "Am", "Dm", "G")
        testSeedProgression("Em", "C", "G", "D")
        testSeedProgression("Am", "F", "C", "G")
    }
}