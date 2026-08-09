# ReasonTouch -- Task List (Living Document)

Update this file directly as tasks are completed, added, or reprioritized -- this is the single source of truth for "what's next," rather than reconstructing it from conversation history each session.

**Last updated:** 2026-08-03

---
## URGENT -- exhaustion-rotation bug affecting all 4 strategies at preferredLength=8

Found and root-caused 2026-08-04 while building LiftStrategy. Full detail: `docs/handoffs/ReasonTouch_Handoff_2026-08-04_LiftStrategy_ExhaustionFix.md`.

Once a harmonic function's candidate pool exhausts within a single continuation (normal at preferredLength=8, e.g. Contrast/Lift's hardcoded 8-bar suggestions), the shared exclusion/fallback pattern freezes onto one identical chord for every remaining position, instead of rotating through the exhausted pool. Fixed in `LiftStrategy.kt` (verified via full hand-trace against instrumented output). **Not yet applied to `ContinueStrategy.kt`, `ContrastStrategy.kt`, `ResolveStrategy.kt`** -- same code shape, same fix needed.

- [x] Fix confirmed in `LiftStrategy.kt`
- [ ] Apply identical fix to `ContinueStrategy.kt`
- [ ] Apply identical fix to `ContrastStrategy.kt`
- [ ] Apply identical fix to `ResolveStrategy.kt`
- [ ] Re-run harness seeds for all three post-fix, confirm no regressions (known-affected seed for Continue: `G C D Am` at preferredLength=8)
- [ ] Strip all `*_TRACE` temporary debug logging from all 4 strategy files once rollout confirmed
- [ ] Update Strategy_Test_Log.md coverage summary once all 4 confirmed

## URGENT -- live empty-suggestion bug affecting 3 CompositionIntents

Confirmed 2026-08-02 via full-project search: `PairingEngine` can and does return `LIFT`, `EXPAND`, and `SURPRISE` as live `PairingType` decisions today (LIFT: tension > 0.6f in high-stability branch; EXPAND: HALF-cadence and default cases in medium-stability branch; SURPRISE: DECEPTIVE cadence in medium-stability branch). But `ProgressionGenerator`'s dispatch only implements CONTINUE/RESOLVE/LIFT-stub/CONTRAST -- LIFT, EXPAND, and SURPRISE all fall through to `else -> emptyList()`.

**Effect:** when a real progression triggers one of these three decisions, SUGGEST shows the rationale text (e.g. "Deceptive cadence -> unexpected turn") but "Suggested continuations" comes back empty -- the exact same symptom as the original 2026-07-25 "Could not detect key" bug, just for these three decision types specifically. This is not a roadmap nice-to-have; it is a live, user-visible bug in the current build.

**Reachability table (2026-08-02):**

| PairingType | Reachable via live PairingEngine? | Has a ProgressionGenerator strategy? | Status |
|---|---|---|---|
| CONTINUE | Yes | Yes | Working |
| CONTRAST | Yes | Yes | Working |
| RESOLVE | Partially (HALF/PREDOMINANT endings only) | Yes | Working where reachable |
| **LIFT** | **Yes** (tension > 0.6f) | **No -- TODO stub** | **BROKEN -- empty suggestions when hit** |
| **EXPAND** | **Yes** (HALF-cadence, medium-stability default) | **No** | **BROKEN -- empty suggestions when hit** |
| **SURPRISE** | **Yes** (DECEPTIVE cadence) | **No** | **BROKEN -- empty suggestions when hit** |
| SIMPLIFY | No (dead branch -- `history` always passed empty) | No | Not reachable either way; lower priority |
| MODULATE | Not seen dispatched by PairingEngine directly | No (`ChordViewModel`/`PianoRollViewModel` map it to `CompositionIntent.DEVELOP` as a placeholder -- "closest existing intent; no direct equivalent yet") | Not reachable either way; lower priority |

**Scale note:** each of LIFT/EXPAND/SURPRISE needs the same treatment RESOLVE got -- its own `Targeting.kt` + `Strategy.kt`, `ProgressionGenerator` dispatch wiring, and full on-device hand-traced verification. RESOLVE took a full session; realistically this is 2-3 sessions' worth of work across all three, not a quick pass.

- [ ] **Build `LiftStrategy`/`LiftTargeting`** -- start here; already has a `TODO` stub waiting in `ProgressionGenerator`, and is reachable via live SUGGEST today so it can be verified end-to-end immediately once built. Use `ContinueStrategy.kt`/`ContinueTargeting.kt` as the template, same as ResolveStrategy's build process.
- [ ] **Build `ExpandStrategy`/`ExpandTargeting`** -- same pattern, same urgency.
- [ ] **Build `SurpriseStrategy`/`SurpriseTargeting`** -- same pattern, same urgency. Note: SURPRISE and RESOLVE's dead DECEPTIVE branch are closely related (see multi-option `PairingDecision` design item below) -- worth being aware both exist for the same cadence type when scoping this one.
- [ ] Small housekeeping: `MODULATE -> CompositionIntent.DEVELOP` mapping is an acknowledged placeholder (per inline comment in both ViewModels) -- decide whether MODULATE deserves its own real `CompositionIntent`/strategy eventually, or whether DEVELOP is intentionally being repurposed to cover it long-term.

## Verification / lower-priority items (deprioritized behind the above)

- [ ] **Remaining Contrast test matrix items** -- LIFT branch (tension > 0.6f should route to LIFT not CONTRAST -- note this branch currently returns empty suggestions, see above), SIMPLIFY dead-branch check (should be unreachable since `history` is always passed empty to `PairingEngine.suggestNext()`), minor-key borrowed labels (informally confirmed already via 2026-08-02 Continue-hunt seeds, worth a dedicated clean test)
- [ ] **Resolve's untested `defaultTrajectory` TONIC/DOMINANT branches** -- low priority, same proven mechanism as verified PREDOMINANT branch
- [ ] **Re-verify repeated-chord/tail-repeat behavior across all 3 existing strategies post-cap-fix** -- the `ChordSuggestionEngine` cap fix (FULL_DIATONIC_POOL, applied 2026-08-02) should reduce or eliminate the `C C C C` / `Am Am Am Am` / `G G G G` tail-repeat pattern seen in every strategy so far. Not yet retested on-device since the fix landed.
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

- [x] ~~`KeyDetector.kt` tie-break behavior~~ -- **RESOLVED 2026-08-03, DISPROVEN.** Confirmed via headless `GenerationTestHarness` run (see `docs/testing/Strategy_Test_Log.md`) that `KeyDetector` does not default to C major -- it correctly found 5 different keys (F major, D minor, A minor, E minor, C major, F major, G major) across the 7 investigation seeds. Both previously-unresolved prediction mismatches (`C Em Dm`, `C G F`) traced exactly once the actual detected key was used instead of an assumed one. No bug; `ProgressionAnalyzer`/`PairingEngine` confirmed behaving correctly throughout.

- [ ] **NEW (2026-08-03): tonic-function exhaustion at `preferredLength = 8`.** Distinct from the already-fixed `ChordSuggestionEngine` cap bug. Any diatonic key has exactly 3 tonic-function chords (I/iii/vi in major, i/III/VI in minor). An 8-bar trajectory whose padded back-half repeatedly targets the same harmonic function (the normal shape of `ContinueTargeting`/`ContrastTargeting`'s `fitLength` padding) can exhaust all 3 real candidates and then have nothing left to exclude to -- confirmed on `G C D Am` (G major) at length 8: `D G C Bm Em G G G`, positions 5-7 forced to repeat G after Bm and Em were already used. No `ChordSuggestionEngine` change can fix this -- the pool is genuinely exhausted, not artificially capped. Needs its own design decision: accept repetition beyond 3 same-function chords as an inherent diatonic limit, vary voicing/octave for forced repeats, or mix in borrowed chords once diatonic options for a function run out (precedent: Contrast already does this deliberately at position 0).
- [ ] **`preferredLength` divergence between `ChordViewModel` and `PianoRollViewModel`** -- confirmed 2026-08-02, not yet actioned. `ChordViewModel` always requests 4; `PianoRollViewModel` requests `pairingDecision.suggestedBars` (4 or 8). Same seed + same `PairingDecision` can produce different-length output depending which screen triggered SUGGEST. See also "Consolidate duplicated `suggestNextSection()` logic" under Medium-scope implementation work, above -- likely the same fix addresses both.

## Housekeeping

- [ ] `docs/uml/` and architecture docs -- separate work-in-progress from another session thread, not yet folded into anything above; worth checking status.

---

## Completed (recent, for reference -- full detail in `docs/handoffs/`)

- [x] ResolveStrategy/ResolveTargeting implemented, verified, committed (`ae46fe8`)
- [x] Same-continuation chord exclusion fix across Continue/Contrast/Resolve (`6ace490`)
- [x] `ChordSuggestionEngine` candidate-cap fix -- `FULL_DIATONIC_POOL` (7) passed by all 3 phrase-generation strategies, `ChordViewModel`'s manual suggestion UI untouched (default `MAX_SUGGESTIONS` = 4 preserved) -- 2026-08-02, not yet retested on-device
- [x] `docs/handoffs/`, `docs/testing/`, `docs/design/` folder structure established, full project history consolidated