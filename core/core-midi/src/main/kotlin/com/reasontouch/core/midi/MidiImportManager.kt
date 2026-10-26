package com.reasontouch.core.midi

import android.content.Context
import android.net.Uri
import com.reasontouch.core.data.MidiTrack
import com.reasontouch.core.data.NoteEvent
import com.reasontouch.core.data.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MidiImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SessionRepository
) {

    suspend fun importMidiToSession(uri: Uri, sessionId: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val parsedFile = MidiParser.parse(inputStream)

            val session = repository.getSession(sessionId).firstOrNull()
            if (session != null) {
                repository.updateSession(session.copy(bpm = parsedFile.bpm, updatedAt = System.currentTimeMillis()))
            }

            val existingTracks = repository.getTracksForSession(sessionId).first()
            existingTracks.forEach { track ->
                repository.deleteNotesForTrack(track.id)
                repository.deleteTrack(track)
            }

            parsedFile.tracks.forEachIndexed { idx, parsedTrack ->
                val trackName = determineTrackName(parsedTrack, idx)
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

    private fun determineGmProgram(trackName: String): Int = when (trackName) {
        "BASS"  -> 33
        "LEAD"  -> 81
        "CHORD" -> 0
        "PAD"   -> 89
        "DRUMS" -> 0
        else    -> 0
    }
}
