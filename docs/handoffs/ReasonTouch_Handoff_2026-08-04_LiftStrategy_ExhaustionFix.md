# ReasonTouch -- Session Handoff: LiftStrategy Implementation & Exhaustion-Rotation Bug Fix

**Date:** 2026-08-04
**Status:** LiftStrategy implemented and wired in. A significant, previously-undiscovered bug in the shared exclusion/fallback pattern (present in all four generation strategies) was found via direct instrumentation, root-caused precisely via hand-trace, and fixed in LiftStrategy. Not yet rolled out to Continue/Contrast/Resolve.
**Follows on from:** `ReasonTouch_Handoff_2026-08-01.md`

---

## 1. LiftStrategy implemented

New files: `LiftTargeting.kt`, `LiftStrategy.kt` (same folder as the other three strategies). Wired into `ProgressionGenerator.kt`'s `CompositionIntent.LIFT` dispatch, replacing the `emptyList() // TODO: LiftStrategy` stub.

**Design:** unlike Continue/Contrast/Resolve, Lift's trajectory doesn't branch on cadence type or ending function -- it's a single, deliberate shape: alternate DOMINANT and PREDOMINANT (the two highest-tension harmonic functions per `ProgressionAnalyzer.calculateTension()`), never repeating a function twice in a row, never touching TONIC. This directly targets the two metrics `PairingEngine.suggestAfterHighStability()` actually checks for LIFT eligibility (`tension > 0.6f`) and what "energy" means in this codebase (`calculateEnergy()`'s `functionTransitions` ratio -- literally how often the harmonic function changes between adjacent chords).

**Known ceiling, not a bug:** diatonic chords in this codebase are restricted to MAJ/MIN/DIM quality only (`MusicTheory.MAJOR_QUALITIES`/`MINOR_QUALITIES`) -- no diatonic 7ths/sus/aug, and borrowed chords share the same restriction. Lift can only manipulate trajectory shape, not chord color, as an energy lever.

**Per-chord swap-out discussion:** while scoping Lift, a related idea was raised -- since SUGGEST's own name implies suggestion rather than imposition, should individual chords within a generated variant be swappable? Investigated and found that `ChordScreen.kt`'s `HarmonyPanel` (the pre-`ManualWorkspace` suggestion UI) already implements exactly this interaction pattern -- a horizontally-scrolling row of tappable chord-suggestion chips, browse-then-commit. The gap isn't UI design, it's that the phrase-generation strategies discard the full ranked candidate pool per position once they've picked a winner. Written up fully in `docs/design/Choice_Granularity_Design_2026-08-03.md`, alongside the related multi-option `PairingDecision` idea -- both are instances of "SUGGEST computes more than it exposes, and discards the rest."

---

## 2. Seed-finding, and a genuine miss along the way

First attempted LIFT-triggering seed, `G Bdim G C`, was hand-predicted to clear the `tension > 0.6f` threshold based on being DOMINANT-chord-heavy. **This was wrong.** Verified via `GenerationTestHarness` (which now prints `PairingEngine`'s actual decision, not just `ProgressionAnalyzer`'s raw numbers, per the 2026-08-03 extension): this seed routes to CONTRAST, not LIFT. Root cause: `calculateTension()` averages `(qualityTension + functionTension) / 2` per chord -- and plain MAJ/MIN triads only carry `qualityTension = 0.3`, capping even a lone DOMINANT chord's tension at `(0.3+0.8)/2 = 0.55`, under the 0.6 line. **No all-plain-triad progression can ever reach LIFT** -- this is a direct, provable consequence of the diatonic quality restriction noted above, not a one-off miscalculation.

Corrected seed: `Bdim G Bdim C` (or equivalently `G Bdim G C` reordered doesn't matter -- the fix is having a DIM chord present at all). DIM's `qualityTension = 0.9` is the only diatonic quality value high enough to push the average over 0.6. Hand-traced stability ~0.93, tension ~0.625 -- confirmed via harness: **routes to LIFT** ("High stability + tension -> energetic lift", 80% confidence), exactly as predicted.

Both seeds (the failed one and the working one) are kept in `GenerationTestHarness.runLiftInvestigationSeeds()` as documented positive/negative examples.

---

## 3. The exhaustion-rotation bug -- discovery, root cause, fix

### Discovery method

Rather than continuing to hand-trace from final output alone (increasingly unreliable across a growing number of strategies), temporary tagged debug instrumentation (`LIFT_TRACE`, following the project's established `BOUDIE`-tag precedent from the July bug investigation) was added directly inside the exclusion/fallback block of all four strategies, printing pool contents, `usedChords` state, and the computed rank at every position. This produced ground-truth evidence rather than inference from output alone.

### What the trace revealed

At `preferredLength = 8`, once a harmonic function's diatonic candidate pool is fully exhausted (every candidate already used earlier in the same continuation), the *existing* code's fallback:

```kotlin
val unusedPool = pool.filterNot { it.chord in usedChords }
val finalPool = unusedPool.ifEmpty { pool }
val rankForThisVariant = variantIndex.coerceAtMost(finalPool.size - 1)
```

reverts to the full `pool` as designed, but then applies `variantIndex` -- a value **fixed for the entire variant, never changing per position** -- as the rank into that pool. Since `pool` for a given function/key/starting-chord combination is deterministic, every subsequent exhausted-pool position lands on the *identical* rank, and therefore the *identical* chord, forever. Confirmed via trace: seed `Bdim G Bdim C`, variant 0, positions 4-7 all resolved to `G F G F` -- not because G/F were "preferred," but because `rank=0` was frozen from position 4 onward.

This is a structural property of the shared exclusion pattern, confirmed present in `ContinueStrategy.kt`, `ContrastStrategy.kt`, `ResolveStrategy.kt`, and `LiftStrategy.kt` alike (same code shape in all four). It supersedes and sharpens the 2026-08-03 "tonic-function exhaustion at preferredLength=8" finding -- that finding correctly identified *that* exhaustion happens, but attributed the resulting repetition to the pool simply running out, not to this specific rank-freezing mechanism. The pool running out is real and unavoidable (only 2-3 diatonic candidates exist per function); the *frozen, always-identical* fallback pick on top of that is the actual, fixable bug.

### Fix

Two attempts were needed; documenting both since the first failed instructively.

**First attempt (failed):** count `usedChords.count { used -> pool.any { it.chord == used } }` (a `Set`) to derive a rotating offset. This saturates at `pool.size` the moment every candidate has been used *once*, then never advances further -- `Set` membership doesn't grow with repeated use. Confirmed via re-run: still froze, just on a different constant rank than before.

**Working fix:** count occurrences in `continuation` (the growing `MutableList`, which genuinely accumulates repeats) instead:

```kotlin
val rankForThisVariant = if (unusedPool.isEmpty()) {
    val timesRevisited = continuation.count { c -> pool.any { it.chord == c } }
    (variantIndex + timesRevisited) % finalPool.size
} else {
    variantIndex.coerceAtMost(finalPool.size - 1)
}
```

**Verified via full manual hand-trace against real LIFT_TRACE output** (all 8 positions, variant 0, seed `Bdim G Bdim C`) -- every position's `timesRevisited`/`rank`/`chosen` value reconciles exactly with the printed trace. Confirmed result: `G F Bdim Dm G F Bdim Dm` -- genuine rotation through both DOMINANT and both PREDOMINANT candidates, not a frozen repeat. Candidate count also increased from 2 to 3 distinct variants (more genuine diversity survives the `distinctBy` dedup step at the end of `generate()`).

**Applied to `LiftStrategy.kt` only so far.** Not yet applied to Continue/Contrast/Resolve.

---

## 4. Next steps

1. Apply the identical fix to `ContinueStrategy.kt`, `ContrastStrategy.kt`, `ResolveStrategy.kt` (same code shape, same fix, straightforward rollout -- see `docs/testing/Strategy_Test_Log.md` for the exact insertion points already used for instrumentation in each file).
2. Re-run existing harness seeds for all three post-fix, confirm no regressions and that previously-observed frozen tails (e.g. Continue's `G C D Am` seed from 2026-08-03, which produced `D G C Bm G G G G`) now rotate correctly.
3. Strip all `*_TRACE` temporary debug logging once rollout is confirmed across all four strategies.
4. Update `docs/testing/Strategy_Test_Log.md` coverage summary and `docs/Task_List.md` to reflect the corrected understanding (this bug supersedes the "tonic exhaustion" framing from 2026-08-03 -- that finding was real but incompletely diagnosed; this document supersedes it).
5. Continue LIFT-specific verification: EXPAND and SURPRISE strategies remain unbuilt (see `docs/Task_List.md`'s URGENT section) -- same live empty-suggestion-bug urgency as LIFT had, now that LIFT itself is closed out.

---

*End of handoff document.*