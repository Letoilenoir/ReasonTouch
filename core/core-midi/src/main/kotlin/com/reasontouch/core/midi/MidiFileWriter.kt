package com.reasontouch.core.midi

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Writes a Type 0 Standard MIDI File from a list of MidiProgressionBars.
 * Ported from the Guitar Chord Generator with package and model adaptations.
 * 480 ticks per quarter note. Strum speed clamped to 60% of step slot.
 */
object MidiFileWriter {

    private const val TICKS_PER_BEAT = 480
    private const val CHANNEL = 0

    fun write(bars: List<MidiProgressionBar>, out: OutputStream) {
        val trackData = buildTrackChunk(bars)
        out.write(buildHeaderChunk(trackData.size))
        out.write(trackData)
    }

    fun toByteArray(bars: List<MidiProgressionBar>): ByteArray {
        val bos = ByteArrayOutputStream()
        write(bars, bos)
        return bos.toByteArray()
    }

    private fun buildHeaderChunk(trackDataSize: Int): ByteArray {
        val buf = ByteArrayOutputStream()
        buf.write("MThd".toByteArray())
        buf.writeInt32(6)
        buf.writeInt16(0)   // format 0
        buf.writeInt16(1)   // 1 track
        buf.writeInt16(TICKS_PER_BEAT)
        return buf.toByteArray()
    }

    private fun buildTrackChunk(bars: List<MidiProgressionBar>): ByteArray {
        val events = mutableListOf<MidiEvent>()
        var currentTick = 0L
        var lastTempo = -1
        var lastProgram = -1

        for (bar in bars) {
            val tempo = bar.tempoBpm.coerceIn(20, 300)

            if (tempo != lastTempo) {
                events.add(MidiEvent(currentTick, buildTempoEvent(tempo)))
                lastTempo = tempo
            }

            if (bar.gmProgram != lastProgram) {
                events.add(MidiEvent(currentTick,
                    byteArrayOf((0xC0 or CHANNEL).toByte(), bar.gmProgram.toByte())
                ))
                lastProgram = bar.gmProgram
            }

            val stepTicks = bar.durationBeats * TICKS_PER_BEAT / 16.0
            val activeNotes = bar.notes.mapIndexedNotNull { i, n -> if (n != null) Pair(i, n) else null }
            val gapCount = (activeNotes.size - 1).coerceAtLeast(1)

            val safeSpeed = if (bar.strumSpeed > 0) {
                val maxSpeed = (stepTicks * 0.60) / gapCount
                minOf(bar.strumSpeed * TICKS_PER_BEAT, maxSpeed)
            } else 0.0

            val sweepTicks  = safeSpeed * gapCount
            val noteTicks   = ((stepTicks - sweepTicks) * 0.98).coerceAtLeast(10.0).toLong()

            for (stepIdx in 0 until 16) {
                val state = bar.steps[stepIdx]
                if (state == StepState.OFF) continue

                val stepStart   = currentTick + (stepIdx * stepTicks).toLong()
                val stringOrder = if (state == StepState.DOWN) activeNotes else activeNotes.reversed()
                val volStart    = if (state == StepState.DOWN) 110 else 85
                val volEnd      = if (state == StepState.DOWN) 90  else 105

                stringOrder.forEachIndexed { i, (_, note) ->
                    val offset    = (i * safeSpeed).toLong()
                    val velocity  = lerp(volStart, volEnd, i, stringOrder.size)
                    val noteStart = stepStart + offset
                    val noteEnd   = noteStart + noteTicks

                    events.add(MidiEvent(noteStart,
                        byteArrayOf((0x90 or CHANNEL).toByte(), note.toByte(), velocity.toByte())
                    ))
                    events.add(MidiEvent(noteEnd,
                        byteArrayOf((0x80 or CHANNEL).toByte(), note.toByte(), 0)
                    ))
                }
            }
            currentTick += (bar.durationBeats * TICKS_PER_BEAT).toLong()
        }

        events.add(MidiEvent(currentTick, byteArrayOf(0xFF.toByte(), 0x2F, 0x00)))
        events.sortWith(compareBy({ it.tick }, { it.priority }))

        val trackBytes = ByteArrayOutputStream()
        var lastTick = 0L
        for (event in events) {
            val delta = (event.tick - lastTick).coerceAtLeast(0)
            lastTick = event.tick
            trackBytes.writeVarLen(delta)
            trackBytes.write(event.data)
        }

        val buf = ByteArrayOutputStream()
        val trackData = trackBytes.toByteArray()
        buf.write("MTrk".toByteArray())
        buf.writeInt32(trackData.size)
        buf.write(trackData)
        return buf.toByteArray()
    }

    private fun buildTempoEvent(bpm: Int): ByteArray {
        val uspb = (60_000_000.0 / bpm).toInt()
        return byteArrayOf(
            0xFF.toByte(), 0x51, 0x03,
            ((uspb shr 16) and 0xFF).toByte(),
            ((uspb shr 8)  and 0xFF).toByte(),
            (uspb          and 0xFF).toByte()
        )
    }

    private fun lerp(start: Int, end: Int, step: Int, total: Int): Int {
        if (total <= 1) return start
        return start + ((end - start) * step.toDouble() / (total - 1)).toInt()
    }

    private fun ByteArrayOutputStream.writeInt32(value: Int) {
        write((value shr 24) and 0xFF); write((value shr 16) and 0xFF)
        write((value shr 8)  and 0xFF); write(value          and 0xFF)
    }

    private fun ByteArrayOutputStream.writeInt16(value: Int) {
        write((value shr 8) and 0xFF); write(value and 0xFF)
    }

    private fun ByteArrayOutputStream.writeVarLen(value: Long) {
        var v = value
        val bytes = mutableListOf<Byte>()
        bytes.add((v and 0x7F).toByte()); v = v shr 7
        while (v > 0) { bytes.add(((v and 0x7F) or 0x80).toByte()); v = v shr 7 }
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
