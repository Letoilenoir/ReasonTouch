# Task List - Strict Per-Track Isolation for .rpack Sound Expansion

- [x] Update `SoundPackManager.resolveSample()` to strictly enforce `manifest.tracks[trackName]` presence and mapping
- [x] Update `Sf2Player.kt` and `DrumSamplePlayer.kt` to accept `trackName` and pass into `resolveSample()`
- [x] Update `Sequencer.kt` to pass `track.name` into audio player calls
- [x] Run gradle build and verify compilation (`gradle_build("app:assembleDebug")`)
- [x] Create walkthrough artifact summarizing changes
