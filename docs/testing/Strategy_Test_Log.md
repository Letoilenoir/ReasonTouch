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
| Contrast | Minor-key borrowed labels | PENDING |
| Continue | `defaultTrajectory`, all branches | PENDING -- see 2026-08-02 entries below |
| Continue | Repeated-chord exclusion fix | PENDING -- no before/after baseline exists for Continue, unlike Resolve/Contrast |

---

## 2026-08-02 -- Continue exclusion-fix first pass

**Goal:** confirm the same-continuation chord exclusion fix (commit `6ace490`) works for `ContinueStrategy`, following the same verification already done for Resolve and Contrast. No prior on-device baseline exists for Continue specifically, so this is a first confirmation pass, not a before/after comparison.

| # | Seed (C major) | Predicted SUGGEST type | Predicted trajectory (pos 1-3) | Actual type | Actual variants | Pass/Fail |
|---|---|---|---|---|---|---|
| 1 | `C - Am - F` (3 bars) | CONTINUE, "Short phrase + medium stability" | DOMINANT -> TONIC -> PREDOMINANT | | | PENDING |
| 2 | `C - Em - Dm` (3 bars) | CONTINUE (medium stability, INTERRUPTED cadence) | DOMINANT -> TONIC -> PREDOMINANT | | | PENDING |
| 3 | `Dm - Em - Am` (3 bars) | CONTINUE (medium stability, ending TONIC-function) | PREDOMINANT -> DOMINANT -> TONIC | | | PENDING |

**What to check per seed:** SUGGEST returns predicted type (if not, note actual type + rationale for review); all 4 variants show no repeated chord within a single continuation.

---

*Add new dated sections above this line as further matrices are run.*