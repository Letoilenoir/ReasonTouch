# ReasonTouch — Bass Arrangement Specification
## Phase 3 Deliverable: Bass Arrangement Roadmap

**Status:** Draft — Phase 3 of the Bass/Drum Arrangement Roadmap
**Precedes:** Phase 4 (Drum Arrangement Specification), Phase 5 (Groove
Relationship Model)
**Depends on:** `CompositionContext_Specification.md` (Phase 1), specifically
Sections 2, 4, 5, 6 (Harmony, Structure/Strategy, Rhythm, Continuation
Inheritance) and Section 7.1–7.2 (Bass audit).
**Method:** This spec defines *what Bass should know, ignore, and do
differently*, grounded in the confirmed current implementation — not a
rewrite proposal. Per the roadmap's own stated discipline: **no
implementation change in this phase.** Paper only.

**Note on the register fix:** the Bass register discrepancy flagged in
Phase 1 (Section 7.1) has since been resolved — `BASS_MIN_MIDI`/
`BASS_MAX_MIDI` corrected to `28`/`52`, verified acceptable on-device. This
spec treats that as settled; register is not revisited below.

---

## 1. What Bass currently has, restated precisely

From Phase 1's audit (Section 7.2), confirmed facts this spec builds on:

- Bass generation is **entirely bar-local** except `WALKING`, which has a
  **single-bar lookahead** to the next chord's root.
- Bass reads **only** `chord.rootMidi` and `chord.midiNotes` (parsed chord
  tones) — **it reads nothing from CHORD's rhythmic data**
  (`strumPatternId`, `strumSpeedValue`) at all.
- Each of the 7 `BassStyle` values has a **fixed, hardcoded rhythmic shape**
  and **hardcoded velocity/accent profile** (e.g. `GROOVE`'s ghost notes at
  velocity 50–55 vs. strong hits at 90–105) — this is real accent data,
  just not currently exposed or varied by context.
- Style selection today is **entirely user-driven** — `generateBass(style,
  appendMode)` takes the style as a direct parameter from the Bass sheet
  UI; there is no recommendation or context-awareness in the selection
  itself.
- The style **vocabulary itself is good** — the handoff docs' own framing
  (Section 7 of the first doc) is right: these seven styles should remain a
  **pattern vocabulary**, not be replaced.

The central design question for this phase, restated from the docs:
**not** "should Bass read the chord rhythm," but **"what should it do
differently once it can?"**

---

## 2. What Bass should consume from CompositionContext

Grounded directly in Phase 1's confirmed fields — nothing proposed here
requires new data that doesn't already exist somewhere in CHORD's state.

### 2.1 Already consumed today (no change needed)

| Field | Source | Used by |
|---|---|---|
| `chord.rootMidi` | `ChordEvent` | All styles |
| `chord.midiNotes` (parsed) | `ChordEvent` | `ARPEGGIO` |
| Next chord's `rootMidi` | `chords.getOrNull(barIdx + 1)` | `WALKING` only |

### 2.2 Available but not yet consumed — recommended additions

| Field | Source (Phase 1 ref) | Proposed use |
|---|---|---|
| `strumPatternId` → active step count | Spec Section 5 | **Chord rhythmic density** — see Section 4 below |
| `strumSpeedValue` | Spec Section 5 | Whether the chord's own attacks are tight/simultaneous or spread/rolled — informs whether Bass should reinforce or contrast |
| `bpm` | Spec Section 1 | Already available session-wide; not currently passed into `BassGenerator.generate()` at all — **should be**, since tempo affects what rhythmic density actually *sounds* busy vs. sparse (a `GROOVE` pattern at 60bpm reads very differently than at 160bpm) |
| `PairingDecision`/`IntentOption.type` (CONTINUE/LIFT/CONTRAST/RESOLVE/etc.) | Spec Section 4 | **Phrase-level intent** — see Section 6 below. This is the single richest piece of unused context Phase 1 surfaced: CHORD already computes *why* a section exists musically, and Bass currently has no access to that reasoning at all. |
| Bar position within phrase (first bar / last bar of a generated phrase) | Derivable from `GeneratedProgression.chords` index, or `addPhrase()`'s `startBarIndex` | Phrase-boundary awareness — see Section 6 |

### 2.3 What Bass should deliberately continue to ignore

Per the handoff docs' own "what should not happen next" principle (Section
20 of the first doc) and this spec's own judgment:

- **Drum data.** Bass should not read Drum `NoteEvent`s directly. Per the
  architectural rule already established (`BassGenerator` should not need
  to understand `DrumGenerator`): any kick/bass coincidence-awareness
  belongs in a future arrangement/evaluation layer (Phase 5), not inside
  `BassGenerator` itself.
- **Individual chord-tone *roles*** (which note is the third vs. the
  seventh). `ARPEGGIO` already works from a sorted, deduplicated tone list
  without needing role labels — adding music-theory role classification
  is a real capability gap, but not one Bass strictly needs yet; flagged as
  a possible Phase 3.5 addition only if a specific style design later
  requires it (e.g. a style that always leads with the third).
- **Detected key / `HarmonyState`.** Bass currently works purely from
  chord-to-chord root/tone relationships. Full key-awareness (e.g. "prefer
  scale tones from the detected key on passing notes") is a real future
  enhancement, but it's a **voice-leading-adjacent** concern — deliberately
  deferred to Section 7 below, not folded into this phase.

---

## 3. Style vocabulary → context mapping

This is the core deliverable the roadmap asks for: **when should each style
be appropriate**, not "replace the styles."

The recommended model: each style gets a **suitability signal**, computed
from context, used to *recommend* (not force) a style in the UI — matching
the project's stated "ranked option lists, not single winners" philosophy
(per the ReasonTouch identity principle already established:
*"If we impose a decision on a user, we are effectively building the
composition for them"*).

| Style | Best fits when... | Chord rhythm context |
|---|---|---|
| `ROOT` | Simplest option; safe default, especially for sparse/ambient material | Any — genuinely context-agnostic, good fallback |
| `ROOT_FIFTH` | Moderate chord activity; wants harmonic reinforcement without density | Works well against sustained/low-strum-density chords |
| `OCTAVE` | Wants rhythmic interest without adding new harmonic information | Similar to `ROOT_FIFTH`'s rhythmic slot |
| `WALKING` | Chord changes are frequent enough to give walking bass somewhere to go; works best with **CONTINUE** or **RESOLVE** intent (smooth linear motion) | Best with low-to-moderate chord strum density — walking bass wants its own rhythmic space |
| `ARPEGGIO` | Chord has 3+ distinct tones (thin voicings give arpeggio little to work with); good with sparse chord rhythm since arpeggio itself fills space | Best when chord strum density is **low** — otherwise two "filling" parts compete |
| `GROOVE` | Chord rhythm is sparse/sustained and needs rhythmic energy from somewhere; good with **LIFT** or high-energy intent | Best when chord strum density is **low-to-moderate** — GROOVE supplies the syncopation the chords aren't |
| `PEDAL` | Chord is harmonically static or the section wants to feel "held"; good with **RESOLVE** (a pedal into a resolution is a classic device) or a deliberately sparse/ambient section | Works regardless of chord rhythm — by design, PEDAL doesn't compete rhythmically, it sits underneath |

**Key relationship pattern, stated once rather than repeated per row:**
*High chord-rhythm density (busy strumming) generally favors simpler,
sparser Bass (`ROOT`, `PEDAL`, `ROOT_FIFTH`). Low chord-rhythm density
(sparse/sustained chords) creates room for busier Bass (`GROOVE`,
`ARPEGGIO`, `WALKING`).* This is the "negative space" principle from the
first handoff doc (Section 4) applied concretely to the actual style
vocabulary that exists today.

---

## 4. Chord rhythm → Bass rhythm: a concrete, implementable relationship

Per Phase 1 Section 5, **chord rhythmic density is already derivable
today** with a one-line calculation:

```
attackDensity = strumPatternId.toStepPattern().steps.count { it != StepState.OFF } / 16f
```

Recommended concrete rule set (a starting proposal, not final — Phase 5
will formalize scoring across all three tracks together):

| Chord `attackDensity` | Suggested Bass tendency |
|---|---|
| 0.0 (block chord / empty pattern) | Any Bass style viable — chord isn't claiming rhythmic space at all |
| 0.0 – 0.25 (sparse strum) | Favor `GROOVE`, `ARPEGGIO`, `WALKING` — chord has left room |
| 0.25 – 0.5 (moderate) | Favor `ROOT_FIFTH`, `OCTAVE` — some rhythmic interest, not competing |
| 0.5+ (dense strum) | Favor `ROOT`, `PEDAL` — chord is already busy, Bass should anchor rather than add |

This is presented as **guidance strength, not a hard constraint** — per the
"choice, not imposition" principle, this should surface as a **recommended/
highlighted style** in the Bass sheet UI (Section 8 below), not a
restriction that removes other options.

---

## 5. Exposing existing accent/velocity data

Phase 1 confirmed each style already carries real, hardcoded accent data
(e.g. `GROOVE`'s alternating strong/ghost velocities). This is a genuine
asset, currently invisible to any evaluation layer. Recommended for Phase 6
(implementation): when `BassGenerator` produces `NoteEvent`s, this velocity
data should remain queryable/inspectable by a future groove-evaluation
layer (Phase 5) without needing `BassGenerator` itself to change — the data
already exists in the output `NoteEvent.velocity` field. **No generator
change needed for this; it's an evaluation-layer consumption point, noted
here so Phase 5 doesn't reinvent it.**

---

## 6. Phrase-level intent awareness

This is the richest opportunity Phase 1 surfaced (Spec Section 4): CHORD's
`SuggestionWorkflow` already computes **why** a section of music exists —
`CONTINUE`, `LIFT`, `CONTRAST`, `RESOLVE`, `EXPAND`, `SURPRISE`, `SIMPLIFY`,
`MODULATE` — with confidence scores, and Bass currently has zero access to
any of it.

Recommended mapping (starting proposal):

| Intent | Bass implication |
|---|---|
| `CONTINUE` | Maintain current Bass style/character if one is already established for the progression; don't introduce a new style mid-continuation without reason |
| `LIFT` | Favor busier/higher-energy styles (`GROOVE`, `WALKING`) — matches the intent's own "increase energy" purpose |
| `CONTRAST` | A deliberate style *change* from whatever preceded it may be appropriate — contrast in harmony can be reinforced by contrast in bass character |
| `RESOLVE` | Favor `PEDAL` or `ROOT` in the final bar(s) — a settling, anchoring bass motion supports harmonic resolution |
| `SIMPLIFY` | Favor `ROOT`/`PEDAL` — matches intent directly |
| `SURPRISE`, `MODULATE`, `EXPAND` | Not enough evidence yet to propose specific mappings — per Phase 1's finding that these strategies' implementation completeness wasn't verified, defer concrete Bass rules until each strategy's own behavior is better understood |

**Important scoping note:** per Section 1 above, this requires threading
`PairingDecision`/`IntentOption.type` into whatever calls `BassGenerator` —
currently `generateBass()` has no such input. This is a real, if small,
interface change for Phase 6/7, not something achievable by changing
`BassGenerator`'s internals alone.

---

## 7. Voice-leading — explicitly deferred, not forgotten

Per Phase 1's Section 9 concern (`bassRegister()` normalizes every pitch
independently, with no awareness of the *previous* bass note's actual
pitch), this spec **does not** propose changing that now. Restated from the
handoff doc's own instruction: this should **not** trigger an immediate
rewrite.

**Recommended framing for later:** true voice-leading (choosing the
*nearest* octave-transposition of a target note relative to the previous
bass note, not just snapping into a fixed register) is a **Phase 7-or-later
concern** — it changes `bassRegister()`'s signature (needs to know the
*previous* note, not just the target), which is a bigger structural change
than anything else in this spec. Flagging its existence here so it isn't
lost, but explicitly out of Phase 3's scope.

---

## 8. UX implications for the Composition Tray

Per the established "simple choices, hidden complexity" principle (Section
19 of the second handoff doc): the user continues to see the existing 7
named styles. What changes is *only* which one(s) are visually
recommended/highlighted, based on Sections 3–4 above — e.g., a subtle
"Suggested" badge on 1–2 styles when the Bass sheet opens, computed from
current chord rhythm density and (once threaded through) phrase intent.

No new user-facing concepts (density scores, syncopation numbers,
intent labels) should be exposed directly — matching the project's
existing discipline of hiding technical parameters behind musical language.

---

## 9. What this implies for `BassGenerator.kt` (future, not now)

Restating clearly: **no code changes are proposed in this phase.** For
Phase 6/7 planning purposes, the eventual interface change is small and
additive:

```
generate(
    chords, style, targetTrackId, beatsPerBar, appendOffset, snapValue,
    // proposed additions:
    bpm: Int,                              // for density-vs-tempo perception
    phraseIntent: PairingType? = null      // optional; null = today's behavior unchanged
)
```

The 7 style-generator functions themselves (`generateRoot`,
`generateWalking`, etc.) do **not** need to change at all under this
proposal — the new context only affects **which style gets suggested**,
not how any individual style generates once chosen. This keeps the change
genuinely incremental, consistent with the roadmap's Section 20 warning
against "replacing all Bass styles."

---

## 10. Open questions for Phase 5 (Groove Relationship Model)

Deliberately left unresolved here, since they require Bass *and* Drum
context together:

1. Should Bass's context-driven style *suggestion* ever become automatic
   selection (no user choice), or should it always remain advisory? This
   spec assumes **always advisory**, per the project's stated philosophy —
   worth confirming explicitly before Phase 5 builds on that assumption.
2. How should the density-based suggestion (Section 4) and the
   intent-based suggestion (Section 6) be reconciled when they disagree
   (e.g. dense chord rhythm suggests `PEDAL`, but `LIFT` intent suggests
   `GROOVE`)? Not resolved here — flagged as exactly the kind of
   cross-signal scoring question Phase 5 exists to answer.
3. Kick/Bass coincidence scoring (Section 5 of the first handoff doc)
   cannot be assessed until Phase 4's Drum audit-equivalent work
   (already done, per Section 7.3 of the CompositionContext spec) is
   combined with this spec's Bass findings — a natural Phase 5 starting
   point, since both halves' raw data are now confirmed.

---

## 11. Summary

- Bass's existing 7-style vocabulary is sound and should not be replaced.
- The single highest-value, lowest-risk addition is **chord rhythmic
  density awareness** (Section 4) — the data already exists, the
  calculation is trivial, and it directly addresses the "sustained block
  chords assumption" gap both handoff docs flagged.
- The single richest *unused* context is **phrase intent** (Section 6) —
  CHORD already reasons about musical purpose; Bass has no access to it
  yet.
- Voice-leading (Section 7) is real but explicitly out of scope for this
  phase.
- No `BassGenerator` code changes are proposed now; Section 9 describes
  the eventual, deliberately small interface change for when Phase 6/7
  implementation begins.

**Recommended next step:** Phase 4 — the equivalent Drum Arrangement
Specification, building on Phase 1's Drum audit (Section 7.3 of the
CompositionContext spec) the same way this document built on Phase 1's
Bass audit.

---

*End of Phase 3 deliverable.*