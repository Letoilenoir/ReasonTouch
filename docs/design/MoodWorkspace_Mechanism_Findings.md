# ReasonTouch -- MoodWorkspace Mechanism Findings

**Date:** 2026-08-02
**Status:** Investigation only -- confirms current mechanism, does not propose a fix. See `docs/Task_List.md` for the associated scoped task ("Mood as a persistent generative motif").

---

## What was asked

MoodWorkspace offers 6 named mood presets (Dark, Uplifting, Cinematic, Ambient, Energetic, Melancholic). Currently these only influence the *initial seed* generation. Question raised: how would the mood established by one of these presets persist as an ongoing creative motif once SUGGEST starts generating further phrases -- since SUGGEST currently has no way to know which mood a progression was seeded with?

## Confirmed mechanism (via direct source inspection, `MoodWorkspace.kt`)

All 6 presets collapse to a single `Float` value on one axis, `moodBias` (range roughly -1..+1):

```kotlin
Mood("Dark",        "Minor keys, melancholic", "moon",     -0.8f),
Mood("Uplifting",   "Major keys, hopeful",     "sun",       0.8f),
Mood("Cinematic",   "Epic, dramatic",          "clapper",   0.3f),
Mood("Ambient",     "Atmospheric, sparse",     "cloud",     0.0f),
Mood("Energetic",   "Fast, driving",           "lightning", 0.6f),
Mood("Melancholic", "Contemplative, sad",      "brokenheart",-0.6f)
```

Selecting a preset sets `moodBiasFine` (a separate, further user-adjustable float) to that preset's value:
```kotlin
moodBiasFine = mood.moodBias
```

`moodBiasFine` is then passed onward as `harmonyBias`:
```kotlin
harmonyBias = moodBiasFine,
```

There is also a manual fine-tune control (`MoodBiasTuner` composable) that lets the user nudge `moodBiasFine` directly, independent of the 6 presets, with descriptive labels at different thresholds (e.g. "Strongly minor -- very dark and introspective" below -0.5, "Leaning minor -- predominantly dark" below -0.1).

**`harmonyBias`'s actual consumer has not yet been located or inspected.** Presumed to feed into seed-generation chord selection (likely biasing major/minor quality choice), but this has not been confirmed by reading the consuming code.

## What this confirms

1. **Mood is currently one-dimensional.** All 6 presets are just different points on a single major-minor-leaning axis (-1 to +1). There is no second parameter distinguishing, for example, "epic and dramatic" (Cinematic, 0.3) from "fast and driving" (Energetic, 0.6) beyond their position on this one dial -- despite both being on the "bright" side of the axis and clearly intended to feel different from each other and from Uplifting (0.8).
2. **Mood does not persist anywhere past generation time.** `harmonyBias`/`moodBiasFine` is UI state local to `MoodWorkspace`'s composable (`var moodBiasFine by remember { mutableStateOf(0f) }`). It is not visible in `ProgressionGenerationRequest`, `ProgressionAnalysis`, or any of the three phrase-generation strategies reviewed so far. Nothing suggests it is persisted to `Session`, `ChordEvent`, or any other durable store.
3. **Consequence:** once a Mood-seeded progression reaches SUGGEST, the harmonic reasoning layer (`PairingEngine`, `ProgressionAnalyzer`, `ChordSuggestionEngine`, the 3 strategies) has no way to know or continue the mood the seed was generated with. It reasons purely from harmonic shape (key, cadence, stability, tension, functional sequence) -- concepts that are largely mood-agnostic and could plausibly be identical for two seeds generated under very different moods.

## Two separable problems, not one

- **Persistence problem:** does the mood value chosen at seed time need to be remembered anywhere the composer can consult it later (e.g. a field on `Session` or `ProgressionGenerationRequest`)? Currently: no, it evaporates once the seed is generated.
- **Expressiveness problem:** even if persisted, a single scalar cannot distinguish all 6 named presets from each other with fidelity -- genuinely honoring "Cinematic" as an ongoing motif (vs. "Uplifting" or "Energetic", which share similar or higher bias values) may require the mood model itself to gain more than one axis, not just a memory of the one axis it already has.

## Not yet done

- Locate and inspect `harmonyBias`'s actual consumer to confirm what it currently does mechanically at generation time.
- Determine whether `Session` or any persisted entity already has an unused/adjacent field that could carry a mood value forward, or whether this needs a new field.
- Decide whether solving the expressiveness problem is in scope alongside persistence, or whether persistence of the existing single axis is a worthwhile incremental step on its own.

---

*End of findings note.*