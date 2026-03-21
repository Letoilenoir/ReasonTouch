package com.reasontouch.feature.chords

/**
 * Guitar chord voicings.
 * Each note array maps to strings [E2, A2, D3, G3, B3, E4].
 * null = muted string. Values are absolute MIDI note numbers.
 */
object GuitarVoicings {

    val OPEN_STRINGS = intArrayOf(40, 45, 50, 55, 59, 64)

    val voicings: Map<String, Map<String, Array<Int?>>> = mapOf(
        "C"  to mapOf("Open" to arrayOf(null,43,50,55,60,64), "Barre 3fr" to arrayOf(48,55,60,64,67,72)),
        "D"  to mapOf("Open" to arrayOf(null,null,50,57,62,66), "Barre 5fr" to arrayOf(50,57,62,66,69,74)),
        "E"  to mapOf("Open" to arrayOf(40,45,52,56,59,64), "Barre 7fr" to arrayOf(52,57,64,68,71,76)),
        "F"  to mapOf("Barre 1fr" to arrayOf(41,46,53,57,60,65), "Barre 8fr" to arrayOf(53,58,65,69,72,77)),
        "G"  to mapOf("Open" to arrayOf(43,47,50,55,59,67), "Barre 3fr" to arrayOf(43,50,55,59,62,67)),
        "A"  to mapOf("Open" to arrayOf(null,45,52,57,61,64), "Barre 5fr" to arrayOf(45,52,57,61,64,69)),
        "B"  to mapOf("Barre 2fr" to arrayOf(null,47,54,59,63,66), "Barre 7fr" to arrayOf(47,54,59,63,66,71)),
        "Bb" to mapOf("Barre 1fr" to arrayOf(null,46,53,58,62,65), "Barre 6fr" to arrayOf(46,53,58,62,65,70)),
        "Eb" to mapOf("Barre 6fr" to arrayOf(51,58,63,67,70,75), "Open shape" to arrayOf(null,null,51,58,63,67)),
        "Ab" to mapOf("Barre 4fr" to arrayOf(null,44,51,56,60,63), "Barre 11fr" to arrayOf(44,51,56,60,63,68)),
        "Db" to mapOf("Barre 4fr" to arrayOf(null,null,54,61,66,70), "Barre 9fr" to arrayOf(null,50,57,61,65,69)),
        "F#" to mapOf("Barre 2fr" to arrayOf(null,null,52,59,64,68), "Barre 9fr" to arrayOf(null,52,57,61,64,69)),
        "Cm" to mapOf("Barre 3fr" to arrayOf(48,55,60,63,67,72), "Open shape" to arrayOf(null,43,50,55,63,67)),
        "Dm" to mapOf("Open" to arrayOf(null,null,50,57,62,65), "Barre 5fr" to arrayOf(50,57,62,65,69,74)),
        "Em" to mapOf("Open" to arrayOf(40,45,52,55,59,64), "Barre 7fr" to arrayOf(52,57,64,67,71,76)),
        "Fm" to mapOf("Barre 1fr" to arrayOf(41,46,53,56,60,65)),
        "Gm" to mapOf("Barre 3fr" to arrayOf(43,50,55,58,62,67)),
        "Am" to mapOf("Open" to arrayOf(null,45,52,57,60,64), "Barre 5fr" to arrayOf(45,52,57,60,64,69)),
        "Bm" to mapOf("Barre 2fr" to arrayOf(null,47,54,59,62,66)),
        "Bbm" to mapOf("Barre 1fr" to arrayOf(null,46,53,58,61,65)),
        "Ebm" to mapOf("Barre 6fr" to arrayOf(51,58,63,66,70,75)),
        "Abm" to mapOf("Barre 4fr" to arrayOf(null,44,51,56,59,63)),
        "Dbm" to mapOf("Barre 4fr" to arrayOf(null,null,54,61,65,69)),
        "F#m" to mapOf("Barre 2fr" to arrayOf(null,null,52,59,63,68)),
        "C7"  to mapOf("Open" to arrayOf(null,43,50,55,58,64)),
        "D7"  to mapOf("Open" to arrayOf(null,null,50,57,60,66)),
        "E7"  to mapOf("Open" to arrayOf(40,45,52,56,59,62), "Barre 7fr" to arrayOf(52,57,64,68,71,74)),
        "G7"  to mapOf("Open" to arrayOf(43,47,50,55,59,65)),
        "A7"  to mapOf("Open" to arrayOf(null,45,52,55,61,64)),
        "B7"  to mapOf("Open" to arrayOf(null,47,51,56,59,63)),
        "F7"  to mapOf("Barre 1fr" to arrayOf(41,46,53,57,58,65)),
        "Cmaj7" to mapOf("Open" to arrayOf(null,43,50,55,59,64)),
        "Dmaj7" to mapOf("Open" to arrayOf(null,null,50,57,61,66)),
        "Emaj7" to mapOf("Open" to arrayOf(40,47,52,56,59,64)),
        "Fmaj7" to mapOf("Barre 1fr" to arrayOf(41,46,53,57,60,64)),
        "Gmaj7" to mapOf("Open" to arrayOf(43,47,50,55,59,66)),
        "Amaj7" to mapOf("Open" to arrayOf(null,45,52,57,61,64)),
        "Dm7"  to mapOf("Open" to arrayOf(null,null,50,57,60,65)),
        "Dim7" to mapOf("Open" to arrayOf(null,null,50,56,59,65)),
        "Em7"  to mapOf("Open" to arrayOf(40,45,52,55,59,64), "Barre 7fr" to arrayOf(52,57,64,67,71,74)),
        "Am7"  to mapOf("Open" to arrayOf(null,45,52,57,60,63), "Barre 5fr" to arrayOf(45,52,55,60,64,67)),
        "Bm7"  to mapOf("Barre 2fr" to arrayOf(null,47,54,57,62,66)),
        "Fm7"  to mapOf("Barre 1fr" to arrayOf(41,46,53,56,58,63)),
        "Asus2" to mapOf("Open" to arrayOf(null,45,52,57,59,64)),
        "Dsus2" to mapOf("Open" to arrayOf(null,null,50,57,60,64)),
        "Esus2" to mapOf("Open" to arrayOf(40,45,52,54,59,64)),
        "Gsus2" to mapOf("Open" to arrayOf(43,45,50,55,59,67)),
        "Asus4" to mapOf("Open" to arrayOf(null,45,52,57,62,64)),
        "Dsus4" to mapOf("Open" to arrayOf(null,null,50,57,62,67)),
        "Esus4" to mapOf("Open" to arrayOf(40,45,52,57,59,64)),
        "Gsus4" to mapOf("Open" to arrayOf(43,48,50,55,60,67)),
        "E5"   to mapOf("Open" to arrayOf(40,47,52,null,null,null), "Barre 7fr" to arrayOf(52,59,64,null,null,null)),
        "A5"   to mapOf("Open" to arrayOf(null,45,52,57,null,null)),
        "D5"   to mapOf("Open" to arrayOf(null,null,50,57,62,null)),
        "G5"   to mapOf("Open" to arrayOf(43,50,55,null,null,null)),
        "Bdim" to mapOf("Open" to arrayOf(null,null,null,56,59,65), "Barre 2fr" to arrayOf(null,47,53,56,62,null)),
        "Cdim" to mapOf("Barre 3fr" to arrayOf(null,48,54,57,63,null)),
        "Ddim" to mapOf("Open" to arrayOf(null,null,50,56,62,65)),
        "Edim" to mapOf("Open" to arrayOf(null,null,52,55,58,64)),
        "Caug" to mapOf("Open" to arrayOf(null,43,52,56,60,null), "Barre 3fr" to arrayOf(48,55,60,64,68,null)),
        "Daug" to mapOf("Open" to arrayOf(null,null,50,54,62,66)),
        "Eaug" to mapOf("Open" to arrayOf(40,45,52,56,60,null)),
        "Gaug" to mapOf("Open" to arrayOf(43,47,52,56,60,null)),
        "Aaug" to mapOf("Open" to arrayOf(null,45,52,57,61,65))
    )

    val categories: Map<String, List<String>> = mapOf(
        "All"   to voicings.keys.toList(),
        "Major" to voicings.keys.filter { it.matches(Regex("[A-G][b#]?$")) },
        "Minor" to voicings.keys.filter { it.endsWith("m") && !it.contains("maj") && !it.contains("7") },
        "7th"   to voicings.keys.filter { it.contains("7") },
        "Sus"   to voicings.keys.filter { it.contains("sus") },
        "Power" to voicings.keys.filter { it.endsWith("5") },
        "Dim"   to voicings.keys.filter { it.contains("dim") },
        "Aug"   to voicings.keys.filter { it.contains("aug") }
    )
}
