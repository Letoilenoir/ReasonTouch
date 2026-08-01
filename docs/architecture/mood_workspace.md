# MoodWorkspace Architecture

## Overview
MoodWorkspace.kt provides a mood-driven composition environment. The user selects a mood, and the workspace coordinates with composition engines to generate harmonically and rhythmically appropriate material.

## Responsibilities
- Display mood selection UI
- Map moods to musical parameters
- Trigger chord, bass, and drum generation
- Manage mood-based state
- Integrate with playback and MIDI routing

## Key Components
- MoodWorkspace
- MoodViewModel
- Mood-to-Music Mapping Engine
- ChordSuggestionEngine
- BassGenerator
- DrumMachine
- PlaybackController

## Data Flow
1. User selects a mood
2. Workspace forwards selection to ViewModel
3. ViewModel maps mood → musical parameters
4. Engines generate MIDI content
5. MidiRouter distributes events
6. PlaybackController syncs timing
7. UI updates with generated material

## Related UML
See /docs/uml/mood_workspace.puml.
