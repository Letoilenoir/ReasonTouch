package com.reasontouch.core.midi

import android.content.Context
import android.net.Uri
import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.MidiTrack
import com.reasontouch.core.data.NoteEvent
import com.reasontouch.core.data.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class MidiImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SessionRepository
) {

    private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    suspend fun importMidiToSession(uri: Uri, sessionId: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val parsedFile = MidiParser.parse(inputStream)

            val maxBeat = parsedFile.tracks.flatMap { it.notes }.maxOfOrNull { it.beat + it.duration } ?: 16f
            val beatsPerBar = 4f
            val totalBars = ceil(maxBeat / beatsPerBar).toInt().coerceAtLeast(4)

            val session = repository.getSession(sessionId).firstOrNull()
            if (session != null) {
                repository.updateSession(
                    session.copy(
                        bpm = parsedFile.bpm,
                        totalBars = totalBars,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            val existingTracks = repository.getTracksForSession(sessionId).first()
            existingTracks.forEach { track ->
                repository.deleteNotesForTrack(track.id)
                repository.deleteTrack(track)
            }
            repository.deleteChordsForSession(sessionId)

            val savedTracks = mutableListOf<MidiTrack>()
            val allNotesMap = mutableMapOf<String, List<MidiNote>>()
            val usedNames = mutableSetOf<String>()

            parsedFile.tracks.forEachIndexed { idx, parsedTrack ->
                var trackName = determineTrackName(parsedTrack, idx)
                if (usedNames.contains(trackName)) {
                    val originalParsedName = parsedTrack.name.trim()
                    trackName = if (originalParsedName.isNotBlank() && !usedNames.contains(originalParsedName.uppercase())) {
                        originalParsedName.uppercase()
                    } else {
                        var suffix = 2
                        while (usedNames.contains("$trackName $suffix")) {
                            suffix++
                        }
                        "$trackName $suffix"
                    }
                }
                usedNames.add(trackName)

                val gmProgram = determineGmProgram(trackName)
                val midiChannel = if (trackName == "DRUMS") 9 else idx

                val newTrack = MidiTrack(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    index = idx,
                    name = trackName,
                    midiChannel = midiChannel,
                    gmProgram = gmProgram,
                    volume = 1.0f,
                    muted = false
                )
                repository.saveTrack(newTrack)
                savedTracks.add(newTrack)
                allNotesMap[newTrack.id] = parsedTrack.notes

                val noteEvents = parsedTrack.notes.map { note ->
                    val pitch = (108 - note.pitch).coerceIn(0, 87)
                    NoteEvent(
                        id = UUID.randomUUID().toString(),
                        trackId = newTrack.id,
                        beat = note.beat,
                        duration = note.duration,
                        pitch = pitch,
                        velocity = note.velocity
                    )
                }
                repository.saveNotes(noteEvents)
            }

            // Generate ChordEvents from CHORD track (or first melodic track)
            val chordTrack = savedTracks.firstOrNull { it.name.uppercase().contains("CHORD") }
                ?: savedTracks.firstOrNull { it.name.uppercase() != "DRUMS" }
            val chordNotes = chordTrack?.let { allNotesMap[it.id] } ?: emptyList()

            val phraseId = UUID.randomUUID().toString()
            for (barIndex in 0 until totalBars) {
                val barStart = barIndex * beatsPerBar
                val barEnd = barStart + beatsPerBar
                val barChordNotes = chordNotes.filter { it.beat >= barStart && it.beat < barEnd }

                val rootPitch = barChordNotes.minByOrNull { it.pitch }?.pitch ?: 60
                val semitone = rootPitch % 12
                val rootName = NOTE_NAMES[semitone]

                val chordEvent = ChordEvent(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    barIndex = barIndex,
                    chordName = rootName,
                    rootMidi = rootPitch,
                    midiNotes = barChordNotes.map { it.pitch }.distinct().joinToString(",").ifEmpty { "$rootPitch" },
                    voicing = "Open",
                    strumPatternId = null,
                    strumSpeedValue = 0.05,
                    phraseId = phraseId,
                    phraseLength = totalBars
                )
                repository.saveChord(chordEvent)
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("MidiImportManager", "Failed to import MIDI", e)
            false
        }
    }

    private fun determineTrackName(track: ParsedMidiTrack, index: Int): String {
        val upperName = track.name.uppercase()
        if (track.channel == 9 || upperName.contains("DRUM") || upperName.contains("PERC")) return "DRUMS"
        if (upperName.contains("BASS")) return "BASS"
        if (upperName.contains("PAD") || upperName.contains("STRING")) return "PAD"
        if (upperName.contains("LEAD") || upperName.contains("MELODY")) return "LEAD"
        if (upperName.contains("CHORD") || upperName.contains("PIANO") || upperName.contains("GUITAR")) return "CHORD"

        if (track.notes.isEmpty()) return "CHORD"
        val avgPitch = track.notes.map { it.pitch }.average()
        return when {
            avgPitch < 48 -> "BASS"
            avgPitch > 72 -> "LEAD"
            else -> "CHORD"
        }
    }

    private fun determineGmProgram(trackName: String): Int = when {
        trackName.contains("BASS")  -> 33
        trackName.contains("LEAD")  -> 81
        trackName.contains("PAD")   -> 89
        trackName.contains("DRUMS") -> 0
        else                        -> 0
    }
}
