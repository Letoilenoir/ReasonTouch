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
 *   ROOT       — whole note on root
 *   ROOT_FIFTH — root on beat 1, perfect fifth on beat 3
 *   OCTAVE     — root on beat 1, root+12 on beat 3
 *   WALKING    — chromatic/scale approach from root to next root
 *   ARPEGGIO   — chord tones ascending: root, third, fifth, octave
 *   GROOVE     — syncopated: root on 1, ghost on 2.5, root on 3, ghost on 3.5
 *   PEDAL      — root sustained for full bar duration
 */
object BassGenerator {

    // Bass register target — keep notes in E1-E3 range (MIDI 28-52)
    private const val BASS_MIN_MIDI = 28
    private const val BASS_MAX_MIDI = 52

    /**
     * Generate bass notes for a complete progression.
     *
     * @param chords       ChordEvent list from Room
     * @param style        Selected BassStyle
     * @param targetTrackId  Room track ID for the bass track
     * @param beatsPerBar  Session bar duration in beats
     * @param appendOffset Beat offset if appending to existing content
     * @param snapValue    Snap grid value for note quantisation
     */
    fun generate(
        chords:        List<ChordEvent>,
        style:         BassStyle,
        targetTrackId: String,
        beatsPerBar:   Float = 4f,
        appendOffset:  Float = 0f,
        snapValue:     Float = 0.25f
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

            val barNotes = when (style) {
                BassStyle.ROOT       -> generateRoot(rootMidi, barStart, beatsPerBar, targetTrackId)
                BassStyle.ROOT_FIFTH -> generateRootFifth(rootMidi, barStart, beatsPerBar, targetTrackId)
                BassStyle.OCTAVE     -> generateOctave(rootMidi, barStart, beatsPerBar, targetTrackId)
                BassStyle.WALKING    -> generateWalking(rootMidi, nextRootMidi, barStart, beatsPerBar, targetTrackId)
                BassStyle.ARPEGGIO   -> generateArpeggio(chordTones, rootMidi, barStart, beatsPerBar, targetTrackId)
                BassStyle.GROOVE     -> generateGroove(rootMidi, barStart, beatsPerBar, targetTrackId)
                BassStyle.PEDAL      -> generatePedal(rootMidi, barStart, beatsPerBar, targetTrackId)
            }
            notes.addAll(barNotes)
        }
        return notes
    }

    // ── Style generators ──────────────────────────────────────────────────

    /** Root — single whole note on beat 1 */
    private fun generateRoot(
        root: Int, barStart: Float, barDur: Float, trackId: String
    ): List<NoteEvent> = listOf(
        note(trackId, root, barStart, barDur * 0.95f, 95)
    )

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
     * Walking Bass — root on beat 1, chromatic approach notes leading
     * to the next chord's root on beat 4.
     * Beat 1: root
     * Beat 2: root + 2 semitones (scale passing note)
     * Beat 3: halfway between root and next root
     * Beat 4: chromatic approach (next root ± 1)
     */
    private fun generateWalking(
        root: Int, nextRoot: Int?, barStart: Float, barDur: Float, trackId: String
    ): List<NoteEvent> {
        val beatDur  = barDur / 4f
        val noteDur  = beatDur * 0.9f
        val target   = nextRoot ?: root
        val approach = if (target > root) target - 1 else target + 1
        val mid      = bassRegister((root + target) / 2)

        return listOf(
            note(trackId, root,              barStart,              noteDur, 100),
            note(trackId, bassRegister(root + 2), barStart + beatDur,   noteDur, 80),
            note(trackId, mid,               barStart + beatDur*2f, noteDur, 85),
            note(trackId, bassRegister(approach), barStart + beatDur*3f, noteDur, 90)
        )
    }

    /**
     * Arpeggio — chord tones in sequence, one per beat.
     * Wraps back to root if fewer than 4 tones.
     */
    private fun generateArpeggio(
        tones: List<Int>, root: Int, barStart: Float, barDur: Float, trackId: String
    ): List<NoteEvent> {
        val beatDur = barDur / 4f
        val noteDur = beatDur * 0.85f
        val pitches = if (tones.size >= 4) tones.take(4)
                      else (tones + listOf(root + 12)).take(4)

        return pitches.mapIndexed { i, pitch ->
            note(trackId, bassRegister(pitch),
                barStart + i * beatDur, noteDur,
                if (i == 0) 100 else 75)
        }
    }

    /**
     * Groove — syncopated pattern:
     * Beat 1:   root (strong)
     * Beat 2.5: root (ghost, quiet)
     * Beat 3:   root (medium)
     * Beat 3.5: root (ghost, quiet)
     */
    private fun generateGroove(
        root: Int, barStart: Float, barDur: Float, trackId: String
    ): List<NoteEvent> {
        val beatDur = barDur / 4f
        val eighth  = beatDur / 2f
        val noteDur = eighth * 0.85f

        return listOf(
            note(trackId, root, barStart,               beatDur * 0.9f, 105),
            note(trackId, root, barStart + beatDur*1.5f, noteDur,        55),
            note(trackId, root, barStart + beatDur*2f,   beatDur * 0.45f, 90),
            note(trackId, root, barStart + beatDur*2.5f, noteDur,         50)
        )
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
        n += 36                // start in C2
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