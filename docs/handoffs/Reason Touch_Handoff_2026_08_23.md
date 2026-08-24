# ReasonTouch — Handoff, 2026-08-23

**Branch:** `master`
**Status:** One fix committed and verified this session. One larger workstream fully
scoped but not started — dedicated roadmap document produced, implementation deferred
to a future session.

---

## 1. What this session did

### Fix: playback stopping one bar early on exact-boundary progressions

**File:** `core/core-playback/src/main/java/com/reasontouch/core/playback/Sequencer.kt`

**Symptom:** A 16-bar progression (E G B A seed via Manual + 2 rounds of SUGGEST
continuations) stopped playback at the end of bar 15, never reaching bar 16.

**Root cause:** `computeEndBeat()`'s epsilon "already at a bar boundary" branch was
introduced in the June 2026 session when the function used note **end** time
(`beat + duration`) as input — under that model, a note ending exactly on a boundary
really has finished, so not rounding up was correct. The August 2026 session's
strum-overrun fix changed the function's input to note **start** time only (`beat`,
never `beat + duration`), for unrelated and still-valid reasons (strummed chord ring
duration must survive into MIDI export undamaged). The epsilon branch was never
re-derived for that change and became silently wrong: a note starting exactly on a
bar boundary (as every bar's first note does, by construction — `beatStart = barIndex *
beatsPerBar`) was being treated as "already ended," undercounting the final bar whenever
a progression's bar count was an exact multiple of the epsilon check's alignment.

**Fix:** Removed the epsilon branch entirely. A start beat should always round up to
include the bar it starts within — there is no remaining case, under start-time
semantics, where landing exactly on a boundary means "don't count this bar."

```kotlin
// Always round up: a note's start beat, even exactly on a bar boundary, means
// that bar is in use and must be counted. (The epsilon "already at boundary"
// special case below was correct when this used note END time -- a note ending
// exactly at a boundary really has finished -- but is invalid now that this is
// START time only. Removed rather than adjusted, since there's no remaining
// case where a start beat on a boundary should NOT round up.)
val barNumber = ((lastNoteBeat / 4f).toInt() + 1)
    .coerceAtLeast(1)
```

**Verified on-device:** E G B A seed + 2 rounds of SUGGEST continuations (16 bars total)
now plays through to the end of bar 16 and stops correctly, rather than stopping at the
end of bar 15. ✅

**Known pre-existing characteristic, not introduced or worsened by this fix:**
`computeEndBeat()` hardcodes `4f` as bar length throughout, rather than deriving it from
session `barDuration`/`timeSignatureNumerator`. Not touched this session; worth keeping
in mind if a non-4/4 or non-default-bar-duration session ever exhibits similar
off-by-one symptoms.

---

## 2. Roadmap produced, not yet started: Strum Persistence & Continuation Inheritance

Investigating a related request (SUGGEST continuations should inherit the seed
progression's strum pattern and speed, not default to flat block chords) surfaced a
chain of findings large enough to warrant its own dedicated roadmap rather than an
in-session fix. Full detail, phased plan, and open decisions are in:

**`ReasonTouch_Roadmap_Strum_Persistence_and_Continuation_Inheritance.md`**

Headline findings, for context if this handoff is read before that roadmap:

- `ChordEvent.strumPatternId` has existed since the prototype but has been written as
  `null` at every call site in every version of the app — strum pattern/speed have only
  ever been ephemeral, send-time values, never persisted per-bar.
- A second, fuller persistence layer for this exact concept (`StrumPattern`, `StrumStep`,
  `StrumPatternDao`, `StrumStepDao`, `StrumSpeed`) already exists and is registered in
  the live Room schema (`AppDatabase`, version 4) but is **completely orphaned** —
  confirmed via project-wide case-sensitive grep, nothing outside `core-data` references
  any of the five identifiers. Flagged as a future cleanup/removal candidate, not
  resurrected.
- `addPhrase()` exists as two separate, divergent implementations
  (`ChordViewModel.addPhrase()` and `PianoRollViewModel.addPhrase()`) — only the Piano
  Roll one is wired to SUGGEST, and it has zero strum awareness today. This duplication
  has to be resolved as a precondition of continuation inheritance, not alongside it.
- Confirmed with Andy: `fallbackToDestructiveMigration(true)` is the existing (only)
  migration strategy — no real `Migration` objects exist in the project. All sessions
  to date are mechanics-testing only and explicitly disposable, so the schema bump this
  roadmap requires (`ChordEvent` v4 → v5, two new nullable fields) is accepted to wipe
  existing session data on next launch. Confirmed acceptable — not a concern for future
  real composition data once this settles.

**Decision confirmed this session:** build on the live `StepPattern`/`STRUM_SPEED_PRESETS`
model, not the orphaned DB tables. `strumPatternId` will store a 16-char encoded step
string (not a preset name/ID), decoupling stored data from future preset renaming.

**Priority, per Andy:** this roadmap is to be completed before beginning Bass/Drums tray
panel integration (item 6 of the 2026-08-22 handoff's Section 5 order), since Bass/Drums
generation will want to read the harmonic+rhythmic data model this roadmap establishes
rather than being built against today's incomplete one.

**Not started this session.** Four phases defined (Schema → Encoding → `addPhrase`
duplication resolution / shared strum-aware generation → Continuation inheritance), each
with build/verify exit criteria. Phase 1 (schema) is fully specified and ready to execute
in a future session without further scoping.

---

## 3. `hasHarmony` signal fix (carried over from earlier this session, verified)

**File:** `feature/feature-pianoroll/src/main/kotlin/com/reasontouch/feature/pianoroll/PianoRollScreen.kt`

Changed `CompositionTray`'s `hasHarmony` from a note-presence heuristic
(`tracks.any { allNotes[it.id]?.isNotEmpty() == true }`) to a chord-presence check
(`chords.isNotEmpty()`, using `PianoRollViewModel.chords`, already-live and reactive but
previously uncollected in this screen). Fixes a false-positive risk: the old heuristic
would report "harmony present" from *any* track having *any* notes, including a
bass-only or drums-only session once those generators exist. Checked explicitly against
the still-unscoped bass-first harmonisation concept (draw bassline → infer chords) and
found compatible — documented as an addendum in the relevant design doc section.
**Verified on-device.**

---

## 4. Remaining from the 2026-08-22 handoff's Section 5 order

Item 5 (`hasHarmony`/`hasBass`/`hasDrums` placeholder signals) — `hasHarmony` now
resolved (Section 3 above). `hasBass`/`hasDrums` remain genuinely unpassed at the
`CompositionTray` call site (not just `false` — absent), correctly deferred to item 6.

Still open, unchanged from 08-22:

6. **Real BASS/DRUMS tray panels**, replacing `TrayStubPanel` — deferred until the
   Strum Persistence roadmap (Section 2 above) is complete, per Andy's stated priority.
7. **EXPAND doc cleanup** (parked, low priority, no functional impact) —
   `UX_Direction.md` Sections 5/6/10 still need a superseded-status note. No urgency,
   can happen at any point independent of other work.

---

## 5. Files touched this session

### Modified
- `core/core-playback/src/main/java/com/reasontouch/core/playback/Sequencer.kt` —
  `computeEndBeat()` epsilon-boundary fix
- `feature/feature-pianoroll/src/main/kotlin/com/reasontouch/feature/pianoroll/PianoRollScreen.kt` —
  `hasHarmony` signal fix (collected `viewModel.chords`, swapped heuristic)

### Created (documentation only, not code)
- `ReasonTouch_Roadmap_Strum_Persistence_and_Continuation_Inheritance.md`

### Not yet touched (roadmap Phase 1, ready when picked up)
- `core/core-data/src/main/kotlin/com/reasontouch/core/data/ChordEvent.kt`
- `core/core-data/src/main/kotlin/com/reasontouch/core/data/AppDatabase.kt`

---

## 6. Working method notes for next session

- The `computeEndBeat()` fix is a good example of the project's recurring lesson: a
  correctness assumption (epsilon branch) baked in under one set of semantics silently
  became wrong when an unrelated fix changed the function's input meaning, without
  anyone revisiting the now-orphaned assumption. Worth a general habit: when changing
  what a value *means* (start time vs end time, seconds vs beats, etc.), grep the
  function for other logic that implicitly assumed the old meaning, not just the line
  that prompted the change.
- The Strum Persistence investigation is a strong example of why "is this schema/code
  actually live?" is worth confirming via grep before building on top of it, rather than
  assuming a class's existence implies active use. The orphaned `StrumPattern`/
  `StrumStep` DB layer would have been easy to assume was the intended foundation; it
  wasn't.
- Continue the practice of pausing to scope multi-file, multi-module changes into a
  written roadmap before starting, rather than discovering the full scope mid-edit.

---

*End of handoff.*
