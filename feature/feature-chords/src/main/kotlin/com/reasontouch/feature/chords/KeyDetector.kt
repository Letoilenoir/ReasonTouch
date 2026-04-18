package com.reasontouch.feature.chords

/**
 * Detects likely keys from a list of chord names.
 *
 * Scoring weights (from revised scope):
 *   matchCount     * 1.0   — each diatonic chord match
 *   tonicBonus     * 1.5   — I or i present
 *   dominantBonus  * 1.2   — V present
 *   lastChordBonus * 1.3   — last chord is tonic of this key
 *
 * Returns top 3 ranked KeyCandidates.
 */
object KeyDetector {

    private const val MATCH_WEIGHT     = 1.0f
    private const val TONIC_WEIGHT     = 1.5f
    private const val DOMINANT_WEIGHT  = 1.2f
    private const val LAST_CHORD_WEIGHT= 1.3f
    private const val TOP_N            = 3

    fun detect(chordNames: List<String>): List<KeyCandidate> {
        if (chordNames.isEmpty()) return emptyList()

        // Parse chord names into TheoryChord objects — skip unparseable
        val parsed = chordNames.mapNotNull { MusicTheory.parseChordName(it) }
        if (parsed.isEmpty()) return emptyList()

        val lastChord = parsed.last()

        val candidates = MusicTheory.ALL_KEYS.mapNotNull { (root, isMinor) ->
            val diatonic = MusicTheory.diatonicChords(root, isMinor)

            var score = 0f
            var matchCount = 0

            parsed.forEach { chord ->
                val degree = MusicTheory.degreeOf(chord, root, isMinor)
                if (degree != null) {
                    score += MATCH_WEIGHT
                    matchCount++
                    // Tonic bonus — degree 1
                    if (degree == 1) score += TONIC_WEIGHT
                    // Dominant bonus — degree 5
                    if (degree == 5) score += DOMINANT_WEIGHT
                }
            }

            // Last chord bonus — if it's the tonic of this key
            if (MusicTheory.degreeOf(lastChord, root, isMinor) == 1) {
                score += LAST_CHORD_WEIGHT
            }

            if (matchCount == 0) return@mapNotNull null

            // Confidence = proportion of input chords that are diatonic
            val confidence = matchCount.toFloat() / parsed.size.toFloat()

            KeyCandidate(
                root       = root,
                isMinor    = isMinor,
                score      = score,
                confidence = confidence,
                label      = MusicTheory.keyLabel(root, isMinor)
            )
        }

        return candidates
            .sortedByDescending { it.score }
            .take(TOP_N)
    }
}