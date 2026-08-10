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
        // First attempt (2026-08-03): does NOT route to LIFT via real
        // PairingEngine. All-plain-triad progressions cap tension at 0.55
        // (a lone DOMINANT chord's max), never clearing the > 0.6f
        // threshold, since qualityTension for plain MAJ/MIN triads is only
        // 0.3 -- there is no diatonic quality available strong enough to
        // push the average past the line without at least one DIM chord.
        // Kept here as a documented negative example, not a working seed.
        testSeedProgression("G", "Bdim", "G", "C", primaryIntent = CompositionIntent.LIFT)
        testSeedProgression(
            "G", "Bdim", "G", "C",
            primaryIntent = CompositionIntent.LIFT,
            preferredLength = 8
        )

        // Corrected seed (2026-08-03): leans on the diminished vii° chord
        // (Bdim, degree 7) specifically, since DIM's qualityTension (0.9)
        // is the only diatonic quality high enough to clear the 0.6f
        // average threshold. Hand-traced: stability ~0.94 (ends TONIC),
        // tension ~0.625 (avg of Bdim/G/Bdim/C chord tensions) -- should
        // route to LIFT via suggestAfterHighStability()'s tension branch.
        testSeedProgression("Bdim", "G", "Bdim", "C", primaryIntent = CompositionIntent.LIFT)
        testSeedProgression(
            "Bdim", "G", "Bdim", "C",
            primaryIntent = CompositionIntent.LIFT,
            preferredLength = 8
        )
    }

    /**
     * Targeted re-verification of the exhaustion-rotation fix (2026-08-04,
     * see docs/handoffs/ReasonTouch_Handoff_2026-08-04_LiftStrategy_ExhaustionFix.md)
     * for CONTRAST and RESOLVE specifically, at preferredLength=8 -- the
     * condition that actually triggers pool exhaustion. LiftStrategy and
     * ContinueStrategy have both already been re-confirmed post-fix; these
     * two seeds close the gap for the remaining strategies.
     *
     * Am F G C is close to the original seed that first surfaced the
     * Am-Am-Am-Am / C-C-C-C frozen-tail pattern during CONTRAST testing
     * (2026-08-01/03 sessions). C Am F G is RESOLVE's own original
     * problem seed from the same period. Both are direct before/after
     * comparisons against known-bad pre-fix behavior.
     */
    fun runExhaustionRotationRegressionSeeds() {
        testSeedProgression(
            "Am", "F", "G", "C",
            primaryIntent = CompositionIntent.CONTRAST,
            preferredLength = 8
        )
        testSeedProgression(
            "C", "Am", "F", "G",
            primaryIntent = CompositionIntent.RESOLVE,
            preferredLength = 8
        )
    }

    /**
     * Targets EXPAND's two PairingEngine trigger conditions
     * (suggestAfterMediumStability): HALF cadence ("Ends on V -> explore
     * harmonically") and the medium-stability default ("Medium stability
     * -> harmonic exploration").
     *
     * IMPORTANT open question this seed set is designed to answer: HALF
     * cadence's functionStability contribution is inherently low (0.15,
     * per calculateHarmonicStability()'s functionStability table) --
     * hand math suggests a HALF-cadence seed may structurally never
     * reach medium stability (0.4-0.7) regardless of other bonuses,
     * meaning it would always fall into suggestAfterLowStability() and
     * route to RESOLVE instead of ever reaching EXPAND's HALF-cadence
     * branch. If seed 1 below reports RESOLVE rather than EXPAND, that
     * confirms EXPAND's HALF-cadence rationale is dead code via the live
     * PairingEngine path, same category of finding as the already-
     * confirmed-dead SIMPLIFY branch and RESOLVE's DECEPTIVE branch --
     * worth logging in Strategy_Test_Log.md / Task_List.md if so.
     */
    fun runExpandInvestigationSeeds() {
        // Seed 1: targets HALF cadence. Hand-traced: Dm -> G, ends on G
        // (DOMINANT, HALF cadence), functionStability=0.15, cadence
        // bonus -0.10, no tonic chords present -> stability ~0.05.
        // Expected to land in LOW stability, not medium -- likely to
        // route to RESOLVE, not EXPAND. See doc comment above.
        testSeedProgression("Dm", "G", primaryIntent = CompositionIntent.EXPAND)

        // Seed 2: targets the medium-stability default branch. Hand-
        // traced: C Am Dm Em F, ends on F (PREDOMINANT),
        // functionStability=0.45, Em->F cadence=INTERRUPTED (-0.05),
        // 3 of 5 chords are TONIC-function (C, Am, Em) -> tonic bonus
        // +0.09 -> stability ~0.49 (medium range), barCount=5 (>4),
        // cadence not HALF/DECEPTIVE -> should hit the else branch,
        // routing to EXPAND.
        testSeedProgression(
            "C", "Am", "Dm", "Em", "F",
            primaryIntent = CompositionIntent.EXPAND
        )
        testSeedProgression(
            "C", "Am", "Dm", "Em", "F",
            primaryIntent = CompositionIntent.EXPAND,
            preferredLength = 8
        )
    }
}