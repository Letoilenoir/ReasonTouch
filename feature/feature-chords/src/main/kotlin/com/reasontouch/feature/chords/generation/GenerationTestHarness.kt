package com.reasontouch.feature.chords.generation

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.feature.chords.CompositionIntent
import com.reasontouch.feature.chords.KeyDetector
import com.reasontouch.feature.chords.PairingEngine
import com.reasontouch.feature.chords.ProgressionAnalyzer
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.ProgressionGenerator

object GenerationTestHarness {

    fun testSeedProgression(
        vararg chordNames: String,
        primaryIntent: CompositionIntent = CompositionIntent.CONTINUE,
        preferredLength: Int = 4
    ) {
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

        val pairingDecision = PairingEngine.suggestNext(sourceAnalysis)
        println("\nPairingEngine Decision (this is what the live SUGGEST dialog would show):")
        println("  Type: ${pairingDecision.type}")
        println("  Rationale: ${pairingDecision.rationale}")
        println("  Confidence: ${(pairingDecision.confidence * 100).toInt()}%")
        println("  Suggested bars: ${pairingDecision.suggestedBars}")

        val request = ProgressionGenerationRequest(
            sourceAnalysis = sourceAnalysis,
            sourceProgression = sourceProgression,
            primaryIntent = primaryIntent,
            targetSection = null,
            targetEnergy = null,
            preferredLength = preferredLength
        )

        println("\nGenerating $primaryIntent candidates...")
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

    /**
     * Re-runs the exact seeds from the 2026-08-02 on-device Continue-hunt
     * session (see docs/testing/Strategy_Test_Log.md) through the real
     * pipeline, printing detected key + stability + cadence for each.
     * Two of these (seeds 2 and 6) produced a PairingEngine-type mismatch
     * against hand-calculated predictions on-device -- this should confirm
     * or resolve that discrepancy without needing a physical device.
     *
     * Note: this only exercises ProgressionGenerator/CONTINUE candidate
     * generation, not PairingEngine's actual SUGGEST-type routing decision
     * (CONTINUE vs. CONTRAST vs. RESOLVE etc.) -- that lives in
     * PairingEngine.suggestNext(), not ProgressionGenerator. The printed
     * stability/cadence/functional-sequence values are what PairingEngine
     * would branch on, so cross-reference against PairingEngine.kt's
     * thresholds by hand, or extend this harness further to call
     * PairingEngine.suggestNext(sourceAnalysis) directly if that becomes
     * a recurring need.
     */
    fun runKeyDetectorInvestigationSeeds() {
        // Seed 1: C Am F -- predicted CONTRAST-range stability, confirmed on-device
        testSeedProgression("C", "Am", "F")

        // Seed 2: C Em Dm -- predicted CONTINUE (medium stability) by hand,
        // actual on-device result was CONTRAST. Unresolved discrepancy.
        testSeedProgression("C", "Em", "Dm")

        // Seed 3: Dm Em Am -- confirmed A minor via borrowed-chord labels
        // on-device (authentic v->i cadence, high stability)
        testSeedProgression("Dm", "Em", "Am")

        // Seed 4: G Dm Em -- confirmed C major on-device, high stability,
        // TONIC-function ending (Em = iii)
        testSeedProgression("G", "Dm", "Em")

        // Seed 5: C F G -- confirmed C major on-device, HALF cadence,
        // low stability, routed to RESOLVE
        testSeedProgression("C", "F", "G")

        // Seed 6: C G F -- predicted CONTINUE (medium stability) by hand,
        // actual on-device result was CONTRAST. Unresolved discrepancy,
        // same shape as seed 2.
        testSeedProgression("C", "G", "F")

        // Seed 7: G C D Am -- first seed to break out of C/Am ambiguity,
        // confirmed G major on-device via Bm/F#dim in generated output.
        // preferredLength was 8 on-device (via PairingDecision.suggestedBars);
        // testing here at both lengths for comparison.
        testSeedProgression("G", "C", "D", "Am")
        testSeedProgression("G", "C", "D", "Am", preferredLength = 8)
    }

    /**
     * Targets LIFT specifically -- PairingEngine.suggestAfterHighStability()
     * requires harmonicStability >= 0.8f AND tension > 0.6f. Per
     * ProgressionAnalyzer.calculateTension(), DOMINANT-function chords score
     * highest (0.8), so a dominant-heavy but still-stable progression should
     * trigger this branch. Also checks whether LiftStrategy's tight 2-per-
     * function diatonic ceiling (most keys have only 2 DOMINANT-function and
     * 2 PREDOMINANT-function diatonic chords) causes exhaustion sooner than
     * the already-logged TONIC-side exhaustion at preferredLength=8 -- see
     * docs/testing/Strategy_Test_Log.md, 2026-08-03.
     */
    fun runLiftInvestigationSeeds() {
        testSeedProgression("G", "Bdim", "G", "C", primaryIntent = CompositionIntent.LIFT)
        testSeedProgression(
            "G", "Bdim", "G", "C",
            primaryIntent = CompositionIntent.LIFT,
            preferredLength = 8
        )
    }
}