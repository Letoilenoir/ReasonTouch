# Implementation Plan: Stage 2 Layout Refactoring — Context-Aware BottomNav & Transport Controls

Reconfigure `BottomNav` and `ReasonTouchApp.kt` for Stage 2 so that the bottom navigation bar is context-aware: omitting the active workspace tab on the left and embedding fixed `TransportControls` on the right.

## User Review Required

> [!IMPORTANT]
> **Architecture & UI Design:**
> 1. **`BottomNav.kt` Refactor:**
   - Update `BottomNav` to accept:
     - `currentRoute: String`
     - `isPlaying: Boolean` (or `transportState: TransportState`)
     - Transport callbacks (`onPlay`, `onStop`, `onRewind`, `onFastForward`, `onSkipToStart`, `onSkipToEnd`)
     - Navigation callbacks (`onChordsClick`, `onPianoRollClick`, `onDrumsClick`)
   - Render only non-active workspace tabs on the left (e.g. if on chords, show Piano Roll and Drums tabs; omit Chords).
   - Render `TransportControls` fixed on the right side of the 56.dp bottom bar.
2. **`ReasonTouchApp.kt` Integration:**
   - Collect `val transport = playbackVm.transport.collectAsState()` inside `showChrome`.
   - Pass transport state and callbacks into `BottomNav`.

## Proposed Changes

### [app]

#### [BottomNav.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/app/src/main/kotlin/com/reasontouch/app/BottomNav.kt)
- Update `BottomNav` to omit active tab and render `TransportControls` on the right.

#### [ReasonTouchApp.kt](file:///C:/Users/andym/StudioProjects/ReasonTouch/app/src/main/kotlin/com/reasontouch/app/ReasonTouchApp.kt)
- Collect transport state and pass to `BottomNav`.

## Verification Plan

### Automated Tests
- Gradle build verification via `gradle_build("app:assembleDebug")`.

### Manual Verification
- Deploy to app, navigate between Chords, Arrange (Piano Roll), and Drums, and verify `BottomNav` dynamically omits the active tab on the left while keeping `TransportControls` pinned on the right.
