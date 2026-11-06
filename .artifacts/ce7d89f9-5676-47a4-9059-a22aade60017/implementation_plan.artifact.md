# Implementation Plan: Core State & Data Models for Inspector and Compose Features

Update core state and data models to support transposition and rhythmic groove modifications across inspectors and compose workflows.

## User Review Required

> [!IMPORTANT]
> **Data Model Specifications:**
> 1. **`GrooveModifierState.kt` (in `core-playback`):**
   - Enums:
     - `GridSubdivisionEnum`: `STRAIGHT_16`, `TRIPLET_8T`, `TRIPLET_16T` (default `STRAIGHT_16`).
     - `ParadiddleModeEnum`: `OFF`, `ACCENT_VELOCITY`, `OCTAVE_BOUNCE`, `ROOT_FIFTH` (default `OFF`).
   - Data class `GrooveModifierState` with `gridSubdivision` and `paradiddleMode`.
2. **`InspectorState.kt` (in `core-playback`):**
   - Data class `InspectorState` with:
     - `trackId: String`
     - `transposeSemitones: Int` (coerced in `-12..12`, default `0`)
     - `grooveState: GrooveModifierState`
     - Computed property `val isPitched: Boolean get() = trackId.uppercase() != "DRUMS" && !trackId.uppercase().contains("DRUM")`.

## Proposed Changes

### [core-playback]

#### [GrooveModifierState.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/core/core-playback/src/main/java/com/reasontouch/core/playback/GrooveModifierState.kt) [NEW]
- Define `GridSubdivisionEnum`, `ParadiddleModeEnum`, and `GrooveModifierState`.

#### [InspectorState.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/core/core-playback/src/main/java/com/reasontouch/core/playback/InspectorState.kt) [NEW]
- Define `InspectorState` data class with `transposeSemitones`, `grooveState`, and `isPitched`.

## Verification Plan

### Automated Tests
- Gradle build verification via `gradle_build("app:assembleDebug")`.
