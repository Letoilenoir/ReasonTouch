# ReasonTouch — Derived Musical Properties
## Phase 2 Deliverable (backfilled): Bass/Drum Arrangement Roadmap

**Status:** Draft — Phase 2, written retroactively after Phases 3 and 4
**Why retroactive:** Phase 2 was skipped as its own deliverable. Its
content was instead distributed piecemeal — a few candidate properties
were flagged as "not yet computed" in `CompositionContext_Specification.md`
(Section 5), and the one property that actually got used (attack density)
was independently formalized inside both `Bass_Arrangement_Specification.md`
(Section 4) and `Drum_Arrangement_Specification.md` (Section 5), using an
identical formula arrived at separately rather than from one shared
definition. This document exists to fix that: **one canonical source for
every derived property either spec relies on**, so Phase 5 (and any future
generator) references a single definition rather than independently
reinventing it.
**No implementation change in this phase.** Paper only, same as Phases 1, 3,
and 4.

---

## 1. Why this matters, concretely

The Bass and Drum specs happened to agree on the density formula this time.
That was luck, not process — nothing forced them to agree, and if a future
session had written one of those specs without the other already
established, there'd have been no guarantee of consistency. A shared
derived-properties layer removes that risk structurally: every generator
reads the *same* computed value, rather than each one re-deriving it from
raw fields independently.

---

## 2. Properties already in active use (confirmed, now formalized once)

### 2.1 Chord attack density

**Definition:** proportion of a bar's 16 steps carrying an active strum
attack (DOWN or UP), regardless of stroke direction.

**Formula (canonical, single source):**
```
attackDensity(chordEvent) =
    chordEvent.strumPatternId.toStepPattern().steps
        .count { it != StepState.OFF } / 16f
```

**Range:** `0.0` (block chord / fully empty pattern) to `1.0` (every step
active).

**Used by:** Bass spec Section 4 (style suggestion), Drum spec Section 5
(preset suggestion) — both should reference *this* definition rather than
their own local restatement, once implementation begins.

**Status:** Fully derivable today, no new data needed — confirmed in
Phase 1 Section 5.

---

## 3. Properties flagged but not yet formalized — formalized here

These were named as candidates in Phase 1 (Section 5) or the original
handoff docs (Section 9 of the first doc) but never given a precise
definition. Formalizing each now, using only fields already confirmed to
exist.

### 3.1 Rhythmic occupancy

**Distinct from attack density** — occupancy accounts for note *duration*,
not just attack count. A bar with 2 attacks that each sustain for 8 beats
occupies more of the bar than a bar with 8 attacks that are all
very short.

**Definition:** proportion of a bar's total beat-duration during which at
least one note from the relevant track is sounding.

**Formula (proposed):**
```
occupancy(notes: List<NoteEvent>, barStart: Float, barDur: Float) =
    // sum of (note duration, clipped to bar bounds) for all notes
    // overlapping [barStart, barStart + barDur), divided by barDur,
    // capped at 1.0 to account for overlapping notes
```

**Applies to:** any track's `NoteEvent` output — Chord's generated notes,
Bass's generated notes, or (once structural variation exists) Drums'.

**Status:** Computable today from existing `NoteEvent` data (confirmed
uniform contract, Phase 1 Section 7.4) — **not yet used by either the Bass
or Drum spec**, since both specs used attack density (a simpler, cheaper
proxy) instead. Flagged here as available for Phase 5 if attack density
proves too coarse a signal once real testing begins.

### 3.2 Syncopation measure

**Definition:** degree to which active attacks fall on rhythmically "weak"
positions (off the beat / off the strong subdivisions) rather than
strong-beat positions.

**Formula (proposed, simplest useful version):**
```
// For a 16-step bar, steps 0, 4, 8, 12 are "strong" (on-beat)
// All other active steps count as syncopated
syncopation(pattern: StepPattern) =
    val strongSteps = setOf(0, 4, 8, 12)
    val activeSteps = pattern.steps.indices.filter { pattern.steps[it] != StepState.OFF }
    if (activeSteps.isEmpty()) 0f
    else activeSteps.count { it !in strongSteps }.toFloat() / activeSteps.size
```

**Range:** `0.0` (every active attack lands on a strong beat) to `1.0`
(every active attack is syncopated).

**Applies to:** chord `StepPattern`, and — once Drums can express variable
patterns — Drum patterns too, using the identical formula.

**Status:** Fully derivable today for Chord data. Not yet used by either
spec — both specs' style/preset recommendation tables (Bass Section 3,
Drum Section 4) currently reason about density only, not syncopation
specifically. This is a real gap: e.g. `"Reggae Skank"` and a dense
straight-eighths pattern could have *identical* attack density but very
different syncopation — the specs as written wouldn't currently
distinguish them on that basis. Worth flagging as a genuine refinement
opportunity for whenever Phase 5/6 revisits the recommendation tables.

### 3.3 Phrase position

**Definition:** a bar's position within its enclosing phrase — specifically
whether it is the phrase's **first bar**, **last bar**, or an interior bar.

**Source data:** already exists via `addPhrase()`'s `startBarIndex` and
`generatedProgression.chords.size` (confirmed, Phase 1 Section 6) — a bar's
phrase-relative index is `barIndex - startBarIndex`, and "is last bar" is
`index == generatedProgression.chords.size - 1`.

**Status:** Fully derivable today. **Directly required** by both specs'
Section 6/8 (`RESOLVE` intent favoring `PEDAL`/simplification "in the final
bar(s)") — those recommendations silently assumed this property without
either spec formally defining it. Formalized here to close that gap.

### 3.4 Harmonic-change rate

**Definition:** how frequently the chord root changes, expressed as
changes per bar (a value of `1.0` = every bar is a new chord; `0.25` =
one chord change per 4 bars).

**Formula (proposed):**
```
harmonicChangeRate(progression: List<ChordEvent>) =
    val changes = progression.zipWithNext().count { (a, b) -> a.rootMidi != b.rootMidi }
    changes.toFloat() / (progression.size - 1).coerceAtLeast(1)
```

**Status:** Fully derivable today. Named in the original handoff doc
(Section 9 of the second doc, "harmonic-change frequency") as a candidate
property, but **not used by either the Bass or Drum spec as written** —
both specs reason per-bar, not about the progression's overall
harmonic pacing. This is a real, unclaimed opportunity: `WALKING` bass
(Bass spec Section 3) is described as fitting "chord changes frequent
enough to give walking bass somewhere to go" — that's precisely what this
metric measures, but the spec left it as a qualitative judgment rather
than tying it to this computable value. Worth revisiting Bass Section 3's
`WALKING` row against this metric specifically.

---

## 4. Properties named in the original handoff docs, deliberately not
## formalized here

Per the first handoff doc's Section 9 ("Accent profile," "Coincidence,"
"Near-coincidence," "Sustained occupancy"):

- **Accent profile** — already partially covered by Section 2 of this doc
  (occupancy) and by the Bass spec's Section 5 observation that velocity
  data already exists per-style. A full accent-profile metric (distribution
  of strong/weak velocities across a bar) is real but explicitly deferred
  to Phase 5, since it's only meaningful once compared *across* tracks —
  a single-track accent profile has limited use on its own.
- **Coincidence / near-coincidence** — explicitly Phase 5's concern (Bass
  spec Section 10, Drum spec Section 6 and 11) since it inherently compares
  two tracks' data. Not formalized here on purpose — this document covers
  single-track derived properties only; cross-track comparison is Phase 5's
  job by design.
- **Sustained occupancy** — this is the same concept as Section 3.1 above
  (Rhythmic occupancy); no separate formalization needed, just noting the
  naming difference between this doc and the original handoff doc's
  terminology, so a future reader doesn't mistake them for two different
  properties.

---

## 5. Summary — the canonical property set

| Property | Formalized | Confirmed derivable today | Currently used by |
|---|---|---|---|
| Attack density | Section 2.1 | Yes | Bass §4, Drum §5 (should be reconciled to reference this doc) |
| Rhythmic occupancy | Section 3.1 | Yes | Not yet used — available for Phase 5 |
| Syncopation | Section 3.2 | Yes | Not yet used — flagged as a real gap in existing recommendation tables |
| Phrase position | Section 3.3 | Yes | Implicitly assumed by Bass §6, Drum §8 (`RESOLVE` rows) — now formally defined |
| Harmonic-change rate | Section 3.4 | Yes | Not yet used — flagged as directly relevant to Bass's `WALKING` recommendation |
| Accent profile | Deferred | Partial (raw velocity data exists) | Phase 5 |
| Coincidence / near-coincidence | Deferred | Data ready (Drum spec §6) | Phase 5 |

**Recommended action, not urgent but worth doing before Phase 6/7
implementation begins:** update the Bass and Drum specs' density
formulas to explicitly cite Section 2.1 of this document as their shared
source, rather than each carrying its own independent restatement — a
small documentation edit, not a design change, since the formulas are
already identical.

---

*End of Phase 2 (backfilled) deliverable.*
