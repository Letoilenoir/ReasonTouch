package com.reasontouch.core.midi

import java.io.ByteArrayOutputStream

/**
 * Writes a Format 1 multi-track MIDI file.
 * Track 0: tempo map + chord progression (guitar channel)
 * Tracks 1-N: piano roll tracks (one per MidiTrack)
 */
object MultiTrackMidiWriter {

    private const val TICKS_PER_BEAT = 480

    data class PianoRollTrack(
        val name: String,
        val channel: Int,
        val gmProgram: Int,
        val notes: List<PianoRollNote>
    )

    data class PianoRollNote(
        val pitch: Int,
        val beat: Float,
        val duration: Float,
        val velocity: Int
    )

    fun write(
        bpm: Int,
        chordBars: List<MidiProgressionBar>,
        pianoTracks: List<PianoRollTrack>
    ): ByteArray {
        val allTracks = mutableListOf<ByteArray>()

        // Track 0 — tempo + chords
        allTracks.add(buildChordTrack(bpm, chordBars))

        // Tracks 1..N — piano roll
        pianoTracks.forEach { track ->
            if (track.notes.isNotEmpty()) {
                allTracks.add(buildPianoTrack(track, bpm))
            }
        }

        // Header
        val header = buildHeader(allTracks.size)
        val out = ByteArrayOutputStream()
        out.write(header)
        allTracks.forEach { out.write(it) }
        return out.toByteArray()
    }

    // ── Header chunk ──────────────────────────────────────────────────────
    private fun buildHeader(numTracks: Int): ByteArray {
        val buf = ByteArrayOutputStream()
        buf.write("MThd".toByteArray())
        buf.writeInt32(6)
        buf.writeInt16(1)           // Format 1
        buf.writeInt16(numTracks)
        buf.writeInt16(TICKS_PER_BEAT)
        return buf.toByteArray()
    }

    // ── Chord track ───────────────────────────────────────────────────────
    private fun buildChordTrack(bpm: Int, bars: List<MidiProgressionBar>): ByteArray {
        val events = mutableListOf<MidiEvent>()
        val ch = 0

        // Tempo meta
        val uspb = (60_000_000.0 / bpm.coerceIn(20,300)).toInt()
        events.add(MidiEvent(0L, byteArrayOf(
            0xFF.toByte(), 0x51, 0x03,
            ((uspb shr 16) and 0xFF).toByte(),
            ((uspb shr 8)  and 0xFF).toByte(),
            (uspb          and 0xFF).toByte()
        )))
        // Time sig 4/4
        events.add(MidiEvent(0L, byteArrayOf(0xFF.toByte(), 0x58, 0x04, 0x04, 0x02, 0x18, 0x08)))
        // Track name
        val name = "Chords"
        events.add(MidiEvent(0L, byteArrayOf(0xFF.toByte(), 0x03, name.length.toByte(),
            *name.toByteArray())))

        if (bars.isEmpty()) {
            events.add(MidiEvent(0L, byteArrayOf(0xFF.toByte(), 0x2F, 0x00)))
            return buildTrackChunk(events)
        }

        var currentTick = 0L
        var lastProgram = -1

        bars.forEach { bar ->
            val tempo = bar.tempoBpm.coerceIn(20, 300)
            if (bar.gmProgram != lastProgram) {
                events.add(MidiEvent(currentTick,
                    byteArrayOf((0xC0 or ch).toByte(), bar.gmProgram.toByte())))
                lastProgram = bar.gmProgram
            }

            val stepTicks  = bar.durationBeats * TICKS_PER_BEAT / 16.0
            val activeNotes = bar.notes.mapIndexedNotNull { i, n -> if (n != null) Pair(i,n) else null }
            val gapCount   = (activeNotes.size - 1).coerceAtLeast(1)
            val safeSpeed  = if (bar.strumSpeed > 0) {
                val maxSpeed = (stepTicks * 0.60) / gapCount
                minOf(bar.strumSpeed * TICKS_PER_BEAT, maxSpeed)
            } else 0.0

            val sweepTicks = safeSpeed * gapCount
            val noteTicks  = ((stepTicks - sweepTicks) * 0.98).coerceAtLeast(10.0).toLong()

            for (stepIdx in 0 until 16) {
                val state = bar.steps[stepIdx]
                if (state == StepState.OFF) continue
                val stepStart   = currentTick + (stepIdx * stepTicks).toLong()
                val strOrder    = if (state == StepState.DOWN) activeNotes else activeNotes.reversed()
                val volStart    = if (state == StepState.DOWN) 110 else 85
                val volEnd      = if (state == StepState.DOWN) 90  else 105

                strOrder.forEachIndexed { i, (_, note) ->
                    val offset    = (i * safeSpeed).toLong()
                    val velocity  = lerp(volStart, volEnd, i, strOrder.size)
                    val noteStart = stepStart + offset
                    events.add(MidiEvent(noteStart,
                        byteArrayOf((0x90 or ch).toByte(), note.toByte(), velocity.toByte())))
                    events.add(MidiEvent(noteStart + noteTicks,
                        byteArrayOf((0x80 or ch).toByte(), note.toByte(), 0)))
                }
            }
            currentTick += (bar.durationBeats * TICKS_PER_BEAT).toLong()
        }

        events.add(MidiEvent(currentTick, byteArrayOf(0xFF.toByte(), 0x2F, 0x00)))
        return buildTrackChunk(events)
    }

    // ── Piano roll track ──────────────────────────────────────────────────
    private fun buildPianoTrack(track: PianoRollTrack, bpm: Int): ByteArray {
        val events = mutableListOf<MidiEvent>()
        val ch = track.channel.coerceIn(0, 15)

        // Track name
        events.add(MidiEvent(0L, byteArrayOf(0xFF.toByte(), 0x03,
            track.name.length.toByte(), *track.name.toByteArray())))
        // Program change
        events.add(MidiEvent(0L,
            byteArrayOf((0xC0 or ch).toByte(), track.gmProgram.coerceIn(0,127).toByte())))

        track.notes.forEach { note ->
            val onTick  = (note.beat * TICKS_PER_BEAT).toLong()
            val offTick = ((note.beat + note.duration) * TICKS_PER_BEAT).toLong()
            val midi    = (108 - note.pitch).coerceIn(0, 127)
            val vel     = note.velocity.coerceIn(1, 127)
            events.add(MidiEvent(onTick,
                byteArrayOf((0x90 or ch).toByte(), midi.toByte(), vel.toByte())))
            events.add(MidiEvent(offTick,
                byteArrayOf((0x80 or ch).toByte(), midi.toByte(), 0)))
        }

        val lastTick = track.notes.maxOfOrNull {
            ((it.beat + it.duration) * TICKS_PER_BEAT).toLong()
        } ?: 0L
        events.add(MidiEvent(lastTick, byteArrayOf(0xFF.toByte(), 0x2F, 0x00)))
        return buildTrackChunk(events)
    }

    // ── Build track chunk from events ─────────────────────────────────────
    private fun buildTrackChunk(events: List<MidiEvent>): ByteArray {
        val sorted = events.sortedWith(compareBy({ it.tick }, { it.priority }))
        val trackBytes = ByteArrayOutputStream()
        var lastTick = 0L
        sorted.forEach { ev ->
            val delta = (ev.tick - lastTick).coerceAtLeast(0)
            lastTick = ev.tick
            trackBytes.writeVarLen(delta)
            trackBytes.write(ev.data)
        }
        val buf = ByteArrayOutputStream()
        val td  = trackBytes.toByteArray()
        buf.write("MTrk".toByteArray())
        buf.writeInt32(td.size)
        buf.write(td)
        return buf.toByteArray()
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private fun lerp(start: Int, end: Int, step: Int, total: Int): Int {
        if (total <= 1) return start
        return start + ((end - start) * step.toDouble() / (total - 1)).toInt()
    }

    private fun ByteArrayOutputStream.writeInt32(v: Int) {
        write((v shr 24) and 0xFF); write((v shr 16) and 0xFF)
        write((v shr 8)  and 0xFF); write(v          and 0xFF)
    }
    private fun ByteArrayOutputStream.writeInt16(v: Int) {
        write((v shr 8) and 0xFF); write(v and 0xFF)
    }
    private fun ByteArrayOutputStream.writeVarLen(v: Long) {
        var n = v
        val bytes = mutableListOf<Byte>()
        bytes.add((n and 0x7F).toByte()); n = n shr 7
        while (n > 0) { bytes.add(((n and 0x7F) or 0x80).toByte()); n = n shr 7 }
        bytes.reversed().forEach { write(it.toInt()) }
    }

    private data class MidiEvent(
        val tick: Long,
        val data: ByteArray,
        val priority: Int = when {
            data.isNotEmpty() && (data[0].toInt() and 0xFF) == 0xFF -> 0
            data.isNotEmpty() && (data[0].toInt() and 0xF0) == 0xC0 -> 1
            data.isNotEmpty() && (data[0].toInt() and 0xF0) == 0x90 -> 2
            else -> 3
        }
    )
}