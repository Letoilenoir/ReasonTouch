# ReasonTouch -- Bug Investigation Handoff
## "Missing Chord" Persistence Bug & Related SUGGEST/Piano Roll Fixes

**Date started:** 2026-07-25
**Date resolved:** 2026-07-29
**Status:** RESOLVED (original bug + three follow-on bugs found during CONTRAST testing -- see Section 8a)

---

## 1. The original symptom

While testing the **SUGGEST** button in the Piano Roll screen, the app reported "Could not detect key" / "Chord count = 0" -- even though the user had clearly just added several chords via the CHORDS/Manual workspace and sent them to Piano Roll.

## 2. What we ruled out

- Wrong session ID being read -- ruled out
- Two separate/duplicate databases -- ruled out
- Broken DAO queries -- ruled out
- Duplicate primary keys silently overwriting each other -- ruled out

## 3. Root cause -- CONFIRMED

**Theory A (stale/cold reactive data stream) was correct.**

`PianoRollViewModel.kt` maintained its own `chords: StateFlow<List<ChordEvent>>`, built with `SharingStarted.WhileSubscribed(5000)`. Because nothing in the Piano Roll UI was actively subscribed to this flow, it remained permanently on its cold `emptyList()` default -- even though the database genuinely contained all the saved chords.

Confirmed via: (1) extracting the live SQLite database (`.db` + `.db-wal` + `.db-shm`, app force-stopped) and querying `chord_events` -- all 4 saved chords present and correct; (2) Logcat cross-reference -- the `chords` StateFlow's logging line never appeared during the failing test, proving the flow was never even collected.

**Theory B (chords not reaching the database) was ruled out.** The WAL-file extraction concern earlier in the investigation was a real methodological gap but not the actual cause.

## 4. Fix applied

### `core/core-data/.../ChordEventDao.kt`
Added a one-shot suspend query:
```kotlin
@Query("SELECT * FROM chord_events WHERE sessionId = :sessionId ORDER BY barIndex ASC")
suspend fun getChordsForSessionOnce(sessionId: String): List<ChordEvent>
```

### `core/core-data/.../SessionRepository.kt`
Added the corresponding repository wrapper.

### `feature/feature-pianoroll/.../PianoRollViewModel.kt`
- `suggestNextSection()` and `suggestNextPhrases()` converted to `suspend fun`, now read via `repository.getChordsForSessionOnce(sessionId)`.
- `addPhrase()`'s `isEmpty()` check and `startBarIndex` calculation moved inside the coroutine, sourced the same way -- a second, latent instance of the same bug.

### `feature/feature-pianoroll/.../PianoRollScreen.kt`
Added `rememberCoroutineScope()` and wrapped the `onSuggestNext` click handler in `scope.launch { }`.

## 5. Verification

Confirmed working on-device: SUGGEST returned a 95%-confidence RESOLVE decision instead of "Could not detect key" -- full pipeline (DB read -> key detection -> progression analysis -> pairing decision -> UI display) verified end-to-end.

## 6. New (expected, non-bug) finding: empty "Suggested continuations"

Confirmed via `ProgressionGenerator.kt`: only CONTINUE and CONTRAST strategies were implemented at that time; RESOLVE, LIFT, EXPAND, MODULATE, SIMPLIFY were explicitly stubbed with TODOs. Expected current behavior, not a regression.

## 7. Environment note (new development machine)

Resolved during this investigation:
- Gradle/AGP version mismatch (AGP 8.13.2 requires Gradle 8.13; wrapper had been auto-bumped to 9.6.1). Fixed by pinning `distributionUrl` back to `gradle-8.13-bin.zip`.
- `JAVA_HOME` not set -- pointed at Android Studio's bundled JBR, set at User environment variable level.
- Recurring "Unable to delete file ...classes.jar" build failure caused by stale `java` processes holding a file lock. Resolved by identifying/killing stale processes and clearing affected build folders.
- Python not installed on new machine (needed for SQLite inspection) -- installed via Microsoft Store.

## 8. Cleanup -- DONE

- Removed temporary `BOUDIE` debug logging from `ChordViewModel.kt` and `PianoRollViewModel.kt`.
- Committed and pushed as commit `53e8277`.

## 8a. Follow-on bugs found while testing CONTRAST (same session, now also RESOLVED)

Commit `13f684e`.

### Bug: SELECT wrote ChordEvents but never rendered notes in Piano Roll

**Root cause:** `addPhrase()` only ever called `repository.saveChord(chordEvent)`, never converting generated chords into `NoteEvent`s.

**Fix:** `addPhrase()` now also builds block-mode `NoteEvent`s and writes them via `repository.saveNotes(...)`, targeting the session's CHORD track. Bar length derived from `session.value.timeSignatureNumerator` rather than hardcoded.

### Bug: New notes required leaving/re-entering ARRANGE to appear

**Root cause:** `PianoRollViewModel`'s `_allNotes` is a plain `MutableStateFlow` populated by a one-time snapshot load in `init`. `addPhrase()`'s new note-writing code wrote to the database but never updated the in-memory cache.

**Fix:** After `repository.saveNotes(newNotes)`, `addPhrase()` now also updates `_allNotes.value` directly and calls `updateActiveNotes()`.

### Bug: Playback stopped one bar early when the last note starts on a bar boundary

**Root cause:** `Sequencer.computeEndBeat()` computed the playback end position from `it.beat` (start time) only, never accounting for `it.duration`.

**Fix:** `computeEndBeat()` now computes `it.beat + it.duration` before applying the existing epsilon-safe bar-rounding logic.

**Verified on-device:** selecting a CONTRAST suggestion writes visible, immediately-rendered notes; playback runs correctly through the full 12 bars tested.

### Known, not yet fixed at time of writing -- queued for later

- No strum pattern applied to SUGGEST-inserted chords (`addPhrase()` writes block-mode notes only).
- CONTRAST suggestion repeated the same chord for consecutive bars (bars 9-12 of a test all resolved to C). **Note: this was subsequently diagnosed precisely -- see the 2026-08-01 handoff, Section 3, for the full root-cause analysis (ChordSuggestionEngine's MAX_SUGGESTIONS cap) and the exclusion-based mitigation applied.**

## 9. Next queued item at time of writing: ResolveStrategy / LiftStrategy

**Note: ResolveStrategy was subsequently implemented -- see the 2026-07-31 and 2026-08-01 handoffs. LiftStrategy remains a TODO stub as of 2026-08-01.**

Per the existing architecture principle ("CONTINUE as canonical template"), planned work was:
- Build `ResolveStrategy` following the same structure as `ContinueStrategy`/`ContrastStrategy`.
- Build `LiftStrategy` next, same pattern.
- Add regression tests following the precedent of `ContinueStrategyTest.kt`.
- `EXPAND`, `MODULATE`, `SIMPLIFY` remain further out.

---

*End of handoff document.*