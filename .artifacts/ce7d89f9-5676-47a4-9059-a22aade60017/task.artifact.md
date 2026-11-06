# Task List - Generalize Transpose & Groove Controls Across All Track Inspectors

- [ ] Update `ChordInspectorDialog.kt` to use `InspectorControlPanel`
- [ ] Update `BassInspectorDialog.kt` to include `InspectorControlPanel` and `onTranspose`
- [ ] Update `LeadInspectorDialog.kt` to include `InspectorControlPanel` and `onTranspose`
- [ ] Update `PadInspectorDialog.kt` to include `InspectorControlPanel` and `onTranspose`
- [ ] Update `DrumInspectorDialog.kt` to include `InspectorControlPanel` (without transpose)
- [ ] Update `PianoRollScreen.kt` to supply `InspectorState`, `onTranspose`, and `onGrooveChange` to all inspector dialogs
- [ ] Run gradle build and verify compilation (`gradle_build("app:assembleDebug")`)
- [ ] Create walkthrough artifact summarizing changes
