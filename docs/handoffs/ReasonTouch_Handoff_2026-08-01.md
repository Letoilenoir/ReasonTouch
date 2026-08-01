# ReasonTouch -- Session Handoff: Repeated-Chord Fix & ChordSuggestionEngine Cap Discovery

**Date:** 2026-08-01
**Status:** Exclusion fix implemented, built, committed, pushed, and verified on-device across Resolve and Contrast. A deeper, more significant limitation was diagnosed as a result -- scoped as next session's primary task.
**Follows on from:** `ReasonTouch_ResolveStrategy_Handoff_2026-07-31.md`
**Commits this session:** `6ace490` (exclusion fix), `a3e09fa` / `a12320f` (unrelated docs/UML work, own session), `0c121fa` (merge). Working tree clean, in sync with `origin/master`.

---

## 1. What was built: same-continuation chord exclusion

**Problem:** Across multiple SUGGEST tests (both RESOLVE and CONTRAST, spanning two sessions), generated variants repeatedly showed the same chord appearing 2+ times within a single 4-bar continuation (e.g. `C C F C`, `Bdim Am G Am`), and in one case a full run of `Am Am Am Am` across 4 consecutive bars.

**Fix applied identically across all three strategies** (`ContinueStrategy.kt`, `ContrastStrategy.kt`, `ResolveStrategy.kt`):

- Added a `usedChords: MutableSet<TheoryChord>` per variant, reset at the start of each variant's continuation loop.
- Before picking `pool[rank]`, filter to `pool.filterNot { it.chord in usedChords }`.
- If that exclusion would leave an empty pool, fall back to the original (possibly-repeating) pool.
- After picking, add the chosen chord to `usedChords`.

`ContrastStrategy` additionally adds the borrowed chord (position 0) to `usedChords`.

`ChordSuggestionEngine.suggest()` itself was **not modified** -- confirmed via full-project search that it has exactly 4 call sites: the three strategies above, plus one unrelated single-chord suggestion call in `ChordViewModel.kt:420`. Keeping exclusion local to each strategy's loop rather than centralizing it in `suggest()` means that fourth caller is unaffected.

Build: successful. Committed as `6ace490`, "Reconfig of Continue, Contrast & Resolve Strategies to mitigate repeats."

---

## 2. On-device verification

### Resolve -- `C Am F G` (direct before/after comparison)

| Variant | Before fix | After fix |
|---|---|---|
| Direct Cadence | `C C F C` | `C C F Em` |
| Contrasting Approach | `F Em Dm Em` | `F Em Dm C` |

Both hand-traced against actual `ChordSuggestionEngine` weighting -- exact match.

### Contrast -- TONIC-ending trajectory trace, `Am - F - G - C`

Full 8-bar trace, all three borrowed-chord variants, matched `ContrastTargeting`'s predicted trajectory `[DOMINANT, PREDOMINANT, DOMINANT, TONIC]` + 4 padded TONIC positions exactly:

| Variant | Pos0 (borrowed) | Pos1 (PREDOM) | Pos2 (DOM) | Pos3 (TONIC) | Pos4-7 (TONIC x4) |
|---|---|---|---|---|---|
| Borrowed bVII | Bb | F | G | C | C C C C |
| Borrowed iv | Fm | Dm | Bdim | Em | C C C C |
| Borrowed bVI | Ab | Dm | Bdim | Am | C C C C |

All three variants independently converged on 4x-repeated C for the padded tail -- this is what led to Section 3's discovery.

---

## 3. Discovery: ChordSuggestionEngine.suggest()'s candidate cap starves tonic-function alternatives

The exclusion fix works correctly and is confirmed doing its job. But testing surfaced a deeper limitation one layer down, in `ChordSuggestionEngine.suggest()` itself, that the strategy-level fix cannot address.

### Mechanism (hand-traced and confirmed across 3 independent test cases)

`suggest()` caps results at `MAX_SUGGESTIONS = 4`, ranking candidates by weight before any function-based filtering happens downstream. Degree-1 (tonic) chords carry a `1.1x` weighting bonus. The consequence: whenever the "last chord" has TONIC function, `suggest()`'s top-4 pool consistently comes back with only ONE entry actually carrying TONIC function (e.g. Am from `[Am, Em, Dm, Bdim]`, or C from `[C, G, F, Dm]`). The other legitimate diatonic TONIC-function chords never make the top-4 cut at all -- crowded out before the strategy's own filtering or exclusion logic ever sees them.

Once a strategy lands on that one surviving TONIC candidate, the next `suggest()` call from that same chord returns the identical pool with the identical single-candidate bottleneck -- the exclusion fix has nothing left to exclude to. This isn't the fix failing; it's correctly detecting "no unused alternative available" and correctly falling back.

### Evidence -- reproduced identically 3 times, 2 different strategies

1. Resolve, `C Am F G`, variant 0, position 1 (from C, targeting TONIC) -> only C survives the cap -> forced repeat.
2. Contrast, `Dm - G - Am`, all 3 variants, positions 4-7 (from Am, targeting TONIC x4) -> only Am survives -> `Am Am Am Am`.
3. Contrast, `Am - F - G - C`, all 3 variants, positions 4-7 (from C, targeting TONIC x4) -> only C survives -> `C C C C`.

### Related finding: why Contrast's padding is more exposed than Resolve's

`PairingEngine.kt` hardcodes `suggestedBars = 8` for every branch of `suggestAfterHighStability()` (CONTRAST, LIFT, and the currently-unreachable SIMPLIFY), versus `suggestedBars = 4` for RESOLVE. `ContrastTargeting.derivePlan()`'s trajectory is a 4-step base, padded to the requested length by repeating the function of the last step -- so an 8-bar Contrast suggestion always asks for the same harmonic function 4 times in a row for the back half, directly creating the conditions the cap bug bites hardest on. Not itself a bug -- 8 bars for "open a new, contrasting section" is defensible -- but it does mean Contrast surfaces the cap limitation far more visibly (4 repeats vs. 1 in the worst case observed).

**Decision made this session:** leave the 8-bar Contrast/Lift bar count as-is for now. Revisit once LIFT actually exists (currently a `TODO` stub in `ProgressionGenerator`), since there's no real material yet to judge the 8-bar assumption against for that intent. Shortening bar counts to sidestep the cap issue would treat the symptom, not the cause.

---

## 4. Next concrete task: fix ChordSuggestionEngine.suggest()'s candidate cap

**Not started -- properly scoped, not yet designed.**

Core problem: `MAX_SUGGESTIONS = 4` combined with degree-1 weighting bonus means the candidate pool, ranked purely by weight before function-filtering, can and does exclude entire harmonic functions' worth of otherwise-valid diatonic chords.

**Rough shape of a fix, undesigned:** guarantee at least one representative chord per relevant harmonic function survives the cap -- e.g. compute candidates per-function first, then take the top-ranked candidate from each function bucket (up to the 4-result cap), rather than a single flat top-4-by-weight cut across all candidates regardless of function. Must preserve existing behavior for the 4th, unaffected caller (`ChordViewModel.kt:420`).

**Also worth deciding as part of scoping:** does this fix belong in `suggest()` itself (benefits all 4 callers), or should the phrase-generation strategies pass an "already excluded" set INTO `suggest()` (via a new optional parameter) so the engine factors exclusion into its own ranking BEFORE the 4-cap is applied, rather than after (as the current strategy-level fix does)? Passing exclusion in before ranking could let a valid-but-currently-excluded candidate survive the cap in cases where it otherwise wouldn't -- a meaningfully different design from a pure engine-side fix.

---

## 5. Remaining test matrix items (quick, deferred, not blocking)

### Contrast -- 3 of 5 items still open
- **Test 3 -- LIFT branch**: high-stability seed with elevated tension (7th/dim chord) should route to LIFT, not CONTRAST. Not yet run.
- **Test 4 -- SIMPLIFY dead-branch check**: `suggestAfterHighStability()`'s `AUTHENTIC && primaryCount >= 2` branch should be unreachable, since both known `suggestNextSection()` call sites call `PairingEngine.suggestNext(analysis)` with no `history` argument (defaults to `emptyList()`, so `primaryCount` is always 0). Not yet run.
- **Test 5 -- minor-key borrowed labels**: high-stability minor-key seed should show V/IV/I (parallel major) borrowed-chord labels, not bVII/iv/bVI. Not yet run.

### Resolve -- lower priority, mechanism already proven twice
- `defaultTrajectory` TONIC/DOMINANT branches untested (only PREDOMINANT verified). Same code path as the verified branch, different `when` arm.

### Continue -- not yet retested post-exclusion-fix
- Verified pre-fix in earlier sessions; NOT yet re-verified on-device since the `6ace490` exclusion fix landed. Resolve and Contrast both got direct before/after comparisons this session; Continue didn't.

---

## 6. Still deferred from previous sessions (unchanged, no new information)

- **DECEPTIVE cadence routing** -- `PairingEngine.suggestAfterMediumStability()` routes `CadenceType.DECEPTIVE` to SURPRISE, never RESOLVE. `ResolveTargeting`'s `deceptiveRecoveryTrajectory` branch remains unreachable via live SUGGEST.
- **isClosed() / high-stability RESOLVE routing** -- `suggestAfterHighStability()` has no RESOLVE path at all. `ResolveTargeting`'s `reopenThenResolveTrajectory` branch remains unreachable via live SUGGEST.
- Both blocked on the same underlying question: should `PairingEngine`/`PairingDecision` support offering the user a choice between multiple ranked candidate intents, rather than the engine always picking one winner? Aligns with the app's design philosophy ("if we impose a decision on a user, we are effectively building the composition for them"). Deferred as its own properly-scoped design piece.
- `IntentEngine.kt` confirmed dead code (zero callers, `PairingEngine` is the sole real dispatcher) -- no action needed.

---

## 7. Suggested order for next session

1. Quick Continue on-device retest (closes out Section 5's last open exclusion-fix verification gap).
2. `ChordSuggestionEngine.suggest()` cap fix -- the main event. Decide the design question (fix inside `suggest()` vs. pass exclusion in as a parameter) before writing code.
3. Remaining Contrast matrix items (3, 4, 5) -- quick, can slot in anywhere.
4. Multi-option `PairingDecision` design -- larger, separate piece, whenever there's a dedicated block of time for it.

---

*End of handoff document.*