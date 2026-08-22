# ReasonTouch — Session Handoff: Multi-Intent Scoring Layer & suggestNextSection() Consolidation

**Date:** 2026-08-18
**Status:** All code changes complete and BUILD SUCCESSFUL confirmed. On-device verification of both SUGGEST NEXT flows deferred to next session.
**Follows on from:** `ReasonTouch_Handoff_2026-08-01.md` (Multi-Intent Pairing design doc) and the same-day session handoff (repeated-chord fix & `ChordSuggestionEngine.suggest()` cap discovery).
**Not yet committed** as of session end — commit instructions at the bottom of this doc.

---

## 1. What was built: multi-intent scoring layer

Implemented the scoring function specified in `ReasonTouch_Handoff_2026-08-01.md`'s "Multi-Intent Pairing" design doc — `PairingEngine` now ranks all viable next-section intents by continuous plausibility instead of picking exactly one via hand-branched logic.

### `PairingEngine.kt` — full rewrite

**Path:** `feature\feature-chords\src\main\kotlin\com\reasontouch\feature\chords\PairingEngine.kt`

New/changed members:

- **`scoreIntent(type: PairingType, analysis: ProgressionAnalysis): Float`** — the scoring function from the design doc, verbatim. Continuous 0–1 plausibility per intent based on `harmonicStability`, `cadenceType`, `endingFunction`, and whether borrowed chords exist for the key. Never excludes — EXPAND/SIMPLIFY/MODULATE fall through to `0.0f` since they have no live strategy.
- **`IMPLEMENTED_INTENTS`** (private `Set<PairingType>`) — the gate that actually controls what gets ranked: `RESOLVE, CONTRAST, CONTINUE, LIFT, SURPRISE`. EXPAND/SIMPLIFY/MODULATE excluded — confirmed this session there is no strategy for any of them, and no musical gap either (Andy's call: Lift already covers "sustain energy," Continue covers "extend phrase neutrally").
- **`rationaleFor(type, analysis): String`** (private) — UI-facing explanation text, mirrors the same branch conditions as `scoreIntent()` so displayed text always matches the score that drove it.
- **`rankIntents(analysis, threshold = 0.65f): List<IntentOption>`** — scores every implemented intent, sorts descending by confidence. Never filters.
- **`suggestNextMulti(currentAnalysis, threshold = 0.65f, filterByThreshold = false): PairingDecisionSet`** — by default returns *all* implemented intents ranked (matches the design doc's "rank, don't filter" principle). `filterByThreshold = true` restores old cut-at-threshold behavior if ever needed for a specific UI surface.
- **`suggestNext(currentAnalysis, history = emptyList()): PairingDecision`** — now a thin wrapper: `suggestNextMulti(currentAnalysis).options.first()`. Existing call sites (both ViewModels) unaffected — same signature. `history` param kept for source compatibility but unused (both known callers already pass `emptyList()`).
- Old `suggestAfterHighStability/MediumStability/LowStability()` branch functions **commented out**, kept as reference only — not deleted, since they document *why* e.g. HALF cadence used to route to EXPAND.

### `PairingDecision.kt` — cleanup

**Path:** `feature\feature-chords\src\main\kotlin\com\reasontouch\feature\chords\PairingDecision.kt`

Removed a broken, never-functional stub (`suggestNextMulti()` free function referencing an undefined `PairingContext` type and a nonexistent `scoreIntent(type, ctx)` overload — this had been sitting in the file since before this session, uncompilable). File now contains only:

- `PairingType` enum (unchanged, all 8 values)
- `PairingDecision` data class (unchanged — single top-ranked result)
- `IntentOption` data class (new) — one ranked intent: `type, suggestedBars, confidence, rationale, isAboveThreshold`. `isAboveThreshold` is informational only (UI styling), never used to exclude.
- `PairingDecisionSet` data class (new) — `options: List<IntentOption>` + `threshold: Float`, with a `topOption` convenience getter.

### `SuggestionWorkflow.kt` — new file

**Path:** `feature\feature-chords\src\main\kotlin\com\reasontouch\feature\chords\SuggestionWorkflow.kt`

New shared object consolidating what were two independently-drifted copies of the SUGGEST NEXT logic in `ChordViewModel` and `PianoRollViewModel`. Synchronous (non-suspend) — callers supply the progression, this object has no opinion on where it came from.

- `suggestNextSection(progression: List<ChordEvent>): PairingDecision`
- `suggestNextPhrases(progression: List<ChordEvent>): List<GeneratedProgression>`
- `private fun PairingType.toCompositionIntent(): CompositionIntent` (moved here from both ViewModels, deleted from both)

**Bug fixed by this consolidation:** `ChordViewModel.suggestNextPhrases()` previously hardcoded `preferredLength = 4` regardless of what `PairingEngine.suggestNext()` returned. `PianoRollViewModel`'s copy always correctly used `pairingDecision.suggestedBars`. This meant the *same seed progression* could produce a 4-bar suggestion from the Chords screen and an 8-bar suggestion from Piano Roll for the identical CONTRAST/LIFT/SURPRISE decision. Now both screens delegate to the same function — this class of bug can't recur.

**Fallback message fix (minor, flagged, applied):** `PianoRollViewModel`'s old empty-progression fallback incorrectly said `"Could not detect key"` (misleading — nothing was analyzed yet). `SuggestionWorkflow` now distinguishes "no progression yet" (confidence 0.5, message: *"No progression yet - start with any 4-bar section"*) from "progression present but key undetected" (confidence 0.3, message: *"Could not detect key"*) for both screens.

### `ChordViewModel.kt` — delegation + preferredLength fix

**Path:** `feature\feature-chords\src\main\kotlin\com\reasontouch\feature\chords\ChordViewModel.kt`

`suggestNextSection()` and `suggestNextPhrases()` now delegate to `SuggestionWorkflow`, one-liners. Verbose `PAIRTRACE` debug logging that was in the old `suggestNextPhrases()` was dropped (not carried into `SuggestionWorkflow` — if needed again, should be re-added as ViewModel-side wrapper logging, not inside the shared object, so it doesn't get duplicated for Piano Roll too). Private `toCompositionIntent()` extension deleted (now lives in `SuggestionWorkflow`).

Confirmed via `Select-String` at session end:
```
Line 764: // Delegates to SuggestionWorkflow -- see that file for the shared logic
Line 768: SuggestionWorkflow.suggestNextSection(progression.value)
Line 775: SuggestionWorkflow.suggestNextPhrases(progression.value)
```

### `PianoRollViewModel.kt` — delegation

**Path:** `feature\feature-pianoroll\src\main\kotlin\com\reasontouch\feature\pianoroll\PianoRollViewModel.kt`

Same pattern, but stays `suspend` — progression is fetched fresh via `repository.getChordsForSessionOnce(sessionId)` (unchanged behavior, not switched to the `chords` StateFlow). Private `toCompositionIntent()` extension deleted from here too.

Confirmed via `Select-String` at session end:
```
Line 24:  import com.reasontouch.feature.chords.SuggestionWorkflow
Line 830: // Delegates to SuggestionWorkflow (feature-chords module) -- see that
Line 838: return SuggestionWorkflow.suggestNextPhrases(currentProgression)
Line 843: return SuggestionWorkflow.suggestNextSection(currentProgression)
```

### `SuggestNextDialog.kt` — incident & recovery

**Path:** `feature\feature-chords\src\main\kotlin\com\reasontouch\feature\chords\components\SuggestNextDialog.kt`

Mid-session, this file's contents were accidentally overwritten with `SuggestionWorkflow.kt`'s content (apparent paste-into-wrong-tab), producing a `Redeclaration: object SuggestionWorkflow` compile error in two files. Restored to its original, correct content — the single-decision `@Composable fun SuggestNextDialog(decision: PairingDecision, options: List<String>, onOptionSelected: (Int) -> Unit, onDismiss: () -> Unit)`. No functional change from before this session; this is purely a recovery, not a design change. Confirmed via `Select-String`:
```
Line 1:  package com.reasontouch.feature.chords.components
Line 31: fun SuggestNextDialog(
```

**Lesson for future sessions:** when creating a new file via Android Studio's "Add File to Git" flow, double check the *active editor tab* before pasting — the dialog confirms the new file's path but doesn't stop you from having pasted into a different already-open tab first.

---

## 2. Where SUGGEST NEXT is actually called from (screen-level, for on-device testing)

Two independent trigger points, both already wired to the corrected code — no further wiring needed for basic verification (multi-option UI is a separate follow-on, see Section 4).

### Chords screen — `ManualWorkspace.kt`
**Path:** `feature\feature-chords\src\main\kotlin\com\reasontouch\feature\chords\workspaces\ManualWorkspace.kt`

`FixedBottomBar`'s `onSuggestNext` callback (bottom action bar, "SUGGEST NEXT" button):
```kotlin
onSuggestNext = {
    val suggestion = viewModel.suggestNextSection()
    currentSuggestion = suggestion
    currentGeneratedPhrases = viewModel.suggestNextPhrases()
    currentOptions = if (currentGeneratedPhrases.isNotEmpty()) {
        currentGeneratedPhrases.map { it.explanation }
    } else {
        listOf("${suggestion.type.name} pathway not yet implemented — ...")
    }
    showSuggestDialog = true
}
```
Both `viewModel.suggestNextSection()` and `viewModel.suggestNextPhrases()` are `ChordViewModel` calls, now delegating to `SuggestionWorkflow`.

### Piano Roll screen — `PianoRollScreen.kt`
**Path:** `feature\feature-pianoroll\src\main\kotlin\com\reasontouch\feature\pianoroll\PianoRollScreen.kt`

`PianoRollToolbar`'s `onSuggestNext` callback ("SUGGEST" toolbar chip):
```kotlin
onSuggestNext = {
    scope.launch {
        val suggestion = viewModel.suggestNextSection()
        currentSuggestion = suggestion
        currentGeneratedPhrases = viewModel.suggestNextPhrases()
        currentOptions = currentGeneratedPhrases.map { it.explanation }
        showSuggestDialog = true
    }
}
```
Both calls are `PianoRollViewModel` (suspend), now delegating to `SuggestionWorkflow`.

Both screens render the same `SuggestNextDialog` composable (`components/SuggestNextDialog.kt`, restored this session) — single decision + phrase-option list, unchanged UI.

---

## 3. On-device test plan for next session

**Core thing to verify:** same seed progression + same resulting `PairingDecision` now produces the **same bar count** regardless of which screen triggered SUGGEST NEXT. Previously Chords screen was stuck at 4 bars always; Piano Roll correctly varied 4/8 by intent.

### Suggested seed progressions (pick ones that land on non-RESOLVE intents, since RESOLVE is the one case where 4 bars is *correct* either way and won't reveal the bug)

| Seed | Expected top intent (per `scoreIntent()`) | Expected bars |
|---|---|---|
| High-stability, authentic cadence (e.g. `C F G C`) | CONTRAST (0.90) or LIFT if tension high | 8 |
| Open/unresolved, medium stability (e.g. `Am F C`, ends mid-phrase) | CONTINUE (0.85, `isOpen()` branch) | 8 |
| Deceptive cadence (V→vi) | RESOLVE (0.90) *or* SURPRISE (0.90) — these tie; whichever sorts first in `IMPLEMENTED_INTENTS` set iteration order wins the tie-break today. Worth noting if the tie-break feels arbitrary on-device — not currently deterministic by design intent, just by `Set` iteration. | RESOLVE→4, SURPRISE→8 |
| Ends on V (dominant) | RESOLVE (0.95) | 4 |

**Test procedure per seed, both screens:**
1. Build the seed progression on the Chords screen (Manual workspace).
2. Tap SUGGEST NEXT. Note the intent name and bar count shown in the dialog's confidence line (`"Suggested: N bars (X% confident)"`).
3. Send the same progression to Piano Roll (or rebuild it there) and tap SUGGEST there.
4. Confirm intent + bar count match between the two screens for the same seed.
5. Also sanity-check the rationale text shown makes musical sense for the seed (this exercises `rationaleFor()`, not just `scoreIntent()`).

**Known open question to resolve during testing, not before:** the deceptive-cadence tie (RESOLVE vs SURPRISE both scoring 0.90) — worth observing what actually gets picked on-device and deciding then whether it needs a deliberate tie-break rule, since this wasn't spec'd in the original design doc and guessing at a rule without a real example in front of us would be premature.

---

## 4. Explicitly deferred / not done this session

- **On-device verification itself** — everything above is ready for it, none of it has been run on a device yet.
- **SUGGEST dialog multi-option UI** — `SuggestNextDialog` still takes a single `PairingDecision`; `PairingDecisionSet` (the ranked list) exists in `PairingEngine`/`PairingDecision.kt` but nothing in the UI layer consumes it yet. Separate, unscoped piece of work — doc explicitly flagged this as the natural next step after ranking existed, per the original design doc's stated goal ("UI can show typical, alternative, and creative options").
- **`ChordSuggestionEngine.suggest()` candidate-cap fix** — carried over unchanged from the 2026-08-01 handoff (Section 3/4 there). Not touched this session. Still the "main event" per that doc's suggested order, now further deferred behind on-device verification of today's work.
- **`ResolveStrategyTest.kt` / `LiftStrategyTest.kt` / `SurpriseStrategyTest.kt`** — still don't exist. `ContinueStrategyTest.kt` remains the only regression suite of the four strategies, and per an earlier conversation this session, none of the existing tests would have caught the `preferredLength = 4` bug anyway (they test `ContinueStrategy` in isolation, one layer below where the bug lived — in `ChordViewModel`/`PairingEngine` integration). A true regression test for bugs like this would need to exercise `SuggestionWorkflow.suggestNextPhrases()` end-to-end.
- **Mojibake corruption sweep** — repo-wide `Select-String` check for the `ChordViewModel.kt`-style Unicode corruption pattern was attempted but the terminal command hung on the special-character pattern; not re-attempted with a working version before session end. Android Studio's file encoding settings were checked and confirmed correct (UTF-8, no transparent native-to-ascii conversion) earlier in the session, which should prevent recurrence — but the sweep to confirm no *other* file already has the issue never completed. Worth a retry next session with:
  ```powershell
  Get-ChildItem -Recurse -Include *.kt -Exclude build,.git | Select-String -Pattern "\xC3\x83\xC3\x82"
  ```

---

## 5. `Task_List.md` — updated this session

- `LiftStrategy`/`LiftTargeting` marked `[x]` — confirmed implemented, not a stub (was stale in the list).
- Note added: `SurpriseStrategy`/`SurpriseTargeting` also confirmed implemented, was previously untracked on the list entirely.
- `Consolidate duplicated suggestNextSection() logic` marked `[x]` — resolved via `SuggestionWorkflow`.
- New entry added: multi-intent scoring layer (`scoreIntent`/`rankIntents`/`suggestNextMulti`) marked `[x]` for implementation, with on-device verification and dialog wiring explicitly called out as still open.
- Regression-suite line updated to include `SurpriseStrategyTest.kt`, left as `[ ]` — strategies existing isn't the same as test coverage existing.

---

## 6. Commit & push — not yet done as of session end

Files touched this session, all confirmed BUILD SUCCESSFUL:

```powershell
git add feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/PairingEngine.kt
git add feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/PairingDecision.kt
git add feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/SuggestionWorkflow.kt
git add feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/ChordViewModel.kt
git add feature/feature-chords/src/main/kotlin/com/reasontouch/feature/chords/components/SuggestNextDialog.kt
git add feature/feature-pianoroll/src/main/kotlin/com/reasontouch/feature/pianoroll/PianoRollViewModel.kt
git add Task_List.md

git commit -m "Add multi-intent scoring layer to PairingEngine; consolidate suggestNextSection() via SuggestionWorkflow

- PairingEngine: scoreIntent()/rankIntents()/suggestNextMulti() rank all
  implemented intents (Resolve, Contrast, Continue, Lift, Surprise) by
  continuous plausibility instead of single hand-branched dispatch.
  EXPAND/SIMPLIFY/MODULATE excluded (no strategy, no musical gap).
  suggestNext() now wraps top-ranked result; old branch functions kept
  commented out for reference.
- PairingDecision.kt: removed broken PairingContext/scoreIntent stub;
  added IntentOption/PairingDecisionSet for ranked multi-intent output.
- New SuggestionWorkflow object (feature-chords) consolidates
  suggestNextSection()/suggestNextPhrases(), fixing a real bug where
  ChordViewModel hardcoded preferredLength = 4 while PianoRollViewModel
  correctly used suggestedBars -- same seed could produce different-length
  suggestions depending which screen triggered SUGGEST NEXT.
  ChordViewModel and PianoRollViewModel both now delegate to it.
- Restored SuggestNextDialog.kt after an accidental overwrite mid-session.

Build confirmed green. On-device verification of both SUGGEST NEXT flows
deferred to next session -- see ReasonTouch_Handoff_2026-08-18.md."

git push
```

(Adjust module paths in `git add` if your actual folder names differ from what's shown — confirmed this session as `feature-chords` and `feature-pianoroll`, not `feature\chords`/`feature\pianoroll`.)

---

## 7. Suggested order for next session

1. Retry the mojibake sweep with the working command (Section 4) — quick, cheap, closes out a loose end.
2. On-device verification per Section 3's test plan — the main event.
3. Resolve the RESOLVE/SURPRISE tie-break question with a real example in hand, if it comes up.
4. Decide next target: `SuggestNextDialog` multi-option UI (natural continuation of today's work) vs. `ChordSuggestionEngine.suggest()` candidate-cap fix (older, higher-priority per the 2026-08-01 handoff's own stated order) — worth a deliberate choice rather than defaulting to whichever comes up first.

---

*End of handoff document.*