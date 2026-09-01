# ReasonTouch — CompositionContext Specification
## Phase 1 Deliverable: CHORD Context Audit

**Status:** Draft — Phase 1 of the Bass/Drum Arrangement Roadmap
**Purpose:** Document, field by field, exactly what the CHORD system currently
exposes, so Bass/Drum arrangement design (Phases 3–4 onward) can proceed from
verified fact rather than assumption.
**Method:** Every field below is marked **Confirmed** (seen directly in source
this session or a prior one) or **Inferred** (seen only via call-site usage —
needs direct source verification before any Phase 2+ design relies on it).
**No implementation change in this phase.** Paper only, per the roadmap's own
Phase 1 scope.

---

## 0. Since the handoff docs were written

Both source design docs (`Interlocking / Rhythmic Complementarity Handoff` and
`Musical Context, Bass & Drum Arrangement`) list "the remaining strum-speed
variable control" as the one outstanding CHORD item. That item is now closed
(2026-08-29 session): the strum speed control is live in `StrumPatternTray`,
wired into all four workspace call sites (Manual, Inspire, Mood, Progression),
verified on-device. CHORD is now feature-complete relative to what those docs
anticipated. This spec reflects CHORD's state as of that closure.

Two known open items exist elsewhere in the codebase, noted here because they
touch fields this spec documents, but neither blocks this audit:
- Piano Roll playback "overspill" bug (queued for investigation) — relevant to
  Structure/Timing fields below (Section 3.3), since it concerns where
  playback/sequencing believes a progression ends.
- Bass register discrepancy (`BassGenerator.kt` comment says MIDI 28–52,
  constants say 40–64) — flagged in the source doc, not yet resolved. Not a
  CHORD-context concern, but noted since Phase 3 (Bass Arrangement Spec) will
  need to resolve it before finalizing Bass's consumption of this context.

---

## 1. Session (Confirmed)

Source: `Session` entity (referenced via `ChordViewModel.session`,
`repository.getSession()`), `ChordUiState`.

| Field | Type | Source | Notes |
|---|---|---|---|
| `bpm` | `Int` | `Session.bpm`, exposed via `ChordViewModel.bpm: StateFlow<Int>` | Defaults to 120 if session is null (`it?.bpm ?: 120`) |
| `barDuration` (beats per bar) | `Double` | `ChordUiState.barDuration` | Default `4.0`. **Note:** this is UI-session state, not per-`ChordEvent` — one value applies to the whole session's chord-add flow at time of add, not stored per bar |
| `totalBars` | `Int` (on `Session`) | `Session.totalBars`, updated by `addBar()`/`addPhrase()` after each write | Kept in sync manually at each write site — not derived automatically from `progression.size` at read time in all paths (worth flagging: two sources of truth exist — `Session.totalBars` and `progression.value.size` — currently kept manually consistent, not enforced) |
| `keyRoot`, `keyQuality`, `timeSignature` | — | `Session` entity, per original handoff's data model doc | **Inferred** — listed in the April handoff's `Session` field list but not directly re-confirmed this session. Needs a direct read of current `Session.kt` before relying on these for time-signature-aware arrangement (e.g. non-4/4 meters). |

---

## 2. Harmony — per-bar (Confirmed)

Source: `ChordEvent.kt`, as it stands after tonight's Phase 1–4 schema/write work.

| Field | Type | Notes |
|---|---|---|
| `id` | `String` (UUID) | |
| `sessionId` | `String` | |
| `barIndex` | `Int` | Position in the progression |
| `chordName` | `String` | e.g. `"E Open"` — chord + voicing name concatenated, not structured separately at this field (voicing is also its own field, see below — some redundancy here worth noting) |
| `rootMidi` | `Int` | Root note MIDI number |
| `midiNotes` | `String` | Comma-separated MIDI note list — **stringly-typed**, parsed at every read site (`.split(",").mapNotNull { it.toIntOrNull() }`) rather than stored as a structured list. Worth flagging as a possible friction point for a context layer that wants typed chord-tone data. |
| `voicing` | `String` | e.g. `"Open"` |
| `strumPatternId` | `String?` | 16-char `D`/`U`/`.` encoded string (Phase 2). `null` only for pre-Phase-2 legacy rows (confirmed not present in any current test data — user's own words: "any progressions so far created have been merely to assist & trial the build") |
| `strumSpeedValue` | `Double?` | Raw `beatsPerString` seconds value (Phase 2). Same legacy-null caveat as above |

**Not currently on `ChordEvent`, computed elsewhere or not at all:**
- Chord quality/function (major/minor/7th/etc.) — not a stored field; only derivable from `chordName` string parsing (see `KeyDetector`/`ProgressionAnalyzer` usage, which does `it.chordName.substringBefore(" ")` then presumably classifies from there — **Inferred**, `KeyDetector.kt`/`ProgressionAnalyzer.kt` not yet reviewed directly this session)
- Individual chord-tone roles (root/third/fifth/seventh) — not stored; would need derivation from `midiNotes` + music-theory logic
- GM instrument/program per chord — exists at `ChordUiState.instrument: GmInstrument` (session-level, not per-bar)

---

## 3. Harmony — session/analysis level (Confirmed + Inferred)

Source: `ChordViewModel.kt`'s `HarmonyState` and related flows.

### 3.1 `HarmonyState` (Confirmed — full definition obtained this session)

```kotlin
data class HarmonyState(
    val isAnalysing:    Boolean               = false,
    val keyCandidates:  List<KeyCandidate>    = emptyList(),
    val selectedKey:    KeyCandidate?         = null,
    val suggestions:    List<ChordSuggestion> = emptyList(),
    val borrowedChords: List<BorrowedChord>   = emptyList(),
    val showPanel:      Boolean               = false,
    val moodBias:       Float                 = 0f   // -1 dark .. 0 neutral .. +1 bright
)
```

| Field | Notes |
|---|---|
| `keyCandidates: List<KeyCandidate>` | Ranked key detection results |
| `selectedKey: KeyCandidate?` | User's chosen key, once selected from candidates |
| `suggestions: List<ChordSuggestion>` | Diatonic next-chord suggestions for the selected key |
| `borrowedChords: List<BorrowedChord>` | Chromatic/borrowed-chord options from the parallel key |
| `moodBias: Float` | -1 (dark/minor) to +1 (bright/major), user-adjustable slider |

### 3.2 `KeyCandidate` (Inferred from usage — not yet seen as a source definition)

Used as: `candidate.root.label`, `candidate.isMinor`, `candidate.confidence`.
**Needs direct verification** of `KeyDetector.kt` before Phase 2/3 relies on
this shape.

### 3.3 `ChordSuggestion` (Inferred from usage)

Used as: `suggestion.chord.label`, `suggestion.chord.midiNotes`, `suggestion.function` (a `HarmonicFunction` enum: `TONIC` / `PREDOMINANT` / `DOMINANT`), `suggestion.description`.
**Needs direct verification** of `ChordSuggestionEngine.kt`.

### 3.4 `BorrowedChord` (Inferred from usage)

Used as: `borrowed.chord.label`, `borrowed.chord.midiNotes`, `borrowed.chord.guitarLabel()`, `borrowed.symbol`, `borrowed.description`.
**Needs direct verification.**

---

## 4. Structure / Continuation strategy (Confirmed, this session)

Source: `SuggestionWorkflow.kt` (obtained this session — first direct read).

This is the **most load-bearing piece** for the "structural context" the
handoff docs describe (Section 3.3 of the first doc: phrase boundaries,
continuation strategy, resolution/contrast/lift/surprise). Confirmed shape:

### 4.1 Flow

```
progression: List<ChordEvent>
    → chordNames (via chordName.substringBefore(" "))
    → KeyDetector.detect(chordNames) → detectedKey
    → ProgressionAnalyzer.analyze(progression, detectedKey) → analysis
    → PairingEngine.suggestNext(analysis) → PairingDecision
      (or PairingEngine.suggestNextMulti(analysis) → ranked List<IntentOption>)
    → ProgressionGenerationRequest(...) per intent
    → ProgressionGenerator.generate(request) → List<GeneratedProgression>
```

### 4.2 `PairingDecision` (Inferred from usage)

Fields used: `type: PairingType`, `suggestedBars: Int`, `confidence: Float`,
`rationale: String`.

### 4.3 `PairingType` (Confirmed — enum values seen directly)

```
CONTINUE, LIFT, CONTRAST, RESOLVE, EXPAND, SURPRISE, SIMPLIFY, MODULATE
```

Maps to `CompositionIntent` (a related but distinct enum — `CONTINUE`, `LIFT`,
`CONTRAST`, `RESOLVE`, `EXPAND`, `SURPRISE`, `SIMPLIFY`, and `DEVELOP` as the
fallback for `MODULATE`, per an explicit code comment: *"closest existing
intent; no direct equivalent yet"* — worth noting `MODULATE` is a known gap).

Per the earlier handoff doc: **CONTINUE and CONTRAST are established;
RESOLVE and LIFT were the next strategies in development** at time of
handoff. Current implementation state of each individual strategy —
**not verified this session.**

### 4.4 `IntentOption` (Inferred from usage)

Fields used: `type`, `suggestedBars`, `confidence`, `rationale`,
`isAboveThreshold: Boolean`. The last field suggests some intents can be
below a confidence threshold and still be offered, distinguished by this flag
— relevant if arrangement ever wants to weight low-confidence suggestions
differently.

### 4.5 `GeneratedProgression` (Inferred from usage)

Used as: `generatedProgression.chords: List<TheoryChord>` (via
`.forEachIndexed { index, theoryChord -> ... }`), and each `theoryChord` has
`.guitarLabel(): String` and `.midiNotes: List<Int>`.

### 4.6 What this section confirms structurally

- The system already computes **key detection** and **harmonic analysis**
  fresh, per-call, from the live progression — not cached/stored per bar.
- **Phrase-level intent** (continue/lift/contrast/resolve/expand/
  surprise/simplify/modulate) is a real, working concept today — not
  speculative. This directly satisfies the handoff doc's wish for
  "resolution/contrast/lift/surprise context" to exist.
- **Confidence scoring** already exists at both the decision level
  (`PairingDecision.confidence`) and multi-option level
  (`IntentOption.confidence`, `isAboveThreshold`) — a real foundation
  for the "candidate evaluation" concept the second handoff doc proposes
  for Bass/Drums (Phase 5, "Groove relationship model").

---

## 5. Rhythm — per-bar (Confirmed)

Source: `ChordEvent.strumPatternId` / `strumSpeedValue` (Section 2 above),
`StrumEncoding.kt`, `StrumNoteGeneration.kt`.

| Derived property | How it's computed | Confirmed / Inferred |
|---|---|---|
| Active step positions | `strumPatternId.toStepPattern().steps.mapIndexedNotNull { i, s -> if (s != OFF) i else null }` | Confirmed — this exact operation exists in `generateStrumNotes()` |
| Stroke direction per step | `StepState.DOWN` / `StepState.UP` / `StepState.OFF`, per step | Confirmed |
| Attack density (this session's proposed derived metric) | Count of non-`OFF` steps out of 16 | **Not currently computed anywhere** — would be new derived logic, trivial to add (`steps.count { it != StepState.OFF }`) |
| Strum speed (real seconds/string) | `strumSpeedValue` field directly | Confirmed, and — as of tonight — genuinely variable per bar via the new UI control, not always `0.02` |
| Actual note-offset timing | `generateStrumNotes()`: `offsetBeats = strumSpeedSeconds * (bpm/60f)`, applied per note within a strummed step | Confirmed (Phase 3 fix) |
| Block vs. strum mode | `activeSteps.isEmpty()` → block mode fallback | Confirmed — an all-`OFF` pattern is a deliberate, valid "plain block chord" state (tonight's guard-removal work), not an error/unset state |

**Not yet computed, but clearly derivable** (flagging for Phase 2 — "define
derived musical properties" — since these look like exactly the kind of thing
that phase is meant to produce):
- Rhythmic occupancy (proportion of the bar actually sounding, factoring in
  note duration, not just attack count)
- Syncopation measure (attacks landing off the implied strong-beat grid)
- Bar-to-bar rhythmic consistency/variation (same pattern repeated vs. varied)

---

## 6. Structure / Continuation inheritance (Confirmed, tonight's Phase 4 work)

Directly relevant to "continuation strategy" context:

- `addPhrase()` now inherits `strumPatternId`/`strumSpeedValue` from the
  **last existing `ChordEvent`** in the progression (not from currently-
  selected UI state) when generating continuation bars — confirmed working
  on-device (E/A seed → 8-bar SUGGEST continuation, correct pattern carried
  forward throughout).
- This means: **by the time Bass/Drums would consume a progression, its
  rhythmic character is already coherent across seed + continuations** —
  arrangement doesn't need to separately solve "which bar's strum pattern
  is authoritative," CHORD has already established one coherent thread.
- Known gap (not a CHORD-context defect, but relevant to arrangement
  timing): inheritance currently propagates a *value*, not variation —
  every bar in a single `addPhrase()` call gets identical inherited
  pattern/speed (documented explicitly as a Phase 3/4 scoping choice, not
  an oversight). If future arrangement design wants per-bar rhythmic
  *variation* within a continuation, that's not yet a CHORD capability.

---

## 7. Existing arrangement state — Bass/Drum audit (Confirmed, this session)

Source: `BassGenerator.kt`, `BassStyle.kt`, `DrumPattern.kt`, `DrumKit.kt`,
`DrumViewModel.kt` — all read directly this session, superseding the earlier
handoff docs' descriptions of them where any discrepancy exists.

### 7.1 Bass — register discrepancy, now resolved to a confirmed fact

The handoff doc flagged a comment/implementation mismatch. Confirmed exactly
as described:

- **Doc comment:** `"All notes target the bass register — MIDI 28-52 (E1-E3)"`
- **Actual constants:** `BASS_MIN_MIDI = 40`, `BASS_MAX_MIDI = 64`
- **`bassRegister()` logic:** reduces any input note to its pitch class
  (`% 12`), re-anchors at MIDI 48 (C3), then nudges by octaves until it falls
  within `[40, 64]`.

So the real, currently-shipping range is **MIDI 40–64**, roughly **E2–E4** —
a full octave higher than the stale comment claims, and narrower (24
semitones vs. the documented 24 semitones — same *width*, different
*center*). This is a **documentation bug, not (necessarily) a musical bug**
— the code has been consistently using 40–64 all along; nothing is
"broken," but the comment actively misleads anyone reading it. Per the
handoff doc's own instruction, this needs an **explicit musical decision**
(confirm 40–64 is correct and fix the comment, or decide 28–52 was actually
intended and change the constants) before Phase 3 (Bass Arrangement Spec)
treats either range as authoritative. **Not decided in this session — flagged
for explicit resolution before Phase 3 begins.**

### 7.2 Bass — confirmed generation model

`BassGenerator.generate()` consumes:

| Input | Type | Notes |
|---|---|---|
| `chords` | `List<ChordEvent>` | Full progression, in order |
| `style` | `BassStyle` | One of 7 enum values (below) |
| `targetTrackId` | `String` | |
| `beatsPerBar` | `Float`, default `4f` | **Not sourced from `Session`/`ChordUiState.barDuration` at the call site shown here** — caller-supplied. Worth checking at the `ChordViewModel.generateBass()` call site (seen earlier tonight) whether it correctly passes `ui.value.barDuration.toFloat()` — confirmed yes, it does. |
| `appendOffset` | `Float`, default `0f` | |
| `snapValue` | `Float`, default `0.25f` | **Declared but not used anywhere in the function body** — dead parameter, worth flagging as a small cleanup item, unrelated to arrangement design |

**Per-bar derivation, confirmed:**
- `rootMidi` — `chord.rootMidi`, transposed into bass register
- `chordTones` — parsed from `chord.midiNotes` (same stringly-typed
  comma-separated parsing noted in Section 2), transposed, deduplicated,
  sorted
- `nextRootMidi` — looks ahead one bar (`chords.getOrNull(barIdx + 1)`) —
  **confirms Bass already does one-bar lookahead for voice-leading-adjacent
  behavior** (used only by `WALKING` style currently, for its chromatic
  approach note)

**`BassStyle` — 7 values, confirmed exact behavior per style:**

| Style | Rhythmic shape | Reads next chord? | Reads chord tones? |
|---|---|---|---|
| `ROOT` | 1 whole-bar note | No | No |
| `ROOT_FIFTH` | Root (beat 1) + 5th (beat 3) | No | No |
| `OCTAVE` | Root (beat 1) + octave (beat 3) | No | No |
| `WALKING` | 4 notes, one per beat, chromatic approach to next root | **Yes** | No |
| `ARPEGGIO` | Up to 4 chord tones, one per beat | No | **Yes** |
| `GROOVE` | Syncopated: root at 1, 2.5 (ghost/quiet), 3, 3.5 (ghost/quiet) | No | No |
| `PEDAL` | 1 sustained whole-bar note | No | No |

**What this confirms structurally, relevant to Phase 3 (Bass Arrangement
Spec):**
- Bass generation is currently **entirely independent of CHORD's rhythmic
  data** — no style reads `strumPatternId`/`strumSpeedValue`/attack density
  at all. This is the exact gap Section 5's handoff docs want closed: *"A
  bassline should not be generated as though the chord track were sustained
  block chords if the guitar part is heavily syncopated"* — confirmed true
  today; Bass has no way to know.
- Every style except `WALKING` is **entirely bar-local** — no
  cross-bar awareness beyond `WALKING`'s single-bar lookahead. True
  multi-bar phrase awareness (phrase boundaries, section context) does not
  exist in Bass at all currently.
- `velocity` values are hardcoded per style (e.g. `GROOVE`'s ghost notes at
  velocity 50–55 vs. strong hits at 90–105) — this is real, already-existing
  accent-profile data, exactly the kind of thing Section 9's "Accent
  compatibility" scoring concept could consume directly, with zero new
  generation logic needed — just exposure.

### 7.3 Drums — confirmed data model

`DrumPattern` — 6 lanes × N steps (16 default, extendable to 32), each cell
a `Boolean` (active/inactive) plus a parallel `velocities` grid
(`Int`, default 100). Confirmed methods: `toggle`, `isActive`, `velocity`,
`setVelocity`, `clear`, `extendTo32`, `trimTo16`.

`DrumKit.lanes` — 6 fixed lanes, **confirmed exact GM note mapping:**

| Lane | GM note | Pitch (`108 - gmNote`) |
|---|---|---|
| Kick | 36 | 72 |
| Snare | 38 | 70 |
| Clap | 39 | 69 |
| Hi-Hat | 42 | 66 |
| Open HH | 46 | 62 |
| Ride | 51 | 57 |

All write to **MIDI channel 9** (`DRUM_MIDI_CHANNEL = 9`) — confirmed
correctly hardcoded at the point of `MidiTrack` creation in
`DrumViewModel.getOrCreateDrumsTrack()` (`midiChannel = 9`). **This is
directly relevant to the still-open MIDI multi-track export issue flagged
earlier tonight** — the *data model* correctly assigns channel 9 for drums;
if exported MIDI still shows drums as piano, the bug is downstream in
`ExportViewModel.kt`'s track-mapping logic, not in this data model. Worth
closing that investigation with this confirmation in hand.

**`DrumPresets`, confirmed exact step patterns** (all as 16-step, 0-indexed):

| Preset | Kick steps | Snare steps | Other |
|---|---|---|---|
| `FOUR_FOUR` | 0, 8 | 4, 12 | HH every 8th (even steps) |
| `ROCK` | 0, 6, 8, 14 | 4, 12 | HH every 8th |
| `FUNK` | 0, 3, 8, 11, 14 | 4, 12 | Clap 2,6,10,14; HH all 16ths |
| `REGGAE` | 0, 12 | 6, 14 (off-beat snare, not 4/12) | HH offbeats 2,6,10,14 |
| `BOSSA` | 0, 9 | 4, 13 | Ride on 11 of 16 steps |

**What this confirms structurally, relevant to Phase 4 (Drum Arrangement
Spec):**
- **Kick step positions are already, right now, real per-preset data** —
  Section 5 of the handoff doc's "kick/bass interaction" scoring concept has
  concrete kick-position data to work from immediately, no new instrumentation
  needed.
- **Drum generation is currently 100% independent of both Bass and Chord
  data** — confirmed no read of `ChordEvent`, `strumPatternId`, or bass
  `NoteEvent`s anywhere in `DrumViewModel`/`DrumPattern`. The entire
  arrangement-awareness gap the handoff docs describe is real and total for
  Drums today, not partially addressed anywhere.
- `writeToPianoRoll()` repeats **one single pattern for the entire session's
  `totalBars`** — confirmed no per-bar or per-section pattern variation
  exists yet (no fills, no intensity changes across a progression). This is
  a bigger gap than Bass's — Bass at least varies content per bar (root
  changes with the chord); Drums currently writes the *identical* pattern
  bar after bar for the whole song. Worth weighting Phase 4's ambitions
  accordingly — "phrase endings, fills, variation" (Section 8 of the second
  handoff doc) starts from a genuine blank slate, not a partial
  implementation.
- Live playback (`play()`) reads `_pattern.value` fresh each step
  ("allows real-time pattern changes," per its own comment) — confirmed
  no cold-StateFlow risk here, unlike the bug we fixed in
  `PianoRollViewModel` earlier tonight. Good pattern, worth noting as a
  positive example rather than only ever cataloguing problems.

### 7.4 Cross-cutting observation: `NoteEvent` is the shared contract

Both Bass and Drums (and Chord's own `generateStrumNotes()`) converge on
identical `NoteEvent` output — same `pitch`/`beat`/`duration`/`velocity`
shape, same `108 - midiNote` pitch convention, same `UUID.randomUUID()` id
generation pattern. This confirms the handoff docs' description of
`NoteEvent` as "the common output representation" is accurate, and — more
importantly for Phase 6 (`CompositionContext` implementation) — means any
future groove-evaluation logic can read/compare Bass and Drum output
symmetrically, without per-generator special-casing, since they already
speak the same structural language.

---

## 8. Open questions this audit surfaces (not resolved here)

1. **`midiNotes` as a comma-separated string, not a structured list** —
   every consumer re-parses it. Worth deciding whether `CompositionContext`
   should expose a pre-parsed `List<Int>` view, and whether that becomes
   the point where this stringly-typed storage finally gets addressed (or
   deliberately left as-is, storage-layer concern, with parsing pushed to
   the context-assembly boundary only).
2. **`Session.totalBars` vs. `progression.size`** — two sources of truth,
   manually kept in sync at each write site. Worth flagging as a place
   `CompositionContext` assembly should pick one canonical source
   (`progression.size` seems safer — always current — rather than trusting
   the separately-maintained counter).
3. **Time signature fields** (`keyRoot`, `keyQuality`, `timeSignature` on
   `Session`) — listed in the original April handoff, not re-verified this
   session. If arrangement ever needs non-4/4 meter awareness, these need
   direct confirmation first.
4. **Individual strategy implementation completeness** — the handoff docs
   note CONTINUE/CONTRAST were established, RESOLVE/LIFT were "next," at
   an earlier point in time. Current state of each `PairingType`'s actual
   generation quality is not something this audit can assess without
   exercising each one directly.
5. **Bass register — 40–64 vs. the documented 28–52** — needs an explicit
   musical decision (Section 7.1) before Phase 3 begins. This is the single
   highest-priority open item from this audit, since it's the one place
   documentation and implementation actively disagree, rather than simply
   being incomplete.
6. **`BassGenerator.generate()`'s unused `snapValue` parameter** — dead
   code, low priority, but worth a cleanup pass at some point (Section 7.2).
7. **MIDI multi-track export instrument mapping** — this audit confirms the
   *data model* correctly assigns `midiChannel = 9` for drums (Section 7.3),
   which narrows the earlier-flagged "everything imports as piano" bug
   specifically to `ExportViewModel.kt`'s track-mapping logic — worth
   revisiting that investigation with this confirmation in hand, rather than
   re-checking the data model again.

---

## 9. Summary — what Phase 1 has established

- **CHORD** (Sections 1–6): thoroughly audited, mostly Confirmed, a handful
  of Inferred types flagged for direct verification if Phase 2 needs them.
- **Bass** (Section 7.1–7.2): fully audited. Confirmed entirely bar-local
  and chord-only aware today — no rhythmic/strum awareness at all, exactly
  the gap the handoff docs predicted. One real discrepancy (register range)
  needs a decision before Phase 3.
- **Drums** (Section 7.3): fully audited. Confirmed entirely independent of
  both Chord and Bass, and — more than either doc anticipated — currently
  incapable of *any* per-bar or per-section variation (one pattern repeats
  for the whole session). Phase 4's ambitions start from a larger gap than
  the handoff docs implied.
- **Shared contract** (Section 7.4): `NoteEvent` confirmed as a genuinely
  uniform output format across all three generators — a real asset for
  Phase 6's `CompositionContext` implementation.

**Recommended next step:** Phase 2 — define derived musical properties
(rhythmic density, syncopation, occupancy, etc.) building on the confirmed
raw fields this document now provides for Chord, Bass, and Drums alike.

---

*End of Phase 1 deliverable.*