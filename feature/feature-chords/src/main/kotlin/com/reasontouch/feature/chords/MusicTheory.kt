package com.reasontouch.feature.chords

/**
 * Core music theory data — scales, diatonic chords, functional harmony.
 * Pure data, no Android dependencies, fully unit-testable.
 */

// ── Note representation ───────────────────────────────────────────────────────

enum class NoteClass(val label: String, val semitone: Int) {
    C("C", 0), Cs("C#", 1), D("D", 2), Ds("D#", 3), E("E", 4),
    F("F", 5), Fs("F#", 6), G("G", 7), Gs("G#", 8), A("A", 9),
    As("A#", 10), B("B", 11);

    companion object {
        fun fromSemitone(s: Int) = entries.first { it.semitone == ((s % 12 + 12) % 12) }
        fun fromLabel(label: String): NoteClass? {
            val normalized = FLAT_TO_SHARP[label] ?: label
            return entries.firstOrNull { it.label == normalized }
        }
        val FLAT_TO_SHARP = mapOf(
            "Db" to "C#", "Eb" to "D#", "Fb" to "E",
            "Gb" to "F#", "Ab" to "G#", "Bb" to "A#", "Cb" to "B"
        )
    }
}

enum class ChordQuality(val label: String) {
    MAJ("maj"), MIN("min"), DIM("dim"), AUG("aug"),
    DOM7("7"), MAJ7("maj7"), MIN7("min7"), SUS2("sus2"), SUS4("sus4")
}

enum class HarmonicFunction { TONIC, PREDOMINANT, DOMINANT }

enum class CompositionMode {
    MANUAL,
    ASSISTED,
    GUIDED
}

data class TheoryChord(
    val root: NoteClass,
    val quality: ChordQuality
) {
    val label get() = when (quality) {
        ChordQuality.MAJ  -> root.label
        ChordQuality.MIN  -> "${root.label}m"
        ChordQuality.DIM  -> "${root.label}dim"
        ChordQuality.AUG  -> "${root.label}aug"
        ChordQuality.DOM7 -> "${root.label}7"
        ChordQuality.MAJ7 -> "${root.label}maj7"
        ChordQuality.MIN7 -> "${root.label}m7"
        ChordQuality.SUS2 -> "${root.label}sus2"
        ChordQuality.SUS4 -> "${root.label}sus4"
    }
    // MIDI notes for this chord (root position, starting at MIDI 60 range)
    val midiNotes: List<Int> get() {
        val r = 48 + root.semitone  // root in C3 range
        return when (quality) {
            ChordQuality.MAJ  -> listOf(r, r+4, r+7)
            ChordQuality.MIN  -> listOf(r, r+3, r+7)
            ChordQuality.DIM  -> listOf(r, r+3, r+6)
            ChordQuality.AUG  -> listOf(r, r+4, r+8)
            ChordQuality.DOM7 -> listOf(r, r+4, r+7, r+10)
            ChordQuality.MAJ7 -> listOf(r, r+4, r+7, r+11)
            ChordQuality.MIN7 -> listOf(r, r+3, r+7, r+10)
            ChordQuality.SUS2 -> listOf(r, r+2, r+7)
            ChordQuality.SUS4 -> listOf(r, r+5, r+7)
        }
    }
}

data class KeyCandidate(
    val root:        NoteClass,
    val isMinor:     Boolean,
    val score:       Float,
    val confidence:  Float,    // 0..1
    val label:       String    // e.g. "G Major (Bright)" / "E Minor (Dark)"
)

// ── Scale intervals ───────────────────────────────────────────────────────────

private val SHARP_TO_FLAT_DISPLAY = mapOf(
    "C#" to "Db", "D#" to "Eb", "F#" to "Gb", "G#" to "Ab", "A#" to "Bb"
)

fun TheoryChord.guitarLabel(): String {
    val flatRoot = SHARP_TO_FLAT_DISPLAY[root.label] ?: root.label
    return when (quality) {
        ChordQuality.MAJ  -> flatRoot
        ChordQuality.MIN  -> "${flatRoot}m"
        ChordQuality.DIM  -> "${flatRoot}dim"
        ChordQuality.AUG  -> "${flatRoot}aug"
        ChordQuality.DOM7 -> "${flatRoot}7"
        ChordQuality.MAJ7 -> "${flatRoot}maj7"
        ChordQuality.MIN7 -> "${flatRoot}m7"
        ChordQuality.SUS2 -> "${flatRoot}sus2"
        ChordQuality.SUS4 -> "${flatRoot}sus4"
    }
}

object MusicTheory {


    // Semitone intervals from root for major and natural minor scales
    private val MAJOR_INTERVALS = listOf(0, 2, 4, 5, 7, 9, 11)
    private val MINOR_INTERVALS = listOf(0, 2, 3, 5, 7, 8, 10)

    // Diatonic chord qualities for major and minor keys (degrees 1-7)
    private val MAJOR_QUALITIES = listOf(
        ChordQuality.MAJ, ChordQuality.MIN, ChordQuality.MIN,
        ChordQuality.MAJ, ChordQuality.MAJ, ChordQuality.MIN, ChordQuality.DIM
    )
    private val MINOR_QUALITIES = listOf(
        ChordQuality.MIN, ChordQuality.DIM, ChordQuality.MAJ,
        ChordQuality.MIN, ChordQuality.MIN, ChordQuality.MAJ, ChordQuality.MAJ
    )

    // Harmonic function per scale degree (1-indexed)
    private val MAJOR_FUNCTIONS = mapOf(
        1 to HarmonicFunction.TONIC,
        2 to HarmonicFunction.PREDOMINANT,
        3 to HarmonicFunction.TONIC,
        4 to HarmonicFunction.PREDOMINANT,
        5 to HarmonicFunction.DOMINANT,
        6 to HarmonicFunction.TONIC,
        7 to HarmonicFunction.DOMINANT
    )
    private val MINOR_FUNCTIONS = mapOf(
        1 to HarmonicFunction.TONIC,
        2 to HarmonicFunction.PREDOMINANT,
        3 to HarmonicFunction.TONIC,
        4 to HarmonicFunction.PREDOMINANT,
        5 to HarmonicFunction.DOMINANT,
        6 to HarmonicFunction.TONIC,
        7 to HarmonicFunction.DOMINANT
    )

    // Valid harmonic function transitions
    val TRANSITIONS = mapOf(
        HarmonicFunction.TONIC        to listOf(HarmonicFunction.PREDOMINANT, HarmonicFunction.DOMINANT),
        HarmonicFunction.PREDOMINANT  to listOf(HarmonicFunction.DOMINANT),
        HarmonicFunction.DOMINANT     to listOf(HarmonicFunction.TONIC)
    )

    // Weighted transitions for natural feel — tonic most likely after dominant,
    // but deceptive cadence (V→vi) is always possible
    val TRANSITION_WEIGHTS = mapOf(
        HarmonicFunction.TONIC        to mapOf(
            HarmonicFunction.PREDOMINANT to 0.5f,
            HarmonicFunction.DOMINANT    to 0.5f
        ),
        HarmonicFunction.PREDOMINANT  to mapOf(
            HarmonicFunction.DOMINANT    to 1.0f
        ),
        HarmonicFunction.DOMINANT     to mapOf(
            HarmonicFunction.TONIC       to 0.85f,
            HarmonicFunction.PREDOMINANT to 0.15f  // deceptive cadence
        )
    )

    /** Returns the 7 diatonic chords for a given key root and mode */
    fun diatonicChords(root: NoteClass, isMinor: Boolean): List<TheoryChord> {
        val intervals  = if (isMinor) MINOR_INTERVALS else MAJOR_INTERVALS
        val qualities  = if (isMinor) MINOR_QUALITIES else MAJOR_QUALITIES
        return intervals.zip(qualities).map { (interval, quality) ->
            TheoryChord(NoteClass.fromSemitone(root.semitone + interval), quality)
        }
    }

    /** Returns the harmonic function of a scale degree (1-7) */
    fun function(degree: Int, isMinor: Boolean): HarmonicFunction =
        (if (isMinor) MINOR_FUNCTIONS else MAJOR_FUNCTIONS)[degree]
            ?: HarmonicFunction.TONIC

    /** Returns the scale degree (1-7) of a chord in a key, or null if not diatonic */
    fun degreeOf(chord: TheoryChord, root: NoteClass, isMinor: Boolean): Int? {
        val diatonic = diatonicChords(root, isMinor)
        val idx = diatonic.indexOfFirst {
            it.root.semitone == chord.root.semitone && it.quality == chord.quality
        }
        return if (idx >= 0) idx + 1 else null
    }

    /** Parse a chord name from the app's format (e.g. "Am Open", "G Barre5") */
    fun parseChordName(chordName: String): TheoryChord? {
        val name = chordName.trim().split(" ").firstOrNull() ?: return null
        // Detect quality suffix
        val quality = when {
            name.endsWith("maj7")  -> ChordQuality.MAJ7
            name.endsWith("m7")    -> ChordQuality.MIN7
            name.endsWith("7")     -> ChordQuality.DOM7
            name.endsWith("dim")   -> ChordQuality.DIM
            name.endsWith("aug")   -> ChordQuality.AUG
            name.endsWith("sus2")  -> ChordQuality.SUS2
            name.endsWith("sus4")  -> ChordQuality.SUS4
            name.endsWith("m") && !name.endsWith("dim") -> ChordQuality.MIN
            else                   -> ChordQuality.MAJ
        }
        val rootStr = when (quality) {
            ChordQuality.MAJ7 -> name.dropLast(4)
            ChordQuality.MIN7 -> name.dropLast(2)
            ChordQuality.DOM7 -> name.dropLast(1)
            ChordQuality.DIM  -> name.dropLast(3)
            ChordQuality.AUG  -> name.dropLast(3)
            ChordQuality.SUS2 -> name.dropLast(4)
            ChordQuality.SUS4 -> name.dropLast(4)
            ChordQuality.MIN  -> name.dropLast(1)
            ChordQuality.MAJ  -> name
        }
        val root = NoteClass.fromLabel(rootStr) ?: return null
        return TheoryChord(root, quality)
    }

    /** All 24 keys (12 major + 12 minor) as root/isMinor pairs */
    val ALL_KEYS: List<Pair<NoteClass, Boolean>> =
        NoteClass.entries.flatMap { listOf(it to false, it to true) }

    fun keyLabel(root: NoteClass, isMinor: Boolean): String {
        val mode  = if (isMinor) "Minor" else "Major"
        val mood  = if (isMinor) "Dark"  else "Bright"
        return "${root.label} $mode ($mood)"
    }
}