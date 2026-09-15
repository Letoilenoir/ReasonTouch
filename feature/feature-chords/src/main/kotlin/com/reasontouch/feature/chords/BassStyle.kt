package com.reasontouch.feature.chords

enum class BassStyle(val label: String, val description: String) {
    ROOT(
        "Root",
        "One note per bar on the root"
    ),
    ROOT_FIFTH(
        "Root + 5th",
        "Root on beat 1, fifth on beat 3"
    ),
    OCTAVE(
        "Octave",
        "Root with octave jump on beat 3"
    ),
    WALKING(
        "Walking Bass",
        "Chromatic approach notes between roots"
    ),
    ARPEGGIO(
        "Arpeggio",
        "Chord tones in sequence"
    ),
    GROOVE(
        "Groove",
        "Syncopated root pattern"
    ),
    PEDAL(
        "Pedal",
        "Sustained root throughout"
    );

    companion object {
        /**
         * Phase 7a of the Bass/Drum Arrangement Roadmap: ranks Bass styles
         * by fit against a chord's rhythmic attack density, per
         * docs/design/Bass_Arrangement_Specification.md Section 4.
         * ...
         */
        fun suggestForDensity(attackDensity: Float): List<BassStyle> = when {
            attackDensity <= 0f   -> values().toList()
            attackDensity < 0.25f -> listOf(GROOVE, ARPEGGIO, WALKING)
            attackDensity < 0.5f  -> listOf(ROOT_FIFTH, OCTAVE)
            else                  -> listOf(ROOT, PEDAL)
        }

        /**
         * Same ranking as suggestForDensity(), with a one-line rationale
         * attached per item -- added 2026-09-07 for Assisted mode's Full
         * Groove panel (GUIDED does not get its own branch; see memory
         * note 2026-09-07 -- explaining suggestions is not Guided-exclusive).
         * Thin wrapper: does not re-rank anything, so it can never drift
         * from suggestForDensity()'s ordering.
         */
        fun suggestForDensityWithRationale(attackDensity: Float): List<RankedSuggestion<BassStyle>> =
            suggestForDensity(attackDensity).map { style ->
                RankedSuggestion(style, rationaleFor(style, attackDensity))
            }

        private fun rationaleFor(style: BassStyle, attackDensity: Float): String = when {
            attackDensity <= 0f ->
                "No strum pattern set — ${style.label} works well as a starting point."
            attackDensity < 0.25f ->
                "The chord rhythm is sparse, leaving room for ${style.label}'s movement."
            attackDensity < 0.5f ->
                "Moderate chord activity suits ${style.label}'s balance of interest and space."
            else ->
                "The chord rhythm is busy, so ${style.label} keeps the arrangement clear."
        }

        /**
         * Phase 7c of the Bass/Drum Arrangement Roadmap: ranks Bass styles by
         * phrase intent, per docs/design/Bass_Arrangement_Specification.md
         * Section 6. Deliberately returned SEPARATELY from suggestForDensity(),
         * not merged -- per the project's "choice, not imposition" principle,
         * reconciling two disagreeing signals is left to the person composing,
         * not decided in code (Bass spec Section 10, open question 2 -- resolved
         * by not resolving it programmatically at all).
         *
         * Returns an empty list for intents with no established mapping yet
         * (SURPRISE, MODULATE, EXPAND -- see spec Section 6), rather than
         * guessing.
         */
        fun suggestForIntent(intent: PairingType): List<BassStyle> = when (intent) {
            PairingType.CONTINUE -> emptyList() // caller should preserve existing style; no new suggestion
            PairingType.LIFT     -> listOf(GROOVE, WALKING)
            PairingType.CONTRAST -> emptyList() // signals "consider changing," not a specific target
            PairingType.RESOLVE  -> listOf(PEDAL, ROOT)
            PairingType.SIMPLIFY -> listOf(ROOT, PEDAL)
            else                 -> emptyList()
        }
    }
}