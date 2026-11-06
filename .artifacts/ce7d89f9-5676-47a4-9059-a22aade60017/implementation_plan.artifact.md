# Implementation Plan: Generalize Transpose & Groove Controls Across All Track Inspectors

Generalize the transpose facility and integrate `GrooveModifierState` controls (`InspectorControlPanel`) across all pitched track inspectors (BASS, LEAD, CHORD, PAD) while omitting transpose for DRUMS using `InspectorState.isPitched`.

## User Review Required

> [!IMPORTANT]
> **Architecture & Integration:**
> 1. **`InspectorControlPanel` Integration:**
   - Embed `InspectorControlPanel` into `ChordInspectorDialog`, `BassInspectorDialog`, `LeadInspectorDialog`, `PadInspectorDialog`, and `DrumInspectorDialog`.
   - `InspectorState` manages `trackId`, `transposeSemitones`, and `grooveState`, automatically hiding transpose controls for DRUMS (`isPitched = false`).
2. **ViewModel Wiring:**
   - Wire `onTranspose` to `chordViewModel.transposeBar(barIndex, delta)`.
   - Wire `onGrooveChange` to update groove settings in the ViewModel / repository.

## Proposed Changes

### [feature-chords] & [feature-pianoroll]

#### [ChordInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/ChordInspectorDialog.kt)
- Integrate `InspectorControlPanel`.

#### [BassInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/BassInspectorDialog.kt)
- Integrate `InspectorControlPanel`.

#### [LeadInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/LeadInspectorDialog.kt)
- Integrate `InspectorControlPanel`.

#### [PadInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/PadInspectorDialog.kt)
- Integrate `InspectorControlPanel`.

#### [DrumInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-pianoroll/src/main/kotlin/com/reasontouch/feature/pianoroll/components/DrumInspectorDialog.kt)
- Integrate `InspectorControlPanel` (unpitched / DRUMS track, omitting transpose).

#### [PianoRollScreen.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-pianoroll/src/main/kotlin/com/reasontouch/feature/pianoroll/PianoRollScreen.kt)
- Pass `InspectorState`, `onTranspose`, and `onGrooveChange` to all track inspector dialog invocations.

## Verification Plan

### Automated Tests
- Gradle build verification via `gradle_build("app:assembleDebug")`.

### Manual Verification
- Deploy to app, open bar inspectors for BASS, LEAD, CHORD, PAD, and DRUMS, verifying that transpose controls appear for pitched tracks and are omitted for DRUMS, and that Grid Subdivision and Paradiddle/Modifier controls function correctly.
