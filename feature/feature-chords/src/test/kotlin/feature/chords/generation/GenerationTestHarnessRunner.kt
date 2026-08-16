package com.reasontouch.feature.chords.generation

import org.junit.Test

/**
 * Not a real regression test -- just a runnable entry point for
 * GenerationTestHarness's println-based inspection output. Run this
 * from Android Studio (click the gutter icon) rather than relying on
 * assertions; there's nothing here to fail. See runKeyDetectorInvestigationSeeds()
 * for the seeds from the 2026-08-02 on-device Continue-hunt session.
 */
class GenerationTestHarnessRunner {

    @Test
    fun `run key detector investigation seeds`() {
        GenerationTestHarness.runKeyDetectorInvestigationSeeds()
    }

    @Test
    fun `run original harness seeds`() {
        GenerationTestHarness.runAllTests()
    }

    @Test
    fun `run lift investigation seeds`() {
        GenerationTestHarness.runLiftInvestigationSeeds()
    }

    @Test
    fun `run exhaustion rotation regression seeds`() {
        GenerationTestHarness.runExhaustionRotationRegressionSeeds()
    }

    @Test
    fun `run expand investigation seeds`() {
        GenerationTestHarness.runExpandInvestigationSeeds()
    }

    @Test
    fun `run surprise investigation seeds`() {
        GenerationTestHarness.runSurpriseInvestigationSeeds()
    }
}
