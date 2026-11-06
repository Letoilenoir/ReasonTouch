# Walkthrough - Wiring Inspector Transpose & Groove Settings into Sequencer

Successfully wired inspector UI transpose and groove/paradiddle settings into `Sequencer.kt` while protecting the master tick emission stream.

## Changes

### [core-playback]

#### [Sequencer.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/core/core-playback/src/main/java/com/reasontouch/core/playback/Sequencer.kt)
- Retrieved `track.transpose` and `track.grooveModifier` in `schedulePass`.
- Applied `track.supportsPitchControls` check so DRUMS ignore transpose.
- Computed `scheduledTick` via groove offset and `finalPitch` via transpose offset during note triggering.
- Ensured master playhead tick emission stream (`playheadBeat`) continues its continuous emission loop unaffected by note pitch or groove calculations.

## Verification Results

### Automated Tests
- Executed `gradle_build("app:assembleDebug")` — **Build finished successfully.**
