# ReasonTouch — Drum Arrangement Specification
## Phase 4 Deliverable: Drum Arrangement Roadmap

**Status:** Draft — Phase 4 of the Bass/Drum Arrangement Roadmap
**Precedes:** Phase 5 (Groove Relationship Model)
**Depends on:** `CompositionContext_Specification.md` (Phase 1), Section 7.3
(Drum audit) and Section 7.4 (`NoteEvent` shared contract); and
`Bass_Arrangement_Specification.md` (Phase 3), for the parallel structure
and cross-references.
**Method:** Same discipline as Phase 3 — grounded in confirmed current
implementation, not the earlier handoff docs' higher-level description.
**No implementation change in this phase.** Paper only.

---

## 1. What Drums currently have, restated precisely

From Phase 1's audit (Section 7.3), confirmed facts this spec builds on:

- `DrumPattern`: 6 fixed lanes × N steps (16 default, extendable to 32),
  boolean active/inactive grid, parallel per-step velocity grid.
- `DrumKit.lanes` — Kick (GM 36), Snare (38), Clap (39), Hi-Hat (42),
  Open HH (46), Ride (51) — all fixed, all writing to **MIDI channel 9**,
  confirmed correct in the data model.
- 5 presets exist (`FOUR_FOUR`, `ROCK`, `FUNK`, `REGGAE`, `BOSSA`), each
  with confirmed exact kick/snare/hi-hat/clap/ride step positions (Phase 1
  Section 7.3's table).
- **Drums are completely independent of both Chord and Bass today** — no
  read of `ChordEvent`, `strumPatternId`, or Bass `NoteEvent`s anywhere.
  This is a larger gap than Bass's (Bass at least reads chord roots/tones
  today; Drums reads nothing from anywhere else).
- **`writeToPianoRoll()` repeats one single pattern for the entire
  session's `totalBars`** — confirmed no per-bar or per-section variation
  exists at all. No fills, no intensity changes, no phrase-ending
  behavior. This is the largest single gap Phase 1 surfaced across either
  instrument.
- Live playback (`DrumViewModel.play()`) reads `_pattern.value` fresh each
  step — confirmed good practice, no cold-StateFlow risk (unlike the bug
  fixed in `PianoRollViewModel` earlier in this project).

The central design question for this phase, by direct analogy with Bass's
framing: **not** "should Drums read Chord/Bass context," but **given
Drums currently has *zero* contextual awareness and *zero* structural
variation, which of those two gaps matters more to close first?**

This spec's position, argued in Section 7: **structural variation (Section
7) is the more urgent gap** — a drum pattern that's contextually perfect
   but repeats identically for 32 bars will still sound wrong long before
   contextual mismatch would be noticed. This differs from Bass, where the
   existing per-bar root movement already gives it some baseline variation
   that Drums entirely lacks.

---

## 2. What Drums should consume from CompositionContext

### 2.1 Already consumed today: nothing

Unlike Bass (which reads `rootMidi`/`midiNotes`), Drums currently consumes
**no** CHORD or Bass data whatsoever. Every field below is a genuine
addition, not an extension of existing usage.

### 2.2 Recommended additions

| Field | Source (Phase 1 ref) | Proposed use |
|---|---|---|
| `strumPatternId` → active step count | Spec Section 5 | **Chord rhythmic density** — same derivation as Bass's Section 4. See Section 5 below for the Drum-specific relationship. |
| `bpm` | Spec Section 1 | Already available in `DrumViewModel.session.value?.bpm`, and already used for playback timing (`stepMs` calculation) — **but not currently passed into any pattern-*selection* logic**, since there isn't any yet. Relevant once preset recommendation exists (Section 4). |
| `PairingDecision`/`IntentOption.type` | Spec Section 4 | Phrase-level intent — see Section 6 below, mirrors Bass's Section 6 |
| Bass `NoteEvent`s (kick-relevant subset only: onset beats, not full note data) | Bass output, per Bass spec Section 5 | **Kick/Bass coincidence** — see Section 6 of this doc. Read-only, one-directional (Drums may read Bass; Bass should not need to read Drums, preserving the "generators shouldn't need to understand each other" rule) |
| Bar position within phrase / total bar count | `Session.totalBars`, `GeneratedProgression` index | **The single most important addition** — needed to make pattern *vary* across bars at all. See Section 7. |

### 2.3 What Drums should deliberately continue to ignore

- **Individual chord tones/harmony.** Drums has no pitched content: there
  is no meaningful way for kick/snare/hat selection to respond to *which*
  notes are in a chord, only to *when* the chord attacks happen. Harmonic
  content (key, chord quality) is correctly out of scope for Drums
  entirely.
- **Bass's specific pitches.** Per the kick/bass relationship (Section 6),
  Drums only needs Bass's **rhythmic onset positions**, not what notes
  Bass is playing. Reading full Bass `NoteEvent` pitch data would be scope
  creep with no clear use.
- **Direct cross-generator coupling.** Per the same architectural rule
  applied to Bass: `DrumGenerator`/`DrumViewModel` should not directly
  query `BassGenerator`'s internals or call into it. Any kick/bass
  relationship should be read from **already-generated Bass `NoteEvent`
  output** (data, not a live coupling to the generator itself) — the same
  one-directional, data-only pattern this spec recommends for
  chord-density awareness.

---

## 3. Preset vocabulary — confirmed exact current behavior

Restating Phase 1's table for this document's self-containedness:

| Preset | Kick steps | Snare steps | Character |
|---|---|---|---|
| `FOUR_FOUR` | 0, 8 | 4, 12 | Simplest, most neutral — four-on-the-floor-adjacent but sparse (kick only on 1 and 3, not every beat) |
| `ROCK` | 0, 6, 8, 14 | 4, 12 | Busier kick (syncopated pickup notes before 1 and 3), same snare backbeat as `FOUR_FOUR` |
| `FUNK` | 0, 3, 8, 11, 14 | 4, 12 | Busiest kick pattern, added clap layer, 16th-note hi-hats throughout — highest overall density |
| `REGGAE` | 0, 12 | 6, 14 (off-beat, not 4/12) | Sparsest kick, and the **only preset with snare off the standard backbeat** — deliberately distinct character |
| `BOSSA` | 0, 9 | 4, 13 | Ride-dominant (11 of 16 steps), kick/snare comparatively minimal — texture carried by the ride, not kick/snare |

**Confirmed density ordering (by total active steps across all lanes,
informal):** `BOSSA`/`REGGAE` (sparsest, though for different reasons —
Bossa via kick/snare restraint with ride fill, Reggae via genuine sparseness)
< `FOUR_FOUR` < `ROCK` < `FUNK` (busiest).

---

## 4. Preset → context mapping

Same model as Bass Section 3: a **suitability signal**, advisory only, not
a forced selection.

| Preset | Best fits when... |
|---|---|
| `FOUR_FOUR` | Safe default; works with most chord rhythm densities; good starting point when no other signal is strong |
| `ROCK` | Chord rhythm has moderate-to-high energy; pairs well with `LIFT` intent or driving/energetic Bass styles (`GROOVE`, `WALKING`) |
| `FUNK` | Chord rhythm is sparse (leaves room for Funk's high drum density) — direct application of the same negative-space principle from Bass Section 3; also pairs well with a sparse/simple Bass style, since Funk's density is enough activity on its own |
| `REGGAE` | Chord rhythm is off-beat/syncopated (e.g. patterns like `"Reggae Skank"` or `"Ska Upstroke"` already in `StrumPatterns.groups`) — a genuine, direct genre-to-genre pairing opportunity that costs nothing to implement, since the chord pattern names already signal the intended genre |
| `BOSSA` | Chord rhythm matches (e.g. `"Bossa Nova"` pattern already exists in `StrumPatterns.groups`) — same direct name-matching opportunity as Reggae |

**Notable, low-effort opportunity specific to Drums:** unlike Bass (which
has no genre-named styles to match against), several existing
`StrumPatterns.groups` pattern names **already share genre labels with
Drum presets** (`"Reggae Skank"` ↔ `REGGAE`, `"Bossa Nova"` ↔ `BOSSA`).
A simple, high-confidence, low-risk recommendation rule can exist purely
from **string/genre matching**, before any density-based scoring is even
built: *if the selected chord strum pattern's name matches a Drum preset's
genre, recommend that preset with high confidence.* This is worth
flagging as a near-zero-cost early win, separate from the more general
density-based model below.

---

## 5. Chord rhythm → Drum density relationship

Same derivation as Bass Section 4 (`strumPatternId` → active step count),
applied to Drums:

| Chord `attackDensity` | Suggested Drum tendency |
|---|---|
| 0.0 (block chord) | Any preset viable — chord isn't claiming rhythmic space |
| 0.0 – 0.25 (sparse) | Favor `FUNK` (fills the space) or a busier custom pattern — same negative-space logic as Bass |
| 0.25 – 0.5 (moderate) | Favor `ROCK`/`FOUR_FOUR` — balanced |
| 0.5+ (dense strum) | Favor `FOUR_FOUR` or sparser — avoid stacking density on density |

**This mirrors Bass's Section 4 rule almost exactly** — which raises a
genuine open question rather than a settled answer: **should Bass and
Drums both independently reduce density in response to a busy chord, or
should only one of them "give way"?** If both simplify simultaneously, the
overall arrangement may end up feeling thin rather than balanced. This is
explicitly a **Phase 5 question** (cross-track reconciliation), not
resolved here — flagged prominently since it's the most direct point of
overlap between this spec and the Bass spec.

---

## 6. Kick/Bass relationship — data available, scoring deferred

Per the first handoff doc's Section 5 ("Kick/bass interaction... one of
the most important relationships for a future groove system"): this spec
confirms the **raw data needed already exists and is symmetric** —

- Drum kick onsets: confirmed exact step positions per preset (Section 3
  above), convertible to beat positions via `stepDur = 1/4` (16th notes)
  the same way `DrumViewModel.writeToPianoRoll()` already does.
- Bass note onsets: every generated Bass `NoteEvent.beat` value, already
  in beats, no conversion needed.

Both are **already expressed in the same beat-based coordinate system**
(confirmed via Phase 1 Section 7.4 — `NoteEvent` is a uniform contract).
This means a coincidence/near-coincidence measure (Section 9 of the first
handoff doc's proposed metrics) is **computable today from existing data
with no new fields**, purely by comparing two lists of beat positions once
both Bass and Drums have generated their respective `NoteEvent`s.

**Explicitly deferred to Phase 5, not this spec:** the actual *scoring
model* (deliberate reinforcement vs. accidental collision, per the first
handoff doc's Section 5 distinction) requires judgment calls this
single-instrument spec shouldn't make unilaterally — e.g., how close is
"near-coincidence," and is reinforcement always good or only sometimes.
This spec's contribution is confirming the data is ready; Phase 5 owns the
model.

---

## 7. The structural gap: per-bar and per-section variation

This is flagged as the **highest-priority Drum-specific finding**,
separate from (and arguably more urgent than) contextual awareness.

**Confirmed problem:** `writeToPianoRoll()` writes the **same 16-step
pattern, unchanged, for every bar** in the session. A 16-bar progression
gets 16 identical repetitions. There is currently no mechanism for:

- **Fills** — a different pattern in the last bar (or last beat) of a
  phrase, signaling a transition
- **Intensity variation** — e.g. hi-hats dropping out for a bar, or an
  extra kick added, to avoid monotony over a long progression
- **Phrase-ending behavior** — coordinating with a `RESOLVE` intent (see
  Section 8) the way Bass's `PEDAL` recommendation does

**Recommended minimal first step (not full generative variation):**
before attempting fills or intensity curves, the simplest, lowest-risk
improvement is **phrase-boundary awareness for pattern *switching*, not
pattern *generation*** — i.e., if a progression has 2 distinct 4-bar
phrases (per `GeneratedProgression`/`addPhrase()` boundaries, already
confirmed to exist in Phase 1 Section 6), allow **different presets per
phrase**, rather than one preset for the whole session. This requires no
new pattern-generation logic — only extending `writeToPianoRoll()` to
accept multiple `(pattern, barRange)` pairs instead of one pattern +
`totalBars`. This is a real, scoped, achievable Phase 6/7 task, distinct
from (and prerequisite to) true generative fills.

---

## 8. Phrase-level intent awareness

Mirrors Bass Section 6, with Drum-specific mappings:

| Intent | Drum implication |
|---|---|
| `CONTINUE` | Maintain current preset/density if already established |
| `LIFT` | Favor busier presets (`FUNK`, `ROCK`) or (once Section 7's phrase-switching exists) a density increase at the phrase's start |
| `CONTRAST` | A preset change may be appropriate, mirroring Bass's contrast recommendation |
| `RESOLVE` | Favor pattern simplification toward the phrase's end (once per-bar variation exists) — e.g. dropping to just kick+snare in the final bar, a classic "settling" device |
| `SIMPLIFY` | Favor `FOUR_FOUR` or sparser |
| `SURPRISE`, `MODULATE`, `EXPAND` | Same caveat as Bass Section 6 — insufficient evidence yet to propose specific mappings |

---

## 9. UX implications for the Composition Tray

Same principle as Bass Section 8: the 5 existing presets remain visible
and user-selectable exactly as they are today. What changes is only which
preset(s) are visually recommended, using:
1. Genre-name matching (Section 4) — highest confidence, cheapest to
   implement
2. Density-based suggestion (Section 5) — same model as Bass

No new user-facing density/syncopation numbers exposed directly, matching
the project's established discipline.

---

## 10. What this implies for `DrumPattern.kt`/`DrumViewModel.kt` (future, not now)

Restating clearly: **no code changes proposed in this phase.** For Phase
6/7 planning:

- `DrumPattern`/`DrumKit`/`DrumPresets` themselves need **no structural
  change** — the existing grid/lane/preset model is sound and sufficient
  for everything in Sections 3–6.
- The one genuinely structural future change is `writeToPianoRoll()`
  evolving from "one pattern, repeated `totalBars` times" to "a sequence
  of `(pattern, barRange)` pairs" (Section 7) — this is the change with
  the most actual musical impact of anything in this document, more so
  than any context-awareness addition, since it's the only one that
  addresses the repetition problem directly.
- Preset recommendation (Sections 4–5) is purely additive UI/selection
  logic — no change to `DrumPattern`'s data model needed at all.

---

## 11. Open questions for Phase 5

1. **Density reconciliation** (Section 5) — if both Bass and Drums
   independently "give way" to a dense chord rhythm, does the arrangement
   end up too thin? This is the most direct overlap point with the Bass
   spec and should likely be Phase 5's first concrete question.
2. **Kick/Bass coincidence scoring** (Section 6) — data confirmed ready;
   the actual scoring model (reinforcement vs. collision) is undecided.
3. **Whether structural variation (Section 7) belongs in Phase 5 at all**,
   or whether it's better treated as its own, earlier, independent
   implementation task — since it doesn't actually require Bass context
   or cross-track scoring to deliver real value (a Drum-only phrase-switch
   capability is useful even before any Bass-awareness exists). Worth
   deciding explicitly rather than defaulting to "everything waits for
   Phase 5."

---

## 12. Summary

- Drums currently have **zero** contextual awareness of Chord or Bass —
  a larger starting gap than Bass, which at least reads chord roots/tones.
- The **structural repetition gap (Section 7)** — one pattern for the
  whole session — is arguably the single most musically impactful finding
  across both this spec and the Bass spec, and is achievable
  independently of any cross-track scoring work.
- A **near-zero-cost genre-name-matching win** exists today (Section 4:
  `"Reggae Skank"` ↔ `REGGAE`, `"Bossa Nova"` ↔ `BOSSA`) — worth
  prioritizing as an easy first implementation step whenever Phase 6/7
  begins.
- Kick/Bass coincidence data is confirmed ready to compute; the scoring
  model is correctly deferred to Phase 5.
- No `DrumPattern`/`DrumViewModel` code changes proposed in this phase.

**Recommended next step:** Phase 5 — the Groove Relationship Model,
now that both Bass (Phase 3) and Drums (Phase 4) have independently
confirmed, evidence-based specs to reconcile. Section 11's three questions
above are natural starting points.

---

*End of Phase 4 deliverable.*