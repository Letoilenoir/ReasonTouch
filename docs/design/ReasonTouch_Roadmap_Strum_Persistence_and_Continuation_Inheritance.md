# ReasonTouch — Roadmap: Strum Persistence & Continuation Inheritance

**Status:** Planning complete, implementation not started
**Priority:** Blocking — precedes Bass/Drums tray panel integration (item 6 of the
2026-08-22 handoff's Section 5 order)
**Created:** 2026-08-23
**Estimated scope:** Multi-session. Do not attempt in a single sitting.

---

## 1. Why this is its own workstream

This started as a one-line fix (`hasHarmony` signal in `CompositionTray`) and, in the
course of investigating a related request — continuations inheriting the seed's strum
character — surfaced a chain of real, load-bearing findings:

- Strum pattern and strum speed are **ephemeral send-time values today**, not persisted
  per-bar, despite `ChordEvent.strumPatternId` existing in the schema since the
  prototype. It has been written as `null` at every call site, in every version of the
  app, without exception.
- A second, fuller persistence layer for this exact concept (`StrumPattern`, `StrumStep`,
  `StrumPatternDao`, `StrumStepDao`, `StrumSpeed`) already exists, is registered in the
  live Room schema (`AppDatabase`, entities list), and is **completely orphaned** — no
  feature module reads or writes it. Confirmed via project-wide case-sensitive grep.
- `addPhrase()` exists as **two separate, divergent implementations**
  (`ChordViewModel.addPhrase()` and `PianoRollViewModel.addPhrase()`), matching the
  duplication already flagged for `sendToPianoRoll()`/`sendProgressionToPianoRoll()`.
  Only one of them (`PianoRollViewModel`'s) is actually wired to the SUGGEST flow, and it
  currently has zero strum awareness — flat block chords only.
- Strum-aware note generation exists in exactly one place today
  (`ChordViewModel.sendToPianoRoll()`). For continuations to inherit strum character, that
  logic has to become reachable from wherever `addPhrase` ends up living — which means
  this cannot be done as an isolated addition. It requires resolving the pre-existing
  `addPhrase` duplication as a precondition, not an optional cleanup alongside it.
- There is a known, unresolved unit mismatch (`strumSpeed` documented as seconds,
  added directly to a beats quantity with no conversion) sitting in the exact code path
  this work will extend. A deliberate decision is needed on whether to fix it in the same
  pass or explicitly defer it — silently inheriting it into new shared code is the one
  outcome to avoid.
- This touches three modules (`core-data`, `feature-chords`, `feature-pianoroll`), all of
  which have prior silent-breakage incidents (shadowed functions, dropped closures,
  doubled braces). The project's own working method — one file at a time, build/Problems
  panel verification after each — means this is inherently multi-session work.

Given all of the above, and that Bass/Drums generation (the next planned major feature)
will itself need to read harmonic + rhythmic context from whatever `ChordEvent` ends up
storing, doing this first — rather than building Bass/Drums against today's incomplete
data model — avoids a second, larger rework later.

---

## 2. Decisions already made (do not re-litigate without cause)

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Build on `ChordEvent` + the live `StepPattern` / `STRUM_SPEED_PRESETS` model, **not** the orphaned `StrumPattern`/`StrumStep` DB tables | Reviving a disconnected second schema mid-feature means reconciling two incompatible vocabularies (`StrumStep.direction: String` vs `StepState` enum; `StrumSpeed.offsetMs: Long` vs `STRUM_SPEED_PRESETS.beatsPerString: Double`) for no functional gain |
| 2 | `strumPatternId` (name is now inaccurate but retained to avoid a rename-driven migration) will store a **16-char encoded step string** (`D`/`U`/`.` per step), not a preset name/ID | Self-contained; not broken by future renaming or removal of named presets in `StrumPatterns.groups` |
| 3 | New `strumSpeedValue: Double?` field stores the **raw `beatsPerString` value**, not a preset label | The raw number is what generation actually consumes; labels are a display concern only |
| 4 | Schema bump is `version 4 → 5`, **no `Migration` object**, relying on existing `fallbackToDestructiveMigration(true)` | Confirmed via `DatabaseModule.kt`; no real migrations exist anywhere in the project today. All current sessions are mechanics-testing only and disposable — confirmed acceptable by Andy (2026-08-23) |
| 5 | `hasHarmony` in `CompositionTray`'s call site now reads `chords.isNotEmpty()` (already applied, verified on-device) instead of the old `tracks.any { allNotes[it.id]?.isNotEmpty() == true }` heuristic | More correct signal; also forward-compatible with a future bass-first harmonisation feature — see Section 6 |
| 6 | `StrumPattern`/`StrumStep`/`StrumPatternDao`/`StrumStepDao`/`StrumSpeed` (core-data) are flagged as a confirmed-orphaned-schema removal candidate | Separate future cleanup pass — a proper `Migration` dropping the tables, not bundled into this work |

---

## 3. Open decisions this roadmap does NOT resolve

These need explicit answers before or during implementation — flagging now so they're
decided on purpose, not defaulted into:

- **`addPhrase` consolidation direction.** Which implementation survives —
  `PianoRollViewModel.addPhrase()` (currently wired to SUGGEST, writes `NoteEvent`s but
  not `ChordEvent`s in the same shape as Manual) or `ChordViewModel.addPhrase()`
  (writes `ChordEvent`s only, no notes)? Most likely resolution: consolidate into a
  shared function reachable from both ViewModels, but the call-site wiring needs a
  concrete decision, not just "extract a shared function" left vague.
- **`strumSpeed` unit mismatch** (seconds documented, added directly to beats with no
  conversion in both `sendToPianoRoll()` and `sendProgressionToPianoRoll()`). Decide:
  fix as part of this work (touches the same generation code being extracted/shared
  anyway), or explicitly defer with a tracked follow-up item.
- **`sendToPianoRoll()` / `sendProgressionToPianoRoll()` duplication.** Already flagged
  in the 2026-08-22 handoff as unaddressed. This roadmap's Phase 3 will likely need to
  touch both regardless (since one of them is the only place strum-aware generation
  currently lives) — worth deciding whether to fully extract a shared function now or
  duplicate the fix a second time and extract later.
- **Bar-level strum editing UI.** Confirmed as the original design intent behind making
  `strumPatternId` per-bar (Andy, 2026-08-23) — allowing a user to change an individual
  bar's pattern/speed after the fact. This roadmap makes that data-model-possible but
  does **not** include building that editing UI. Worth flagging as a natural next feature
  once persistence lands, not assuming it's in scope here.
- **Safe one-time write scope, this roadmap vs. follow-on.** See Section 6a. Phase 4 as
  currently scoped is append-only (new bars go after `progression.value.size`) and does
  not itself hit the overwrite-safety problem. But Phase 3's duplication-resolution work
  touches `sendToPianoRoll()`, which already performs a full destructive
  `repository.deleteNotesForTrack()` on non-append sends today — and the bar-level strum
  editing UI flagged above (a near-certain next feature) will make the "regenerate a bar
  that may contain hand-edited notes" scenario live almost immediately after persistence
  lands. Decide before Phase 3 begins whether the write-time safety check (Section 6a)
  is built now, alongside the shared generation function it would sit next to anyway, or
  explicitly deferred to its own follow-on roadmap.

---

## 4. Phased implementation plan

### Phase 1 — Schema (small, low-risk, ready to execute)

1. `ChordEvent.kt` — add `strumPatternId: String?` (repurpose existing field, update
   comment) and `strumSpeedValue: Double? = null`.
2. `AppDatabase.kt` — bump `version = 4` → `version = 5`.
3. Build, verify clean compile. Uninstall app / clear app data on device before next
   run (destructive fallback will wipe on next launch regardless, but do this
   deliberately rather than relying on it silently).
4. Verify on-device: create a session, confirm no crash, confirm existing flows
   (add bar, send to roll) still work with the two fields present but unpopulated.

**Exit criteria:** Clean build, app launches, existing functionality unaffected, new
fields exist in the schema but are not yet written to.

---

### Phase 2 — Encoding + write-side population (small-medium)

1. Create `StrumEncoding.kt` in `feature-chords`:
    - `StepPattern.toChordEventString(): String`
    - `String.toStepPattern(): StepPattern` (tolerant of null/malformed → falls back to
      `StepPattern.EMPTY`)
2. Update `ChordViewModel.kt` write sites to populate both new fields instead of `null`:
    - `addBar()`
    - `addSuggestedChord()`
    - `addBorrowedChord()`
3. Build, verify. Test: add several bars via Manual with different patterns/speeds
   selected, pull the DB via the existing sqlite extraction process
   (`ReasonTouch_Bug_Investigation_Handoff.md` Section 6/7), confirm the two new
   columns contain expected encoded values per bar.

**Exit criteria:** Every newly-created `ChordEvent` correctly persists its pattern and
speed at time of creation; verified directly against the database, not just UI behaviour.

---

### Phase 3 — Resolve `addPhrase` duplication + make strum-aware generation shared
(medium-large, the core of this work)

1. Decide `addPhrase` consolidation direction (see Section 3, open decision).
2. Extract the strum-to-`NoteEvent` conversion logic currently embedded in
   `ChordViewModel.sendToPianoRoll()` into a shared, pure function — callable with
   `(chordEvent, pattern, speed, beatStart, beatsPerBar, targetTrackId)` or similar —
   independent of which ViewModel calls it.
3. Decide, and resolve, the `strumSpeed` unit mismatch (Section 3) as part of this
   extraction, since the function is being touched regardless.
4. Wire the surviving `addPhrase` implementation to use this shared function instead of
   its current flat block-chord generation.
5. One file at a time; build + Problems panel check after each file, per established
   working method.

**Exit criteria:** A single, shared, strum-aware note-generation function exists and is
reachable from the continuation flow. No behavioural change yet to what continuations
actually produce — this phase makes inheritance *possible*, Phase 4 makes it *happen*.

---

### Phase 4 — Continuation inheritance (small, once Phase 3 lands)

1. In the surviving `addPhrase`, read the **last** `ChordEvent` in the existing
   progression before generating the continuation.
2. Decode its `strumPatternId` / `strumSpeedValue` via `StrumEncoding.kt`.
3. Pass the decoded pattern + speed into the shared generation function (Phase 3) instead
   of defaulting to block chords.
4. Ensure the newly-created continuation `ChordEvent`s also populate their own
   `strumPatternId`/`strumSpeedValue` (inherited values), so a second round of
   continuation correctly inherits from the first, not from the original seed only.
5. Verified on-device: reproduce the exact test case that surfaced this need — seed
   E G B A via Manual, run 2+ iterations of SUGGEST continuations, confirm strum
   character is audibly consistent across all bars, not just the seed.

**Exit criteria:** Confirmed via the E-G-B-A repro case (or equivalent) that
continuations audibly inherit the seed's strum pattern and speed, across multiple
rounds of continuation, not just the first.

---

## 5. Related, not blocking — can proceed independently

- **Bar 15/16 playback overrun** (E-G-B-A / 16-bar test session): separate root cause
  under investigation — likely a stale epsilon-boundary check in
  `Sequencer.computeEndBeat()` left over from when the function used note end-time
  rather than start-time (post the 2026-08-22 overrun fix). Can be diagnosed and fixed
  independently of this roadmap; not a prerequisite for any phase above.
- **Items 6/7 from the 2026-08-22 handoff** (real BASS/DRUMS tray panels; EXPAND doc
  cleanup) — explicitly sequenced *after* this roadmap per Andy's stated priority, since
  Bass/Drums generation will want to read the harmonic+rhythmic data model this roadmap
  establishes, rather than being built against the current incomplete one.
- **"Safe One-Time Write Behavior for Auto-Generating Actions"** (2026-08-01 design
  note, reviewed and folded in 2026-08-23) — see Section 6a. Not blocking Phase 1 or 2;
  needs a scope decision before Phase 3 (see Section 3's corresponding open decision).

---

## 6a. Safe one-time write (non-destructive regeneration) — guardrail, not yet scoped

Surfaced by a separate 2026-08-01 design note ("Safe One-Time Write Behavior for
Auto-Generating Actions"), reviewed and folded in here on 2026-08-23. Full reasoning
lives in that document; summarized here so it isn't lost or rediscovered mid-Phase-3.

**The problem it identifies:** any action that regenerates/overwrites notes in a bar
that may already contain manually-edited content risks silently destroying that manual
work. Not hypothetical — `sendToPianoRoll()` already does a full destructive
`repository.deleteNotesForTrack()` on non-append sends today, and the bar-level strum
editing UI (flagged as a near-certain next feature once this roadmap's persistence work
lands — see Section 3) will make this a live, everyday scenario almost immediately:
user re-picks a bar's strum speed expecting regeneration, but may have hand-drawn notes
on top of the originally-generated ones since.

**Explicit non-goal — stated as a guardrail, not to be silently reintroduced:**
the natural-seeming answer (durable, persistent "generated / manual / mixed" provenance
tracked per bar or per note, indefinitely) was considered and deliberately rejected in
the source document. Reasoning: ReasonTouch's stated identity is a composition
*sketching* tool that hands off to a real DAW for development — not a mobile DAW. A
permanent system reconciling arbitrary generate/hand-edit/regenerate cycles forever is
solving for a workflow (repeated iterative editing over the same material, indefinitely)
that belongs to what a DAW is for, not this app. If a future contributor reaches for
persistent provenance tracking as the obvious fix, treat that as a signal to re-read the
source document's reasoning before proceeding, not as a green light.

**The scoped alternative:** a transient, write-time-only safety check — no new persisted
state. Before an auto-generating action overwrites a bar's notes, compare what's about to
be written against what currently exists in that beat range; if the existing content
doesn't match what the last generation from this source would have produced, surface a
conflict (confirmation, warning, or non-destructive "add as new" path — leaning toward
the app's existing "choice, not imposition" philosophy over a hard block) rather than
silently overwriting. The comparison uses only currently-live data and does not need to
be remembered afterward. If beat-range comparison proves too imprecise in practice, the
documented fallback is a single ephemeral "last generated content" marker per
`ChordEvent` — kept only long enough to support one write-time comparison, not a durable
history.

**Also identifies the same mechanism serves bass-first harmonisation** (Section 6b) in
reverse: inferring a `ChordEvent` backward from drawn notes is a one-time operation with
an identical "what's already there, and what happens if the user re-runs it" shape — not
a case for ongoing provenance either.

**Open questions this roadmap does not resolve** (from the source document, still valid):
where strum-speed review-editing actually lives in the UI (bar-chip list vs. Piano Roll
ghost lane); what conflict-surfacing looks like concretely; whether the write-time
comparison needs to be exact or tolerate minor manual nudges before triggering.

**Note on a discrepancy in the source document:** its Section 2.1 describes the
`core-data.StrumSpeed` enum as "still fully alive in the legacy `ChordScreen`/
`ChordViewModel`/`StrumData.kt`/MIDI-writer pipeline." That does not match this
session's confirmed, case-sensitive, project-wide grep (Section 2 above) showing zero
references to the `core-data.StrumSpeed` enum outside `core-data` itself — every match
was `ChordUiState.strumSpeed: Double` or `STRUM_SPEED_PRESETS`, a separate
`feature-chords` system that happens to share a name. The source document predates that
grep (written 2026-08-01) and most likely uses "StrumSpeed" loosely to mean the
strum-speed *concept*, not the literal `core-data` enum — but flagged explicitly so a
future reader isn't misled into thinking the orphaned `core-data` enum is actually wired
up somewhere it isn't.

---

## 6b. Note on bass-first harmonisation (forward compatibility)

Addendum carried over from the `hasHarmony` fix (2026-08-23): the still-unscoped
bass-first concept (draw a bassline → infer chord candidates) was checked against the
`hasHarmony = chords.isNotEmpty()` change and found compatible — see the design docs
addendum already recorded there. Worth re-checking against this roadmap once bass-first
is actually scoped: if inferred chords end up persisted as real `ChordEvent` rows (the
current working assumption), they will need their own `strumPatternId`/`strumSpeedValue`
population strategy — most likely inheriting from whatever chord/bar they're paired
with, following the same inheritance logic this roadmap establishes for SUGGEST
continuations. Not in scope now; flagged so it isn't rediscovered as a surprise later.
Also see Section 6a — bass-first's inference step shares its safe-write shape.

---

*End of roadmap.*