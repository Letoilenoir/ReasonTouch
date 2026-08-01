# ManualWorkspace Architecture

## Overview
ManualWorkspace.kt provides a fully manual composition environment where the user directly controls musical structure without automated generation. It acts as the low-level editing workspace.

## Responsibilities
- Provide manual editing tools
- Manage user-driven musical state
- Allow direct manipulation of notes, chords, or patterns
- Integrate with MIDI routing
- Sync edits with playback

## Key Components
- ManualWorkspace
- ManualViewModel
- MIDI Editing Tools
- PlaybackController

## Data Flow
1. User performs manual edits
2. Workspace forwards actions to ViewModel
3. ViewModel updates musical model
4. MIDI events are regenerated
5. MidiRouter distributes events
6. PlaybackController syncs timing
7. UI updates

## Related UML
See /docs/uml/manual_workspace.puml.
