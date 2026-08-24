# ReasonTouch — Sprint 0 Decision Doc
## Strum Persistence & Continuation Inheritance — Pre-Phase-3 Decisions

**Status:** Confirmed — all three decisions signed off by Andy, 2026-08-24. Ready for
Sprint 1.
**Scope:** Resolves the three open decisions flagged in
`ReasonTouch_Roadmap_Strum_Persistence_and_Continuation_Inheritance.md` Section 3,
so Phase 3 (Sprint 3a/3b/3c) never stalls mid-file waiting on a decision that should've
been made up front.
**No code changes in this sprint.** Paper only.

---

## Decision 1 — `addPhrase` consolidation direction

### The situation
Two separate, divergent implementations exist:

| | `ChordViewModel.addPhrase()` | `PianoRollViewModel.addPhrase()` |
|---|---|---|
| Writes | `ChordEvent`s only | `NoteEvent`s, not `ChordEvent`s in Manual's shape |
| Wired to SUGGEST? | No | Yes |
| Strum awareness | N/A (no notes generated) | None — flat block chords |

Only one of these is reachable from the flow this roadmap needs to fix (SUGGEST
continuations), but it's the one that doesn't write `ChordEvent`s in the same shape as
Manual — which matters because Phase 4 needs to read the *last* `ChordEvent`'s
`strumPatternId`/`strumSpeedValue` to inherit from.

### Option A — Consolidate into `PianoRollViewModel.addPhrase()`
Keep the SUGGEST-wired implementation as the survivor. Extend it to also write
`ChordEvent`s (currently doesn't), in the same shape Manual produces, so continuation
bars are readable by the same inheritance logic as seed bars.

- **Pro:** No change to what SUGGEST calls — lowest risk to the entry point that's
  actually exercised by users today.
- **Con:** `PianoRollViewModel` currently doesn't touch `ChordEvent` persistence at all;
  this pulls chord-writing responsibility into a ViewModel that hasn't owned it before,
  which is a bigger scope change to that file than it looks at first glance.

### Option B — Consolidate into `ChordViewModel.addPhrase()`
Keep the `ChordEvent`-writing implementation (already correct shape) as the survivor.
Extend it to also generate `NoteEvent`s via the Phase 3 shared strum-aware function
(the same one `sendToPianoRoll()` will use). Re-wire SUGGEST's call site to call this
instead of `PianoRollViewModel.addPhrase()`.

- **Pro:** `ChordViewModel` already owns chord persistence and is where
  `sendToPianoRoll()`'s strum-aware generation currently lives — this keeps chord logic
  and strum-aware note generation in the same ViewModel, avoiding a cross-ViewModel
  call for the shared function.
- **Con:** Requires re-wiring the SUGGEST call site (currently in Piano Roll's UI layer)
  to call into `ChordViewModel` instead — a bigger blast radius on the UI/wiring side,
  even though the ViewModel-side change is more natural.

### Recommendation: **Option B**
Rationale: the shared strum-aware generation function (Phase 3a) is being extracted
*from* `ChordViewModel.sendToPianoRoll()` — it's most natural for `ChordViewModel` to
also own the surviving `addPhrase()`, rather than having `PianoRollViewModel` reach
across to call a function that isn't "from" its own file. Chord persistence and
strum-aware generation living in the same ViewModel also matches the project's existing
"don't extract until multiple contexts need it" principle — here, keeping them together
avoids a premature cross-ViewModel dependency in the other direction.

The re-wiring cost (Con of Option B) is real but one-time and localized to the SUGGEST
call site — versus Option A's cost, which is ongoing scope creep into
`PianoRollViewModel` every time chord-shape logic needs to change in the future.

**✅ Confirmed by Andy: Option B** — consolidate into `ChordViewModel.addPhrase()`,
re-wire the SUGGEST call site.

---

## Decision 2 — `strumSpeed` unit mismatch: fix now or defer

### The situation
`strumSpeed` is documented as seconds but is added directly to a beats quantity with no
BPM-aware conversion, in both `sendToPianoRoll()` and `sendProgressionToPianoRoll()`.
This sits in the exact code path Phase 3 extracts into the shared function.

### Option A — Fix now, inside the Phase 3 extraction
Resolve the unit mismatch as part of pulling the conversion logic into the new shared,
pure function (Sprint 3b). Since the function is being touched and isolated anyway, this
is the cheapest point in the roadmap to fix it — it's testable in isolation immediately
after extraction, before any new behavior (Phase 4) is layered on top.

- **Pro:** Fixed once, in the one place, at the point of maximum isolation and testability.
- **Con:** Adds a genuine behavior change to Sprint 3b — strum timing will audibly shift
  for existing strum patterns wherever the mismatch was previously producing a
  usable-by-accident result. Needs explicit on-device verification that existing strum
  patterns still sound correct (or better) after the fix, not just that the code compiles.

### Option B — Defer, track as a follow-up item
Leave the mismatch as-is through this roadmap. Extract the function faithfully
(bug included), land Phases 3–4, and log a separate follow-up item for the unit fix.

- **Pro:** Smaller, more isolated Phase 3 — no behavior change beyond the duplication
  resolution itself.
- **Con:** The roadmap explicitly calls out "silently inheriting it into new shared code
  is the one outcome to avoid" — deferring without an explicit decision *is* silent
  inheritance. Deferring also means continuation inheritance (Phase 4) will inherit and
  perpetuate the same bug into the continuation flow, doubling the surface area that
  eventually needs re-testing when the fix does land.

### Recommendation: **Option A**
Rationale: the roadmap's own language flags silent inheritance as the failure mode to
avoid, and Sprint 3b is precisely the moment this is cheapest to fix — the function is
already being isolated and rewritten, so the marginal cost of also fixing the unit
conversion is low, while the cost of fixing it *later*, after Phase 4 has built
continuation inheritance on top of the buggy version, is meaningfully higher.

**Condition on choosing Option A:** Sprint 3b's exit criteria must include an explicit
before/after on-device listening check — strum timing for at least one known pattern at
a known speed, seed vs. after-fix — since this is a behavior change, not just a refactor,
and the project's working method calls for verifying behavior at each file, not just
compilation.

**✅ Confirmed by Andy: Option A** — fix now, in Sprint 3b, with the explicit
before/after on-device listening check as an exit condition.

---

## Decision 3 — Section 6a (safe one-time write / non-destructive regeneration)

### The situation
`sendToPianoRoll()` already performs a full destructive `repository.deleteNotesForTrack()`
on non-append sends today. The roadmap's Section 6a documents a scoped, transient,
write-time-only safety check (explicitly *not* durable provenance tracking — that was
already considered and rejected in the source design note) as a near-certain need once
bar-level strum editing UI exists, since that UI will make "regenerate a bar that may
contain hand-edited notes" a live, everyday scenario almost immediately after this
roadmap's persistence work lands.

### Option A — Fold into this roadmap (Sprint 3c or a Sprint 3d)
Build the write-time comparison check alongside the shared generation function, since
Phase 3 is already touching the exact call sites (`sendToPianoRoll()`,
`sendProgressionToPianoRoll()`) where the destructive delete happens.

- **Pro:** Same files, same sitting, avoids a second pass through code that's already
  open and already understood in detail.
- **Con:** Section 6a itself is not fully scoped — open questions remain (where the
  strum-speed review-editing UI lives, what conflict-surfacing looks like concretely,
  exact-vs-tolerant comparison). Folding an unscoped guardrail into an already
  multi-session roadmap risks exactly what Sprint 0 is trying to prevent elsewhere:
  starting implementation before the open questions are closed.

### Option B — Defer to its own follow-on roadmap
Land Phases 1–4 as scoped (append-only Phase 4 doesn't itself hit the overwrite-safety
problem — confirmed in the roadmap's Section 3). Write a short follow-on roadmap for
Section 6a immediately after, before starting bar-level strum editing UI or Bass/Drums
integration — both of which are the features that actually make the destructive-overwrite
scenario live.

- **Pro:** Keeps this roadmap's scope matched to what's actually specified. The
  guardrail becomes its own small, cleanly-scoped piece of work instead of a rider on
  an already-large one.
- **Con:** A second scoping pass is needed later — though this is a small, known cost,
  not a blocker.

### Recommendation: **Option B**
Rationale: Section 6a is explicitly flagged in the roadmap itself as "not yet scoped" —
it has open UI-location and comparison-precision questions that Sprint 0 is not the
place to resolve (Sprint 0 is closing decisions the roadmap *has* already framed as
binary choices; Section 6a isn't at that stage yet). Phase 4 as scoped is append-only and
doesn't trigger the destructive-overwrite scenario, so nothing in Sprints 1–4 is blocked
by deferring this. The real deadline is "before bar-level strum editing UI ships," not
"before Phase 4" — so there's no correctness risk in treating it as its own follow-on,
scoped once Phases 1–4 are stable and the UI questions can be answered with a working
data model in hand rather than a hypothetical one.

**✅ Confirmed by Andy: Option B** — defer to its own follow-on roadmap, scoped before
bar-level strum editing UI begins.

---

## Summary table (for quick sign-off)

| # | Decision | Confirmed | Andy confirms |
|---|----------|--------------|----------------|
| 1 | `addPhrase` consolidation | Option B — consolidate into `ChordViewModel.addPhrase()`, re-wire SUGGEST call site | ✅ |
| 2 | `strumSpeed` unit mismatch | Option A — fix now, in Sprint 3b, with explicit before/after on-device check | ✅ |
| 3 | Section 6a safe-write guardrail | Option B — defer to its own follow-on roadmap, scoped before bar-level strum editing UI begins | ✅ |

All three confirmed 2026-08-24. Sprint 1 (Phase 1: schema) can begin without further
scoping pauses through Sprint 4.

---

*End of Sprint 0 decision doc.*
