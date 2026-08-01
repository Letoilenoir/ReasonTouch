# ReasonTouch -- Project Handoff Document

**Last updated:** April 2026
**Build status:** BUILD SUCCESSFUL
**Platform:** Android API 29+, Kotlin + Jetpack Compose + Room + Hilt
**Project path:** `C:\Users\Andy Meakin\AndroidStudioProjects\ReasonTouch\`

---

## What This App Does

ReasonTouch is a unified Android composition tool combining a **Guitar Chord Generator** (with strum simulation) and a **multi-track Piano Roll** arranger. It exports multi-track MIDI files compatible with Reason DAW and other DAWs. The key differentiator is the tight integration between guitar-voicing intelligence and a piano roll -- no other Android app does this combination.

---

## Architecture

Multi-module Gradle project:

| Module | Purpose |
|--------|---------|
| `:app` | Shell, navigation, MainActivity, SessionList, TransportBar, BottomNav |
| `:core:core-data` | Room DB, entities, DAOs, SessionRepository |
| `:core:core-midi` | MidiFileWriter, MultiTrackMidiWriter, MidiProgressionBar, StepState |
| `:core:core-audio` | SynthEngine, SampleRenderer, SynthVoice, AudioModule (Hilt) |
| `:core:core-ui` | Shared UI (minimal currently) |
| `:feature:feature-chords` | GuitarVoicings, StrumData, ChordViewModel, ChordScreen |
| `:feature:feature-pianoroll` | PianoRollViewModel, PianoRollScreen, PianoRollState, PianoRollCanvas, PianoRollDrawing, PianoRollGestures, PianoRollInteractions, PianoRollUiState |
| `:feature:feature-export` | ExportViewModel (factory-based, no Hilt), ExportDialog |

**Build config:** AGP 8.10.0, Kotlin 2.1.20, KSP 2.1.20-2.0.0, Gradle 9.3.1, Java 17, compileSdk 36, minSdk 29

---

## Key Files

### App Shell
- `app/.../MainActivity.kt` -- injects `SessionRepository`, passes to `ReasonTouchApp`
- `app/.../ReasonTouchApp.kt` -- Nav host, transport bar, bottom nav, export dialog
- `app/.../SessionListScreen.kt` -- Swipe-to-delete sessions, create new session dialog
- `app/.../TransportBar.kt` -- BPM + MIDI export button
- `app/.../BottomNav.kt` -- CHORDS / ARRANGE tabs
- `app/.../Screen.kt` -- Navigation routes

### Core Data
- `core-data/.../Session.kt` -- id, name, bpm, keyRoot, keyQuality, timeSignature, totalBars, defaultStrumSpeed, strumSimulationEnabled, gmProgram
- `core-data/.../MidiTrack.kt` -- id, sessionId, index, name, voice, color, midiChannel, muted, solo, volume
- `core-data/.../NoteEvent.kt` -- id, trackId, pitch, beat, duration, velocity
- `core-data/.../ChordEvent.kt` -- id, sessionId, barIndex, chordName, rootMidi, midiNotes, voicing, strumPatternId
- `core-data/.../SessionRepository.kt` -- All DB operations
- `core-data/.../DatabaseModule.kt` -- Hilt module providing Room DB

### Core Audio
- `core-audio/.../SynthEngine.kt` -- AudioTrack static mode
- `core-audio/.../SampleRenderer.kt` -- Float PCM sample rendering
- `core-audio/.../SynthVoice.kt` -- SAW, SQUARE, SINE, TRIANGLE, PWM, FM
- `core-audio/.../AudioModule.kt` -- Hilt singleton provider

### Core MIDI
- `core-midi/.../MultiTrackMidiWriter.kt` -- Format 1 MIDI
- `core-midi/.../MidiProgressionBar.kt` -- Data class for chord export
- `core-midi/.../StepState.kt` -- OFF, DOWN, UP enum

### Feature Chords
- `feature-chords/.../ChordScreen.kt`, `ChordViewModel.kt`, `GuitarVoicings.kt` (63 chords, 8 categories), `StrumData.kt` (18 patterns), `StrumPatterns.kt`

### Feature Piano Roll (refactored into 6 files)
- `PianoRollScreen.kt`, `PianoRollCanvas.kt`, `PianoRollGestures.kt`, `PianoRollInteractions.kt`, `PianoRollDrawing.kt`, `PianoRollUiState.kt`, `PianoRollState.kt`, `PianoRollViewModel.kt`

### Feature Export
- `ExportViewModel.kt` -- Factory pattern (no Hilt), `ExportDialog.kt`

---

## Data Model

### Pitch Convention
Piano roll uses `pitch` where `0 = top of 88-key range (C8)` and `108 - pitch = MIDI note number`. Middle C (MIDI 60) = pitch 48. A4 (MIDI 69) = pitch 39.

### Default Tracks Per Session
Sessions created with 4 tracks: BASS, LEAD, CHORD, PAD.

---

## Navigation

SessionList -> Chords/{sessionId} / PianoRoll/{sessionId}. Bottom nav switches between Chords and PianoRoll within the same session, sharing Room session data.

---

## Working Features (Confirmed on Device, April 2026)

- Session list with swipe-to-delete; create/open sessions
- Chord Generator: category filter, chord grid, voicing selector, 16-step pattern grid, presets, settings panel, audio on tap, send-to-Roll (Block/Strum)
- Piano Roll: canvas grid with piano keys, track selector with mute, velocity strip, pinch zoom, loop region with draggable handles, loop playback, MIDI export, auto scroll, transport, DRAW/ERASE/SELECT tools, ghost notes, track muting
- MIDI Export: ExportDialog, saves to Downloads, covers chord progression + all piano roll tracks

---

## ExportViewModel -- Important Note

Does NOT use `@HiltViewModel` or `SavedStateHandle`. Uses a manual `ViewModelProvider.Factory`. Do NOT add `@HiltViewModel` -- will crash with `IllegalStateException` since there is no nav back stack entry providing `sessionId` to `SavedStateHandle`.

---

## Audio Engine

`SynthEngine` uses Android `AudioTrack` static mode for low-latency preview, rendering via `SampleRenderer` (oscillator + one-pole LP filter + ADSR envelope + optional FM). Phase 2 planned: TinySoundFont integration for General MIDI quality playback.

---

## Remaining Items (Pending, as of April 2026)

Velocity editing, note duration editing, rubber band selection, landscape rotation for Arrange, session settings screen, TinySoundFont upgrade, MIDI import, chord progression ghost lane in piano roll.

**Note:** most of the above have since been superseded/completed by later sessions -- see subsequent handoff documents in this folder for current state.

---

## Known Issues / Gotchas

1. File write safety in PowerShell -- never regex-replace large Kotlin files; use `[System.IO.File]::WriteAllText` with a complete rewrite.
2. PianoRollScreen file history -- corrupted several times (ViewModel code accidentally written into Screen file); restore from the 6-file refactor structure if it breaks again.
3. ChordScreen LazyColumn structure -- all `item {}`/`items()` calls must be direct children of `LazyColumn`.
4. SettingsPanel in ChordScreen -- single `item {}` wrapping everything.
5. Send to Piano Roll dialog -- `showSendDialog` state lives in `ChordScreen`, not `ChordPanel`.
6. Scroll on Chord screen -- `LazyColumn` has `contentPadding = PaddingValues(bottom = 120.dp)`.

---

## Dependency Versions (gradle/libs.versions.toml)

agp 8.10.0, kotlin 2.1.20, ksp 2.1.20-2.0.0, hilt 2.51.1, room 2.7.0, compose-bom 2025.02.00, lifecycle 2.8.7, navigation-compose 2.8.9, hilt-navigation-compose 1.2.0, coroutines 1.9.0

---

*Note: project has since moved to `C:\Users\andym\StudioProjects\ReasonTouch` on a new development machine (see 2026-07 handoffs for environment migration notes).*