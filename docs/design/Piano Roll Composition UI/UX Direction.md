# ReasonTouch — Piano Roll Composition UX/UI Direction

## Working design brief

> “As already stated SURPRISE is under development. I am trying to visualise the UI work path the user would follow to incorporate the bass and drums into a composition.”

This document records the UX/UI direction developed from that question, with particular emphasis on making composition inside Piano Roll smooth, continuous and non-technical for the user.

It is a design/architecture brief rather than an implementation specification.

## 1. Design objective

The central objective is to make **Piano Roll the user's composition workspace**.

The user should not feel that they are moving between separate “Chord”, “Arrange”, “Drums” and “Starting Point” applications. Instead, they should experience one continuous composition environment in which the Composition Tray provides the contextual choices needed to create and develop the piece.

The desired mental model is:

> **This is my composition. The tray is where I decide what I want to develop next.**

Rather than:

> “Which technical composition mode or screen do I need to navigate to?”

This is particularly important because terms such as **Manual, Assisted and Guided** are useful development concepts but can sound technical or intimidating when presented as mandatory up-front choices.

## 2. Existing Piano Roll navigation and controls

### Top editing controls

- Draw
- Select
- Erase
- Loop
- Suggest

These are Piano Roll editing/composition functions.

### Top channel selectors

- Bass
- Lead
- Chord
- Pad
- Drums

These are **not navigation**. They are the controllers/selectors for the corresponding note channels in the Piano Roll. In particular, the **Drums selector remains essential** because it controls the notes in the Drum channel within Piano Roll.

### Lower session and transport controls

- **Home** — returns to the opening/session-management screen where sessions can be created/deleted.
- **Session title** — opens session parameters.
- **Back** — returns to the Starting Point area in the current architecture.
- **Transport controls** — playback and movement through the composition.

These remain persistent Piano Roll controls.

### Legacy bottom navigation

The existing **CHORDS | ARRANGE | DRUMS** row is a legacy of the prototype navigation used by `ChordScreen.kt`.

It does not represent the desired current product architecture:

- `ARRANGE` is effectively the Piano Roll itself.
- `CHORDS` returns to the Starting Point screens.
- `DRUMS` navigates to the Drum Generator/Machine.
- The current user journey already enters Piano Roll from the Starting Point screens.
- Drum generation can instead be reached through the Composition Tray.

### Proposed direction

Remove the legacy **CHORDS | ARRANGE | DRUMS** navigation row.

This is a navigation simplification, not a removal of the underlying functionality.

## 3. Proposed high-level navigation: Home → Piano Roll

The longer-term navigation model should be:

```text
HOME
  │
  │ Create / open session
  ▼
PIANO ROLL
  │
  ▼
COMPOSITION TRAY
```

The current separate Starting Point screens should no longer be required as an intermediate navigation destination.

Instead, the Starting Point choice becomes an **initial state of the Composition Tray**.

The existing Manual / Assisted / Guided functionality can remain underneath this UX change.

The important product change is that the user goes directly from:

**Home → Piano Roll**

and then starts composing from within the same workspace.

## 4. Starting a new composition

When a new session opens with an empty Piano Roll, the Composition Tray can automatically open or present a clear invitation to begin.

The user-facing language should avoid technical terminology wherever possible.

Instead of immediately presenting:

- Manual
- Assisted
- Guided

the UI could present:

### START COMPOSING

**Build it yourself**  
Start with your own chords.

**Give me some ideas**  
Explore chord options.

**Guide me**  
Build a progression step by step.

Internally these can map to:

```text
Build it yourself → MANUAL
Give me some ideas → ASSISTED
Guide me → GUIDED
```

The architecture therefore retains the established methodologies while the product UI exposes the user's creative intention rather than the implementation terminology.

## 5. Composition Tray: established design principle

The Composition Tray should appear as an **overlay above the Piano Roll**, with the Piano Roll remaining visible behind it in a faded/blurred state.

A modest degree of transparency should be retained so that the user remains visually anchored to the composition.

The tray should collapse after a committed action such as **ADD TO PIANO ROLL**.

This preserves the Piano Roll as the primary workspace.

The tray is therefore a contextual creative layer, not a replacement screen.

## 6. Composition Tray home state

Once a composition contains material, the main tray should evolve into a compact composition control surface.

Conceptually:

```text
┌──────────────────────────────┐
│            COMPOSE           │
│                              │
│ HARMONY                      │
│                              │
│      SUGGEST NEXT            │
│                              │
│ ARRANGEMENT                  │
│                              │
│      BASS        DRUMS       │
│                              │
│ ──────────────────────────── │
│                              │
│      CHANGE APPROACH         │
└──────────────────────────────┘
```

This deliberately separates:

### Harmony
**Suggest Next** — the forward-composition mechanism.

### Arrangement
**Bass / Drums** — arrangement/generation functions.

### Change Approach
Access to the Manual / Assisted / Guided methodologies when appropriate.

## 7. Suggest Next remains a harmonic function

The existing two-stage Suggest Next concept should remain intact.

The user should first choose the **strategy**, and only then see the granular phrase choices belonging to that strategy.

```text
SUGGEST NEXT
      │
      ▼
Choose strategy
      │
      ▼
Choose phrase
      │
      ▼
Optional chord swaps/refinement
      │
      ▼
ADD TO PIANO ROLL
```

The strategy choices include the work already developed, with SURPRISE currently under construction.

This should not be diluted by placing Bass and Drums inside the same strategy-selection experience.

## 8. Bass workflow

Bass should be treated as an arrangement layer which responds to the harmonic composition.

From the Composition Tray:

```text
COMPOSE
   ↓
BASS
```

The tray becomes a Bass-specific interaction surface.

A conceptual workflow is:

```text
BASS
  │
  ├── Scope
  │     ├── Next section
  │     ├── Whole composition
  │     └── Selected bars
  │
  ├── Style
  │
  ├── Preview
  │
  └── Add to Piano Roll
```

The first version does not need to expose every possible scope, but the architecture should avoid assuming that Bass generation always means “append to the end”.

The intended lifecycle is:

**Generate → Preview → Accept / Regenerate → Add**

Once added, the tray collapses and the new Bass material becomes visible in Piano Roll.

The existing Bass Generator/BassStyleSheet functionality can be retained underneath this interaction model.

## 9. Drum workflow

Drums should be treated similarly, but as a rhythmic/arrangement layer rather than a harmonic strategy.

From the Composition Tray:

```text
COMPOSE
   ↓
DRUMS
```

The user can select an appropriate feel/style, preview the result and add it to the composition.

Conceptually:

```text
DRUMS
  │
  ├── Scope
  │
  ├── Feel / Style
  │
  ├── Preview
  │
  └── Add to Piano Roll
```

The existing Drum Machine/Generator should not be discarded.

Instead, the proposed change is primarily **how the user reaches it**.

Current prototype route:

```text
Piano Roll
   ↓
bottom DRUMS
   ↓
Drum Machine
```

Proposed route:

```text
Piano Roll
   ↓
Composition Tray
   ↓
DRUMS
   ↓
Drum Generator / Drum Machine
```

This makes Drum generation part of the same composition workflow as Bass.

## 10. Critical distinction: Drum channel vs Drum generator

The top Piano Roll **DRUMS** selector remains.

It means:

> Work with the existing Drum channel/material in the Piano Roll.

The Composition Tray **DRUMS** option means:

> Create or generate new drum material.

These are deliberately different functions.

```text
TOP DRUMS SELECTOR
        ↓
Edit / view drum notes in Piano Roll
```

whereas:

```text
COMPOSITION TRAY
        ↓
DRUMS
        ↓
Generate / preview / replace drum material
```

## 11. Composition lifecycle

The desired overall workflow is:

```text
HOME
  ↓
Create / open session
  ↓
PIANO ROLL
  ↓
START COMPOSING
  ↓
Manual / Assisted / Guided
  ↓
Initial harmony
  ↓
SUGGEST NEXT
  ↓
Strategy
  ↓
Phrase
  ↓
Add
  ↓
BASS
  ↓
Preview / Add
  ↓
DRUMS
  ↓
Preview / Add
  ↓
SUGGEST NEXT
  ↓
Continue developing
```

The user can therefore remain inside Piano Roll for the complete creative journey.

The order is not mandatory; it is a natural progression:

**Harmony → Bass → Drums**

but each layer can be revisited independently.

## 12. Existing composition state

The Composition Tray should be contextual.

For example, after harmony has been created:

```text
HARMONY ✓
SUGGEST NEXT

BASS +
DRUMS +
```

After Bass has been added:

```text
HARMONY ✓
SUGGEST NEXT

BASS ✓
DRUMS +
```

After Drums have been added:

```text
HARMONY ✓
SUGGEST NEXT

BASS ✓
DRUMS ✓
```

The exact visual treatment of state indicators remains a UI design decision.

The important principle is that the tray reflects the current composition rather than presenting an unchanging list of tools.

## 13. Change Approach

The Starting Point methodologies should remain available without requiring the user to leave Piano Roll.

For an established composition, the secondary action can be:

**CHANGE APPROACH**

rather than simply “Starting Point”.

This could reveal:

```text
HOW DO YOU WANT TO
DEVELOP THE HARMONY?

Build it yourself
Give me some ideas
Guide me
```

The user should not be forced to understand the distinction between the underlying methodologies before composing.

The key principle is:

> **Expose decisions; hide mechanisms.**

## 14. Important safeguard when changing approach

Changing approach within an existing composition must not silently destroy existing material.

The exact interaction remains to be designed, but the conceptual options are:

```text
CHANGE APPROACH

Continue from the current progression

or

Start a new progression
```

This becomes particularly important if Manual / Assisted / Guided are eventually used to develop subsequent sections rather than restart the entire piece.

## 15. Architectural UX principle

The Piano Roll should ultimately be understood as the central composition workspace.

```text
                    PIANO ROLL
                        │
                COMPOSITION TRAY
                        │
          ┌─────────────┼─────────────┐
          │             │             │
        HARMONY      ARRANGEMENT    CHANGE
          │             │           APPROACH
          │          ┌──┴──┐           │
   SUGGEST NEXT      BASS DRUMS     M/A/G
          │
     Strategy
          │
       Phrase
```

This means that Manual, Assisted and Guided are **composition methodologies**, not navigation destinations.

Likewise, Bass and Drums are **arrangement functions**, not separate top-level workspaces.

## 16. Product-language principle

The underlying application architecture can remain sophisticated.

It may contain concepts such as:

- Starting Point
- Manual
- Assisted
- Guided
- Resolve
- Lift
- Contrast
- Surprise
- Bass Generator
- Drum Generator
- Composition Context
- Strategy Engine

The user does not need to be presented with all of that vocabulary.

The user-facing experience should instead communicate:

```text
Start something
      ↓
Choose a way to build
      ↓
Choose what comes next
      ↓
Choose a phrase
      ↓
Add it
      ↓
Add bass
      ↓
Add drums
      ↓
Keep going
```

This is intended to make the application feel like a musical composition environment rather than a collection of technical modes.

## 17. Proposed final navigation model

```text
                         HOME
                           │
                    Create / open session
                           │
                           ▼
                      PIANO ROLL
                           │
                  COMPOSITION TRAY
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
      START              CONTINUE          ARRANGE
        │                  │                  │
     M/A/G           SUGGEST NEXT        BASS / DRUMS
```

The old route:

```text
HOME → STARTING POINT → PIANO ROLL
```

becomes:

```text
HOME → PIANO ROLL → COMPOSITION TRAY
```

with the Starting Point functionality absorbed into the initial tray state.

## 18. What should remain unchanged

### Piano Roll channel selectors

**Bass / Lead / Chord / Pad / Drums** remain as channel controls.

### Transport

Playback controls remain persistent.

### Session controls

Home and session parameters remain available.

### Existing composition engines

Manual, Assisted, Guided and the strategy/generator implementations can remain intact beneath the revised UX.

The principal change is **where and how the user accesses them**.

## 19. Implementation sequencing — design recommendation

The document is primarily concerned with UX/UI, but the proposed direction suggests a sensible order of work.

### Current priority

**Complete SURPRISE.**

The existing strategy work should not be destabilised by the navigation refactor.

### Next design/implementation stage

Build the Composition Tray around:

1. Existing Suggest Next flow.
2. Strategy selection.
3. Phrase selection.
4. Phrase refinement/chord swapping.
5. Add to Piano Roll and collapse.
6. Bass entry point.
7. Drum entry point.

### Subsequent navigation refactor

1. Remove legacy bottom `CHORDS | ARRANGE | DRUMS`.
2. Preserve top channel selectors.
3. Move Drum Generator access into Composition Tray.
4. Establish Home → Piano Roll as the primary composition entry route.
5. Absorb Starting Point selection into the empty-state Composition Tray.
6. Later support Change Approach from an established composition.

## 20. Design principles to retain

1. **Piano Roll is the workspace.** Do not repeatedly navigate the user away from the piece they are constructing.
2. **The tray is contextual.** It should offer actions appropriate to the current state of the composition.
3. **Expose decisions; hide mechanisms.** Users should not need to understand the architecture in order to compose.
4. **Suggest Next is not the same thing as Bass or Drums.** Harmony strategies and arrangement generation should remain conceptually distinct.
5. **Preview before committing.** Generated material should be previewable before it is added.
6. **Adding material should collapse the tray.** Return visual attention to the Piano Roll after a committed action.
7. **Existing material should be respected.** Generate/replace operations should have clear scope and should not silently overwrite unrelated material.
8. **Avoid redundant navigation.** The legacy `CHORDS | ARRANGE | DRUMS` row should not be carried forward simply because it exists in the prototype.
9. **Keep sophisticated capabilities behind simple language.** The application can become musically sophisticated without becoming intimidating.
10. **The composition should remain the user's focus.** The UI should continually answer: “What can I do with my piece now?” rather than “Which part of the application should I navigate to?”

## 21. Current design position

The immediate design direction is therefore:

**SURPRISE remains under development.**

Alongside that work, the wider UX model should move toward:

> **Home → Piano Roll → Composition Tray**

with the Composition Tray becoming the principal contextual interface for:

- Starting a composition
- Continuing harmonic development
- Choosing and refining suggested phrases
- Adding Bass
- Adding Drums
- Eventually changing the compositional approach

The old Starting Point screens need not disappear functionally; they can become **embedded composition choices within the Piano Roll experience**.

The intended result is a continuous creative workflow in which the user stays with the music rather than navigating through the application's internal architecture.

## Summary

> **Make Piano Roll the home of composition, and make the Composition Tray the user's gateway to deciding what happens next.**


## Addendum — hasHarmony signal and bass-first harmonisation (2026-08-23)

When hasHarmony was changed from a note-presence heuristic (tracks.any { allNotes[it.id]?.isNotEmpty() == true }) to a chord-presence check (chords.isNotEmpty()), the impact on the still-unscoped bass-first concept (draw bassline → infer chord candidates) was considered explicitly rather than left implicit.

Conclusion: chords.isNotEmpty() is more correct for bass-first, not less. The old heuristic would have falsely reported hasHarmony = true the moment any notes existed on any track — including a drawn bassline with no harmony inferred from it yet. The new signal correctly reads false until harmony actually exists, whether that harmony came from a chord/manual workspace or (eventually) from bass inference.
This provides a coherent path from the initial creation of a session through harmonic development and onward into bass and drum arrangement, while reducing the prominence of technical terminology and eliminating legacy navigation that no longer reflects the product architecture.
