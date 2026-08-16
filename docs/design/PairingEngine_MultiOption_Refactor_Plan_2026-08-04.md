# ReasonTouch -- PairingEngine Multi-Option Refactor: Implementation Plan

**Date:** 2026-08-04
**Status:** Planning document -- not yet started. This is the critical-path task per `docs/Task_List.md`.
**Depends on:** `docs/design/Choice_Granularity_Design_2026-08-03.md` (the "why" -- three pieces of evidence for why single-winner routing is insufficient). This document is the "how."
**Blocks:** The Piano Roll Composition Tray UX design's two-stage Suggest Next flow (choose strategy, then choose phrase) cannot be built until this lands.

---

## 1. Goal, stated precisely

`PairingEngine.suggestNext()` currently returns exactly one `PairingDecision`. Every strategy that isn't the single computed winner is unreachable via live SUGGEST, regardless of how musically valid it might be for the same seed. The goal is: **`PairingEngine` should return a ranked list of viable options, not a single answer** -- letting the user (eventually, via the Composition Tray) choose freely among genuinely plausible directions, with the engine's own pick highlighted but not enforced.

This is explicitly NOT "make every one of the 6 strategies always selectable regardless of the seed" (a much larger, probably-undesirable goal -- CONTINUE will never be a sensible option for a progression crying out for RESOLVE). It IS "surface every strategy that the harmonic analysis genuinely supports as plausible for this seed, ranked, rather than silently discarding all but one."

## 2. Confirmed status of prerequisite work (as of 2026-08-04)

All 6 strategies (`ContinueStrategy`, `ContrastStrategy`, `ResolveStrategy`, `LiftStrategy`, `ExpandStrategy`, `SurpriseStrategy`) are confirmed correct at the generation-logic level -- see `docs/testing/Strategy_Test_Log.md`. This refactor can proceed without touching any strategy's own `generate()` logic. The problem is entirely upstream, in `PairingEngine.kt`'s three `suggestAfter*Stability()` functions.

## 3. What currently exists that can likely be reused

- **`IntentEngine.kt`** -- confirmed dead code (zero callers anywhere in the project, per a full-project search done in an earlier session), but it already has the right *shape*: `rank()` returns `List<IntentRanking>` sorted by confidence, where `IntentRanking` is `(intent, confidence, rationale)`. This is extremely close to what the new `PairingEngine` needs to return, just for the wrong dispatcher and with a much simpler (and less accurate) stability-only bucketing than `PairingEngine`'s real branches use. Worth deciding: adapt `IntentEngine`'s output shape, or design fresh -- but the shape itself (ranked list of intent+confidence+rationale) is sound and shouldn't need reinventing.
- **`PairingEngine`'s existing `when` branch structure** already implicitly encodes a priority order within each stability bucket (e.g. `suggestAfterHighStability()` checks AUTHENTIC+primaryCount first, then tension, then falls to the `else`/CONTRAST default). This ordering is valuable domain knowledge -- the refactor should preserve it as a *ranking signal*, not discard it.

## 4. Proposed new return shape

Replace the single `PairingDecision` return with something like:

```kotlin
data class PairingSuggestion(
    val decisions: List<PairingDecision>,  // ranked, index 0 = engine's top pick
)
```

or, if `PairingDecision` itself should carry its own rank/confidence (it already has a `confidence: Float` field per prior sessions' review), simply:

```kotlin
fun suggestNext(
    currentAnalysis: ProgressionAnalysis,
    history: List<PairingType> = emptyList()
): List<PairingDecision>
```

**Open question, needs a decision before implementation starts:** should this be a flat `List<PairingDecision>` (simplest, `first()` gives the old single-winner behavior for any caller not yet updated) or a richer wrapper type distinguishing "the recommended default" from "other viable options" more explicitly? Leaning toward the flat list for simplicity and backward-compatibility during rollout (existing callers can keep working via `.first()` while the UI layer is updated separately), but worth confirming before coding.

## 5. Per-stability-bucket design: what "viable" means in each branch

This is the actual design work -- each of the three `suggestAfter*Stability()` functions currently picks exactly one branch via early-return `when` semantics. Each needs to become "evaluate all applicable branches, return all that clear some plausibility bar, ranked."

### `suggestAfterHighStability()` (stability >= 0.8f)
Current branches: AUTHENTIC+primaryCount>=2 (→ SIMPLIFY or CONTRAST), tension>0.6f (→ LIFT), else (→ CONTRAST).
**Proposed:** CONTRAST should likely almost always be included (it's the natural "high stability, ready for variation" default). LIFT should be included whenever tension>0.6f, ranked alongside CONTRAST rather than instead of it -- both are genuinely plausible for a stable, tense progression. SIMPLIFY's `primaryCount>=2` condition depends on `history`, which is currently always passed empty by both call sites (a separate, already-logged issue) -- worth deciding whether this refactor is the moment to also fix that, or whether SIMPLIFY stays excluded until `history` threading is fixed separately.

### `suggestAfterMediumStability()` (0.4f..0.7f)
Current branches: DECEPTIVE cadence (→ SURPRISE), HALF cadence (→ EXPAND), barCount<=4 (→ CONTINUE), else (→ EXPAND).
**Proposed:** DECEPTIVE and HALF are mutually exclusive by `cadenceType`, so at most one of SURPRISE/EXPAND's cadence-triggered path applies per seed -- but CONTINUE (via barCount) and EXPAND (via the else) currently compete on `barCount` alone; both could plausibly be offered together for a seed near that boundary, letting the user decide whether a short phrase should stay short (CONTINUE) or open up (EXPAND).

### `suggestAfterLowStability()` (else, or -- pending Option C -- explicitly < 0.4f)
Current branches: DOMINANT ending (→ RESOLVE, 95% conf), PREDOMINANT ending (→ RESOLVE, 75% conf), tension>0.7f (→ CONTINUE), else (→ CONTINUE).
**Proposed:** RESOLVE should likely remain the strong default whenever ending on DOMINANT/PREDOMINANT (per its already-high confidence values), but CONTINUE could reasonably be offered alongside it as a lower-ranked alternative -- "you could resolve now, or keep going a bit longer" is a legitimate creative choice, not just an engine artifact.

**This section (4-5) is the actual design work still needed -- the shape above is a starting proposal, not a final spec.** Recommend a dedicated design pass on each bucket's exact inclusion criteria before writing code, ideally validated against a handful of real seeds the same way strategy verification was done.

## 6. Interaction with already-known dead/blocked branches

This refactor is expected to naturally resolve several previously-logged reachability gaps, without any additional code beyond the refactor itself:
- **RESOLVE's DECEPTIVE branch** (`ResolveTargeting.deceptiveRecoveryTrajectory`) -- currently dead because `suggestAfterMediumStability()` always routes DECEPTIVE to SURPRISE alone. Once DECEPTIVE can return both SURPRISE and RESOLVE as ranked options, this branch becomes reachable for the first time.
- **RESOLVE's isClosed() branch** (`reopenThenResolveTrajectory`) -- currently dead because `suggestAfterHighStability()` has no RESOLVE path at all. Whether this refactor resolves it depends on whether RESOLVE gets added as a viable (if low-ranked) option in the high-stability bucket -- worth deciding explicitly during the Section 5 design pass, since it's not automatic the way the DECEPTIVE case is.
- **EXPAND's HALF-cadence branch** -- separately proven mathematically dead (not a routing problem, a stability-formula ceiling problem). This refactor does NOT fix this on its own; Option C (or an equivalent fix to `suggestAfterLowStability()`'s DOMINANT/PREDOMINANT branches, see `Choice_Granularity_Design_2026-08-03.md` section 7) is still separately required. Worth doing Option C as part of this same work session, since it touches the same functions, even though it's conceptually a different fix.

## 7. Call-site impact

Both `ChordViewModel.kt` and `PianoRollViewModel.kt` have their own separate `suggestNextSection()` implementations (already flagged in `Task_List.md` as a duplication worth consolidating). Both currently do something like:

```kotlin
val decision = PairingEngine.suggestNext(analysis)
// ... use decision.type directly
```

Once `suggestNext()` returns a list, both call sites need updating -- at minimum to take `.first()` for continuity during rollout, but ideally this refactor is the natural moment to also address the long-standing duplication between these two ViewModels, given both need touching anyway. **Recommend folding the `suggestNextSection()` consolidation into this same piece of work**, rather than doing the PairingEngine change now and the duplication cleanup later as a second pass touching the same files again.

## 8. Testing approach

Given how effective direct instrumentation (`*_TRACE` println logging) and the `GenerationTestHarness` were for verifying the exhaustion-rotation fix and all 6 strategies, the same approach should be used here:
1. Extend `GenerationTestHarness` (or add a new harness function) to print the *full ranked list* `PairingEngine.suggestNext()` returns for a seed, not just the single decision.
2. Re-run every seed already used across this session's strategy verification work (the full corpus is documented across `Strategy_Test_Log.md`'s dated entries) against the new multi-option output, checking that: (a) the previous single-winner behavior is still present as the top-ranked option for continuity, (b) genuinely plausible alternatives now appear alongside it, (c) implausible strategies are still correctly excluded (e.g. CONTINUE should not appear as a viable option for a seed crying out for RESOLVE).
3. Specifically target the DECEPTIVE-cadence seed (`C C F G Am`, already confirmed to produce SURPRISE as the sole result) to confirm RESOLVE now appears as a ranked alternative post-refactor.

## 9. Explicitly out of scope for this piece of work

- The actual Composition Tray UI (per the UX design doc) -- this refactor only needs to make the *data* available; presenting it is separate, later work.
- Per-chord swap-out (`docs/design/Choice_Granularity_Design_2026-08-03.md` section 4) -- a related but independent piece of the same broader "surface alternatives, don't discard them" principle, scoped separately.
- Mood-as-persistent-motif, strum-speed field, safe-write mechanism -- all separately tracked, unrelated to this work.

---

## 10. Suggested order of work

1. Decide the exact return-shape question (Section 4).
2. Design pass on each stability bucket's inclusion criteria (Section 5) -- validate against real seeds before coding, same discipline as strategy verification.
3. Implement the refactor in `PairingEngine.kt`.
4. Update both ViewModels' `suggestNextSection()` call sites, folding in the long-standing duplication cleanup (Section 7).
5. Apply Option C (EXPAND/RESOLVE HALF-cadence disambiguation) in the same pass, since it touches the same functions.
6. Extend `GenerationTestHarness` for multi-option output, re-verify the full existing seed corpus (Section 8).
7. Update `Strategy_Test_Log.md` and `Task_List.md` to close out the newly-resolved dead branches (RESOLVE's DECEPTIVE and, if included per Section 6, isClosed()).

---

*End of planning document.*