package com.reasontouch.feature.export

import android.app.Application
import android.content.ContentValues
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.data.SessionRepository
import com.reasontouch.core.midi.MidiProgressionBar
import com.reasontouch.core.midi.MultiTrackMidiWriter
import com.reasontouch.core.midi.StepState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportViewModel(
    private val sessionId: String,
    private val repository: SessionRepository,
    private val application: Application
) : ViewModel() {

    sealed class ExportState {
        object Idle : ExportState()
        object Exporting : ExportState()
        data class Success(val fileName: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun exportMidi(fileName: String) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val bytes = buildMidiBytes()
                val safeName = fileName
                    .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
                    .trim()
                    .ifBlank { "reasontouch_session" }
                val displayName = "$safeName.mid"
                withContext(Dispatchers.IO) {
                    val resolver   = application.contentResolver
                    val collection = MediaStore.Downloads.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                        put(MediaStore.Downloads.MIME_TYPE, "audio/midi")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(collection, values)
                        ?: throw Exception("Could not create file")
                    resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                _exportState.value = ExportState.Success(displayName)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    private suspend fun buildMidiBytes(): ByteArray {
        val sessionVal = repository.getSession(sessionId).first()
        val tracksVal  = repository.getTracksForSession(sessionId).first()
        val chordsVal  = repository.getChordsForSession(sessionId).first()
        val bpm        = sessionVal?.bpm ?: 120

        val chordBars = chordsVal.map { chord ->
            val notes = chord.midiNotes.split(",").mapNotNull { it.trim().toIntOrNull() }
            MidiProgressionBar(
                chordName     = chord.chordName,
                notes         = Array<Int?>(6) { i -> notes.getOrNull(i) },
                steps         = List(16) { if (it == 0) StepState.DOWN else StepState.OFF },
                durationBeats = 4.0,
                tempoBpm      = bpm,
                strumSpeed    = 0.0,
                gmProgram     = sessionVal?.gmProgram ?: 25
            )
        }

        val pianoTracks = tracksVal.mapIndexed { i, track ->
            val notes = repository.getNotesForTrackOnce(track.id)
            MultiTrackMidiWriter.PianoRollTrack(
                name      = track.name,
                channel   = (i + 1).coerceIn(0, 15),
                gmProgram = 0,
                notes     = notes.map { note ->
                    MultiTrackMidiWriter.PianoRollNote(
                        pitch    = note.pitch,
                        beat     = note.beat,
                        duration = note.duration,
                        velocity = note.velocity
                    )
                }
            )
        }

        return MultiTrackMidiWriter.write(bpm, chordBars, pianoTracks)
    }

    fun resetState() { _exportState.value = ExportState.Idle }

    class Factory(
        private val sessionId: String,
        private val repository: SessionRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExportViewModel(sessionId, repository, application) as T
    }
}