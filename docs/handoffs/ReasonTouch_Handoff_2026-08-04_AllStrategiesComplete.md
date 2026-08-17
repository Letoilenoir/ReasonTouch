# ReasonTouch — Session Handoff: All 6 Strategies Complete; Next Session Starts PairingEngine Refactor

**Date:** 2026-08-04
**Status:** Major milestone reached — all 6 CompositionIntent strategies (Continue, Contrast, Resolve, Lift, Expand, Surprise) are built and verified. Next session's task is fully scoped and ready to start immediately.
**Follows on from:** `ReasonTouch_Handoff_2026-08-04_LiftStrategy_ExhaustionFix.md`

---

## 1. What was completed this session

### LiftStrategy — built and verified
`LiftTargeting`/`LiftStrategy` implemented (alternating DOMINANT/PREDOMINANT trajectory, avoids TONIC, maximizes the two metrics `PairingEngine` actually checks for LIFT eligibility). Routing confirmed via `Bdim G Bdim C` (needed a DIM chord specifically — plain triads can never clear LIFT's tension threshold, since `qualityTension` for MAJ/MIN is only 0.3, capping even a lone DOMINANT chord's tension at 0.55, under the 0.6 line).

### Major bug found, root-caused, and fixed: exhaustion-rotation freeze
Discovered while testing Lift at `preferredLength=8`: once a harmonic function's diatonic candidate pool is exhausted within one continuation, the existing exclusion/fallback logic froze onto one identical chord forever, rather than rotating through the exhausted pool. Root cause: the fallback used `variantIndex` as a fixed rank into the pool, which never changes per position. Fixed by computing a rotating offset from how many times pool-matching chords have already appeared in the growing `continuation` list (not `usedChords`, which is a `Set` and saturates — a documented, informative first-attempt failure).

**Centralized rather than copy-pasted**: extracted the fix into a new shared `ChordCandidateSelector.kt`, replacing four near-identical inline copies across Continue/Contrast/Resolve/Lift. This was a deliberate DRY decision — the original bug existing identically in 4 files, undiscovered in 3 of them, was itself the argument for centralizing. Rolled out and independently re-verified in all 4 files via direct seed comparison at `preferredLength=8`.

### ExpandStrategy — built, partially verified, one branch mathematically proven dead
`ExpandTargeting`/`ExpandStrategy` implemented (3-function cycle: TONIC→PREDOMINANT→DOMINANT). Generation logic and exhaustion-rotation fix both fully confirmed via forced-intent seeds.

Two live-routing findings:
- **HALF-cadence branch proven mathematically dead**, not just hard to reach. `functionStability(DOMINANT)=0.15` and HALF's `cadenceBonus=-0.10` cap total stability at ~0.20 for any HALF-cadence progression — structurally below the 0.4 medium-stability floor `PairingEngine` requires. No seed can ever reach it under current constants.
- **Medium-stability-default branch**: 9 seed attempts across the session all missed (either falling outside `[0.4, 0.7]` or hitting the `barCount<=4` sibling branch instead). Root cause of the difficulty: `functionStability`'s ending-chord-function term (0.95/0.45/0.15) dominates the whole score, so any seed edit that changes what the progression *ends on* swings stability 30-80 points — safe seed construction must hold the ending function fixed. Full attempt log and a not-yet-tried next-attempt seed (`Dm Edim Am Dm Gm`) are in `docs/testing/Strategy_Test_Log.md`.

### SurpriseStrategy — built and FULLY verified
`SurpriseTargeting`/`SurpriseStrategy` implemented (primes an expected resolution via PREDOMINANT/DOMINANT alternation, then substitutes a borrowed chord at the *final* position — generalizing the deceptive cadence pattern to a whole phrase, as opposed to Contrast's borrowed chord at position 0). Both generation logic and live `PairingEngine` routing confirmed, via `C C F G Am` (C major, stability 56%, DECEPTIVE cadence → SURPRISE, 75% confidence). Every prediction (key, stability, cadence, routing) matched exactly.

### Root cause of repeated seed-prediction failures — now fixed methodologically
Most of this session's wasted attempts (across both Expand and Surprise investigation) came from hand-*intuiting* which key `KeyDetector` would pick, rather than *computing* its actual weighted formula:


Specific trap: any progression ending on scale-degree-6 of an intended key (exactly what a deceptive cadence needs) is simultaneously scale-degree-1 of that key's relative minor/major — so `lastChordTonicBonus` fires strongly for the *competing* interpretation precisely when testing deceptive cadences. Fix: compute both candidate keys' scores by hand before proposing a seed; if the intended key doesn't win by a comfortable margin, reinforce its own bonuses (repeat the tonic, ensure the dominant is present) rather than trying to suppress the competitor. Confirmed effective on first use (`C C F G Am` — every prediction correct).

**This methodology is now documented in `Strategy_Test_Log.md` and should be used for all future seed construction**, including revisiting Expand's still-open medium-stability question.

score = matchCount1.0 + tonicBonus1.5 + dominantBonus1.2 + lastChordTonicBonus1.3

---

## 2. Full status: all 6 strategies

| Strategy | Generation logic | Live PairingEngine routing |
|---|---|---|
| Continue | ✅ PASS | ✅ PASS |
| Contrast | ✅ PASS | ✅ PASS |
| Resolve | ✅ PASS | ✅ PASS (HALF/PREDOMINANT paths) — DECEPTIVE and isClosed() paths structurally blocked, see below |
| Lift | ✅ PASS | ✅ PASS |
| Expand | ✅ PASS | ⚠️ PARTIAL — HALF-cadence proven dead; medium-default unconfirmed but not proven dead |
| Surprise | ✅ PASS | ✅ PASS |

**Key conclusion: every remaining gap is at the `PairingEngine` routing layer, not the strategy implementations.** All 6 strategies' own generation code is confirmed correct. This directly motivates next session's task.

---

## 3. Product design discussion this session

Clarified the actual target for multi-option choice, directly from you: **a ranked menu with one default highlighted** (e.g. "Expand fits best, but you can choose Lift/Resolve etc.") — not an unranked free-for-all, and not "every strategy always selectable regardless of seed." This is more tractable than earlier framings and maps naturally onto `IntentEngine.rank()`'s already-existing (but dead/unused) `List<IntentRanking>` shape.

Also received and reviewed a full UX/UI design brief (already filed at `docs/design/Piano Roll Composition UI/UX Direction.md`) describing the target Composition Tray experience — Home → Piano Roll → Composition Tray as the primary navigation model, with a two-stage "choose strategy, then choose phrase" Suggest Next flow. This document independently confirms the sequencing: complete the strategies (now done) → build the multi-option `PairingEngine` (next) → build the Composition Tray UI → later navigation refactor removing the legacy CHORDS|ARRANGE|DRUMS row.

---

## 4. Next session: start here

**Task:** `PairingEngine` multi-option refactor. Fully scoped in a dedicated planning document: `docs/design/PairingEngine_MultiOption_Refactor_Plan_2026-08-04.md`. Read that document first — it covers:
- Proposed return-shape options (flat `List<PairingDecision>` vs. richer wrapper)
- Per-stability-bucket design questions (what counts as "viable" in each of the three `suggestAfter*Stability()` functions)
- Which currently-dead branches this refactor should naturally resolve (RESOLVE's DECEPTIVE branch — should resolve automatically; RESOLVE's isClosed() branch — depends on a design decision made during the refactor, not automatic)
- Call-site impact (both `ChordViewModel` and `PianoRollViewModel`'s duplicated `suggestNextSection()` need updating — good opportunity to fold in that long-standing consolidation while both files are open anyway)
- Testing approach (extend `GenerationTestHarness` to print the full ranked list, re-verify the existing seed corpus)
- Suggested order of work (7 steps, laid out in the plan doc's section 10)

**Also queued, same functions, worth doing in the same pass per the plan doc:** Option C — the EXPAND/RESOLVE HALF-cadence disambiguation fix (differentiate by `barCount` inside `suggestAfterLowStability()`, without touching `calculateHarmonicStability()`'s shared constants). This was discussed and a specific code shape agreed, but not yet applied — see `docs/design/Choice_Granularity_Design_2026-08-03.md` section 7 for the full reasoning on why this is a stopgap under the current single-winner architecture, worth doing now anyway as a contained correctness fix.

**Smaller open items, not blocking, pick up opportunistically:**
- Expand's medium-stability-default seed still unconfirmed — retry `Dm Edim Am Dm Gm` using the corrected KeyDetector-aware methodology, or fold verification into the post-refactor multi-option re-test pass instead (may be more efficient to do it once, after the refactor, rather than twice)
- Strip stale inline comments from `ContinueStrategy.kt`/`ContrastStrategy.kt`/`ResolveStrategy.kt` describing the old (now-centralized) exclusion logic
- Consider a dedicated `ChordCandidateSelectorTest.kt` regression suite

---

## 5. Documents map, for orientation

- `docs/Task_List.md` — living task list, updated throughout this session, reflects everything above
- `docs/testing/Strategy_Test_Log.md` — full seed-by-seed verification history for all 6 strategies, including the KeyDetector methodology writeup
- `docs/design/Choice_Granularity_Design_2026-08-03.md` — the "why" for multi-option choice (3 pieces of evidence: DECEPTIVE cadence ambiguity, chord-level swap-out, stability threshold gap)
- `docs/design/PairingEngine_MultiOption_Refactor_Plan_2026-08-04.md` — the "how," fully scoped, start here next session
- `docs/design/Piano Roll Composition UI/UX Direction.md` — the target UX this refactor unblocks

---

*End of handoff document.*