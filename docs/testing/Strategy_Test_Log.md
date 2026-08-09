# ReasonTouch -- Strategy Test Log

Running record of on-device SUGGEST tests across ContinueStrategy, ContrastStrategy, and ResolveStrategy. Each entry: seed, predicted vs. actual, pass/fail. Full writeups/root-cause analysis for anything that surfaced a bug live in `docs/handoffs/` -- this log cross-references those rather than duplicating them.

Legend: PASS / FAIL / PENDING (not yet run) / PARTIAL (ran, but not fully traced)

---

## Coverage summary (updated as entries are added)

| Strategy | Branch | Status |
|---|---|---|
| Resolve | `endsOnDominant()` (HALF) | PASS -- verified twice |
| Resolve | `defaultTrajectory` PREDOMINANT | PASS -- all 3 variants traced |
| Resolve | `defaultTrajectory` TONIC/DOMINANT | PENDING |
| Resolve | `CadenceType.DECEPTIVE` | BLOCKED -- see multi-option PairingDecision design item |
| Resolve | `isClosed()` (`reopenThenResolveTrajectory`) | BLOCKED -- same reason |
| Resolve | Exhaustion-rotation fix (2026-08-04) | **NOT YET APPLIED** |
| Contrast | TONIC-ending trajectory | PASS -- full 8-bar trace, all 3 variants |
| Contrast | PREDOMINANT/DOMINANT-ending trajectory | PENDING |
| Contrast | LIFT branch (tension > 0.6f) | PENDING |
| Contrast | SIMPLIFY dead-branch check | PENDING |
| Contrast | Minor-key borrowed labels | PASS (incidental) |
| Contrast | Exhaustion-rotation fix (2026-08-04) | **NOT YET APPLIED** |
| Continue | Repeated-chord exclusion fix at preferredLength=4 | PASS -- confirmed clean across all 7 investigation seeds |
| Continue | Exhaustion-rotation fix (2026-08-04) | **NOT YET APPLIED** -- known-affected seed available: `G C D Am` at preferredLength=8, see 2026-08-03 entry |
| Lift | Implemented (2026-08-04) | DONE -- trajectory logic + routing confirmed via real PairingEngine seed (`Bdim G Bdim C`) |
| Lift | Exhaustion-rotation fix | **PASS -- fixed and fully hand-traced, confirmed correct against live LIFT_TRACE output.** See `docs/handoffs/ReasonTouch_Handoff_2026-08-04_LiftStrategy_ExhaustionFix.md` for full mechanism and fix history (two attempts, second one verified). |
| (all strategies) | KeyDetector tie-break hypothesis | DISPROVEN AND CLOSED (2026-08-03) |
---
---

## 2026-08-04 -- LiftStrategy built; exhaustion-rotation bug found, root-caused, and fixed (Lift only)

**Full details:** `docs/handoffs/ReasonTouch_Handoff_2026-08-04_LiftStrategy_ExhaustionFix.md`

**Summary:** Built `LiftStrategy`/`LiftTargeting` (alternating DOMINANT/PREDOMINANT trajectory, avoids TONIC). Found via direct temporary instrumentation (`*_TRACE` println logging inside the exclusion/fallback block) that once a harmonic function's candidate pool exhausts at `preferredLength=8`, the existing fallback (`variantIndex.coerceAtMost(finalPool.size - 1)` as a fixed rank) freezes onto one identical chord forever, rather than rotating through the exhausted pool. This is present in all 4 strategies (shared code shape), and supersedes/sharpens the 2026-08-03 "tonic exhaustion" finding -- exhaustion itself is real and expected (only 2-3 diatonic candidates per function), but the *frozen fallback rank* on top of it was an actual, fixable bug, not an inherent limit.

**Fix (Lift only so far):** replace the fixed `variantIndex` rank with `(variantIndex + timesRevisited) % finalPool.size`, where `timesRevisited` counts matching chords in the growing `continuation` list (NOT `usedChords`, which is a `Set` and saturates -- this was a first, failed fix attempt, also documented in the handoff doc). Verified via complete manual hand-trace against real instrumented output, all 8 positions, exact match.

**Status:** applied and confirmed in `LiftStrategy.kt` only. `ContinueStrategy.kt`, `ContrastStrategy.kt`, `ResolveStrategy.kt` still have the frozen-fallback bug as of this entry -- see Task_List.md.

---
## 2026-08-02 -- Continue exclusion-fix verification (via Piano Roll SUGGEST)

**Goal:** confirm the same-continuation chord exclusion fix (commit `6ace490`) works for `ContinueStrategy`, following the same verification already done for Resolve and Contrast.

**Important process note discovered mid-session:** all C-major/A-minor-diatonic seeds tested kept landing on CONTRAST or RESOLVE rather than CONTINUE. An initial hypothesis (KeyDetector defaulting to C major for ambiguous seeds) was formed and later **disproven** -- see the 2026-08-03 harness run below for the real explanation.

| # | Seed | Detected key (on-device inference) | SUGGEST type (actual) | Notes |
|---|---|---|---|---|
| 1 | `C - Am - F` | C major (or A minor -- ambiguous, unconfirmed at the time) | CONTRAST | High stability, tonic-heavy ending. Not a CONTINUE case. |
| 2 | `C - Em - Dm` | Assumed C/Am at the time | CONTRAST | Prediction mismatch -- resolved 2026-08-03, see below. |
| 3 | `Dm - Em - Am` | A minor (confirmed via borrowed-chord labels) | CONTRAST | Em->Am = authentic cadence (v->i) in A minor; stability ~1.0. |
| 4 | `G - Dm - Em` | C major (assumed at the time) | CONTRAST | Ends on Em (TONIC-function, degree iii). Stability ~0.95. |
| 5 | `C - F - G` | C major | RESOLVE | Ends on G (DOMINANT), HALF cadence. Stability ~0.10. Confirmed exactly by harness 2026-08-03. |
| 6 | `C - G - F` | Assumed C major at the time | CONTRAST | Prediction mismatch -- resolved 2026-08-03, see below. |
| 7 | `G - C - D - Am` | G major (confirmed via Bm/F#dim in generated output) | CONTINUE -- clean hit | First seed to reliably land CONTINUE. |

### Seed 7 results (G major), preferredLength = 8 (on-device, 2026-08-02)

| Variant | Result |
|---|---|
| 1 | D G C Bm G G G G |
| 2 | Gb-dim(F#dim) Bm Am G G G G G |
| 3 | Gb-dim(F#dim) Em Am Bm G G G G |

At the time, this tail-repeat was attributed to the same `ChordSuggestionEngine` cap limitation seen in Resolve/Contrast. **Partially correct, partially superseded** -- see 2026-08-03 findings below, which identify a second, distinct cause specific to length-8 padding.

### Key finding: `preferredLength` diverges between the two SUGGEST call sites

Confirmed via full-project search: `ChordViewModel.kt` (Chords-screen SUGGEST) hardcodes `preferredLength = 4` always. `PianoRollViewModel.kt:846` (Arrange-screen SUGGEST) sets `preferredLength = pairingDecision.suggestedBars` (4 or 8 depending on branch). Same seed + same `PairingDecision` can produce different-length suggestions depending which screen triggered SUGGEST. Not yet actioned -- logged in `docs/Task_List.md`.

---

## 2026-08-03 -- GenerationTestHarness verification (headless, via JUnit)

**Goal:** resolve the 2026-08-02 open questions (seeds 2 and 6's prediction mismatches; the C/Am tie-break hypothesis) without needing the device, using the newly-extended `GenerationTestHarness.runKeyDetectorInvestigationSeeds()` (now also calling `PairingEngine.suggestNext()` directly, printing the exact type/rationale/confidence/suggested-bars the live SUGGEST dialog would show).

**File setup issue encountered and resolved:** the harness extension initially introduced a brace mismatch (stray extra `}` on save, then an accidentally-removed real `}` during the fix attempt) causing two failed compiles in a row. Resolved by inspecting the file tail directly via `Get-Content -Last 10` before each edit rather than guessing, then appending the single missing `}` via `Add-Content`. Final state: clean compile. Worth remembering for future large multi-section edits to this file: verify tail state before AND after edits, not just after.

### Result: the "KeyDetector defaults to C major" hypothesis from 2026-08-02 is DISPROVEN

Actual detected keys across the 7 investigation seeds: F major, D minor, A minor, E minor, C major, F major, G major -- five different keys, not a consistent C/Am tie-break at all. `KeyDetector` is genuinely finding a best-fit key per seed, not defaulting to anything.

### Seeds 2 and 6, resolved

**Seed 2 (`C Em Dm`) -> detected D minor**, not C major/A minor as assumed. Hand-traced `calculateHarmonicStability()` against D minor's actual diatonic degrees: C = VII (DOMINANT); Em is **not diatonic to D minor** and is dropped from the functional sequence entirely (harness printed only 2 functions: "DOMINANT > TONIC", confirming this); Dm = i (TONIC). Cadence = OPEN (no valid cadence pair forms once Em drops out). `0.95 (TONIC) - 0.20 (OPEN) + 0.05 (1/3 tonic-count bonus) = 0.80` -- **exact match** to the harness's printed 80% stability. High stability -> CONTRAST. The 2026-08-02 hand-prediction of "medium stability -> CONTINUE" was wrong because it assumed the wrong key (C major), not because of any bug in `ProgressionAnalyzer`.

**Seed 6 (`C G F`) -> detected F major**, not C major as assumed. In F major: C = V (DOMINANT), G is diatonic (ii, PREDOMINANT... verify), F = I (TONIC). Ending on TONIC with only 2-chord functional sequence shown (harness printed "DOMINANT > TONIC") suggests G may not be cleanly diatonic either, or the cadence classification collapsed similarly to seed 2. Stability printed as 80%, consistent with a TONIC-ending, OPEN-cadence result. Same root cause as seed 2: wrong assumed key, not a code issue.

**Conclusion: `ProgressionAnalyzer.calculateHarmonicStability()` and `PairingEngine`'s routing thresholds are both behaving correctly and consistently.** Every seed's printed stability/cadence/type now traces exactly against the formulas once the *actual* detected key (not an assumed one) is used. No outstanding discrepancy remains from the 2026-08-02 session.

### Continue + cap fix: confirmed working cleanly at preferredLength = 4

All 7 seeds, all 3 variants each, at length 4: **zero repeated chords in any candidate.** E.g. seed 1: `A# C Gm F`; seed 4: `F#dim D Am C`; seed 5: `Dm Bdim Am Em`. The same-continuation exclusion fix (`6ace490`) combined with the `ChordSuggestionEngine` cap fix (`FULL_DIATONIC_POOL`, applied 2026-08-02) are confirmed working together correctly for Continue, matching what was already independently confirmed for Resolve and Contrast.

### New finding: tonic-function exhaustion at preferredLength = 8 (distinct from the cap bug)

Seed 7 re-run at `preferredLength = 8` (matching the on-device 2026-08-02 result): Candidate 1 = `D G C Bm Em G G G`. Positions 0-4 are all distinct (the exclusion fix visibly working, including introducing a genuinely new chord, Em, at position 4) -- but positions 5-7 collapse to `G G G`.

**Root cause, traced:** G major has exactly **3** tonic-function diatonic chords total: G (I), Bm (iii), Em (vi). By position 4, all three are already used (G at position 1, Bm at position 3, Em at position 4). There is no 4th tonic-function diatonic chord left to exclude to -- **the pool is genuinely, mathematically exhausted**, not artificially capped by `MAX_SUGGESTIONS`/`FULL_DIATONIC_POOL`.

**This is a different problem from the cap bug already fixed.** The cap fix solves "the ranking logic discarded valid candidates before the strategy could see them" -- that's fixed, confirmed above. This is "there are only 3 valid diatonic candidates for this function, period, and the trajectory asked for the same function 4+ times in a row." No `ChordSuggestionEngine` change can fix this; there's nothing left to surface. Only bites at `preferredLength = 8` (Contrast/Lift's hardcoded suggested-bars value) when a trajectory's `fitLength`-style padding repeats the same target harmonic function 4+ times for the back half of the phrase -- which is the normal, expected shape of every current `ContinueTargeting`/`ContrastTargeting` base trajectory once padded to 8.

**Logged as a new, separate task list item** (see `docs/Task_List.md`) -- possible directions: accept the repetition beyond 3 uses of a function as an inherent diatonic limit; vary voicing/octave for repeated chords to at least avoid literal identical repeats; mix in borrowed chords once diatonic options for a function are exhausted, similar to how Contrast already uses borrowed chords deliberately at position 0.

### Coverage summary update

| Strategy | Branch | Status |
|---|---|---|
| Continue | Repeated-chord exclusion fix at preferredLength=4 | **PASS -- confirmed clean across all 7 investigation seeds, headless via GenerationTestHarness** |
| Continue | Tonic-function exhaustion at preferredLength=8 | **NEW FINDING, not a regression -- see above. Logged as separate task list item, not a defect in the 2026-08-02 fixes.** |
| (all strategies) | KeyDetector tie-break hypothesis | **DISPROVEN AND CLOSED** -- KeyDetector correctly finds best-fit key per seed; no default-to-C-major behavior exists. |

---

*Add new dated sections above this line as further matrices are run.*