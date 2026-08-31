# Musical Context, Bass & Drum Arrangement_2026-08-31
## Design / Handoff Document

**Purpose:** Establish the design position and forward roadmap for integrating the completed/near-complete CHORD composition system with Bass and Drums, while preserving the existing Piano Roll and Composition Tray UX direction.

**Status:** Design phase / handoff  
**Scope:** Musical context, architecture, arrangement relationships, UI implications, and design roadmap  
**Current implementation position:** CHORD is effectively complete apart from the remaining strum-speed variable control; SURPRISE is under development.

---

# 1. The current musical picture

The CHORD component is now sufficiently mature that it can be treated as the first established source of musical context for the wider arrangement system.

The existing implementation already captures considerably more than simply a sequence of chord names. At composition level, the system has access to:

- chord identity
- chord root
- complete MIDI voicing
- voicing name
- bar position
- strum pattern
- strum speed
- BPM
- bar duration
- total bar count
- detected harmonic/key candidates
- selected harmonic key
- mood/harmonic bias
- continuation suggestions
- borrowed-chord possibilities
- progression history
- inherited strum characteristics

This leads to the central design proposition:

> **Identify everything the CHORD system knows → define that as the musical context available to arrangement → design Bass and Drums around that context.**

The next stage should therefore not be approached as simply building two independent generators beside the chord generator. The objective is coordinated composition.

---

# 2. Files consulted

The following Kotlin structures were supplied and reviewed as the basis for this design:

### `ChordViewModel.kt`

Principal source of composition-level state and behaviour.

Relevant areas include:

- `ChordUiState`
- session, track and progression flows
- BPM
- bar duration
- strum speed and enablement
- chord/voicing selection
- step sequencer / strum pattern
- `ChordEvent` creation
- progression generation and audition
- harmonic analysis and key detection
- continuation suggestion workflow
- `sendProgressionToPianoRoll()`
- `sendToPianoRoll()`
- `addPhrase()`
- Bass generation entry point

Of particular importance is `addPhrase()`, which carries forward the previous chord's strum pattern and speed when creating continuation bars.

### `BassGenerator.kt`

Current Bass pattern engine.

Existing styles:

- `ROOT`
- `ROOT_FIFTH`
- `OCTAVE`
- `WALKING`
- `ARPEGGIO`
- `GROOVE`
- `PEDAL`

Inputs currently include the chord progression, Bass style, target track, beats per bar, append offset and snap value.

### `BassStyle.kt`

Current Bass vocabulary and descriptions:

- Root
- Root + 5th
- Octave
- Walking Bass
- Arpeggio
- Groove
- Pedal

These should initially be treated as an existing pattern vocabulary rather than replaced.

### `ChordEvent.kt`

Current persistence model includes:

- `sessionId`
- `barIndex`
- `chordName`
- `rootMidi`
- `midiNotes`
- `voicing`
- `strumPatternId`
- `strumSpeedValue`

The presence of rhythmic information here is architecturally significant.

### `NoteEvent.kt`

Shared Piano Roll event representation:

- `trackId`
- `pitch`
- `beat`
- `duration`
- `velocity`

This is the common output representation used by generated Bass and Drum material.

### `DrumPattern.kt`

Current drum pattern model provides:

- six lanes
- configurable 16/32 steps
- active/inactive cells
- per-step velocity
- clearing
- extension/trimming

Current presets:

- 4/4 Basic
- Rock
- Funk
- Reggae
- Bossa Nova

### `DrumKit.kt`

Current drum lane definitions:

- Kick
- Snare
- Clap
- Hi-Hat
- Open HH
- Ride

Also establishes the 16-step default and 32-step extended grid.

### `DrumViewModel.kt`

Current Drum behaviour includes:

- pattern editing
- preset application
- live pattern playback
- BPM-derived timing
- DRUMS track creation/lookup
- writing patterns to the Piano Roll
- repetition across the session
- append/replace behaviour

The Drum implementation is therefore already a usable pattern engine and Piano Roll writer.

---

# 3. What the existing structures tell us

## 3.1 CHORD already contains rhythmic information

The chord system does not merely describe:

`C → F → G → C`

It can also describe how those chords are performed.

`ChordEvent` retains the strum pattern and strum speed alongside harmonic information.

This means arrangement can eventually distinguish between harmonically identical but rhythmically very different chord performances.

That is likely to be important when determining Bass and Drum density.

---

## 3.2 Bass is currently primarily a pattern-template engine

The existing Bass styles are distinct and useful, but they currently operate mainly as predefined rhythmic/melodic templates applied to the chord progression.

Examples:

- ROOT = one sustained root
- ROOT_FIFTH = root plus fifth
- OCTAVE = octave displacement
- WALKING = passing/approach movement
- ARPEGGIO = chord-tone movement
- GROOVE = syncopated root activity
- PEDAL = sustained root

This is a sound foundation.

The design task is therefore not to discard these styles, but to determine when each is appropriate within a given musical context.

---

## 3.3 Drums are currently pattern-oriented

`DrumPattern` and the preset collection provide a useful starting vocabulary.

At present, however, a drum pattern is fundamentally independent of the CHORD and Bass material.

A future arrangement layer should be able to consider:

- chord rhythmic activity
- harmonic-change frequency
- Bass activity
- phrase boundaries
- desired overall density

rather than simply selecting a preset by genre/name.

---

# 4. Proposed `CompositionContext`

The recommended architectural direction is a conceptual `CompositionContext`.

It does **not** initially need to be a new Room entity. It can first be a derived domain model assembled from existing session, progression and track information.

Conceptually:

```text
CompositionContext
│
├── Session
│   ├── BPM
│   ├── beatsPerBar
│   └── totalBars
│
├── Harmony
│   ├── chords
│   ├── roots
│   ├── chord tones
│   ├── key
│   └── harmonic character
│
├── Rhythm
│   ├── strum patterns
│   ├── strum speeds
│   └── rhythmic density
│
├── Structure
│   ├── number of bars
│   ├── phrase boundaries
│   └── continuation history
│
└── Arrangement
    ├── bass present?
    ├── drums present?
    └── existing rhythmic density
```

The exact class structure should be decided during implementation design.

The key principle is:

> **CompositionContext becomes the contract between composition and arrangement.**

---

# 5. Proposed architectural relationship

Recommended relationship:

```text
                 CompositionContext
                         │
             ┌───────────┼───────────┐
             ↓           ↓           ↓
          Chords        Bass        Drums
             │           │           │
             └───────────┼───────────┘
                         ↓
                     Arrangement
```

A crucial architectural rule should be maintained:

> `BassGenerator` should not need to understand `DrumGenerator`, and `DrumGenerator` should not need to understand `BassGenerator`.

Their relationship belongs in the arrangement/context layer.

This prevents tight coupling and allows each generator to evolve independently.

---

# 6. What Bass should eventually consume

At minimum, the Bass arrangement layer should be able to use:

### Harmonic

- current chord
- root
- chord tones
- next chord
- key/harmonic context

### Structural

- bar number
- phrase position
- phrase ending
- continuation boundary

### Rhythmic

- beats per bar
- BPM
- chord strum pattern
- strum speed
- estimated chord rhythmic density

### Arrangement

- whether drums are active
- existing drum density
- desired overall groove density

The generator should then respond to context rather than simply apply an isolated style.

---

# 7. What Drums should eventually consume

The equivalent Drum context should include:

### Session

- BPM
- meter / beats per bar
- total bars

### Harmony

- chord changes
- phrase boundaries
- harmonic rhythm

### Rhythm

- chord strum activity
- strum speed
- overall rhythmic density

### Bass

- Bass presence
- Bass rhythmic density
- Bass accents
- Bass phrase behaviour

### Arrangement

- desired groove character
- density
- section intensity
- transition/fill opportunities

The Drum generator should not directly own the complete musical model; the arrangement layer should provide the relevant information.

---

# 8. The central musical design problem: groove

The eventual objective is not simply:

```text
Chord generator
Bass generator
Drum generator
```

It is coordinated groove.

Conceptually:

```text
                 GROOVE
                /      \
            Chords     Bass
                \      /
                  Drums
```

The arrangement system should eventually be capable of recognising relationships such as:

- busy chord rhythm → simpler Bass may be preferable
- sparse chord rhythm → Bass can provide movement
- strong Bass pulse → drums need not duplicate every accent
- sparse Bass → kick may carry more of the rhythmic foundation
- phrase ending → Bass and Drums can jointly signal closure
- active chords + active Bass + dense drums → risk of excessive rhythmic clutter

These are design principles, not yet implementation rules.

---

# 9. Bass voice-leading consideration

One issue surfaced in the current implementation that should be recorded for later design consideration.

`BassGenerator` currently normalises pitches into its selected register using `bassRegister()`.

This is straightforward, but it does not yet represent true bass voice-leading.

A future arrangement layer may want to consider:

> What is the musically appropriate nearby bass pitch for the next chord?

rather than independently normalising every root/chord tone.

Potential benefits include:

- smoother movement
- more natural walking bass
- better arpeggio continuity
- more coherent transitions

This should **not** trigger an immediate rewrite of the existing Bass generator. It belongs on the future arrangement/voice-leading roadmap.

---

# 10. Register discrepancy to resolve

The supplied `BassGenerator.kt` contains a documentation/implementation discrepancy.

The comment describes the bass register as MIDI 28–52 (E1–E3), while the implementation constants are:

```text
BASS_MIN_MIDI = 40
BASS_MAX_MIDI = 64
```

The latter corresponds to a considerably higher range.

This needs an explicit musical design decision before Bass is considered final.

It should not be silently changed as part of arrangement work.

---

# 11. Implications for the Composition Tray

The existing Composition Tray direction remains important.

The user should not have to understand the underlying arrangement model.

The tray can eventually expose simple composition-level decisions such as:

```text
COMPOSE

Continue the chords
    Suggest Next

Build the groove
    Bass
    Drums
    Full Groove
```

Internally, the system can use `CompositionContext` to determine suitable choices.

The user sees musical decisions rather than technical implementation parameters.

---

# 12. Relationship to the established two-stage Suggest Next design

The established two-stage Suggest Next principle should remain intact.

### Stage 1 — choose the strategy

The user first sees the available composition strategies.

For example:

- Resolve
- Lift
- Surprise
- other established strategies where applicable

### Stage 2 — choose within the selected strategy

Only after selecting a strategy should the user see its candidate phrase choices.

The same progressive-choice principle should apply when Bass/Drum arrangement choices are eventually incorporated.

---

# 13. Starting Points and Home → Piano

The wider UX direction established previously should also be preserved.

The proposed navigation is effectively:

```text
HOME
  ↓
PIANO
  ↓
Composition Tray
```

rather than requiring the user to repeatedly pass through technical Starting Point screens.

Starting Point concepts can potentially become contextual options within the Composition Tray when beginning a new composition.

The guiding UX principle is:

> **Composition should feel like one continuous activity rather than a sequence of technical screens.**

---

# 14. Bass and Drums should not become separate composition silos

The specialist Bass and Drum screens/generators remain useful for detailed editing.

However, the Piano Roll experience should increasingly allow composition without repeatedly navigating away.

Likely flow:

```text
Piano Roll
    ↓
Composition Tray
    ↓
Choose what to develop
    ↓
Bass / Drums / Full Groove
    ↓
Review / audition
    ↓
Apply to Piano Roll
```

The tray should act as the composition-level orchestration surface, not necessarily replace every specialist editor.

---

# 15. Undo and composition actions

As the Composition Tray becomes the primary forward-composition mechanism, applied suggestions should be reversible.

The user may:

1. choose a phrase
2. add it to the Piano Roll
3. play the result
4. decide it was the wrong direction

The system should support meaningful composition-level undo.

This is especially important for:

- adding a chord phrase
- generating Bass
- generating Drums
- generating a combined groove

The preferred conceptual model is an **atomic composition action**.

For example:

```text
Apply Phrase
    → chord changes
    → piano-roll chord notes
    → associated arrangement changes
```

should ideally be one undoable user action.

Detailed implementation belongs to a later architecture phase.

---

# 16. Chord substitution

The composition model should support changing a specific chord inside an accepted phrase.

Example:

```text
C – F – G – C
```

becoming:

```text
C – F – D – C
```

without requiring the whole phrase to be discarded.

The eventual architecture should distinguish between:

- phrase selection
- phrase acceptance
- individual chord substitution
- downstream arrangement reconciliation

The primary home for substitution is the composition/harmony layer, with arrangement responding to the resulting context.

---

# 17. Proposed design roadmap

## Phase 1 — CHORD context audit

Document precisely what the completed CHORD system exposes.

Deliverable:

**Composition Context Specification**

Cover:

- harmonic fields
- rhythmic fields
- structural fields
- phrase metadata
- session timing
- arrangement-relevant derived properties

No implementation change is required initially.

---

## Phase 2 — Define derived musical properties

Identify properties not explicitly stored but derivable from current data.

Candidates include:

- rhythmic density
- harmonic-change frequency
- phrase position
- chord-change rate
- strum activity
- average strum speed
- phrase-ending status
- repeated root/chord behaviour

These may prove more useful to arrangement than raw implementation fields.

---

## Phase 3 — Bass arrangement specification

Before changing `BassGenerator`, define:

- what Bass should know
- what it should ignore
- how existing BassStyle choices map to contexts
- when a style should be recommended
- how chord rhythm affects Bass rhythm
- how voice-leading should eventually work
- how Bass interacts with phrase boundaries

Deliverable:

**Bass Arrangement Design Specification**

---

## Phase 4 — Drum arrangement specification

Define:

- rhythmic density model
- kick/chord relationship
- kick/Bass relationship
- snare/backbeat behaviour
- hi-hat density
- phrase endings
- fills/transitions
- pattern variation
- how existing presets become arrangement vocabulary

Deliverable:

**Drum Arrangement Design Specification**

---

## Phase 5 — Groove relationship model

Only after Bass and Drum inputs have been defined independently should the cross-instrument model be designed.

Questions:

- Who owns the downbeat?
- When should Bass follow Kick?
- When should Bass deliberately diverge?
- How much rhythmic duplication is desirable?
- How does density change by section?
- How do phrase endings coordinate?
- How are fills introduced?
- How is groove preserved when the user substitutes a chord?

Deliverable:

**Groove / Arrangement Rules Specification**

---

## Phase 6 — CompositionContext implementation

Once the design is stable:

- implement the context model
- derive it from existing Room/session data
- expose it to arrangement services
- avoid prematurely migrating all generators

Existing generators can initially consume an adapter/context projection.

---

## Phase 7 — Adapt existing generators

Gradually adapt:

- `BassGenerator`
- Drum pattern selection/generation

to consume contextual information.

Existing styles and presets should remain available.

The first objective is **context-aware selection**, not a completely new generative engine.

---

## Phase 8 — Tray integration

Add arrangement concepts to the existing Composition Tray.

Potential hierarchy:

```text
COMPOSE
│
├── Continue
│   └── Strategy → Phrase
│
└── Build Groove
    ├── Bass
    ├── Drums
    └── Full Groove
```

Exact labels remain a UX decision.

The tray should remain an overlay over the Piano Roll, with the Piano Roll visually receding behind it as previously established.

---

## Phase 9 — Atomic Apply + Undo

Introduce a transaction/action model for composition changes.

Examples:

```text
Apply Phrase
Apply Bass
Apply Drums
Apply Full Groove
Substitute Chord
```

Each should have a clearly defined undo boundary.

---

## Phase 10 — Optional "Want to know more"

The previously discussed optional explanatory layer can eventually expose the underlying musical reasoning without forcing it on the user.

For example, the user could select:

> **Want to know more?**

and receive an explanation of why a particular Bass or Drum choice works with the current progression.

This should remain optional and secondary to composition.

---

# 18. Design principles to carry forward

### 1. Context before generation

Do not build isolated generators and attempt to connect them afterwards.

### 2. Preserve existing working components

The current CHORD, Bass and Drum implementations are valuable foundations.

### 3. Separate generators from arrangement

Generators create musical material.

Arrangement decides what material is appropriate and how components relate.

### 4. Keep technical complexity underneath the UI

The user should make musical decisions, not manage implementation parameters.

### 5. Progressive choice

Present the broad decision first, then expose the appropriate finer choices.

This mirrors the established two-stage Suggest Next model.

### 6. Composition should be reversible

Applying a musical idea should not feel like an irreversible commitment.

### 7. Substitution should be local

Changing one chord should not unnecessarily invalidate the whole composition.

### 8. Design for relationships, not just parts

The end goal is a coherent groove, not three technically successful tracks.

---

# 19. Immediate next design task

The next design document should be:

## `CompositionContext_Specification.md`

It should enumerate, field by field:

**What CHORD currently knows → what is directly available → what can be derived → what arrangement needs → what should remain internal.**

That document should become the contract against which the Bass and Drum arrangement designs are developed.

Only after that should implementation work begin on contextual Bass/Drum generation.

---

# 20. Current design position

The application has reached an important architectural transition.

The CHORD component is sufficiently mature to stop being treated merely as a feature and start being treated as the **source of musical context** for arrangement.

The immediate objective is therefore not:

> "How do we make Bass and Drums more sophisticated?"

It is:

> **"What does the composition already know, and how can that knowledge inform everything that comes next?"**

That is the foundation for moving from a collection of generators toward a coherent composition environment.
