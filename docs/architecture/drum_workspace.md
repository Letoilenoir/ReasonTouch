# DrumWorkspace Architecture

## Overview
DrumWorkspace.kt provides the UI and interaction model for drum pattern creation. It manages step sequencing, velocity editing, pattern variation, and integration with the DrumMachine engine.

## Responsibilities
- Display drum grid (steps × instruments)
- Handle tap/step interactions
- Manage velocity and accent editing
- Trigger DrumMachine pattern generation
- Sync drum playback with transport
- Update UI based on generated patterns

## Key Components

### DrumWorkspace
Handles:
- UI layout
- step interactions
- pattern visualization
- state hoisting

### DrumViewModel
Provides:
- pattern state
- instrument mapping
- velocity model
- generation triggers
- commit operations to DrumMachine

### DrumMachine
Generates:
- step patterns
- swing
- accents
- fills and variations

### PlaybackController
Ensures drum patterns sync with:
- tempo
- transport
- metronome

## Data Flow
1. User edits drum steps  
2. Workspace forwards actions to ViewModel  
3. ViewModel updates pattern model  
4. DrumMachine generates updated pattern  
5. MIDI events are routed via MidiRouter  
6. PlaybackController syncs timing  
7. UI updates pattern display  

## Related UML
See /docs/uml/drum_workspace.puml.

## Drum Fills & Arrangement (New)
ReasonTouch includes an integrated **Drum Fills Engine** featuring 10 classic beginner and essential drum fills:
1. **8th-Note Roll** — Steady 8th notes leading to a crash
2. **16th-Note Roll** — Busy 16th-note snare roll ending in crash
3. **Quarter-Note Fill** — Simple, spacious hits on each beat
4. **Triplet Fill** — Swinging triplet feel across the bar
5. **Single Stroke Roll** — Rapid alternating single hits
6. **Double Stroke Roll** — Classic double-hit pattern
7. **Paradiddle Fill** — RLRR LRLL sticking rhythm
8. **Splat-Boom** — Flammed snare rush ending in heavy kick and crash
9. **Motown Roll** — Classic 16th pickup into downbeat crash
10. **Tom Descent** — Descending tom roll (High Tom → Mid Tom → Floor Tom → Crash)

### Access Points & Workflow:
- **Composition Tray (`CompositionTray.kt`):** Access via the **DRUM FILLS** action button in `TrayHome`. Select a fill style, specify the target bar (1-indexed via the touch-friendly Bar Stepper), and tap **GENERATE FILL** (defaulting to append mode so fills layer gracefully on top of your main groove).
- **Standalone Drum Machine (`DrumScreen.kt`):** Access via the 2-column **DRUM FILLS** grid located in the lower estate below the step sequencer grid, allowing instant insertion of fills into custom patterns.
