usi# ReasonTouch -- Early Design Decisions & Context

*Captured from the pre-project conversation. These decisions were made before development moved to the ReasonTouch project and may not be referenced there.*

---

## App Concept & Positioning

### Why This App Exists
ReasonTouch fills a specific gap: **compositional sketching with guitar voicing intelligence**. No Android DAW currently combines a guitar chord generator (with strum articulation physics) and a piano roll in a single unified app with direct data transfer between them.

### Comparison to Audio Evolution Mobile
Audio Evolution Mobile is the most capable Android DAW (in development since ~2012). It supports audio recording, VST plugins via desktop bridge, a full mixer with EQ/compression, and MIDI hardware controllers. ReasonTouch is not competing at the full DAW level.

**Where ReasonTouch is differentiated:**
- Guitar chord generator with strum simulation is unique -- no other Android DAW has this
- Strum engine with pattern sequencer and physics-accurate timing offsets is original
- Tight integration: chord -> piano roll without file export/import round trip
- Opinionated workflow: sketch chord-based compositions, export clean MIDI to Reason

### Target User
Guitarists and keyboard players who want to quickly sketch chord progressions with realistic strum articulation, layer piano roll arrangements, and export multi-track MIDI to Reason DAW or any other DAW.

---

## Key Architectural Decisions

### Send to Piano Roll -- Two Modes
The decision to offer **Block** and **Strum** modes when sending chord progression to piano roll was deliberate:

- **Block mode** -- all chord notes at the same beat position, duration = bar duration. Best for harmonic framework that will be manually edited in the piano roll.
- **Strum mode** -- applies the active step pattern and strum speed, offsetting each string by `strumSpeed * stringIndex`. Notes arrive in the piano roll already reflecting strum timing. Better for export but harder to edit meaningfully once in the grid.

Block mode is the more useful compositional tool. Strum mode is useful when you want the piano roll to exactly represent what the chord generator would have played.

### Why -> ROLL Button Lives in the Toolbar
The original design put the Send to Piano Roll button at the bottom of the chord progression list (after the bars). This caused a scroll accessibility problem -- users couldn't reach it because the LazyColumn wasn't scrolling far enough past the Add Bar button. Moving it to the toolbar (appearing when progression.isNotEmpty()) was the practical fix and also improves discoverability.

### ExportViewModel Factory Pattern
`ExportViewModel` intentionally does NOT use `@HiltViewModel`. Reason: it needs `sessionId` but is shown as a dialog from `ReasonTouchApp` (the app shell), not from a navigation destination that would provide a `SavedStateHandle`. Using `@HiltViewModel` here causes `IllegalStateException: Required value was null` because no nav back stack entry provides `sessionId`.

The factory pattern (`ViewModelProvider.Factory`) is the correct solution. Do NOT add `@HiltViewModel` to `ExportViewModel` -- this was a recurring bug during development.

### Piano Roll 6-File Refactor
The original `PianoRollScreen.kt` was a single monolithic file (~525 lines). It was refactored into 6 files after Kotlin overload resolution ambiguity errors caused by multiple `pointerInput` blocks with similar signatures in the same scope:

1. `PianoRollScreen.kt` -- high-level composition only
2. `PianoRollCanvas.kt` -- canvas composable
3. `PianoRollGestures.kt` -- `Modifier` extensions for gesture handling
4. `PianoRollInteractions.kt` -- business logic for user interactions
5. `PianoRollDrawing.kt` -- all `DrawScope` extension functions
6. `PianoRollUiState.kt` -- aggregated state data class

### TinySoundFont -- Phase 2
TinySoundFont was discussed as a replacement for the custom `SynthEngine` for sequenced playback. Decision: **retain the current synthesiser for real-time audition** (piano key taps, chord previews) because it has very low latency. Add TinySoundFont for sequenced playback (transport play) and export preview in Phase 2 because it gives General MIDI quality soundfonts.

The `SynthEngine` uses Android `AudioTrack` in static mode with a custom oscillator/filter/envelope renderer. It's functional but sounds synthetic. TinySoundFont would dramatically improve playback quality without breaking the audition path.

---

## Pitch Convention (Critical)
The piano roll uses an inverted pitch scheme where:
- `pitch = 0` -> top of 88-key range (C8, MIDI note 108)
- `pitch = 87` -> bottom of range (A0, MIDI note 21)
- Conversion: `midiNote = 108 - pitch`

This means when drawing piano keys, pitch 0 is at the top of the canvas and pitch 87 is at the bottom. The `PianoRollState` coordinate helpers handle this:
```kotlin
fun pitchToY(pitch: Int) = pitch * noteHeight - scrollY + headerHeight
fun yToPitch(y: Float) = ((y + scrollY - headerHeight) / noteHeight).toInt()
```

---

## PowerShell File Writing -- Critical Warning
During development, Kotlin source files were repeatedly corrupted by PowerShell regex replacement operations. The root cause: multi-line string replacements with `-replace` or `.Replace()` on large files would silently fail or partially apply, leaving files truncated or with duplicate class definitions.

**Safe approach:** Always use `[System.IO.File]::WriteAllText` with the complete file content. Never use incremental regex replacement on large Kotlin files. If a file needs significant changes, rewrite it completely.

Signs of file corruption to watch for:
- Build error: `Redeclaration: class X`
- Build error: `Unresolved reference` for things that are clearly declared in the same file
- File line count much lower than expected
- `Select-String "^class "` returns multiple hits in one file

**Note:** this warning proved directly relevant again in the 2026-08-01 session, where a `[System.IO.File]::WriteAllText` call (not a regex replace, but the same file-writing tool) introduced a UTF-8 BOM into `ProgressionAnalysis.kt`, and separately, long multi-document PowerShell heredocs proved unreliable for pasting this handoff-document series into `docs/handoffs/` -- several silently truncated or produced 0-byte files. The safe fallback that emerged: create the file via PowerShell (`New-Item`), then paste content directly in Android Studio's editor, which has no heredoc-length or console-encoding limitations.

---

## Features Implemented in Pre-Project Conversation

These were all confirmed working on device before the project was transferred:

| Feature | Status | Notes |
|---------|--------|-------|
| Session list + swipe-to-delete | DONE | `SwipeToDeleteSessionCard` in `SessionListScreen.kt` |
| Chord Generator UI | DONE | Category filter, chord grid, pattern grid, presets, settings |
| Chord audio on tap | DONE | `auditionChord` called from `selectChord` |
| Send to Piano Roll | DONE | Block + Strum modes, track selection dialog, -> ROLL toolbar button |
| Piano Roll canvas | DONE | 6-file refactor, gesture handling, drawing |
| Piano keys audio | DONE | `pianoKeyGestures` modifier, `auditionNote` |
| Track muting (M button) | DONE | `muteTrack` in ViewModel, M button in `TrackSelector` |
| Pinch zoom at centroid | DONE | `pianoRollZoomGestures` modifier |
| Loop region + draggable handles | DONE | L/R/BODY drag in `PianoRollGestures`, state in `PianoRollState` |
| Loop playback | DONE | Cycle-based scheduling in `play()`, re-schedules on each loop |
| Transport (<<, >, \|\|, []) | DONE | In `PianoRollToolbar`, wired to ViewModel |
| Auto scroll during playback | DONE | `LaunchedEffect(playheadBeat, isPlaying)` in `PianoRollScreen` |
| ERASE tool with drag | DONE | Separate drag handler in `PianoRollGestures` |
| MIDI export to Downloads | DONE | `ExportViewModel` factory pattern, `ExportDialog` |
| core-audio SynthEngine | DONE | AudioTrack static mode, SAW/SQUARE/SINE/TRIANGLE/PWM/FM voices |

---

## Features Discussed But Not Yet Implemented at Transfer

| Item | Priority | Notes |
|------|----------|-------|
| Velocity editing (drag on strip) | High | Drag on velocity bar to change note velocity |
| Rubber band selection | High | Drag to select multiple notes, then move/delete |
| Note duration resize | Medium | Drag right edge of note |
| Session rename | Medium | In SessionSettings screen |
| Landscape rotation for Arrange | Low | Layout adaptation needed |
| TinySoundFont integration | Phase 2 | For sequenced playback quality |
| MIDI import | Phase 2 | Discussed, not started |

---

## Git Commit History at Transfer Point

1. Initial scaffolding
2. core-data + navigation shell
3. feature-chords UI complete
4. feature-pianoroll complete
5. Send to Piano Roll working -- block and strum modes, track selection dialog
6. feature-export MIDI export to Downloads
7. core-audio synth engine complete
8. Round 1 complete -- swipe delete, piano keys audio, track muting, auto scroll, transport cleanup

*After commit 8, development moved to the ReasonTouch project.*

---

## Source References

- Original Guitar Chord App (ported): `C:\Users\Andy Meakin\Downloads\GuitarChordApp_Panda\GuitarChordPanda\`
- Browser prototype piano roll (reference): `/mnt/user-data/outputs/reason-piano-roll.html`
- Full session transcript: `/mnt/transcripts/2026-04-05-19-54-34-reason-touch-android-dev.txt`