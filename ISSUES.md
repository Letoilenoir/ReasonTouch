# Known Issues

## Piano Roll Playback Duration
**Severity:** Medium  
**Status:** Open  
**Details:** When notes are sent to piano roll via MoodWorkspace, playback stops after bar 5 (blank bar) instead of at the end of the last note. The PlaybackController.drawDuration isn't recalculated after epository.saveNotes().

**Fix:** After saveNotes, query max beat+duration from saved notes and update drawDuration.

---

## StrumPatternTray: Rock & Folk Both Selected
**Severity:** Low (UI only, logic works)  
**Status:** Open  
**Details:** In StrumPatternTray, selecting Rock also highlights Folk, and vice versa. Selection state logic is incorrect.

**Fix:** Debug StrumPatternTray state management, ensure only one pattern selected at a time.

---

## Phase 3: StepSequencerEditor Abstraction
**Severity:** Medium (code organization)  
**Status:** Deferred  
**Details:** PatternGrid + StepButton used only in ChordScreen; DrumMachine has duplicate grid code. Extract generic StepSequencerEditor (16-step grid, mode-aware) to core-ui for reuse.

**Plan:** Phase 3a (build), 3b (retrofit chords), 3c (retrofit drums), 3d (integrate StrumPatternTray custom editing).

---

## Phase 4: MIDI Pattern Import
**Severity:** Low (future feature)  
**Status:** Deferred  
**Details:** Allow users to import .mid files as strum patterns (1 bar, 16th resolution).

**Prerequisites:** Phase 3 (StepSequencerEditor) must be complete.