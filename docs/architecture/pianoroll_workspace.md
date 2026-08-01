# PianoRollWorkspace Architecture

## Overview
PianoRollWorkspace.kt provides the editing environment for MIDI note manipulation. It manages note selection, editing gestures, grid snapping, quantization, and integration with the MIDI routing and playback engines.

## Responsibilities
- Display piano roll grid and notes
- Handle user gestures (drag, resize, select)
- Apply quantization and snapping rules
- Manage note editing state
- Communicate edits to MIDI engine
- Sync with playback transport
- Render updated note data in real time

## Key Components

### PianoRollWorkspace
Handles:
- UI scaffolding
- Note rendering
- Gesture detection
- State hoisting

### PianoRollViewModel
Provides:
- note list state
- selection state
- editing operations
- quantization logic
- commit operations to MIDI engine

### MidiRouter
Receives updated MIDI events from the workspace.

### PlaybackController
Provides timing information for:
- snapping
- grid alignment
- preview playback

## Data Flow
1. User edits notes in the piano roll  
2. Workspace forwards actions to ViewModel  
3. ViewModel updates note model  
4. MIDI events are regenerated  
5. MidiRouter distributes updated events  
6. PlaybackController syncs timing  
7. UI re-renders updated notes  

## Related UML
See /docs/uml/pianoroll_workspace.puml.
