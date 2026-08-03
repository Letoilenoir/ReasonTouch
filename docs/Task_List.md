# ReasonTouch -- Task List (Living Document)

Update this file directly as tasks are completed, added, or reprioritized -- this is the single source of truth for "what's next," rather than reconstructing it from conversation history each session.

**Last updated:** 2026-08-02

---

## Immediate / next up

- [ ] **Remaining Contrast test matrix items** -- LIFT branch (tension > 0.6f should route to LIFT not CONTRAST), SIMPLIFY dead-branch check (should be unreachable since `history` is always passed empty to `PairingEngine.suggestNext()`), minor-key borrowed labels (informally confirmed already via 2026-08-02 Continue-hunt seeds, worth a dedicated clean test)
- [ ] **Resolve's untested `defaultTrajectory` TONIC/DOMINANT branches** -- low priority, same proven mechanism as verified PREDOMINANT branch
- [ ] **Re-verify repeated-chord/tail-repeat behavior across all 3 strategies post-cap-fix** -- the `ChordSuggestionEngine` cap fix (FULL_DIATONIC_POOL, applied 2026-08-02) should reduce or eliminate the `C C C C` / `Am Am Am Am` / `G G G G` tail-repeat pattern seen in every strategy so far. Not yet retested on-device since the fix landed.

## Larger, properly-scoped design work

- [ ] **Multi-option `PairingDecision` design** -- let SUGGEST offer more than one ranked intent (e.g. RESOLVE vs. SURPRISE for an ambiguous deceptive cadence) rather than the engine always picking one winner. Motivated by the "don't build the composition for the user" design philosophy. Needs `PairingDecision.kt`, `PairingType.kt`, and the SUGGEST dialog composable reviewed before design starts. Unlocks two currently-dead `ResolveTargeting` branches (DECEPTIVE routing, `isClosed()`/high-stability RESOLVE routing).
- [ ] **Mood as a persistent generative motif, not just a seed-time bias** -- see `docs/design/MoodWorkspace_Mechanism_Findings.md` for the confirmed current mechanism. Currently `moodBias`/`moodBiasFine` (a single -1..+1 float, `harmonyBias`) only fires at initial seed generation and has no representation in `ProgressionAnalysis`/`ProgressionGenerationRequest`/the strategies -- SUGGEST has no way to know which mood a progression was seeded with, so it cannot continue it as a motif. Two separable problems: (1) persistence -- does the value even need remembering, and where; (2) expressiveness -- one scalar cannot distinguish all 6 named mood presets from each other (e.g. Cinematic 0.3 vs. Energetic 0.6 differ only by position on one axis, despite very different creative intents ("epic, dramatic" vs. "fast, driving")). Needs `harmonyBias`'s consumer identified before scoping further.
- [ ] **Safe one-time write mechanism** -- see `docs/design/Safe_Write_Design_2026-08-01.md`. Needed before strum-speed review-stage re-editing can be built safely; later reusable for bass-first harmonisation.
- [ ] **Strum speed field on `ChordEvent`** -- schema + migration + workspace UI control (Manual, Mood, Inspire, Progression all currently missing this; `strumPatternId` exists but not `strumSpeed`). Can proceed independently of the safe-write mechanism -- composition-time strum speed doesn't need write-safety logic, only review-time re-editing does.

## Medium-scope implementation work

- [ ] **`LiftStrategy`/`LiftTargeting`** -- still a `TODO: emptyList()` stub in `ProgressionGenerator`. Same CONTINUE-template pattern as Resolve/Contrast.
- [ ] **`ResolveStrategyTest.kt` / `LiftStrategyTest.kt` regression suite** -- following `ContinueStrategyTest.kt`'s precedent (10 tests, confirmed passing).
- [ ] **Consolidate duplicated `suggestNextSection()` logic** -- confirmed 2026-08-02 that `ChordViewModel` and `PianoRollViewModel` have separate copies with a real behavioral divergence: `ChordViewModel` always uses `preferredLength = 4`; `PianoRollViewModel` uses `pairingDecision.suggestedBars` (4 or 8 depending on branch). Same seed + same `PairingDecision` can produce different-length suggestions depending which screen triggered SUGGEST.

## Longer-horizon roadmap (from earlier sessions)

- [ ] Strum pattern support for `addPhrase()`-inserted (SUGGEST-generated) notes -- currently block-mode only
- [ ] `EXPAND`, `MODULATE`, `SIMPLIFY` strategies -- remaining `ProgressionGenerator` stubs beyond Resolve/Lift
- [ ] Bass-first harmonisation -- draw a bassline, infer chord candidates from it. Now has a scoped write-safety approach to lean on (see Safe Write design doc).
- [ ] Tray-driven forward composition -- contextual harmonic extension from the velocity tray repurposed as chord insertion, for full forward composition within Piano Roll
- [ ] Session Settings consolidation -- Chords module settings, bass instrument picker, key transposition; Mode setting to be removed (Harmony Engine now owns tonality)
- [ ] MIDI import -- the one major feature still just "discussed, not started" since the earliest design doc

## Investigation / open questions (not yet actioned)

- [ ] `KeyDetector.kt` tie-break behavior -- informally observed defaulting to C major for ambiguous C-major/A-minor-diatonic seeds (5 of 7 test seeds on 2026-08-02). Not confirmed by reading source directly. Two unresolved prediction misses (`C Em Dm`, `C G F` both predicted CONTINUE via hand-calculated stability, both actually returned CONTRAST) -- see `docs/testing/Strategy_Test_Log.md`, 2026-08-02 entry, "Open questions."

## Housekeeping

- [ ] `docs/uml/` and architecture docs -- separate work-in-progress from another session thread, not yet folded into anything above; worth checking status.

---

## Completed (recent, for reference -- full detail in `docs/handoffs/`)

- [x] ResolveStrategy/ResolveTargeting implemented, verified, committed (`ae46fe8`)
- [x] Same-continuation chord exclusion fix across Continue/Contrast/Resolve (`6ace490`)
- [x] `ChordSuggestionEngine` candidate-cap fix -- `FULL_DIATONIC_POOL` (7) passed by all 3 phrase-generation strategies, `ChordViewModel`'s manual suggestion UI untouched (default `MAX_SUGGESTIONS` = 4 preserved) -- 2026-08-02, not yet retested on-device
- [x] `docs/handoffs/`, `docs/testing/`, `docs/design/` folder structure established, full project history consolidated