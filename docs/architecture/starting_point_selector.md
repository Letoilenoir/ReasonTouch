# StartingPointSelector Architecture

## Overview
StartingPointSelector.kt provides the UI and logic for choosing how a new composition begins. It acts as the entry point into Inspire, Manual, Mood, or Progression workspaces.

## Responsibilities
- Display starting point options
- Route user to correct workspace
- Initialize workspace-specific state
- Provide context for composition engines
- Integrate with navigation and app-level state

## Key Components
- StartingPointSelector
- StartingPointViewModel
- Workspace Initializers
- Navigation Controller

## Data Flow
1. User selects a starting point
2. Selector forwards choice to ViewModel
3. ViewModel initializes workspace state
4. Navigation transitions to chosen workspace
5. Workspace loads its engines and UI

## Related UML
See /docs/uml/starting_point_selector.puml.
