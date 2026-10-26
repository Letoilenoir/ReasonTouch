package com.reasontouch.core.midi

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MidiNote(
    val pitch: Int,
    val beat: Float,
    val duration: Float,
    val velocity: Int
)

data class ParsedMidiTrack(
    val name: String,
    val channel: Int,
    val notes: List<MidiNote>
)

data class ParsedMidiFile(
    val bpm: Int,
    val ppq: Int,
    val tracks: List<ParsedMidiTrack>
)

object MidiParser {

    fun parse(inputStream: InputStream): ParsedMidiFile {
        val bytes = inputStream.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        // Read Header MThd
        val chunkId = String(bytes, 0, 4)
        if (chunkId != "MThd") throw IllegalArgumentException("Invalid MIDI header")
        val headerLen = buf.getInt(4)
        val numTracks = buf.getShort(10).toInt()
        val ppq = buf.getShort(12).toInt().let { if (it <= 0) 480 else it }

        var pos = 8 + headerLen
        var bpm = 120
        val parsedTracks = mutableListOf<ParsedMidiTrack>()

        for (t in 0 until numTracks) {
            if (pos + 8 > bytes.size) break
            val trackHeader = String(bytes, pos, 4)
            val trackLen = buf.getInt(pos + 4)
            pos += 8

            if (trackHeader != "MTrk") {
                pos += trackLen
                continue
            }

            val trackEnd = pos + trackLen
            var currentTick = 0L
            var trackName = "Track $t"
            var channel = 0
            val activeNotes = mutableMapOf<Int, Pair<Long, Int>>()
            val notes = mutableListOf<MidiNote>()

            var runningStatus: Byte = 0

            while (pos < trackEnd) {
                // Read variable-length delta time
                var delta = 0L
                var b: Int
                do {
                    if (pos >= bytes.size) break
                    b = bytes[pos++].toInt() and 0xFF
                    delta = (delta shl 7) or (b and 0x7F).toLong()
                } while ((b and 0x80) != 0)

                currentTick += delta

                if (pos >= bytes.size) break
                var status = bytes[pos].toInt() and 0xFF

                if ((status and 0x80) != 0) {
                    pos++
                    runningStatus = status.toByte()
                } else {
                    status = runningStatus.toInt() and 0xFF
                }

                if (status == 0xFF) { // Meta Event
                    if (pos + 1 >= bytes.size) break
                    val metaType = bytes[pos++].toInt() and 0xFF
                    var metaLen = 0
                    do {
                        if (pos >= bytes.size) break
                        b = bytes[pos++].toInt() and 0xFF
                        metaLen = (metaLen shl 7) or (b and 0x7F)
                    } while ((b and 0x80) != 0)

                    if (pos + metaLen > bytes.size) break
                    val metaData = bytes.copyOfRange(pos, pos + metaLen)
                    pos += metaLen

                    if (metaType == 0x03) { // Track Name
                        trackName = String(metaData)
                    } else if (metaType == 0x51 && metaData.size >= 3) { // Set Tempo
                        val usPerBeat = ((metaData[0].toInt() and 0xFF) shl 16) or
                                ((metaData[1].toInt() and 0xFF) shl 8) or
                                (metaData[2].toInt() and 0xFF)
                        if (usPerBeat > 0) {
                            bpm = (60_000_000.0 / usPerBeat).toInt().coerceIn(20, 300)
                        }
                    }
                } else {
                    val eventType = status and 0xF0
                    channel = status and 0x0F

                    when (eventType) {
                        0x90, 0x80 -> { // Note On / Note Off
                            if (pos + 1 >= bytes.size) break
                            val note = bytes[pos++].toInt() and 0xFF
                            val vel = bytes[pos++].toInt() and 0xFF
                            val isNoteOn = (eventType == 0x90) && (vel > 0)

                            if (isNoteOn) {
                                activeNotes[note] = Pair(currentTick, vel)
                            } else {
                                val startPair = activeNotes.remove(note)
                                if (startPair != null) {
                                    val startTick = startPair.first
                                    val startBeat = startTick.toFloat() / ppq
                                    val durBeats = ((currentTick - startTick).toFloat() / ppq).coerceAtLeast(0.125f)
                                    notes.add(MidiNote(pitch = note, beat = startBeat, duration = durBeats, velocity = startPair.second))
                                }
                            }
                        }
                        0xC0, 0xD0 -> { // Program Change / Channel Pressure
                            if (pos < bytes.size) pos++
                        }
                        0xA0, 0xB0, 0xE0 -> { // Poly Pressure / Controller / Pitch Bend
                            if (pos + 1 < bytes.size) pos += 2
                        }
                    }
                }
            }

            if (notes.isNotEmpty() || trackName.isNotBlank()) {
                parsedTracks.add(ParsedMidiTrack(name = trackName, channel = channel, notes = notes))
            }
            pos = trackEnd
        }

        return ParsedMidiFile(bpm = bpm, ppq = ppq, tracks = parsedTracks)
    }
}
