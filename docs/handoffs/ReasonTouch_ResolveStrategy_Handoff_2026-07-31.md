# ReasonTouch -- ResolveStrategy Implementation & Testing Handoff

**Date:** 2026-07-31
**Status:** ResolveStrategy implemented and building successfully. On-device testing in progress -- 1 of 3 trajectory branches fully verified. Paused to document a design-relevant finding before continuing.
**Follows on from:** `ReasonTouch_Bug_Investigation_Handoff (1).md`, Section 9 ("Next queued item: ResolveStrategy / LiftStrategy")

---

## 1. What was built

Following the established CONTINUE-as-canonical-template pattern (same shape as `ContinueStrategy`/`ContinueTargeting`, and cross-checked against `ContrastStrategy`/`ContrastTargeting` as a second reference point):

### New files
- `feature/feature-chords/.../generation/strategies/ResolveTargeting.kt`
- `feature/feature-chords/.../generation/strategies/ResolveStrategy.kt`

### Modified
- `feature/feature-chords/.../ProgressionGenerator.kt` -- `CompositionIntent.RESOLVE` dispatch now calls `ResolveStrategy.generate(request)` instead of `emptyList() // TODO: ResolveStrategy`.

### Design summary

**`ResolveTargeting.deriveTrajectory(analysis, phraseLength)`** -- branches on the source `ProgressionAnalysis`:

| Condition | Trajectory base (4-step) |
|---|---|
| `endsOnDominant()` (half cadence) | `[DOMINANT, TONIC, PREDOMINANT, TONIC]` -- resolve immediately, then a plagal "amen" tag |
| `cadenceType == DECEPTIVE` | `[TONIC, PREDOMINANT, DOMINANT, TONIC]` -- acknowledge the tonic-function chord we landed on, then run a full ii-V-I |
| `isClosed()` (stability > 0.75) | `[DOMINANT, PREDOMINANT, DOMINANT, TONIC]` -- reopen before resolving again |
| default (open/interrupted), keyed by `endingFunction` | standard cadential run home |

Unlike `ContinueTargeting`'s `fitLength()`, truncation uses `takeLast()` instead of `take()`, so trimming to a shorter phrase length preserves the cadential tail rather than cutting it off.

**`ResolveStrategy.generate(request)`** -- same skeleton as `ContinueStrategy.generate()` (4 ranked variants, iterate `preferredLength` positions, filter `ChordSuggestionEngine.suggest()` results by target function, pick by variant rank). One addition beyond Continue's pattern: **the final step is always forced to `HarmonicFunction.TONIC`**, overriding whatever the trajectory said for that position. This is what makes Resolve's guarantee real rather than just a strongly-biased Continue.

Build: successful, no compilation errors.

---

## 2. On-device test results

### Verified -- `endsOnDominant()` (half cadence)

Seed: `C - Am - F - G` (C major, ends on V).

SUGGEST -> RESOLVE, "Ends on V -> resolution needed", 95% confidence, 4 bars.

| Variant | Result |
|---|---|
| Direct Cadence | C C F C |
| Alternate Voicing | Em C Dm Em |
| Extended Approach | Am C Dm Em |
| Contrasting Approach | F Em Dm Em |

All four hand-verified against `ChordSuggestionEngine`'s actual weighting/transition logic -- exact match. Every variant's final chord correctly lands on tonic (C or Em, both valid tonic-function diatonic chords), confirming the forced-final-step-TONIC logic is functioning.

SELECT -> notes written to Piano Roll immediately (no screen re-entry needed). Playback ran cleanly through bar 8 with no early cutoff.

**Note (not a bug, previously flagged):** Direct Cadence variant repeated C at positions 0-1 (`C C F C`). Root cause identified and fixed in the following session -- see the 2026-08-01 handoff.

### Attempted deceptive-cadence test -- invalid, needs re-run

Seed used: `Dm - G - Am`. Intended as a deceptive cadence (V->vi), but the analyzer detected the key as **A minor**, not C major. In natural A minor, G is the diatonic **VII** (not V), so this progression is actually `VII -> i`, a strong, legitimate close -- not a deceptive cadence at all. Confirmed by the SUGGEST dialog: it returned **CONTRAST** ("High stability -> ready for variation"), not RESOLVE.

**Corrected seed for the actual deceptive-cadence branch:** `C - F - G - Am` in C major (G is the real V of C, resolving to vi=Am instead of I=C).

---

## 3. Finding: IntentEngine's ranking logic makes one ResolveTargeting branch currently unreachable

`IntentEngine.rank()` only ever offers `CompositionIntent.RESOLVE` when `analysis.harmonicStability < 0.4f`. But `ProgressionAnalysis.isClosed()` is defined as `harmonicStability > 0.75f`. These two conditions are mutually exclusive by construction -- by the time RESOLVE is even a candidate intent, `isClosed()` can never simultaneously be true. So `ResolveTargeting`'s `isClosed()` branch (`reopenThenResolveTrajectory`) is structurally correct but dead code from the live SUGGEST UI's perspective.

**Superseded finding (see 2026-08-01 handoff):** `IntentEngine` was subsequently confirmed to be dead code entirely -- zero callers anywhere in the project. `PairingEngine.kt` is the real dispatcher for SUGGEST decisions, and has its own, separate reachability gaps for these same branches. This section is preserved for historical accuracy but `PairingEngine.kt`'s equivalent gaps (documented 2026-08-01) are the ones that actually matter.

---

## 4. Product thought raised this session -- user-selectable intent, not just auto-ranked

**Idea floated:** rather than the engine silently picking one winning intent, could the SUGGEST dialog let the user choose between top-ranked candidates directly (e.g. RESOLVE vs. CONTRAST when both are plausible)?

**Status: idea only, not scoped.** Driving philosophy behind this, articulated later in discussion: "if we impose a decision on a user, we are effectively building the composition for them" -- this is a real design principle worth taking seriously, not just a nice-to-have. Deferred as its own properly-scoped architecture piece (see 2026-08-01 handoff, Section 6).

---

## 5. Next steps (superseded by 2026-08-01 handoff -- see that document for current state)

*End of handoff document.*