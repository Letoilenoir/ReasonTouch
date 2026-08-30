# ReasonTouch -- Strategy Test Log

Running record of on-device SUGGEST tests across ContinueStrategy, ContrastStrategy, and ResolveStrategy. Each entry: seed, predicted vs. actual, pass/fail. Full writeups/root-cause analysis for anything that surfaced a bug live in `docs/handoffs/` -- this log cross-references those rather than duplicating them.

Legend: PASS / FAIL / PENDING (not yet run) / PARTIAL (ran, but not fully traced)

---

## Coverage summary (updated as entries are added)
---

## 2026-08-04 (continued) -- SurpriseStrategy built and verified; live routing seed-hunt unsuccessful, KeyDetector mechanism now precisely understood

**Strategy logic: fully verified.** `SurpriseTargeting`/`SurpriseStrategy` implemented (primes an expected cadential resolution via PREDOMINANT->DOMINANT trajectory, then substitutes a borrowed chord at the FINAL position instead of the expected tonic -- generalizing the deceptive-cadence pattern V-expecting-I-landing-on-vi to a whole phrase). Confirmed via forced-intent seeds at both preferredLength=4 and 8: borrowed chord correctly lands at the final position in every candidate, diatonic trajectory beforehand shows genuine PREDOMINANT/DOMINANT alternation with no frozen tails (ChordCandidateSelector's exhaustion-rotation fix confirmed working for Surprise too).

**Live routing (CadenceType.DECEPTIVE): NOT YET CONFIRMED.** Five seed attempts, all failed to produce a genuine DECEPTIVE cadence classification via live KeyDetector+ProgressionAnalyzer:

| # | Seed | Detected key | Cadence | Why it missed |
|---|---|---|---|---|
| 1 | `C F G Am` | A minor | INTERRUPTED | Incorrectly assumed "already confirmed" from earlier session work without re-verification -- see KeyDetector mechanism finding below for the actual cause |
| 2 | `C F G Am` (length 8) | A minor | INTERRUPTED | Same seed, same result |
| 3 | `C F G Am` (unforced routing) | A minor | INTERRUPTED | Routed to CONTRAST, not SURPRISE -- confirms #1's cadence misclassification carries through to real PairingEngine behavior |
| 4 | `C F Bdim G Am` | A minor | INTERRUPTED | Bdim wrongly assumed C-major-exclusive; it's diatonic to BOTH keys (vii in C major, ii in A minor per MINOR_QUALITIES), did nothing to disambiguate |
| 5 | `C F G Am F` | **F major** | INTERRUPTED | Fix correctly removed A minor's advantage (see below) but the seed wasn't anchored enough to hold C major over every other candidate key -- F major won instead, a third key not previously considered |

**KeyDetector mechanism now fully traced (KeyDetector.kt reviewed directly) -- root cause of the whole seed-hunt difficulty:**

`KeyDetector.detect()` scores every candidate key by: `matchCount * 1.0` (chords diatonically valid in that key) + `tonicBonus (1.5)` if any chord is scale-degree-1 + `dominantBonus (1.2)` if any chord is scale-degree-5 + **`lastChordBonus (1.3)` if the progression's LAST chord is that key's tonic**.

Hand-traced `C F G Am` exactly: C major scores 7.7 (5 matches + tonic bonus for C + dominant bonus for G, no last-chord bonus since Am isn't C major's tonic). A minor scores 7.8 (5 matches + tonic bonus for Am as ITS tonic + last-chord bonus since Am IS A minor's tonic, but no dominant bonus since A minor's dominant is E, not present). **A minor wins by exactly 0.1 -- entirely due to lastChordBonus.**

**This is a structural conflict, not a one-off coincidence:** any seed constructed to end on the deceptive cadence's "surprise" landing chord (vi, by definition -- that's what makes it deceptive) will, by KeyDetector's own scoring, tend to make the relative minor's tonic-ending bonus fire, competing directly against the very key needed for the cadence to classify as DECEPTIVE rather than a strong VII->i close in the relative minor. **The DECEPTIVE cadence and KeyDetector's lastChordBonus pull in opposite directions for any seed shaped like "end on the relative-minor-styled vi chord."**

**Not yet resolved.** Recommendation for next attempt: anchor the seed harder toward C major specifically using a chord degree that differs in ROOT (not just quality) between C major and A minor -- e.g. degree IV is F in both keys (unhelpful, as seen), but degree ii differs: C major's ii is Dm (minor quality, root D), A minor's ii is Bdim (diminished, root B) -- entirely different roots. A seed using Dm specifically as an anchor, ending on a chord that is NOT any competing key's tonic (avoiding F major's trap from attempt 5 too), is the next concrete idea -- not attempted this session.

**Given 5 consecutive misses across a well-understood, precisely-traced mechanism, further blind seed construction was judged unproductive for this session. Logged as PENDING rather than forcing a sixth attempt.**

---
---

## 2026-08-04 (continued) -- Exhaustion-rotation fix centralized and rolled out to all 4 strategies

**Full details:** `docs/handoffs/ReasonTouch_Handoff_2026-08-04_LiftStrategy_ExhaustionFix.md`

Following the fix and full hand-trace verification in `LiftStrategy.kt` (documented above), the identical selection logic (function-filter -> exclusion -> exhaustion-rotation fallback) was extracted into a new shared file, `ChordCandidateSelector.kt`, rather than copy-pasting the fix into `ContinueStrategy.kt`/`ContrastStrategy.kt`/`ResolveStrategy.kt` independently. Rationale: the four strategies already carried near-identical copies of this logic, which is exactly how the original bug went unnoticed in three files after being found in one -- centralizing it means any future fix only needs to happen once.

**Rollout sequence:**
1. `ChordCandidateSelector.select()` created, compiled standalone.
2. `LiftStrategy.kt` retrofitted to call it (replacing its inline block). Re-ran `run lift investigation seeds` -- output byte-for-byte identical to the pre-refactor confirmed-good run, confirming the extraction was a pure refactor with no behavior change.
3. `ContinueStrategy.kt`, `ContrastStrategy.kt`, `ResolveStrategy.kt` retrofitted identically.
4. One brace-mismatch error during the Lift retrofit (old/new code replacement consumed too much surrounding context, deleting the `continuation`/`confidenceSum`/`currentChord`/`usedChords` update lines and misplacing `if (continuation.isNotEmpty())` inside the `repeat` block) -- caught via full-file `Get-Content` inspection before rebuilding, same discipline as prior brace-mismatch incidents this project.
5. Re-ran `run key detector investigation seeds` (confirms Continue at preferredLength=8 via the `G C D Am` seed) and a new dedicated `run exhaustion rotation regression seeds` test (added specifically to close the gap for Contrast and Resolve at preferredLength=8, using seeds closely related to the original bug-discovery seeds).

**Result: all 4 strategies confirmed rotating correctly at preferredLength=8, no frozen tails.** Side benefit observed across Continue, Resolve, and Lift (not yet specifically checked for Contrast): candidate counts increased by one in several cases, since genuine chord diversity in the tail means fewer variants collapse into duplicates via the final `distinctBy` dedup step.

**Not yet done:** stale inline comments in `ContinueStrategy.kt`/`ContrastStrategy.kt`/`ResolveStrategy.kt` describing the old (now-centralized) exclusion logic should be cleaned up -- not incorrect, just redundant now that the real logic lives in `ChordCandidateSelector.kt`. Also worth considering a small dedicated regression test file for `ChordCandidateSelector` itself, given how much manual re-verification (4 separate seed runs) went into confirming it -- a permanent test would catch future regressions automatically.

---

| Strategy | Branch | Status |
|---|---|---|
| Resolve | `endsOnDominant()` (HALF) | PASS -- verified twice |
| Resolve | `defaultTrajectory` PREDOMINANT | PASS -- all 3 variants traced |
| Resolve | `defaultTrajectory` TONIC/DOMINANT | PENDING |
| Resolve | `CadenceType.DECEPTIVE` | BLOCKED -- see multi-option PairingDecision design item |
| Resolve | `isClosed()` (`reopenThenResolveTrajectory`) | BLOCKED -- same reason |
| Resolve | Exhaustion-rotation fix (2026-08-04) | **PASS -- confirmed via `C Am F G` seed at preferredLength=8, rotates cleanly (C Em F Am C Em Am C), 4 distinct candidates survive dedup vs. usual 3** |
| Contrast | TONIC-ending trajectory | PASS -- full 8-bar trace, all 3 variants |
| Contrast | PREDOMINANT/DOMINANT-ending trajectory | PENDING |
| Contrast | LIFT branch (tension > 0.6f) | PENDING |
| Contrast | SIMPLIFY dead-branch check | PENDING |
| Contrast | Minor-key borrowed labels | PASS (incidental) |
| Contrast | Exhaustion-rotation fix (2026-08-04) | **PASS -- confirmed via `Am F G C` seed at preferredLength=8 (direct descendant of the original Am-Am-Am-Am/C-C-C-C bug seeds), rotates cleanly across all 3 variants** |
| Continue | Repeated-chord exclusion fix at preferredLength=4 | PASS -- confirmed clean across all 7 investigation seeds |
| Continue | Exhaustion-rotation fix (2026-08-04) | **PASS -- confirmed via `G C D Am` seed at preferredLength=8 (D G C Bm Em G Bm Em, was frozen as D G C Bm G G G G pre-fix), 4th distinct candidate survives dedup vs. usual 3** |
| Lift | Implemented (2026-08-04) | DONE -- trajectory logic + routing confirmed via real PairingEngine seed (`Bdim G Bdim C`) |
| Lift | Exhaustion-rotation fix (2026-08-04) | **PASS -- fixed, fully hand-traced against live instrumented output, independently re-confirmed after `ChordCandidateSelector` extraction (byte-for-byte identical output pre/post refactor)** |
| (all strategies) | Exhaustion-rotation fix -- overall rollout | **COMPLETE. All 4 strategies confirmed via direct seed comparison at preferredLength=8. Centralized in `ChordCandidateSelector.kt` (new shared object) rather than duplicated per-strategy -- see rollout details below.** |
| (all strategies) | KeyDetector tie-break hypothesis | DISPROVEN AND CLOSED (2026-08-03) |

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
| Expand | Trajectory logic (3-function cycle) + exhaustion-rotation | PASS -- confirmed via forced-intent seeds, both length 4 and 8, no frozen tails |
| Expand | Medium-stability default routing | PENDING -- retry seed needed, see 2026-08-04 key-ambiguity notes |
| Expand | HALF-cadence routing | **PROVEN DEAD -- see Task_List.md. Mathematical ceiling (~0.20) can never reach medium-stability floor (0.4). Not a testing gap, a structural fact.** |
---
---

## 2026-08-04 (continued) -- ExpandStrategy built and verified; medium-stability routing seed-hunt exhausted without success

**Strategy logic: fully verified.** `ExpandTargeting`/`ExpandStrategy` implemented (3-function cycle: TONIC->PREDOMINANT->DOMINANT, starting one step after the source's ending function). Confirmed via forced-intent seeds at both preferredLength=4 and 8 -- genuine 3-function cycling in output, no frozen tails (ChordCandidateSelector's exhaustion-rotation fix confirmed working for Expand too), diversity-bonus candidate count increases at length 8 same as other strategies.

**HALF-cadence routing: proven mathematically dead.** `suggestAfterMediumStability()`'s HALF branch ("Ends on V -> explore harmonically") can never fire via live PairingEngine. Proof: `functionStability(DOMINANT) = 0.15` and HALF's `cadenceBonus = -0.10` are both fixed whenever a progression ends on the dominant (`detectCadence()` guarantees any degree-5 ending classifies as HALF). Maximum possible stability via `functionDistributionBonus` alone: `0.15 - 0.10 + 0.15 = 0.20` ceiling -- far below the 0.4 medium-stability floor. Confirmed empirically too: every HALF-cadence seed tried (`Dm G`, `G Em C D Bm`) routed to RESOLVE, never EXPAND. Same category as SIMPLIFY and RESOLVE's DECEPTIVE branch -- see `Task_List.md`.

**Medium-stability-default routing: PENDING, not dead, but seed construction proved much harder than expected.** Nine seed attempts across this session, all either missing the [0.4, 0.7] range or landing in it but hitting a different medium-stability branch (`barCount<=4`->CONTINUE) instead of the `else`->EXPAND default. Full attempt log, useful for whoever picks this up next:

| # | Seed | Detected key | Stability | Outcome | Why it missed |
|---|---|---|---|---|---|
| 1 | `Dm G` | G major | 82% | High (CONTRAST) | HALF-cadence attempt, not medium-targeted |
| 2 | `C Am Dm Em F` | F major | 78% | Falls in the [0.7,0.8) threshold gap -> low-stability routing (CONTINUE) | Wrong key assumed (predicted C major); this is also the seed that discovered the threshold gap (see Task_List.md) |
| 3 | `Dm Am Dm Gm` | D minor | **47%** | Medium range, but `barCount=4` triggers the `barCount<=4` check first -> CONTINUE, not EXPAND's `else` | Correct range, wrong sibling branch -- closest clean hit all session |
| 4 | `C F Dm G Am` | A minor | 92% | High (CONTRAST) | Wrong key assumed (predicted C major); ends on Am=i=TONIC in A minor |
| 5 | `G Em C D Bm` | E minor | 8% | Low (RESOLVE) | Wrong key assumed (predicted G major); ends on Bm=v=DOMINANT in E minor, HALF cadence |
| 6 | `Dm Am Dm Gm C` | D minor | 16% | Low (RESOLVE) | Extended seed 3 with C, wrongly assumed TONIC-function; C is actually degree VII = DOMINANT function in D minor (MINOR_FUNCTIONS mapping) |
| 7 | `Dm Am Dm Gm F` | D minor | 96% | High (CONTRAST) | Extended seed 3 with F, correctly TONIC-function this time, but ending-chord function change alone swings stability ~50 points (functionStability contributes 0.95 of the total for any TONIC ending) -- any edit that changes what the progression ends on dominates the whole calculation |
| 8 | `Dm Am Dm Am Gm` | D minor | 36% | Low (RESOLVE, "Unresolved and off the tonic") | Preserved the correct ending (Gm) from seed 3, but repeating Am introduced a second DOMINANT-function chord, triggering `dominantPenalty=-0.1` -- a mechanism not accounted for, dragged 47%->36%, just below the medium floor |

**Root causes of the difficulty, generalized for future reference:**
1. Hand-predicting `KeyDetector`'s output for short/ambiguous progressions remains unreliable (repeated finding across multiple sessions now) -- 5 of 8 seeds above detected a different key than assumed.
2. `calculateHarmonicStability()`'s `functionStability` term (0.95/0.45/0.15 for TONIC/PREDOMINANT/DOMINANT) dominates the total score -- changing the progression's *ending function* swings stability by ~30-80 points, dwarfing every other bonus/penalty combined. Safe seed construction must hold the ending chord's function fixed and vary only earlier chords.
3. Scale-degree-to-function mapping must be checked against the *actual detected key*, not assumed by analogy to relative major/minor (e.g. degree VII is DOMINANT-function in minor keys, not TONIC-adjacent as its relative-major intuition might suggest).
4. `dominantPenalty` (any second degree-5 chord anywhere in the progression, not just the ending) is an easy-to-forget swing factor when extending a seed.
5. `barCount<=4` is checked *before* the `else`/EXPAND branch inside `suggestAfterMediumStability()` -- a seed with correct stability range and correct cadence type still needs `barCount>4` specifically to reach EXPAND rather than its CONTINUE sibling.

**Recommendation for next attempt:** start from seed 3 (`Dm Am Dm Gm`, confirmed 47%, D minor) and extend to 5 bars by inserting a chord *before* position 3 that is diatonic, not degree-5 (avoiding the dominant-penalty), and not the final position (avoiding the ending-function-dominance problem) -- e.g. `Dm Edim Am Dm Gm` (inserting Edim, degree ii=PREDOMINANT, between the first Dm and Am). Not attempted this session; logged as the next concrete idea rather than a tenth blind guess.

---
---

## 2026-08-04 (continued) -- SurpriseStrategy built and fully verified; all 5 built strategies now confirmed working

**Strategy logic: fully verified.** `SurpriseTargeting`/`SurpriseStrategy` implemented (primes an expected resolution via PREDOMINANT/DOMINANT alternation, then substitutes a borrowed chord at the FINAL position -- generalizing the deceptive cadence's "expect V->I, get V->vi" pattern to a whole phrase). Confirmed correct via forced-intent testing (`C F G Am`, A minor): trajectory, borrowed-chord substitution at the correct position, and ChordCandidateSelector's exhaustion-rotation fix all verified at both length 4 and 8.

**Live PairingEngine routing: CONFIRMED**, via `C C F G Am` -- detected key C major, stability 56%, cadence DECEPTIVE, routed to SURPRISE ("Deceptive cadence -> unexpected turn", 75% confidence). Every prediction (key, stability, cadence, routing decision) matched exactly on the first attempt using the corrected methodology below. This is the first genuinely successful real-routing confirmation for SURPRISE, closing out the last open verification question for this strategy.

### Corrected seed-construction methodology (established this session, should be used for all future seed work)

Root cause of repeated seed-prediction failures across this session (EXPAND and SURPRISE both): hand-*intuiting* which key `KeyDetector` would pick, rather than *computing* its actual weighted formula. `KeyDetector.kt`'s scoring is precise and mechanical:

## Phase 3 — Strum Persistence Roadmap (2026-08-27)
Sprint 3b unit-mismatch fix (strumSpeed seconds→beats conversion) verified via:
- On-device listening check: strum audibly present, timing subjectively correct
- MIDI export → import into Reason 14: staggered note onsets visible in piano roll,
  consistent stagger pattern across multiple bars, confirming strum offsets are
  present and structurally sound in the exported file, not just in live playback
  Exit criteria (Sprint 0 Decision 2, Option A) met. Phase 3 complete.

## Phase 4 — Continuation Inheritance (2026-08-27)
addPhrase() now inherits strumPatternId/strumSpeedValue from the last existing
ChordEvent in the progression, decoded via StrumEncoding.kt, rather than defaulting
to whatever pattern is currently selected in the UI.

Verified on-device: seed progression E (All Down) -> A (Motown, via append),
SUGGEST continuation generated G D E A Dbm Gbm A Dbm. Playback confirmed first
continuation bar audibly carried A's Motown pattern (the last seed bar), not E's
All Down (the first) or a default -- correct inheritance behavior.

Known gap surfaced during this test, NOT a Phase 4 defect: Piano Roll grid did not
visually refresh to show the new continuation notes until navigating away and back.
Root cause confirmed: PianoRollViewModel's _allNotes/_activeNotes are populated once
in init{}, and only re-fetch when the track list itself changes -- not when notes are
written via a separate ChordViewModel instance (the one addPhrase() now uses per
Phase 3's Sprint 0 Decision 1 re-wiring). Playback was unaffected since
PlaybackController reads notes independently of PianoRollViewModel's cache. This is
a side effect of Phase 3's architecture, not a Phase 4 logic bug -- underlying data
was correct throughout, only the visible grid was stale. Logged as its own follow-up,
not blocking Phase 4 completion.

Exit criteria (roadmap Phase 4, Section 4) met. Phase 4 complete. Strum Persistence
& Continuation Inheritance roadmap (Phases 1-4) fully complete.

## Piano Roll Grid Staleness Fix (2026-08-28)
Fixed the bug logged 2026-08-27: grid did not visually refresh after SUGGEST
continuations, because PianoRollViewModel's _allNotes only populated once in
init{} and had no way to know notes had been written by a separate ChordViewModel
instance.

Fix: added PianoRollViewModel.refreshNotes() (public, forces a re-fetch via a new
private fetchAllNotes() helper extracted from init{}). ChordViewModel.addPhrase()
now takes an optional onComplete callback, invoked on both the early-return
(empty progression) and normal completion paths. PianoRollScreen's onOptionSelected
now calls viewModel.refreshNotes() from that callback.

Verified on-device: SUGGEST continuation (8 bars) appeared in the Piano Roll grid
immediately, no navigation required. Strum pattern correctly matched the seed
across all 8 bars, consistent with Phase 4 inheritance behavior.

- *Add new dated sections above this line as further matrices are run.*