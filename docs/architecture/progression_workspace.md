# ProgressionWorkspace Architecture

## Overview
ProgressionWorkspace.kt provides a workspace for building, editing, and exploring chord progressions. It integrates tightly with the ChordSuggestionEngine and BassGenerator.

## Responsibilities
- Display progression timeline
- Allow chord editing and replacement
- Trigger progression generation
- Manage harmonic context (key/scale)
- Integrate with bassline generation
- Sync progression playback with transport

## Key Components
- ProgressionWorkspace
- ProgressionViewModel
- ChordSuggestionEngine
- BassGenerator
- PlaybackController

## Data Flow
1. User edits progression
2. Workspace forwards actions to ViewModel
3. ViewModel updates progression model
4. Engines generate updated harmony + bass
5. MidiRouter distributes events
6. PlaybackController syncs timing
7. UI updates progression display

## Related UML
See /docs/uml/progression_workspace.puml.
