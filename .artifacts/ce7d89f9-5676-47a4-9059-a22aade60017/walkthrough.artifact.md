# Walkthrough - Generalize Transpose & Groove Controls Across All Track Inspectors

Successfully implemented the Inspector control panel UI components and generalized transpose & groove modifier state handlers across all pitched track inspectors (BASS, LEAD, CHORD, PAD) while keeping transpose hidden for DRUMS.

## Changes

### [core-ui] & [feature-chords] & [feature-pianoroll]

#### [InspectorControlPanel.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/core/core-ui/src/main/kotlin/com/reasontouch/core/ui/components/InspectorControlPanel.kt) [NEW]
- Created the reusable `InspectorControlPanel` component rendering a transpose control row (when `inspectorState.isPitched` is true), a grid subdivision selector (`1/16`, `8T`, `16T`), and a paradiddle/modifier selector (`Off`, `Accent`, `Octave`, `Root/5th`).

#### [ChordInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/ChordInspectorDialog.kt), [BassInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/BassInspectorDialog.kt), [LeadInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/LeadInspectorDialog.kt), [PadInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/PadInspectorDialog.kt), [DrumInspectorDialog.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-pianoroll/src/main/kotlin/com/reasontouch/feature/pianoroll/components/DrumInspectorDialog.kt), [PianoRollScreen.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/feature/feature-pianoroll/src/main/kotlin/com/reasontouch/feature/pianoroll/PianoRollScreen.kt)
- Integrated `InspectorControlPanel` into all 5 track inspector dialogs (`Chord`, `Bass`, `Lead`, `Pad`, `Drum`), wiring `onTranspose` and `onGrooveChange` callbacks to `PianoRollScreen`.

## Verification Results

### Automated Tests
- Executed `gradle_build("app:assembleDebug")` — **Build finished successfully.**
