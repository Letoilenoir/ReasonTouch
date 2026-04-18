package com.reasontouch.feature.chords

/**
 * Generates next chord suggestions given a selected key and the last
 * chord in the progression.
 *
 * Uses functional harmony transition rules:
 *   Tonic       → Predominant or Dominant
 *   Predominant → Dominant
 *   Dominant    → Tonic (strong) or Predominant (deceptive)
 *
 * Returns up to 4 suggestions, weighted by musical probability.
 * Each suggestion includes the TheoryChord and a descriptive label.
 */

data class ChordSuggestion(
    val chord:       TheoryChord,
    val degree:      Int,           // scale degree 1-7
    val function:    HarmonicFunction,
    val description: String         // e.g. "IV — Predominant", "V — Dominant (strong)"
)

object ChordSuggestionEngine {

    private const val MAX_SUGGESTIONS = 4

    fun suggest(
        key:       KeyCandidate,
        lastChordName: String?
    ): List<ChordSuggestion> {
        val diatonic     = MusicTheory.diatonicChords(key.root, key.isMinor)
        val lastTheory   = lastChordName?.let { MusicTheory.parseChordName(it) }
        val lastDegree   = lastTheory?.let {
            MusicTheory.degreeOf(it, key.root, key.isMinor)
        }
        val lastFunction = lastDegree?.let {
            MusicTheory.function(it, key.isMinor)
        } ?: HarmonicFunction.TONIC  // default to tonic if unknown

        // Determine valid next functions based on transitions
        val nextFunctions = MusicTheory.TRANSITIONS[lastFunction]
            ?: listOf(HarmonicFunction.PREDOMINANT, HarmonicFunction.DOMINANT)

        // Collect candidate chords by function, weighted
        val candidates = mutableListOf<Pair<ChordSuggestion, Float>>()

        diatonic.forEachIndexed { idx, chord ->
            val degree   = idx + 1
            val function = MusicTheory.function(degree, key.isMinor)

            if (function in nextFunctions) {
                // Weight from transition table
                val transitionWeights = MusicTheory.TRANSITION_WEIGHTS[lastFunction] ?: emptyMap()
                val baseWeight = transitionWeights[function] ?: 0.5f

                // Tonic degree gets extra weight for strong resolution feel
                val degreeWeight = when (degree) {
                    1    -> 1.3f  // tonic
                    5    -> 1.2f  // dominant
                    4    -> 1.1f  // subdominant
                    else -> 1.0f
                }

                val weight = baseWeight * degreeWeight

                val romanNumeral = romanNumeral(degree, key.isMinor, chord.quality)
                val funcLabel    = function.name.lowercase()
                    .replaceFirstChar { it.uppercase() }
                val description  = "$romanNumeral — $funcLabel"

                candidates.add(
                    ChordSuggestion(chord, degree, function, description) to weight
                )
            }
        }

        // Sort by weight, take top N, deduplicate
        return candidates
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.degree }
            .take(MAX_SUGGESTIONS)
    }

    /** Also generate "all diatonic" suggestions for when no last chord exists */
    fun suggestAll(key: KeyCandidate): List<ChordSuggestion> {
        val diatonic = MusicTheory.diatonicChords(key.root, key.isMinor)
        return diatonic.mapIndexed { idx, chord ->
            val degree   = idx + 1
            val function = MusicTheory.function(degree, key.isMinor)
            val roman    = romanNumeral(degree, key.isMinor, chord.quality)
            val funcLabel = function.name.lowercase()
                .replaceFirstChar { it.uppercase() }
            ChordSuggestion(chord, degree, function, "$roman — $funcLabel")
        }
    }

    private fun romanNumeral(degree: Int, isMinor: Boolean, quality: ChordQuality): String {
        val base = listOf("I","II","III","IV","V","VI","VII")[degree - 1]
        return when (quality) {
            ChordQuality.MIN, ChordQuality.MIN7 -> base.lowercase()
            ChordQuality.DIM                    -> "${base.lowercase()}°"
            else                                -> base
        }
    }
}