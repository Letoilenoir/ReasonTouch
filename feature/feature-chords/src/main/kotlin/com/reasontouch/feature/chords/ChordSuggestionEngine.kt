package com.reasontouch.feature.chords


data class ChordSuggestion(

    val chord: TheoryChord,

    val degree: Int,

    val function: HarmonicFunction,

    /**
     * Relative confidence (0.0–1.0).
     *
     * This reflects how naturally this chord follows the
     * current harmonic context according to the music theory
     * rules used by the suggestion engine.
     */
    val confidence: Float,

    /**
     * Short explanation shown to the user.
     */
    val description: String,

    val isBorrowed: Boolean = false
)

data class BorrowedChord(
    val chord:       TheoryChord,
    val symbol:      String,
    val description: String
)

object ChordSuggestionEngine {

    private const val MAX_SUGGESTIONS = 4

    /**
     * There are exactly 7 diatonic chords in any key -- this is the true
     * ceiling for requesting the full pool, not a guess. Phrase-generation
     * strategies (Continue/Contrast/Resolve) pass this so their own
     * function-filtering and same-continuation exclusion logic always has
     * every matching diatonic candidate available, rather than being
     * silently starved by MAX_SUGGESTIONS' default curation for the
     * single-chord manual suggestion UI. See docs/handoffs -- the cap was
     * confirmed (by hand-traced weight math) to let exactly one TONIC-
     * function candidate survive when the "last chord" is itself TONIC-
     * function, because TRANSITION_WEIGHTS has no explicit TONIC->TONIC
     * entry and degree-weighting only breaks the resulting 4-way tie in
     * favour of degree 1.
     */
    const val FULL_DIATONIC_POOL = 7

    fun suggest(
        key:           KeyCandidate,
        lastChordName: String?,
        maxResults:    Int = MAX_SUGGESTIONS
    ): List<ChordSuggestion> {
        val diatonic     = MusicTheory.diatonicChords(key.root, key.isMinor)
        val lastTheory   = lastChordName?.let { MusicTheory.parseChordName(it) }
        val lastDegree   = lastTheory?.let {
            MusicTheory.degreeOf(it, key.root, key.isMinor)
        }
        val lastFunction = lastDegree?.let {
            MusicTheory.function(it, key.isMinor)
        } ?: HarmonicFunction.TONIC

        val nextFunctions = MusicTheory.TRANSITIONS[lastFunction]
            ?: listOf(HarmonicFunction.PREDOMINANT, HarmonicFunction.DOMINANT)

        val candidates = mutableListOf<Pair<ChordSuggestion, Float>>()

        diatonic.forEachIndexed { idx, chord ->
            val degree   = idx + 1
            val function = MusicTheory.function(degree, key.isMinor)

            if (function in nextFunctions) {
                val transitionWeights = MusicTheory.TRANSITION_WEIGHTS[lastFunction]
                    ?: emptyMap()
                val baseWeight   = transitionWeights[function] ?: 0.5f
                // Reduced from (1.3/1.2/1.1) so degree-1/4/5 remain favoured
                // for single-chord suggestions without erasing other diatonic
                // options (needed for phrase-level planning, e.g. Continue).
                val degreeWeight = when (degree) {
                    1 -> 1.1f; 5 -> 1.05f; 4 -> 1.03f; else -> 1.0f
                }
                val weight    = baseWeight * degreeWeight
                val roman     = romanNumeral(degree, key.isMinor, chord.quality)
                val funcLabel = function.name.lowercase()
                    .replaceFirstChar { it.uppercase() }
                candidates.add(
                    ChordSuggestion(
                        chord = chord,
                        degree = degree,
                        function = function,
                        confidence = weight.coerceIn(0f, 1f),
                        description = "$roman — $funcLabel"
                    ) to weight
                )
            }
        }

        return candidates
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.degree }
            .take(maxResults)

    }

    fun suggestAll(key: KeyCandidate): List<ChordSuggestion> {
        val diatonic = MusicTheory.diatonicChords(key.root, key.isMinor)
        return diatonic.mapIndexed { idx, chord ->
            val degree    = idx + 1
            val function  = MusicTheory.function(degree, key.isMinor)
            val roman     = romanNumeral(degree, key.isMinor, chord.quality)
            val funcLabel = function.name.lowercase().replaceFirstChar { it.uppercase() }
            ChordSuggestion(
                chord = chord,
                degree = degree,
                function = function,
                confidence = 1.0f,
                description = "$roman — $funcLabel"
            )
        }
    }

    /**
     * Borrowed chords from the parallel key.
     *
     * Major key — borrows from parallel minor:
     *   bVII  flat seventh  (e.g. Bb in C Major) — bold, anthemic
     *   iv    minor fourth  (e.g. Fm in C Major) — dark, emotional
     *   bVI   flat sixth    (e.g. Ab in C Major) — lush, cinematic
     *
     * Minor key — borrows from parallel major:
     *   V     major dominant (e.g. E in Am)      — strong resolution
     *   IV    major fourth   (e.g. D in Am)      — brighter feel
     *   I     major tonic    (e.g. C in Am)      — Picardy third
     */
    fun borrowedChords(key: KeyCandidate): List<BorrowedChord> {
        val result = mutableListOf<BorrowedChord>()
        val root   = key.root

        if (!key.isMinor) {
            val minorDiatonic = MusicTheory.diatonicChords(root, isMinor = true)
            result.add(BorrowedChord(
                chord       = minorDiatonic[6],
                symbol      = "\u266DVII",
                description = "Parallel minor — bold, anthemic"
            ))
            result.add(BorrowedChord(
                chord       = minorDiatonic[3],
                symbol      = "iv",
                description = "Parallel minor — dark, emotional"
            ))
            result.add(BorrowedChord(
                chord       = minorDiatonic[5],
                symbol      = "\u266DVI",
                description = "Parallel minor — lush, cinematic"
            ))
        } else {
            val majorDiatonic = MusicTheory.diatonicChords(root, isMinor = false)
            result.add(BorrowedChord(
                chord       = majorDiatonic[4],
                symbol      = "V",
                description = "Parallel major — strong resolution"
            ))
            result.add(BorrowedChord(
                chord       = majorDiatonic[3],
                symbol      = "IV",
                description = "Parallel major — brighter feel"
            ))
            result.add(BorrowedChord(
                chord       = majorDiatonic[0],
                symbol      = "I",
                description = "Picardy third — major tonic"
            ))
        }
        return result
    }

    private fun romanNumeral(degree: Int, isMinor: Boolean, quality: ChordQuality): String {
        val base = listOf("I","II","III","IV","V","VI","VII")[degree - 1]
        return when (quality) {
            ChordQuality.MIN, ChordQuality.MIN7 -> base.lowercase()
            ChordQuality.DIM                    -> "${base.lowercase()}\u00B0"
            else                                -> base
        }
    }
}