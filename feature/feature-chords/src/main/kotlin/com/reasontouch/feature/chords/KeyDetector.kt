package com.reasontouch.feature.chords

/**
 * Detects likely keys from a list of chord names.
 *
 * Scoring weights:
 *   matchCount     * 1.0   — each diatonic chord match
 *   tonicBonus     * 1.5   — I or i present
 *   dominantBonus  * 1.2   — V present
 *   lastChordBonus * 1.3   — last chord is tonic of this key
 *
 * moodBias: -1.0 = strongly prefer minor (dark)
 *            0.0 = neutral
 *           +1.0 = strongly prefer major (bright)
 *
 * Returns top 3 ranked KeyCandidates.
 */
object KeyDetector {

    private const val MATCH_WEIGHT      = 1.0f
    private const val TONIC_WEIGHT      = 1.5f
    private const val DOMINANT_WEIGHT   = 1.2f
    private const val LAST_CHORD_WEIGHT = 1.3f
    private const val MOOD_MAX_BONUS    = 1.8f  // max multiplier from mood bias
    private const val TOP_N             = 3

    fun detect(
        chordNames: List<String>,
        moodBias:   Float = 0f    // -1.0 dark/minor .. 0 neutral .. +1.0 bright/major
    ): List<KeyCandidate> {
        if (chordNames.isEmpty()) return emptyList()

        val parsed = chordNames.mapNotNull { MusicTheory.parseChordName(it) }
        if (parsed.isEmpty()) return emptyList()

        val lastChord  = parsed.last()
        val clampedBias = moodBias.coerceIn(-1f, 1f)

        val candidates = MusicTheory.ALL_KEYS.mapNotNull { (root, isMinor) ->
            val diatonic = MusicTheory.diatonicChords(root, isMinor)

            var score      = 0f
            var matchCount = 0

            parsed.forEach { chord ->
                val degree = MusicTheory.degreeOf(chord, root, isMinor)
                if (degree != null) {
                    score += MATCH_WEIGHT
                    matchCount++
                    if (degree == 1) score += TONIC_WEIGHT
                    if (degree == 5) score += DOMINANT_WEIGHT
                }
            }

            if (MusicTheory.degreeOf(lastChord, root, isMinor) == 1) {
                score += LAST_CHORD_WEIGHT
            }

            if (matchCount == 0) return@mapNotNull null

            // Apply mood bias — positive bias boosts major, negative boosts minor
            val moodMultiplier = when {
                clampedBias > 0f && !isMinor ->
                    1f + clampedBias * (MOOD_MAX_BONUS - 1f)
                clampedBias < 0f && isMinor  ->
                    1f + (-clampedBias) * (MOOD_MAX_BONUS - 1f)
                clampedBias > 0f && isMinor  ->
                    1f - clampedBias * 0.4f   // slight penalty for opposing mode
                clampedBias < 0f && !isMinor ->
                    1f - (-clampedBias) * 0.4f
                else -> 1f
            }.coerceAtLeast(0.1f)

            score *= moodMultiplier

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