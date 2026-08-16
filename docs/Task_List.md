# ReasonTouch -- Task List (Living Document)

Update this file directly as tasks are completed, added, or reprioritized -- this is the single source of truth for "what's next," rather than reconstructing it from conversation history each session.

**Last updated:** 2026-08-03

---
## RESOLVED -- exhaustion-rotation bug (was: URGENT, affecting all 4 strategies at preferredLength=8)

Found and root-caused 2026-08-04 while building LiftStrategy. Fixed, centralized into a new shared `ChordCandidateSelector.kt`, and rolled out to all 4 strategies. Full detail: `docs/handoffs/ReasonTouch_Handoff_2026-08-04_LiftStrategy_ExhaustionFix.md` and `docs/testing/Strategy_Test_Log.md`'s 2026-08-04 entries.

- [x] Fix confirmed in `LiftStrategy.kt` (full hand-trace)
- [x] Logic extracted to shared `ChordCandidateSelector.kt`
- [x] `LiftStrategy.kt` retrofitted, re-confirmed byte-for-byte identical output
- [x] `ContinueStrategy.kt` retrofitted, confirmed via `G C D Am` @ preferredLength=8
- [x] `ContrastStrategy.kt` retrofitted, confirmed via `Am F G C` @ preferredLength=8
- [x] `ResolveStrategy.kt` retrofitted, confirmed via `C Am F G` @ preferredLength=8
- [ ] Strip stale inline exclusion-logic comments from Continue/Contrast/Resolve (now describes centralized logic, not what's actually inline)
- [ ] Consider a dedicated `ChordCandidateSelectorTest.kt` regression suite, given how much manual re-verification went into confirming this fix
- [ ] `*_TRACE` temporary debug logging already removed during the `ChordCandidateSelector` retrofit -- no cleanup needed here

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

- [x] ~~Build `LiftStrategy`/`LiftTargeting`~~ -- **DONE 2026-08-04.** Trajectory logic and exhaustion-rotation fix both fully confirmed. See handoff doc.
- [x] ~~Build `ExpandStrategy`/`ExpandTargeting`~~ -- **DONE 2026-08-04, PARTIALLY VERIFIED.** Strategy logic (3-function cycle) and exhaustion-rotation fix both fully confirmed via forced-intent seeds. Live PairingEngine routing: HALF-cadence branch **proven mathematically dead** (same category as SIMPLIFY/RESOLVE-DECEPTIVE, see below). Medium-stability-default branch **not yet confirmed reachable** -- 9 seed attempts across the session all missed, either falling outside [0.4,0.7] or hitting the barCount<=4 sibling branch instead. Full attempt log and root-cause analysis in `docs/testing/Strategy_Test_Log.md`'s 2026-08-04 "ExpandStrategy built and verified" entry, including a concrete next-attempt seed idea (`Dm Edim Am Dm Gm`) not yet tried.
- [ ] Find and confirm a genuine medium-stability-default EXPAND routing seed (see test log for next-attempt idea and root-cause analysis of why 9 prior attempts missed)
- [x] ~~Build `SurpriseStrategy`/`SurpriseTargeting`~~ -- **DONE 2026-08-04, FULLY VERIFIED.** Both generation logic and live routing confirmed via `C C F G Am`. See `docs/testing/Strategy_Test_Log.md` for the corrected KeyDetector-aware seed-construction methodology established while closing this out.
    - [ ] Find and confirm a genuine DECEPTIVE-cadence SURPRISE routing seed (see test log for KeyDetector mechanism and next-attempt idea using degree-ii root difference between C major/A minor)
- [ ] Small housekeeping: `MODULATE -> CompositionIntent.DEVELOP` mapping is an acknowledged placeholder (per inline comment in both ViewModels) -- decide whether MODULATE deserves its own real `CompositionIntent`/strategy eventually, or whether DEVELOP is intentionally being repurposed to cover it long-term.

## Verification / lower-priority items (deprioritized behind the above)

- [ ] **Remaining Contrast test matrix items** -- LIFT branch (tension > 0.6f should route to LIFT not CONTRAST -- note this branch currently returns empty suggestions, see above), SIMPLIFY dead-branch check (should be unreachable since `history` is always passed empty to `PairingEngine.suggestNext()`), minor-key borrowed labels (informally confirmed already via 2026-08-02 Continue-hunt seeds, worth a dedicated clean test)
- [ ] **Resolve's untested `defaultTrajectory` TONIC/DOMINANT branches** -- low priority, same proven mechanism as verified PREDOMINANT branch
- [ ] **Re-verify repeated-chord/tail-repeat behavior across all 3 existing strategies post-cap-fix** -- the `ChordSuggestionEngine` cap fix (FULL_DIATONIC_POOL, applied 2026-08-02) should reduce or eliminate the `C C C C` / `Am Am Am Am` / `G G G G` tail-repeat pattern seen in every strategy so far. Not yet retested on-device since the fix landed.
## Larger, properly-scoped design work

- [ ] **KeyDetector's `lastChordBonus` structurally conflicts with deceptive-cadence seed construction** -- found 2026-08-04 while hunting a SURPRISE routing seed. Any progression ending on scale-degree-vi (required for a deceptive cadence, by definition) tends to make the relative minor's own tonic-ending bonus outscore the intended major key, since vi in a major key IS i in its relative minor. Not necessarily a bug (KeyDetector's scoring logic seems reasonable in isolation), but worth being aware this makes certain classes of progression genuinely hard to seed-test, and may be worth a dedicated look if it turns out to affect real user progressions, not just synthetic test seeds.
## NEXT PRIORITY -- PairingEngine multi-option refactor

**Status as of 2026-08-04: all 6 strategy implementations (Continue/Contrast/Resolve/Lift/Expand/Surprise) are confirmed correct at the generation-logic level.** Every remaining reachability gap (RESOLVE's DECEPTIVE/isClosed() branches, EXPAND's medium-default branch being hard to isolate, and the general "only one PairingDecision is ever returned" limitation) is a `PairingEngine` architecture problem, not a strategy problem. This is now the critical path -- see `docs/design/Choice_Granularity_Design_2026-08-03.md` for the full design rationale (sections 3, 7).

****External confirmation this is the right next step:** the Piano Roll Composition Tray UX design (`docs/design/Piano Roll Composition UI/UX Direction.md`) explicitly sequences "Complete SURPRISE" as the current priority,ternal confirmation this is the right next step:** the Piano Roll Composition Tray UX design (received 2026-08-04, not yet filed under docs/design -- see below) explicitly sequences "Complete SURPRISE" as the current priority, then builds the tray's core "Suggest Next" flow around a two-stage "choose strategy, then choose phrase" interaction (design doc section 7). That flow cannot be built against a `PairingEngine` that only ever returns one strategy -- the tray needs a ranked list of viable strategies to present as the first-stage choice. **The multi-option PairingEngine refactor is a hard blocking dependency for the Composition Tray design, not just a nice-to-have improvement.**

**Scope, per the "always offer viable options, rank but don't hide" principle established 2026-08-04:**
- `PairingEngine.suggestNext()` should return a ranked list of viable `PairingDecision`s (or similar), not a single winner. The long-dead `IntentEngine.rank()` already returns `List<IntentRanking>` sorted by confidence -- worth checking whether its shape/pattern can be reused or adapted rather than designed from scratch, even though the object itself is unused dead code.
- The existing `when` branches in `suggestAfterHighStability()`/`suggestAfterMediumStability()`/`suggestAfterLowStability()` already encode an implicit priority order -- this ordering logic likely generalizes into "rank these N viable options" rather than "pick 1 and discard the rest."
- Both `ChordViewModel` and `PianoRollViewModel`'s `suggestNextSection()` (duplicated logic, already flagged as a separate task below) will need updating to consume a list rather than a single decision.
- The SUGGEST dialog UI (or its Composition Tray successor, per the new UX design) needs to actually present the ranked list -- not in scope for the PairingEngine refactor itself, but the refactor should be designed with that consumer in mind from the start.

**Not in scope for this refactor:** the EXPAND HALF-cadence/RESOLVE conflict (Option C fix, still separately pending) and the actual Composition Tray UI build are both downstream of this work, not part of it.

- [ ] Review `PairingDecision.kt`, `PairingType.kt`, `IntentEngine.kt` (dead code, but potentially reusable shape) before designing
- [ ] Design the new `PairingEngine.suggestNext()` return shape (ranked list, confidence per option, etc.)
- [ ] Refactor `suggestAfterHighStability()`/`suggestAfterMediumStability()`/`suggestAfterLowStability()` to build a ranked list instead of an early-return single decision
- [ ] Update both `ChordViewModel` and `PianoRollViewModel`'s `suggestNextSection()` call sites
- [ ] Re-verify all 6 strategies' reachability against the new multi-option routing (should newly unlock RESOLVE's DECEPTIVE/isClosed() branches and EXPAND's HALF-cadence branch, in addition to whatever else the ranking surfaces)
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
- - [ ] **Diatonic chord-quality variation as an exploration lever** -- `ChordQuality` enum already defines DOM7/MAJ7/MIN7/SUS2/SUS4/AUG, and `MusicTheory.kt` already fully handles all of them (midiNotes generation, label rendering, parseChordName round-trip) -- but `MAJOR_QUALITIES`/`MINOR_QUALITIES` only ever assign MAJ/MIN/DIM to diatonic degrees, so no generated suggestion ever uses the richer qualities. Raised while scoping ExpandStrategy: "explore harmonic territory" currently only means visiting different roots within a fixed 3-quality palette, not exploring chord color at all -- would primarily benefit Expand, but the enum/rendering work already exists and is shared infrastructure.
- **De-risking check completed 2026-08-04:** confirmed via full-project search that no downstream consumer (MIDI export -- `MultiTrackMidiWriter.kt`/`MidiFileWriter.kt`, Piano Roll rendering/playback, audition/SynthEngine) branches on `ChordQuality` or hardcodes an assumed chord size (searched for `notes.size == 3`/`== 4`, zero hits). All consumers work generically off `midiNotes: List<Int>`. This means adding quality variation should be additive at the `ChordSuggestionEngine`/diatonic-generation layer only -- unlikely to require touching rendering/export/playback. Not a 100% guarantee (search finds textual references, not runtime behavior, and something UI-layout-specific could still assume 3-note voicings visually rather than in code), but no code-level red flags found.
- Real scope if pursued: deciding which degrees get quality variants and under what conditions (e.g. V7 is far more idiomatic than randomly assigning 7ths to any degree), and giving `ChordSuggestionEngine` a quality dimension in its candidate weighting, not just root+function.

## Investigation / open questions (not yet actioned)

- [x] ~~`KeyDetector.kt` tie-break behavior~~ -- **RESOLVED 2026-08-03, DISPROVEN.** Confirmed via headless `GenerationTestHarness` run (see `docs/testing/Strategy_Test_Log.md`) that `KeyDetector` does not default to C major -- it correctly found 5 different keys (F major, D minor, A minor, E minor, C major, F major, G major) across the 7 investigation seeds. Both previously-unresolved prediction mismatches (`C Em Dm`, `C G F`) traced exactly once the actual detected key was used instead of an assumed one. No bug; `ProgressionAnalyzer`/`PairingEngine` confirmed behaving correctly throughout.

- [ ] **NEW (2026-08-03): tonic-function exhaustion at `preferredLength = 8`.** Distinct from the already-fixed `ChordSuggestionEngine` cap bug. Any diatonic key has exactly 3 tonic-function chords (I/iii/vi in major, i/III/VI in minor). An 8-bar trajectory whose padded back-half repeatedly targets the same harmonic function (the normal shape of `ContinueTargeting`/`ContrastTargeting`'s `fitLength` padding) can exhaust all 3 real candidates and then have nothing left to exclude to -- confirmed on `G C D Am` (G major) at length 8: `D G C Bm Em G G G`, positions 5-7 forced to repeat G after Bm and Em were already used. No `ChordSuggestionEngine` change can fix this -- the pool is genuinely exhausted, not artificially capped. Needs its own design decision: accept repetition beyond 3 same-function chords as an inherent diatonic limit, vary voicing/octave for forced repeats, or mix in borrowed chords once diatonic options for a function run out (precedent: Contrast already does this deliberately at position 0).
- [ ] **`preferredLength` divergence between `ChordViewModel` and `PianoRollViewModel`** -- confirmed 2026-08-02, not yet actioned. `ChordViewModel` always requests 4; `PianoRollViewModel` requests `pairingDecision.suggestedBars` (4 or 8). Same seed + same `PairingDecision` can produce different-length output depending which screen triggered SUGGEST. See also "Consolidate duplicated `suggestNextSection()` logic" under Medium-scope implementation work, above -- likely the same fix addresses both.
- [x] **PairingEngine stability threshold gap: [0.7, 0.8) silently routes to low-stability logic.** Confirmed 2026-08-04 via real seed (`C Am Dm Em F`, F major, stability=0.78): `stability >= 0.8f` and `stability in 0.4f..0.7f` leave a gap at [0.7, 0.8) that falls through to the `else` (low-stability) branch, despite being much closer to high-stability territory. Not yet fixed -- likely fix is widening the medium check to `stability < 0.8f` (i.e. `>= 0.4f && < 0.8f`), but not yet decided or applied. Affects routing for ALL intents, not just EXPAND -- discovered while investigating EXPAND seed behavior but is a PairingEngine-wide issue.
- [x] **EXPAND's HALF-cadence branch is provably unreachable via live PairingEngine.** Mathematically proven, not just empirically observed (2026-08-04): functionStability(DOMINANT)=0.15 and HALF cadence's cadenceBonus=-0.10 are both fixed whenever a progression ends on the dominant (detectCadence() guarantees degree-5 endings always classify as HALF). Maximum possible stability via functionDistributionBonus alone: 0.15-0.10+0.15=0.20 ceiling, far below the 0.4 medium-stability floor. Same category as SIMPLIFY and RESOLVE's DECEPTIVE branch (docs/handoffs -- multi-option PairingDecision design item). EXPAND is only reachable via the medium-stability default branch, never the "Ends on V -> explore harmonically" HALF-cadence one.
## Housekeeping

- [ ] `docs/uml/` and architecture docs -- separate work-in-progress from another session thread, not yet folded into anything above; worth checking status.
- - [x] ~~File the Piano Roll Composition Tray UX/UI design brief~~ -- **Already filed.** Confirmed 2026-08-04 at `docs/design/Piano Roll Composition UI/UX Direction.md`. Covers the Home -> Piano Roll -> Composition Tray navigation model, the two-stage Suggest Next flow (strategy then phrase), Bass/Drums as arrangement-layer tray entries distinct from harmony strategies, and sequencing (complete SURPRISE and PairingEngine multi-option work first, then build the tray, then the navigation refactor removing the legacy CHORDS|ARRANGE|DRUMS row).

## Completed (recent, for reference -- full detail in `docs/handoffs/`)

- [x] ResolveStrategy/ResolveTargeting implemented, verified, committed (`ae46fe8`)
- [x] Same-continuation chord exclusion fix across Continue/Contrast/Resolve (`6ace490`)
- [x] `ChordSuggestionEngine` candidate-cap fix -- `FULL_DIATONIC_POOL` (7) passed by all 3 phrase-generation strategies, `ChordViewModel`'s manual suggestion UI untouched (default `MAX_SUGGESTIONS` = 4 preserved) -- 2026-08-02, not yet retested on-device
- [x] `docs/handoffs/`, `docs/testing/`, `docs/design/` folder structure established, full project history consolidated