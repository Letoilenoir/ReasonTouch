# Implementation Plan: Full Playback Handling for Rhythm Subdivisions & Paradiddle Modifiers in Sequencer

Implement full playback evaluation and note transformation handling for rhythm subdivisions and paradiddle/modifiers in `Sequencer.kt` for both drum and melodic tracks, ensuring the master playhead cursor remains fully linear and independent.

## User Review Required

> [!IMPORTANT]
> **Playback Pipeline Enhancements (`Sequencer.kt`):**
> 1. **Rhythm Subdivisions & Timing Scaling:**
   - Process `STRAIGHT_16`, `TRIPLET_8T`, `TRIPLET_16T`, and swing/subdivision settings to compute `scheduledTick` via `stepScale`.
2. **Paradiddle & Dynamic Modifiers:**
   - **Drums (`isDrum = true`):** Apply sticking sequence velocity accents (e.g., accenting downbeats and alternating hits based on pattern index).
   - **Melodic (`supportsPitch = true`):** Apply pattern modifiers (`ACCENT_VELOCITY`, `OCTAVE_BOUNCE`, `ROOT_FIFTH`) dynamically based on note indices.
3. **Playhead Cursor Protection:**
   - Ensure `playheadBeat` (playbackPositionTicks) updates strictly based on elapsed wall-clock time (`elapsed / beatDurMs`), remaining completely independent of note pitch/groove transformations.

## Proposed Changes

### [core-playback]

#### [Sequencer.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/core/core-playback/src/main/java/com/reasontouch/core/playback/Sequencer.kt)
- Enhance `schedulePass` with robust rhythm subdivision mapping, drum sticking/accent evaluation, and melodic pattern modifiers while keeping the tick clock independent.

## Verification Plan

### Automated Tests
- Gradle build verification via `gradle_build("app:assembleDebug")`.

### Manual Verification
- Deploy to app, play sessions with various rhythm subdivisions (`8T`, `16T`) and paradiddle modifiers (`Accent`, `Octave`, `Root/5th`) on both drum and melodic tracks, and verify smooth audio transformation and UI cursor tracking.
