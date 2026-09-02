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
         *
         * Returns a RANKED LIST, not a single style -- per the spec's own
         * table, each density bucket maps to multiple viable styles, and
         * per the project's "choice, not imposition" principle, this
         * function suggests, it never decides. The caller (Phase 8's
         * Bass sheet UI) is responsible for how it surfaces the ranking
         * (e.g. a "Suggested" badge on the top 1-2 entries) -- all 7
         * styles always remain independently selectable regardless of
         * this ranking.
         *
         * attackDensity of exactly 0f (block chord / empty pattern)
         * returns every style in their declared order, since the spec's
         * table treats this case as "any style viable" -- ROOT leads
         * that list as the documented safe default (Section 3), not
         * because 0f density specifically favors it.
         */
        fun suggestForDensity(attackDensity: Float): List<BassStyle> = when {
            attackDensity <= 0f   -> values().toList()
            attackDensity < 0.25f -> listOf(GROOVE, ARPEGGIO, WALKING)
            attackDensity < 0.5f  -> listOf(ROOT_FIFTH, OCTAVE)
            else                  -> listOf(ROOT, PEDAL)
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