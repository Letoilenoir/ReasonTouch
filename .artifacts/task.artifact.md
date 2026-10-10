# Task List - GM-Native Drum Fills & Playback Fallback

- `[x]` Update `DrumFillGenerator.kt` to use genuine General MIDI drum codes (Toms, Crashes, Snares) for accurate MIDI export
- `[x]` Update `DrumSamplePlayer.kt` to add safe playback fallback mapping (unbuffered GM notes like Toms/Crashes gracefully map to Snare/Ride during playback)
- `[x]` Run gradle build and verify compilation (`gradle_build("app:assembleDebug")`)
- `[x]` Create walkthrough artifact summarizing changes
