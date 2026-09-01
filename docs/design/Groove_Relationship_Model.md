# ReasonTouch — Groove Relationship Model
## Phase 5 Deliverable: Bass/Drum Arrangement Roadmap

**Status:** Draft — Phase 5, the final specification phase before
implementation (Phase 6 onward)
**Depends on:** `CompositionContext_Specification.md` (Phase 1),
`Bass_Arrangement_Specification.md` (Phase 3),
`Drum_Arrangement_Specification.md` (Phase 4),
`Derived_Musical_Properties.md` (Phase 2, backfilled)
**Purpose:** Resolve the cross-track questions Phases 3 and 4 each
explicitly deferred, since neither spec alone had enough information to
answer them.
**No implementation change in this phase.** Paper only — this is the last
purely-paper phase; Phase 6 begins implementation.

---

## 1. The three deferred questions, restated

From Bass spec Section 10 and Drum spec Section 11:

1. **Density reconciliation** — both specs independently propose
   "simplify when chord rhythm is busy." If both act on this simultaneously
   and independently, does the arrangement end up too thin?
2. **Kick/Bass coincidence scoring** — data confirmed ready (same beat-based
   coordinate system, per Phase 1 Section 7.4); the actual scoring model
   (reinforcement vs. collision) was left undecided.
3. **Whether structural variation (Drum spec Section 7) needs Phase 5 at
   all** — already answered in the earlier discussion this session: **no**,
   it's independently implementable and shouldn't wait.

This document resolves questions 1 and 2. Question 3's answer (already
settled) is restated in Section 5 for completeness.

---

## 2. Question 1 — Density reconciliation

### 2.1 The actual risk, stated precisely

Bass spec Section 4: dense chord rhythm → favor `ROOT`/`PEDAL` (simplify).
Drum spec Section 5: dense chord rhythm → favor `FOUR_FOUR`/sparser
(simplify).

If a session has genuinely dense chord strumming, and both recommendations
fire independently, the result is: busy chords + simple bass + simple
drums. That is **not necessarily wrong** — a busy rhythm guitar part
carrying all the rhythmic interest while bass and drums lay down a steady
foundation is a completely standard, common arrangement (many funk and pop
arrangements work exactly this way: one busy element, everything else
locked down simple underneath it).

**The actual risk is narrower than "both simplify":** it's specifically
**both simplifying to the point of near-inactivity at the same time** —
e.g. Bass on a single sustained `PEDAL` note *and* Drums reduced to just
kick-on-1. That combination genuinely could feel thin, since nothing is
providing rhythmic movement at all except the chords.

### 2.2 Proposed resolution: a floor, not a ban

Rather than preventing both from simplifying (which would fight the
legitimate "one busy part, steady foundation" arrangement described
above), the recommended rule is a **minimum combined activity floor**:

```
Let bassActivity = attackDensity-equivalent for the generated Bass NoteEvents
Let drumActivity = attackDensity-equivalent for the generated Drum pattern

If chord attackDensity is HIGH (>0.5, per Phase 2 §2.1) AND
   bassActivity is at its simplest tier (ROOT/PEDAL) AND
   drumActivity is at its simplest tier (FOUR_FOUR or sparser):
       → this is an ACCEPTABLE, common arrangement — do not block it
```

In other words: **this spec does not recommend preventing simultaneous
simplification.** The "busy chords, simple everything else" arrangement is
musically valid and common. What this spec *does* recommend: the
suggestion UI (Bass sheet, Drum sheet) should **not silently pick this
combination for the user without them seeing both recommendations
together** — i.e., if the Composition Tray's future "Full Groove" option
(per the second handoff doc, Section 11) generates Bass and Drums in one
action, it should show the combined result for audition (already an
established principle — Section 14 of the second handoff doc,
"Preview is especially important") **before** committing, rather than
applying two independent simplification rules invisibly and presenting
only the final, possibly-too-sparse, result as a fait accompli.

### 2.3 What this resolves

- No new blocking logic needed in either `BassGenerator` or `DrumViewModel`.
- The real fix is a **UX guarantee** (preview before commit, already
  established doctrine), not a **generation rule**. This keeps both
  generators independent, per the architectural rule already established
  in both specs, and pushes the actual judgment call to the one place
  that's supposed to make it: the user, with the full combined result
  audible before accepting it.

---

## 3. Question 2 — Kick/Bass coincidence scoring

### 3.1 The data, confirmed ready (restated from Drum spec Section 6)

Both kick onsets and Bass note onsets are expressible as beat positions in
the same coordinate system. No new fields needed.

### 3.2 Proposed model

Per the first handoff doc's own framing (Section 5): the goal is
distinguishing **deliberate reinforcement** from **accidental
overcrowding** — not eliminating all coincidence, since kick/bass unison is
a legitimate, common groove device.

**Proposed three-tier classification**, using a small time-window
tolerance (not exact-beat matching, since real-feeling grooves rarely land
notes at bit-for-bit identical positions even when "aligned" by ear):

```
For each kick onset beat K and each Bass note onset beat B:
    delta = abs(K - B)

    if delta < 0.0625 (a 64th note):     COINCIDENT (near-simultaneous)
    else if delta < 0.25 (a 16th note):  NEAR-COINCIDENT
    else:                                 INDEPENDENT
```

**Proposed interpretation (not a hard rule, a scoring signal):**

| Pattern across a bar | Read as |
|---|---|
| Most/all kicks are COINCIDENT with a Bass onset | Deliberate reinforcement — kick and bass are locked together, a recognizable, intentional-sounding groove device. **Do not flag as a problem.** |
| A few isolated COINCIDENT hits among mostly INDEPENDENT onsets | Ambiguous — could be intentional accent or coincidental. **Do not flag; insufficient signal either way.** |
| Kicks and Bass onsets are frequently NEAR-COINCIDENT (close but not locked) | **This is the pattern most likely to sound like clutter** — two rhythmic events close enough to blur together without reinforcing each other cleanly. This is the one case worth surfacing. |

### 3.3 What this resolves, and what it deliberately doesn't

This spec recommends the coincidence measure exist as a **diagnostic
signal available to a future evaluation layer**, surfaced only in the
NEAR-COINCIDENT case — not as an automatic corrective action. Per the same
"choice, not imposition" principle used throughout: if a future Composition
Tray "Full Groove" preview shows a high near-coincidence rate, that's
useful information to *show* the user (e.g. a subtle indicator, not a
blocking warning), not a signal that should silently alter what either
generator produces.

**Explicitly not resolved here:** the exact time-window thresholds above
(1/64, 1/16) are a reasonable starting proposal, not empirically tuned —
they should be treated as adjustable constants once real on-device testing
with actual generated material is possible, not as fixed values.

---

## 4. Accent compatibility — a smaller, related question worth settling now

Not one of the three original deferred questions, but directly adjacent
and cheap to resolve while this document is already reconciling
cross-track behavior.

Per Phase 2 Section 4 (Accent profile, deferred there specifically because
"a single-track accent profile has limited use on its own") — now that
Bass and Drums are being considered together, this becomes answerable:

**Proposed use:** Bass's hardcoded per-style velocity data (e.g.
`GROOVE`'s ghost-note velocities at 50–55) and Drums' per-step velocity
grid (already a first-class field on `DrumPattern`, confirmed Phase 1
Section 7.3) can be compared **only at COINCIDENT/NEAR-COINCIDENT beat
positions** (Section 3.2 above) — i.e., when a kick and a bass note land
together, do their velocities reinforce each other (both strong, or both
intentionally soft as a shared ghost-note moment) or clash (one strong,
one weak, at the same instant)? This is a refinement **on top of** Section
3's coincidence model, not a separate mechanism — flagged here so it isn't
lost, but explicitly marked as a Phase 5.5/Phase 6 refinement, not
required before implementation of the core coincidence model can begin.

---

## 5. Question 3, restated — structural variation does not wait

Already settled in this session's discussion, restated here for the
document set's completeness: **Drum spec Section 7's phrase-boundary
pattern-switching capability does not require this document's cross-track
model at all.** It's a Chord+Drums-only capability (phrase boundaries come
from `addPhrase()`/`GeneratedProgression`, not from Bass) and should be
treated as independently implementable, likely *before* the cross-track
work in Sections 2–4 above, since it's cheaper and its musical impact was
assessed (Drum spec Section 12) as the single most impactful finding
across either instrument spec.

---

## 6. What Phase 5 concludes about implementation ordering

This document does not itself propose an implementation phase plan (that's
Phase 6/7's job), but the reconciliation work above surfaces a natural
priority ordering, worth recording:

1. **Drum structural variation** (Drum spec §7) — highest musical impact,
   no cross-track dependency, cheapest to build.
2. **Genre-name matching** (Drum spec §4) — near-zero cost, no cross-track
   dependency.
3. **Chord-density-based style/preset suggestion** (Bass spec §4, Drum
   spec §5) — single-track each, now safely groundable in this document's
   Section 2 resolution (both simplifying together is acceptable, not a
   bug to prevent).
4. **Phrase intent mapping** (Bass spec §6, Drum spec §8) — single-track
   each, needs `PairingType` threaded through, moderate implementation
   cost.
5. **Kick/Bass coincidence diagnostic** (this document, §3) — genuinely
   cross-track, needs both generators' output compared, and needs the
   Composition Tray's "Full Groove" preview UX to exist as a place to
   surface it.
6. **Accent compatibility** (this document, §4) — builds directly on #5,
   should follow it, not precede it.
7. **Voice-leading** (Bass spec §7) — explicitly deferred in its own spec,
   structurally the biggest change (`bassRegister()`'s signature), lowest
   urgency of everything listed.

This ordering is a **recommendation for Phase 6/7 planning**, not a
commitment — but it reflects genuine dependency structure (items 5–6
cannot precede having both generators producing real output to compare)
rather than arbitrary sequencing.

---

## 7. Summary

- **Density reconciliation** (Q1): resolved as "acceptable, not a bug" —
  the real fix is preview-before-commit UX, not a new generation
  constraint.
- **Kick/Bass coincidence** (Q2): resolved with a proposed 3-tier
  classification model, explicitly diagnostic/advisory, not corrective.
  Thresholds flagged as provisional, pending real on-device tuning.
- **Accent compatibility**: identified as a natural extension of Q2's
  model once it exists, not urgent on its own.
- **Structural variation**: confirmed (again) as independently
  implementable, and — per Section 6 — recommended as the actual first
  implementation priority, ahead of everything cross-track.

**This closes the paper-only phase of the roadmap.** Phases 1–5 are now
complete and internally consistent: a confirmed audit (1), a canonical
derived-property set (2), single-track specs for Bass (3) and Drums (4),
and a cross-track reconciliation model (5). Phase 6 (implementation) can
begin from here with a full, evidence-grounded design to work from.

---

*End of Phase 5 deliverable — and of the paper-only design phase.*