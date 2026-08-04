# ReasonTouch -- Design Note: Choice at Multiple Granularities in SUGGEST

**Date:** 2026-08-03
**Status:** Design discussion, not yet scoped into implementation tasks
**Context:** Two related ideas raised in separate conversations (2026-08-01 and 2026-08-02/03), recognized here as two altitudes of the same underlying principle rather than separate features.

---

## 1. The shared principle

Driving design philosophy, as articulated directly: *"if we impose a decision on a user, we are effectively building the composition for them."* SUGGEST currently honors this principle at exactly one level -- variant choice (pick 1 of 3-4 generated candidates) -- but imposes a single decision at two other levels that sit above and below that one.

## 2. Three levels of choice, only one currently supported

| Level | What's being chosen | Currently supported? |
|---|---|---|
| **Intent** (which creative direction) | RESOLVE vs. SURPRISE vs. CONTRAST, etc. -- which `PairingDecision` to pursue at all | **No.** `PairingEngine.suggestNext()` returns exactly one `PairingDecision`; the user never sees the runner-up. See Section 3. |
| **Variant** (which realization of that intent) | "Direct Cadence" vs. "Alternate Voicing" vs. "Extended Approach" vs. "Contrasting Approach" | **Yes.** This is what SUGGEST's dialog already does today. |
| **Chord** (which candidate at one specific position within a chosen variant) | E.g. keeping bars 1, 2, 4 of a variant but swapping bar 3 for a different diatonic option | **No.** Whole-variant accept/reject only; no per-position editing exists once a variant is generated. See Section 4. |

## 3. Intent-level choice (originally: "multi-option PairingDecision")

Raised 2026-08-01, in the context of a deceptive cadence that could plausibly route to either SURPRISE (the current behavior) or RESOLVE (currently dead code specifically because of this). `PairingEngine.suggestNext()` computes a single winning `PairingDecision` and discards any competing read entirely -- there's no ranked list surfaced anywhere, unlike `IntentEngine.rank()` (confirmed dead code, zero callers) which *does* return a ranked `List<IntentRanking>` but was superseded before ever being wired in.

**Not yet scoped.** Would need `PairingDecision.kt`, `PairingType.kt`, and the SUGGEST dialog composable reviewed as a first step -- not yet done as of this writing.

## 4. Chord-level choice ("swap out individual chords")

Raised 2026-08-03, prompted directly by the word "suggest" in "Suggest Next" -- if the feature is a suggestion, the user should be able to accept or reject at finer grain than the whole block.

**Key finding: the interaction pattern already exists in the codebase**, just not connected to this use case. `ChordScreen.kt`'s `HarmonyPanel` (part of the pre-`ManualWorkspace` Chords-screen suggestion flow) already renders `state.suggestions: List<ChordSuggestion>` as a horizontally-scrolling row of individually-tappable chips -- chord label, Roman numeral, function, color-coded by TONIC/PREDOMINANT/DOMINANT function. Tapping a chip calls `onAudition` (plays it) then `onAddChord` (commits it). A second row does the identical thing for borrowed chords. This is precisely "browse several ranked alternatives, tap to commit one" -- the exact primitive chord-level swapping needs.

**What's missing is data plumbing, not UI design.** `ChordSuggestionEngine.suggest()` already computes the full ranked candidate pool at every position, every time a strategy calls it (further widened by the 2026-08-02 `FULL_DIATONIC_POOL` fix, which requests all 7 diatonic candidates rather than the curated top-4). The three phrase-generation strategies (`ContinueStrategy`/`ContrastStrategy`/`ResolveStrategy`) each pick one winning candidate per position via `pool[rankForThisVariant]` and discard the rest -- the alternatives don't survive past that single line. To support chord-level swapping, `GeneratedProgression` (or a richer type alongside it) would need to retain the full pool per position, not just the winning `TheoryChord`.

**Proposed shape, not yet built:**
- Extend whatever `SUGGEST`'s per-variant display shows so each bar/position is individually tappable (not just the whole variant card).
- Tapping a position opens (or inlines) a `HarmonyPanel`-style chip row, populated from that position's already-computed candidate pool rather than a fresh `ChordSuggestionEngine.suggest()` call -- reusing both the UI pattern and, where possible, the data already generated.
- Selecting a replacement chip updates just that position within the candidate `GeneratedProgression` before SELECT commits the whole (now user-edited) variant.

**Not yet scoped as implementation work** -- this document records the finding and a rough shape, not a task breakdown.

## 5. Why these are one design problem, not two

Both are instances of the same gap: **SUGGEST computes more information than it currently exposes, and discards the rest as soon as it picks a winner.** `PairingEngine` computes (implicitly, via its branching logic) more than one plausible decision but returns only the winner. Each strategy computes a full ranked pool per chord position but returns only the winner. In both cases, the fix is conceptually the same shape: stop discarding the alternatives early, and give the user a UI moment to choose among them instead of the engine choosing silently on their behalf.

Recommend treating these as two phases of one underlying "retain and surface alternatives, don't just pick winners silently" effort, rather than fully independent designs -- likely sharing some UI/data-flow patterns (e.g. however "browse ranked alternatives, tap to select" ends up implemented for chord-level swapping could plausibly generalize to intent-level choice too, since both are fundamentally "here are several ranked options, which do you want").

## 6. Relationship to other in-flight work

- Independent of the `ChordSuggestionEngine` candidate-cap fix (2026-08-02, already shipped) and the tonic-exhaustion-at-length-8 finding (2026-08-03) -- those are about the *correctness* of the candidate pool; this document is about *exposing* that pool to the user rather than just consuming it internally.
- Independent of the LIFT/EXPAND/SURPRISE strategy-completion work currently in progress -- those strategies should be built first without this, then could adopt whatever chord-level/intent-level choice mechanism eventually gets built, same as CONTINUE/CONTRAST/RESOLVE would.
- Loosely related to the Safe One-Time Write design (2026-08-01) -- that document is about auto-generation overwriting existing manual edits; this one is about giving the user more control *at* generation time so there's less need to manually fix things *after*. Different problems, complementary motivations.

---

## 7. Open questions, not yet resolved

1. Does intent-level choice show competing `PairingDecision`s as tabs/segmented control before drilling into variants, or some other layout?
2. For chord-level choice: does swapping one position re-run any downstream logic (e.g. does changing bar 3 affect whether bar 4's candidate pool should be recomputed, given strategies currently chain position-to-position via `currentChord`), or is each position's pool frozen at original-generation time regardless of edits to earlier positions?
3. Should chord-level swap UI live inline in the SUGGEST dialog itself, or open a secondary view (closer to how `HarmonyPanel` currently works as its own panel)?
4. Does either feature want its own regression test coverage pattern (e.g. asserting `GeneratedProgression` retains full pools, not just winners), and if so, should that be added now while `ResolveStrategyTest.kt`/`LiftStrategyTest.kt` etc. are being written, to avoid a second retrofit later?

---

*End of design note.*