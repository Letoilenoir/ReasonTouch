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
