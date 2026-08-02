# ReasonTouch -- Design Note: Safe One-Time Write Behavior for Auto-Generating Actions

**Date:** 2026-08-01
**Status:** Design discussion, not yet scoped into implementation tasks
**Context:** Arose from investigating a strum-speed persistence gap, then generalized after recognizing it shares a root problem with the roadmap's bass-first harmonisation item

---

## 1. The problem, in one sentence

When an auto-generating action is about to write notes into a bar that may already contain notes -- whether from a previous generation, from manual drawing, or both -- how should it behave, without building a system that tracks note history forever?

---

## 2. How this was found

### 2.1 Starting point: strum speed went missing

The legacy pre-project design (`ProgressionBar` data class, `MidiProgressionBar.kt`) modeled strum speed as a **per-bar** field, alongside strum pattern. Investigation confirmed this is still fully alive in the legacy `ChordScreen`/`ChordViewModel`/`StrumData.kt`/MIDI-writer pipeline (`STRUM_SPEED_PRESETS`, `StrumSpeed` enum, `Session.defaultStrumSpeed`), but it never made it into `ChordEvent` -- the entity that actually persists chords in the current, `ManualWorkspace`-driven flow.

`ChordEvent`'s current fields (confirmed via direct inspection):
```kotlin
val id: String
val sessionId: String
val barIndex: Int
val chordName: String
val rootMidi: Int
val midiNotes: String
val voicing: String
val strumPatternId: String? = null
```

`strumPatternId` is present (which pattern was used); `strumSpeed` is not (how fast it was played). This gap affects every workspace that writes `ChordEvent`s with strum patterns -- confirmed to include Manual, Mood, Inspire, and Progression (the Assisted workspaces), not just Manual.

### 2.2 The design question that followed

Initial framing: should strum pattern/speed remain fixed at composition time (consistent throughout a progression), or become editable per bar during a later review/audition stage?

Per-bar is already `ChordEvent`'s natural granularity (each row is indexed by `barIndex`), so adding `strumSpeed` to `ChordEvent` already supports per-bar values without further schema work. The open question was purely about *when* editing happens: composition time (new bars inherit current settings) vs. review time (editing an existing bar's settings after the fact).

### 2.3 Where it got harder: regeneration safety

Editing a `ChordEvent`'s strum speed after it has already been sent to Piano Roll does not retroactively change anything -- the `NoteEvent`s already exist as fixed `beat`/`pitch`/`duration` values, disconnected from the `ChordEvent` that produced them. Making a review-stage edit audible requires regenerating the affected notes.

`NoteEvent`'s current fields have no reference back to a source `ChordEvent` or bar:
```kotlin
val id: String
val trackId: String
val pitch: Int
val beat: Float
val duration: Float
val velocity: Int
```

"Regenerate just this bar's notes" would have to infer which notes belong to a bar via beat-range math. This is fragile, and actively dangerous if the user has manually drawn, erased, or nudged notes in that range since the original generation -- a naive regenerate-and-overwrite would silently destroy manual edits with no warning.

### 2.4 The generalization: bass-first harmonisation has the identical shape, in reverse

The roadmap already includes "bass-first harmonisation": draw a bassline in Piano Roll, infer chord candidates from it. This is the mirror image of the strum-speed problem:

- **Strum-speed re-edit**: notes that *did* originate from a `ChordEvent`, need to be regenerated from a changed parameter, risk clobbering anything drawn on top since.
- **Bass-first harmonisation**: notes that *never* originated from a `ChordEvent`, need to be read *backward* to infer a `ChordEvent` that didn't exist before.

Both hinge on the same missing piece: a real, queryable relationship between raw `NoteEvent` content and the harmonic structure (`ChordEvent`/bar) it belongs to or implies. Currently that relationship is implicit, one-directional at best (chord generates notes once, then the link is lost), and doesn't exist in reverse at all.

---

## 3. Why this is NOT scoped as permanent provenance tracking

The natural-seeming next step is a durable "is this bar's content generated / manual / mixed" state, tracked indefinitely per bar or per note. This was considered and deliberately rejected.

**Reasoning:** ReasonTouch's stated identity is a composition *sketching* tool that hands off to a real DAW for development -- explicitly not competing at the mobile-DAW level. The early design doc already drew this exact boundary once before, ruling out features like biquad filters and velocity-to-filter modulation as pushing toward "mobile DAW" territory rather than "composition sketch" territory. A permanent, durable provenance system that safely reconciles arbitrary generate/hand-edit/regenerate cycles indefinitely is architecturally the same category of overreach: it solves for a workflow (repeated iterative editing over the same material, forever) that is closer to what a DAW is for, not what this app is for.

The user is not expected to cycle indefinitely between auto-generation and manual editing on the same bars. They are expected to reach a good-enough sketch and then take it elsewhere to develop further. Provenance tracking as *durable state* would be solving a problem the app's own premise says shouldn't really arise.

---

## 4. The scoped alternative: safe one-time write

Reframe the requirement narrowly: **when an auto-generating action is about to write notes into a bar, how does it behave safely with respect to whatever's already there in that moment -- without remembering that decision forever?**

This is a transient, in-the-moment safety check, not a persistent data model addition.

### 4.1 What this looks like for strum-speed re-editing

The regenerate action itself needs to be safe at the moment it fires:
- Before overwriting a bar's notes, compare what's about to be written against what currently exists in that beat range.
- If the existing content in that range doesn't match what the *previous* generation from this `ChordEvent` would have produced (i.e. something has changed since), surface this to the user before overwriting -- a confirmation, a diff-style warning, or a non-destructive "regenerate as new notes alongside" option, rather than a silent overwrite.
- No new field needs to persist this decision afterward. The check happens once, at write time, using only currently-live data (the `ChordEvent`'s last-known generated output vs. the bar's current actual content).

### 4.2 What this looks like for bass-first harmonisation

The inference step is inherently one-time by nature:
- User draws a bassline -> triggers inference -> `ChordEvent`s are created from the inferred chords -> from that point forward, those `ChordEvent`s are the source of truth for that bar, exactly like any other bar created through Manual or Assisted workspaces.
- No ongoing tracking of "this came from a drawn bassline" is needed after inference completes. The provenance question only matters for the single moment of inference, not as continuing system state.
- The main safety question here is narrower than the strum-speed case: what happens if the user runs inference on a bassline, then keeps editing the bassline further? Does re-running inference on the same bar overwrite the previously-inferred `ChordEvent`, or create a new one? This is a smaller, more contained version of the same "about to overwrite -- what's actually there right now?" check as 4.1.

### 4.3 Shared mechanism, no shared persistent state

Both cases can likely be served by the same small piece of logic: **a "did the target range change since I last touched it" check**, computed at write time by comparing current `NoteEvent` content in the relevant beat range against what would be expected if nothing had changed. This does not require a new column on `NoteEvent`, a provenance enum, or any additional persisted state -- it's a runtime comparison, not a durable fact the system needs to remember.

If, in practice, this comparison proves too imprecise via beat-range inference alone (e.g. false positives from legitimate but content-identical regenerations), the fallback is a much smaller addition than full provenance: a single, ephemeral marker (e.g. "last generated content hash for this `ChordEvent`", stored only long enough to support one write-time comparison) rather than a durable per-note history.

---

## 5. Open questions, not yet resolved

1. **Where does strum-speed review-editing actually live?** Back in `ManualWorkspace`'s bar-chip list (edit before/instead of resending), or directly against the chord ghost lane in Piano Roll (edit while auditioning, which is where the original framing -- "review/audition stage" -- points)? The latter is more ambitious, since Piano Roll currently only displays chord data, not edits it.
2. **What does "surfacing a conflict" actually look like in the UI** -- a blocking confirmation dialog, a subtle non-blocking warning, or an alternate non-destructive write path ("add as new" instead of "overwrite")? Given the app's "choice, not imposition" design philosophy (see the multi-option `PairingDecision` discussion), a non-blocking or user-choice-driven approach likely fits better than a hard block.
3. **Does the comparison need to be exact or fuzzy?** A single manually-nudged note within an otherwise-generated bar is a different situation from a bar that's been substantially hand-rewritten -- worth deciding whether "any difference at all" or "difference beyond some threshold" triggers the safety check.
4. **Bass-first harmonisation's re-run behavior** (4.2) still needs its own small decision once that feature is actually scoped -- overwrite vs. new `ChordEvent` vs. prompt.

---

## 6. Relationship to other in-flight work

- Independent of the `ChordSuggestionEngine` candidate-cap fix and the multi-option `PairingDecision` design -- no direct dependency in either direction.
- The strum-speed field addition to `ChordEvent` (schema + migration + workspace UI control) can proceed as a contained task on its own, ahead of the safe-write mechanism -- composition-time strum speed doesn't need any of this write-safety logic, only review-time re-editing does.
- Bass-first harmonisation remains a longer-horizon roadmap item; this document doesn't move it up in priority, it just records that when it is eventually scoped, it should reuse the same write-safety approach as strum-speed re-editing rather than developing its own separate mechanism.

---

*End of design note.*