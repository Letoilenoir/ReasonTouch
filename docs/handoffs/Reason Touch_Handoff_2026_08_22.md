# ReasonTouch — Handoff, 2026-08-22

**Branch:** `master`
**Status:** Composition Tray implemented and verified on-device on Piano Roll. Manual's
duplicate Suggest Next entry point identified as redundant and slated for removal (not yet
done). Ready to commit.

---

## 1. What shipped this session

The Composition Tray described in `ReasonTouch_Piano_Roll_Composition_UX_UI_Direction.md`
(Sections 5–7) has been implemented end-to-end and now replaces `VelocityOverlayTray` as the
persistent bottom-anchored control surface in Piano Roll. This is the first concrete
implementation step following that design doc's Section 19 sequencing ("build the Composition
Tray around the existing Suggest Next flow").

### New files

| File | Purpose |
|---|---|
| `feature/feature-chords/.../components/SuggestNextFlow.kt` | The strategy-picker → phrase-list Suggest Next interaction, extracted from `SuggestNextDialog.kt` so it can be hosted in either a `Dialog` (legacy) or the Composition Tray. Two-level flow: vertical list of ranked strategy buttons, tap through to that strategy's phrase cards. Supports `pinHeader` mode (see below). |
| `feature/feature-pianoroll/.../CompositionTray.kt` | The persistent tray itself. Pill-header expand/collapse (replacing `VelocityOverlayTray`'s interaction pattern exactly), `TraySection` enum (`HOME`/`SUGGEST_NEXT`/`BASS`/`DRUMS`), `TrayHome` compact composition-state surface, `TrayStubPanel` placeholders for Bass/Drums. |

### Modified files

| File | Change |
|---|---|
| `SuggestNextDialog.kt` | Reduced to a thin `Dialog` wrapper around `SuggestNextFlow` (`pinHeader = false`). No behavioural change from the pre-session version. |
| `PianoRollScreen.kt` | `VelocityOverlayTray` call site replaced with `CompositionTray`. Toolbar SUGGEST chip now opens the tray directly into `SUGGEST_NEXT` rather than a modal dialog. State hoisted (`trayExpanded`, `traySection`) so both the pill tap and the toolbar chip drive the same tray instance. |

### Deleted / superseded

- `VelocityOverlayTray.kt` — superseded by `CompositionTray.kt`, which reuses its exact
  pill-header expand/collapse mechanics. **Velocity editing UI is not currently reachable** — see
  Section 4 below.

---

## 2. Composition Tray — design and iteration history

Built in the following order, each step verified on-device before the next:

1. **Initial full-screen overlay version** (scrim + faded Piano Roll behind, per UX_Direction
   Section 5) — built, but on first on-device test only opened via the toolbar SUGGEST chip, with
   no standing access point. Diagnosed as a real architecture gap, not a bug: the tray needs to be
   a persistent surface (UX_Direction Section 6), not a modal triggered by one button.
2. **Rebuilt as a persistent bottom-anchored pill** in the exact slot and interaction style of the
   retired `VelocityOverlayTray`, so composition state (HARMONY ✓ / ARRANGEMENT +/+) is visible
   and reachable at any time, matching UX_Direction Section 12's "tray reflects current
   composition state" principle.
3. **`TrayHome` layout bug**: first on-device render showed only the BASS/DRUMS row, with
   everything above it invisible. Root cause: `TrayHome`'s composables weren't wrapped in a
   `Column`, so they inherited `AnimatedVisibility`'s Box-like stacking behaviour and were all
   drawn on top of one another — not a data or state bug, purely a missing layout container.
   Fixed with a one-line `Column { ... }` wrap.
4. **Strategy tab overflow**: the original horizontal-tab version of `SuggestNextFlow` (inherited
   unchanged from the pre-tray dialog) broke once a 5th strategy (CONTINUE) pushed the tab row
   past the tray's narrower width — tabs ran off-screen with no way to reach them. Redesigned as
   a two-level flow: vertical strategy-picker list → tap through to phrase list for that
   strategy. Fixes both screens, since `SuggestNextDialog` also consumes `SuggestNextFlow`.
5. **BACK/SELECT reachability**: initial phrase-list layout put BACK/SELECT below the phrase
   cards, where they scrolled out of reach for strategies with several phrases (confirmed
   on-device on LIFT's phrase list). Moved into a header row alongside the title.
6. **Header title + row consolidation**: header title switches to "SUGGESTED CONTINUATIONS" once
   a strategy is picked; the `‹ STRATEGY_NAME` breadcrumb and rationale text were consolidated
   onto one row (single-line, ellipsized) to reclaim vertical space, per an on-device mockup
   supplied mid-session.
7. **Icon buttons**: text-label BACK/SELECT buttons wrapped to two lines once the header title
   grew to "SUGGESTED CONTINUATIONS", breaking the layout. Replaced with fixed-size 32dp icon
   buttons (`‹` for back, `✓` for select, select icon in green) that can't wrap regardless of
   title length.
8. **Pinned-header scroll fix**: confirmed on-device that scrolling to the last phrase in a longer
   list (e.g. SURPRISE) still carried the header — including BACK/SELECT — off-screen with it,
   because the entire tray body (header + breadcrumb + phrase cards) lived inside one
   `verticalScroll` Column. Fixed by splitting `SuggestNextFlow` into a fixed header and a
   separately-scrolled body via a new `pinHeader: Boolean` parameter (default `false`, preserving
   `SuggestNextDialog`'s original content-sized, non-scrolling behaviour exactly).
   `CompositionTray` passes `pinHeader = true` and gives the flow a bounded, scrollable region.
   **Confirmed fixed on-device**: header and BACK/SELECT now stay pinned regardless of scroll
   position.

All of the above are implemented and verified in Piano Roll only.

---

## 3. Manual's Suggest Next — degraded, and recommended for removal rather than repair

### What happened

Partway through this session's iteration, `ManualWorkspace.kt`'s own **SUGGEST NEXT** button
(in `FixedBottomBar`, calling `viewModel.suggestNextOptions()` and opening the legacy
`SuggestNextDialog`) stopped working on-device. This was first suspected to be fallout from a
mid-session file-truncation incident in `SuggestNextFlow.kt` (a paste that dropped the trailing
`IconButtonSquare` function, causing `feature-chords` to fail to compile — see git history around
that point). A full `clean` + rebuild was run to rule this out. **The problem persists after a
verified clean build**, so it is not stale-APK fallout — there is a genuine regression somewhere
in Manual's Suggest Next path.

**Root cause not yet diagnosed.** No changes were made to `SuggestNextDialog.kt`'s public
signature, to `PairingType`, or to `SuggestionWorkflow.SuggestionOption` this session that would
explain a break in `ManualWorkspace.kt`'s call site, which is unchanged from before this session
began. Logcat has not yet been captured for this specific failure.

### Why we're not chasing the fix

Rather than debug this, the session concluded that **Manual's Suggest Next button should be
removed rather than repaired**, for reasons independent of the bug itself:

1. **It was always fully redundant with the tray's Suggest Next, not a distinct feature.**
   `ChordViewModel.suggestNextOptions()` reads the progression via
   `repository.getChordsForSessionOnce(sessionId)` — the *persisted* database state, not any
   Manual-local draft or UI-only context. Since `addBar()` already writes each chord to the DB
   immediately on add, Manual's Suggest Next and Piano Roll tray's Suggest Next query and act on
   *identical* underlying data through the *identical* suspend function. There was never a
   Manual-specific case this served that the tray doesn't already serve.

2. **Maintaining two independent UI implementations of the same feature repeats a known mistake.**
   This project already has one instance of this exact problem, previously flagged and never
   resolved: `suggestNextSection()` living separately in both `ChordViewModel.kt` and
   `PianoRollViewModel.kt` (see `PairingEngine_MultiOption_Refactor_Plan_2026-08-04.md`, Section 7,
   which recommended folding that consolidation into the multi-option refactor and explicitly
   flagged the risk of "touching the same files again" as a second pass). Letting
   `SuggestNextDialog` (Manual) and `CompositionTray` (Piano Roll) diverge as two independently
   -evolving UIs over the same feature is the same category of problem in a new location — this
   session's tray iteration (items 3–8 above) already only landed in the tray, not the dialog,
   which is exactly the kind of drift this creates.

3. **It works against the direction the product is already moving.** Per
   `ReasonTouch_Piano_Roll_Composition_UX_UI_Direction.md` and the follow-up design discussion
   from this session (attached separately, on folding "Starting Point" into the tray's empty
   state), the intended mental model is "the tray is where I decide what I want to develop next" —
   with Manual, Assisted, and Guided becoming *composition methodologies selected inside the
   tray*, not separate destinations with their own parallel feature sets. A Manual-local Suggest
   Next button is a small but real instance of exactly the "separate destination" pattern the
   wider redesign is trying to dissolve.

### What removal actually involves (not yet done)

In `ManualWorkspace.kt`:
- Remove the `SUGGEST NEXT` box from `FixedBottomBar`'s button row (leaving `-> ROLL` as the sole
  action, or widened to fill the row — layout decision still open).
- Remove `showSuggestDialog` / `currentSuggestions` state and the `onSuggestNext` callback.
- Remove the `SuggestNextDialog` invocation block and its now-unused import.

**Downstream effect:** once this lands, `SuggestNextDialog.kt` has zero remaining callers and
becomes fully dead code (candidate for deletion in the same pass or a follow-up cleanup, grouped
with the pre-existing `PianoRollViewModel.kt` dead `toCompositionIntent()` item). It also resolves
a module-boundary question raised mid-session — whether `CompositionTray.kt` would need to move
from `feature-pianoroll` into `feature-chords` to be shared with Manual — since after removal
nothing in `feature-chords` needs to call it.

**One alternative considered and left open:** rather than deleting the button outright, it could
instead route the user to Piano Roll with the tray already expanded to `SUGGEST_NEXT`, preserving
a fast path for someone deep in Manual who wants to think ahead before formally sending to Piano
Roll. Not adopted by default — flagged as an option only if this turns out to be a real workflow
people hit often, since otherwise it reintroduces complexity the redesign is trying to remove.

---

## 4. Known regressions / follow-ups from this session

- **Velocity editing is currently unreachable.** `VelocityOverlayTray` (and the
  `VelocityStripCanvas` it hosted) was removed wholesale in favour of `CompositionTray`, on the
  basis that it was described as "now redundant." Confirm this was the intended trade-off —
  `VelocityStripCanvas.kt` itself was not deleted and has no other call sites after this change,
  so it's currently dead code, not repurposed.
- **Manual's Suggest Next is broken** (Section 3 above) — root cause undiagnosed, removal
  recommended over repair, not yet implemented.
- **Playback overrun bug, still open, unrelated to this session's work**: chords written as a
  4-bar block progression (e.g. G C F C) play past the end of bar 4 and stop at the end of bar 5.
  `Sequencer.computeEndBeat()` was inspected and found to be computing correctly given its input
  (`lastNoteEnd = beat + duration`, epsilon-safe rounding) — the leading hypothesis is that a
  strummed chord's last NoteEvent in bar 4 is being assigned a duration that isn't clipped to the
  remaining beats in that bar, causing `beat + duration` to genuinely extend into bar 5. Not yet
  confirmed — needs the actual `(beat, duration)` values logged for the last CHORD-track
  NoteEvent after a repro, and likely a look at wherever strum-to-NoteEvent conversion happens
  (not yet located in this session).
- **`hasHarmony` in `CompositionTray`'s call site is a placeholder heuristic**
  (`tracks.any { allNotes[it.id]?.isNotEmpty() == true }`). Flagged twice this session as
  possibly not the correct signal — worth checking whether a cleaner `progression.isNotEmpty()`
  -style check already exists elsewhere before this becomes load-bearing for the tray's ✓/+
  indicators.
- **`hasBass` / `hasDrums` have no real data source yet** — currently always `false` since Bass
  and Drums generation isn't wired into the tray beyond stub panels (`TrayStubPanel`, referencing
  UX_Direction Sections 8–9, not yet built).
- **EXPAND cleanup still outstanding** — unrelated to this session, carried over: EXPAND was
  confirmed excluded from `IMPLEMENTED_INTENTS` (superseded by the other five strategies) rather
  than fixed via the `PairingEngine_MultiOption_Refactor_Plan_2026-08-04.md` document's proposed
  "Option C." That document's Sections 5/6/10 still describe EXPAND/Option C as pending work and
  need a superseded-status note. Not touched this session — still parked from earlier.
- **`PianoRollViewModel.kt`'s dead `toCompositionIntent()`** — pre-existing item from the 08-20
  handoff, still not cleaned up. Natural to fold into the same pass as `SuggestNextDialog.kt`'s
  removal, once that happens.

---

## 5. Suggested next-session order

1. Diagnose Manual's Suggest Next break properly if there's any chance of wanting the fallback
   route (Section 3's "alternative considered") — otherwise this is moot once removal happens.
2. Implement Manual Suggest Next removal (Section 3), confirm `SuggestNextDialog.kt` has zero
   callers, delete it and `PianoRollViewModel.kt`'s dead `toCompositionIntent()` together.
3. Confirm the velocity-editing removal (Section 4) was intentional; either formally retire
   `VelocityStripCanvas.kt` or find it a new home if it's still wanted.
4. Chase the playback-overrun bug (Section 4) — likely in whatever file performs strum-to-
   NoteEvent conversion; not yet located.
5. Resolve the `hasHarmony`/`hasBass`/`hasDrums` placeholder signals once there's a real data
   source for arrangement-layer completion state.
6. Begin real BASS/DRUMS tray panels (UX_Direction Sections 8–9), replacing `TrayStubPanel`.
7. EXPAND doc cleanup (parked, low priority, no functional impact).

---

*End of handoff.*
