# ChordWorkspace Architecture

## Overview
ChordWorkspace.kt provides the environment for chord selection, progression editing, and harmonic exploration. It integrates with the ChordSuggestionEngine and BassGenerator to produce harmonic and rhythmic structure.

## Responsibilities
- Display chord palette and progression timeline
- Handle chord selection and replacement
- Trigger chord progression generation
- Integrate with BassGenerator for harmonic grounding
- Update UI based on generated progressions
- Sync chord playback with transport

## Key Components

### ChordWorkspace
Handles:
- UI scaffolding
- chord palette rendering
- progression timeline
- state hoisting

### ChordViewModel
Provides:
- progression state
- key/scale context
- generation triggers
- commit operations to ChordSuggestionEngine

### ChordSuggestionEngine
Generates:
- chord progressions
- harmonic substitutions
- key-relative suggestions

### BassGenerator
Produces basslines based on chord progression.

### PlaybackController
Ensures chord playback syncs with:
- tempo
- transport
- metronome

## Data Flow
1. User selects or edits chords  
2. Workspace forwards actions to ViewModel  
3. ViewModel updates progression model  
4. ChordSuggestionEngine generates updated progression  
5. BassGenerator produces harmonic bassline  
6. MIDI events are routed via MidiRouter  
7. PlaybackController syncs timing  
8. UI updates progression display  

## Related UML
See /docs/uml/chord_workspace.puml.
