package com.reasontouch.feature.chords.pairing

import com.reasontouch.feature.chords.KeyCandidate
import com.reasontouch.feature.chords.MusicTheory
import com.reasontouch.feature.chords.TheoryChord

class ProgressionPairingEngine {


    private val templates = listOf(

        ProgressionPair(
            title = "Lift",
            description = "Strong chorus energy",
            type = PairingType.VERSE_TO_CHORUS,
            degrees = listOf(1, 5, 6, 4)
        ),

        ProgressionPair(
            title = "Emotional",
            description = "Modern pop movement",
            type = PairingType.VERSE_TO_CHORUS,
            degrees = listOf(6, 4, 1, 5)
        ),

        ProgressionPair(
            title = "Reflective",
            description = "Gentle continuation",
            type = PairingType.VERSE_TO_BRIDGE,
            degrees = listOf(2, 5, 1, 6)
        ),

        ProgressionPair(
            title = "Cinematic",
            description = "Expansive movement",
            type = PairingType.VERSE_TO_CHORUS,
            degrees = listOf(4, 1, 5, 6)
        )
    )

    fun suggest(
        key: KeyCandidate,
        currentProgression: List<TheoryChord>,
        type: PairingType
    ): List<PairingSuggestion> {

        return templates
            .filter { it.type == type }
            .map { template ->
                PairingSuggestion(
                    title = template.title,
                    description = template.description,
                    chords = degreesToChords(template.degrees, key)
                        .map { it.label }
                )
            }
    }

    private fun degreesToChords(
        degrees: List<Int>,
        key: KeyCandidate
    ): List<TheoryChord> {

        val diatonic = MusicTheory.diatonicChords(
            root = key.root,
            isMinor = key.isMinor
        )

        return degrees.mapNotNull { degree ->
            diatonic.getOrNull(degree - 1)
        }
    }


}
