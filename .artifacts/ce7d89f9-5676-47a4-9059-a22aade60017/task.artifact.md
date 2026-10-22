# Task List - Explicit SampleResolution Pipeline for .rpack Sound Expansion

- [x] Create `SampleResolution.kt` sealed interface in `core-audio`
- [x] Refactor `SoundPackManager.kt` to pre-build validated note indexes during import and expose `resolveSample()`
- [x] Update `Sf2Player.kt` and `DrumSamplePlayer.kt` to use explicit `resolveSample()` queries
- [x] Run gradle build and verify compilation (`gradle_build("app:assembleDebug")`)
- [x] Create walkthrough artifact summarizing changes
