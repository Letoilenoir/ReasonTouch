# ReasonTouch -- Strategy Test Log

Running record of on-device SUGGEST tests across ContinueStrategy, ContrastStrategy, and ResolveStrategy. Each entry: seed, predicted vs. actual, pass/fail. Full writeups/root-cause analysis for anything that surfaced a bug live in `docs/handoffs/` -- this log cross-references those rather than duplicating them.

Legend: PASS / FAIL / PENDING (not yet run) / PARTIAL (ran, but not fully traced)

---

## Coverage summary (updated as entries are added)

| Strategy | Branch | Status |
|---|---|---|
| Resolve | `endsOnDominant()` (HALF) | PASS -- verified twice (2026-07-31, 2026-08-01 repeat-fix retest) |
| Resolve | `defaultTrajectory` PREDOMINANT | PASS -- all 3 variants traced (2026-08-01) |
| Resolve | `defaultTrajectory` TONIC/DOMINANT | PENDING |
| Resolve | `CadenceType.DECEPTIVE` | BLOCKED -- PairingEngine routes to SURPRISE, never RESOLVE. See multi-option PairingDecision design item. |
| Resolve | `isClosed()` (`reopenThenResolveTrajectory`) | BLOCKED -- suggestAfterHighStability() has no RESOLVE path. Same blocker as above. |
| Contrast | TONIC-ending trajectory | PASS -- full 8-bar trace, all 3 variants (2026-08-01) |
| Contrast | PREDOMINANT/DOMINANT-ending trajectory | PENDING |
| Contrast | LIFT branch (tension > 0.6f) | PENDING |
| Contrast | SIMPLIFY dead-branch check | PENDING |
| Contrast | Minor-key borrowed labels | PASS (incidental) -- see 2026-08-02 entries; G/D/Am-type seeds confirmed borrowed V/IV/I labels, not bVII/iv/bVI, consistent with minor-key handling |
| Continue | Repeated-chord exclusion fix | PASS (partial) -- confirmed on `G C D Am` seed (2026-08-02); all variants ended in repeated-G tail, consistent with the same known ChordSuggestionEngine cap limitation seen in Resolve/Contrast, not a fix failure |
| Continue | Full position-by-position trace at preferredLength=8 | NOT ATTEMPTED -- see notes below |

---

## 2026-08-02 -- Continue exclusion-fix verification (via Piano Roll SUGGEST)

**Goal:** confirm the same-continuation chord exclusion fix (commit `6ace490`) works for `ContinueStrategy`, following the same verification already done for Resolve and Contrast.

**Important process note discovered mid-session:** all C-major/A-minor-diatonic seeds tested kept landing on CONTRAST or RESOLVE rather than CONTINUE. Root cause traced to `KeyDetector` consistently resolving ambiguous seeds (any progression built purely from the 7 chords shared between C major and A minor) to C major, combined with `PairingEngine`'s stability thresholds -- tonic-heavy endings push into the high-stability/CONTRAST bucket, dominant endings push into low-stability/RESOLVE, leaving only a narrow medium-stability band for CONTINUE. Several seeds were tried before finding one that reliably lands on CONTINUE.

| # | Seed | Detected key (inferred from output) | SUGGEST type (actual) | Notes |
|---|---|---|---|---|
| 1 | `C - Am - F` | C major (or A minor -- ambiguous) | CONTRAST | High stability, tonic-heavy ending. Not a CONTINUE case. |
| 2 | `C - Em - Dm` | C major (or A minor -- ambiguous) | CONTRAST | Same as above; hand-math predicted medium stability but actual result was CONTRAST -- discrepancy not resolved, possibly