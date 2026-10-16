package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.NoteEvent
import java.util.UUID

/**
 * Generates bass NoteEvents from a chord progression.
 *
 * All notes target the bass register — MIDI 28-52 (E1-E3).
 * Pitch convention: pitch = 108 - midiNote, so MIDI 40 = pitch 68.
 *
 * Each style produces musically distinct patterns:
 *   ROOT       — whole note on root (with optional octave variation on bars 3-4)
 *   ROOT_FIFTH — root on beat 1, perfect fifth on beat 3
 *   OCTAVE     — root on beat 1, root+12 on beat 3
 *   WALKING    — Jamerson-style chromatic approach & passing tones leading to next root
 *   ARPEGGIO   — Jaco-inspired upper-extension arpeggios (7ths/9ths)
 *   GROOVE     — Joe Dart-style pocket groove with micro-timed ghost notes
 *   PEDAL      — root sustained for full bar duration
 */
object BassGenerator {

    // Bass register target — keep notes in E1-E3 range (MIDI 28-52)
    private const val BASS_MIN_MIDI = 28
    private const val BASS_MAX_MIDI = 52

    /**
     * Generate bass notes for a complete progression.
     *
     * @param chords           ChordEvent list from Room
     * @param style            Selected BassStyle
     * @param targetTrackId    Room track ID for the bass track
     * @param beatsPerBar      Session bar duration in beats
     * @param appendOffset     Beat offset if appending to existing content
     * @param snapValue        Snap grid value for note quantisation
     * @param useMotownWalk    Jamerson chromatic approaches toggle
     * @param useJacoExtensions Jaco 7ths/9ths toggle
     * @param useDartGhostNotes Joe Dart ghost notes toggle
     * @param useMotifVariation  Phrase-level motif variation toggle
     */
    fun generate(
        chords:            List<ChordEvent>,
        style:             BassStyle,
        targetTrackId:     String,
        beatsPerBar:       Float = 4f,
        appendOffset:      Float = 0f,
        snapValue:         Float = 0.25f,
        context:           CompositionContext? = null,
        useMotownWalk:     Boolean = true,
        useJacoExtensions: Boolean = true,
        useDartGhostNotes: Boolean = true,
        useMotifVariation: Boolean = true
    ): List<NoteEvent> {
        if (chords.isEmpty()) return emptyList()

        val notes = mutableListOf<NoteEvent>()

        chords.forEachIndexed { barIdx, chord ->
            val barStart  = appendOffset + (barIdx * beatsPerBar)
            val rootMidi  = bassRegister(chord.rootMidi)

            // Parse chord tones from midiNotes field
            val chordTones = chord.midiNotes
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .map { bassRegister(it) }
                .distinct()
                .sorted()

            // Next chord root for walking bass approach
            val nextRootMidi = chords.getOrNull(barIdx + 1)?.rootMidi
                ?.let { bassRegister(it) }

            val isLastInPhrase = context?.bars?.getOrNull(barIdx)?.isLastInPhrase == true
            val effectiveStyle = if (isLastInPhrase && style in listOf(BassStyle.WALKING, BassStyle.GROOVE, BassStyle.ARPEGGIO)) {
                BassStyle.ROOT
            } else {
                style
            }

            // Motif & Variation Control: introduce subtle variation on bars 3 and 4 of a loop
            val isVariationBar = useMotifVariation && (barIdx % 4 >= 2)

            val barNotes = when (effectiveStyle) {
                BassStyle.ROOT       -> generateRoot(rootMidi, barStart, beatsPerBar, targetTrackId, isVariationBar)
                BassStyle.ROOT_FIFTH -> generateRootFifth(rootMidi, barStart, beatsPerBar, targetTrackId)
                BassStyle.OCTAVE     -> generateOctave(rootMidi, barStart, beatsPerBar, targetTrackId)
                BassStyle.WALKING    -> generateWalking(rootMidi, nextRootMidi, barStart, beatsPerBar, targetTrackId, useMotownWalk)
                BassStyle.ARPEGGIO   -> generateArpeggio(chordTones, rootMidi, barStart, beatsPerBar, targetTrackId, useJacoExtensions)
                BassStyle.GROOVE     -> generateGroove(rootMidi, barStart, beatsPerBar, targetTrackId, useDartGhostNotes)
                BassStyle.PEDAL      -> generatePedal(rootMidi, barStart, beatsPerBar, targetTrackId)
            }
            notes.addAll(barNotes)
        }
        return notes
    }

    // ── Style generators ──────────────────────────────────────────────────

    /** Root — single whole note on beat 1 (with octave variation on bars 3-4) */
    private fun generateRoot(
        root: Int, barStart: Float, barDur: Float, trackId: String, isVariation: Boolean
    ): List<NoteEvent> {
        return if (isVariation) {
            val octave = (root + 12).coerceAtMost(BASS_MAX_MIDI)
            val halfBar = barDur / 2f
            listOf(
                note(trackId, root, barStart, halfBar * 0.95f, 95),
                note(trackId, octave, barStart + halfBar, halfBar * 0.95f, 85)
            )
        } else {
            listOf(note(trackId, root, barStart, barDur * 0.95f, 95))
        }
    }

    /** Root + 5th — root on beat 1, perfect fifth on beat 3 */
    private fun generateRootFifth(
        root: Int, barStart: Float, barDur: Float, trackId: String
    ): List<NoteEvent> {
        val fifth       = bassRegister(root + 7)
        val halfBar     = barDur / 2f
        val noteDur     = halfBar * 0.92f
        return listOf(
            note(trackId, root,  barStart,           noteDur, 100),
            note(trackId, fifth, barStart + halfBar, noteDur, 85)
        )
    }

    /** Octave — root on beat 1, octave above on beat 3 */
    private fun generateOctave(
        root: Int, barStart: Float, barDur: Float, trackId: String
    ): List<NoteEvent> {
        val octave  = (root + 12).coerceAtMost(BASS_MAX_MIDI)
        val halfBar = barDur / 2f
        val noteDur = halfBar * 0.92f
        return listOf(
            note(trackId, root,   barStart,           noteDur, 100),
            note(trackId, octave, barStart + halfBar, noteDur, 80)
        )
    }

    /**
     * Jamerson-style Walking Bass — root on beat 1, passing minor third, fifth,
     * and chromatic approach leading to the next root on beat 4.
     */
    private fun generateWalking(
        root: Int, nextRoot: Int?, barStart: Float, barDur: Float, trackId: String, useMotownWalk: Boolean
    ): List<NoteEvent> {
        val beatDur  = barDur / 4f
        val noteDur  = beatDur * 0.9f
        val target   = nextRoot ?: root
        val approach = if (useMotownWalk) {
            if (target > root) target - 1 else target + 1
        } else {
            target
        }
        val passing  = if (useMotownWalk) bassRegister(root + 3) else bassRegister(root + 4)

        return listOf(
            note(trackId, root,                   barStart,              noteDur, 100),
            note(trackId, passing,                barStart + beatDur,    noteDur, 82),
            note(trackId, bassRegister(root + 7), barStart + beatDur*2f, noteDur, 88),
            note(trackId, bassRegister(approach), barStart + beatDur*3f, noteDur, 95)
        )
    }

    /**
     * Jaco-inspired Arpeggio — upper-extension arpeggiation (7ths & 9ths)
     * outlining chord tones in sequence.
     */
    private fun generateArpeggio(
        tones: List<Int>, root: Int, barStart: Float, barDur: Float, trackId: String, useJacoExtensions: Boolean
    ): List<NoteEvent> {
        val beatDur = barDur / 4f
        val noteDur = beatDur * 0.85f
        val extendedTones = if (useJacoExtensions && tones.size >= 3) {
            listOf(root, tones[1], tones.last(), bassRegister(tones.first() + 14))
        } else if (tones.size >= 4) {
            tones.take(4)
        } else {
            tones + listOf(root + 12)
        }
        val pitches = extendedTones.take(4)

        return pitches.mapIndexed { i, pitch ->
            note(trackId, bassRegister(pitch),
                barStart + i * beatDur, noteDur,
                if (i == 0) 100 else 78)
        }
    }

    /**
     * Joe Dart-style Groove — syncopated pattern with micro-timed low-velocity ghost notes.
     */
    private fun generateGroove(
        root: Int, barStart: Float, barDur: Float, trackId: String, useDartGhostNotes: Boolean
    ): List<NoteEvent> {
        val beatDur = barDur / 4f
        val eighth  = beatDur / 2f
        val noteDur = eighth * 0.85f

        val notes = mutableListOf(
            note(trackId, root, barStart,               beatDur * 0.9f, 105),
            note(trackId, root, barStart + beatDur * 2.0f,  beatDur * 0.45f, 95)
        )
        if (useDartGhostNotes) {
            notes.add(note(trackId, root, barStart + beatDur * 0.75f, noteDur * 0.6f, 45))
            notes.add(note(trackId, root, barStart + beatDur * 1.5f,  noteDur,        60))
            notes.add(note(trackId, root, barStart + beatDur * 2.75f, noteDur * 0.6f, 50))
            notes.add(note(trackId, root, barStart + beatDur * 3.5f,  noteDur,        55))
        }
        return notes.sortedBy { it.beat }
    }

    /** Pedal — sustained root for the full bar */
    private fun generatePedal(
        root: Int, barStart: Float, barDur: Float, trackId: String
    ): List<NoteEvent> = listOf(
        note(trackId, root, barStart, barDur, 88)
    )

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Transpose a MIDI note into the bass register (MIDI 28-52) */
    private fun bassRegister(midiNote: Int): Int {
        var n = midiNote % 12  // reduce to pitch class
        n += 48                // start in C3
        while (n < BASS_MIN_MIDI) n += 12
        while (n > BASS_MAX_MIDI) n -= 12
        return n.coerceIn(BASS_MIN_MIDI, BASS_MAX_MIDI)
    }

    private fun note(
        trackId:  String,
        midiNote: Int,
        beat:     Float,
        duration: Float,
        velocity: Int
    ) = NoteEvent(
        id       = UUID.randomUUID().toString(),
        trackId  = trackId,
        pitch    = (108 - midiNote).coerceIn(0, 87),
        beat     = beat,
        duration = duration.coerceAtLeast(0.0625f),
        velocity = velocity.coerceIn(1, 127)
    )
}
