# InspireWorkspace Architecture

## Overview
InspireWorkspace.kt provides the Compose workspace environment for the Inspire feature. It manages UI state, layout, interactions, and communication with underlying composition engines. The workspace acts as the central hub for user-driven musical creation within the Inspire workflow.

## Responsibilities
- Host the Inspire Compose UI
- Manage workspace-level state
- Coordinate interactions between UI components
- Trigger composition engines (chords, bass, drums)
- Route user actions to domain logic
- Display generated musical content
- Integrate with playback and MIDI routing

## Key Components

### InspireWorkspace
The main Compose entry point for the Inspire feature. Handles:
- UI scaffolding
- State hoisting
- Event callbacks
- Integration with composition engines

### InspireViewModel
Provides state and exposes actions such as:
- generateChordProgression()
- generateBassline()
- generateDrumPattern()
- updateWorkspaceState()

### Composition Engines
Used by the workspace to generate musical content:
- ChordSuggestionEngine
- BassGenerator
- DrumMachine

### PlaybackController
Allows the workspace to:
- play
- pause
- seek
- sync generated content with transport

## Data Flow
1. User interacts with Inspire UI  
2. InspireWorkspace forwards actions to InspireViewModel  
3. ViewModel calls composition engines  
4. Engines produce MIDI events  
5. MidiRouter distributes events  
6. PlaybackController manages transport  
7. UI updates based on new musical content  

## Related UML
See /docs/uml/inspire_workspace.puml for the full class diagram.
