package com.reasontouch.feature.pianoroll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.audio.DrumSamplePlayer
import com.reasontouch.core.audio.Sf2Player
import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.MidiTrack
import com.reasontouch.core.data.NoteEvent
import com.reasontouch.core.data.Session
import com.reasontouch.core.data.SessionRepository
import com.reasontouch.core.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PianoRollViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val sf2Player: Sf2Player,
    private val drumSamplePlayer: DrumSamplePlayer,
    private val playbackController: PlaybackController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val session: StateFlow<Session?> = repository.getSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tracks: StateFlow<List<MidiTrack>> = repository.getTracksForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chords: StateFlow<List<ChordEvent>> = repository.getChordsForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTrackIndex = MutableStateFlow(0)
    val activeTrackIndex: StateFlow<Int> = _activeTrackIndex.asStateFlow()

    private val _activeNotes = MutableStateFlow<List<NoteEvent>>(emptyList())
    val activeNotes: StateFlow<List<NoteEvent>> = _activeNotes.asStateFlow()

    private val _allNotes = MutableStateFlow<Map<String, List<NoteEvent>>>(emptyMap())
    val allNotes: StateFlow<Map<String, List<NoteEvent>>> = _allNotes.asStateFlow()

    enum class Tool { DRAW, SELECT, ERASE }

    private val _currentTool = MutableStateFlow(Tool.DRAW)
    val currentTool: StateFlow<Tool> = _currentTool.asStateFlow()

    val snapValues = listOf(1f, 0.5f, 0.25f, 0.125f, 0.0625f)
    val snapLabels = listOf("1/4", "1/8", "1/16", "1/32", "1/64")
    private val _snapIndex = MutableStateFlow(2)
    val snapIndex: StateFlow<Int> = _snapIndex.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    private var clipboard: List<NoteEvent> = emptyList()
    private val _hasClipboard = MutableStateFlow(false)
    val hasClipboard: StateFlow<Boolean> = _hasClipboard.asStateFlow()

    val transport = playbackController.state

    init {
        viewModelScope.launch {
            tracks.collect { trackList ->
                val notes = trackList.associate { track ->
                    track.id to repository.getNotesForTrackOnce(track.id)
                }
                _allNotes.value = notes
                updateActiveNotes()
            }
        }
    }

    private fun updateActiveNotes() {
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        _activeNotes.value = _allNotes.value[activeTrack.id] ?: emptyList()
    }

    fun setActiveTrack(index: Int) {
        _activeTrackIndex.value = index
        updateActiveNotes()
    }

    fun setTrackVolume(index: Int, volume: Float) {
        viewModelScope.launch {
            val track = tracks.value.getOrNull(index) ?: return@launch
            repository.updateTrack(track.copy(volume = volume.coerceIn(0f, 1f)))
        }
    }

    fun muteTrack(index: Int) {
        viewModelScope.launch {
            val track = tracks.value.getOrNull(index) ?: return@launch
            repository.updateTrack(track.copy(muted = !track.muted))
        }
    }

    fun setTool(tool: Tool) {
        _currentTool.value = tool
        _selectedNoteIds.value = emptySet()
    }

    fun setSnapIndex(index: Int) {
        _snapIndex.value = index
    }

    fun addNote(pitch: Int, beat: Float, duration: Float, velocity: Int = 100) {
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val snap = snapValues[_snapIndex.value]
        val note = NoteEvent(
            id = UUID.randomUUID().toString(),
            trackId = activeTrack.id,
            pitch = pitch,
            beat = (Math.round(beat / snap) * snap),
            duration = duration.coerceAtLeast(snap),
            velocity = velocity
        )
        viewModelScope.launch {
            repository.saveNote(note)
            val current = _allNotes.value.toMutableMap()
            current[activeTrack.id] = (current[activeTrack.id] ?: emptyList()) + note
            _allNotes.value = current
            updateActiveNotes()
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
            val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return@launch
            val current = _allNotes.value.toMutableMap()
            current[activeTrack.id] = (current[activeTrack.id] ?: emptyList())
                .filter { it.id != noteId }
            _allNotes.value = current
            updateActiveNotes()
        }
    }

    /**
     * Move selected notes by deltaBeats horizontally and deltaPitch vertically.
     * Called repeatedly during drag — updates in-memory immediately,
     * persists to Room on drag end (commitMove).
     */
    fun moveSelectedNotes(deltaBeats: Float, deltaPitch: Int, snapValue: Float) {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val current = _allNotes.value.toMutableMap()
        val notes = current[activeTrack.id] ?: return

        val updated = notes.map { note ->
            if (note.id in ids) {
                val newBeat = (note.beat + deltaBeats).coerceAtLeast(0f)
                val newPitch = (note.pitch + deltaPitch).coerceIn(0, 87)
                note.copy(beat = newBeat, pitch = newPitch)
            } else note
        }
        current[activeTrack.id] = updated
        _allNotes.value = current
        updateActiveNotes()
    }

    /** Persist moved notes to Room after drag ends */
    fun commitMove() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val notes = _allNotes.value[activeTrack.id] ?: return
        viewModelScope.launch {
            notes.filter { it.id in ids }.forEach { repository.saveNote(it) }
        }
    }

    fun copySelectedNotes() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val notes = _allNotes.value[activeTrack.id] ?: return
        clipboard = notes.filter { it.id in ids }
        _hasClipboard.value = clipboard.isNotEmpty()
    }

    /**
     * Paste clipboard notes starting at pasteAtBeat.
     * Notes preserve their relative positions — the earliest note in the
     * clipboard lands at pasteAtBeat, others are offset accordingly.
     */
    fun pasteNotes(pasteAtBeat: Float, snapValue: Float) {
        if (clipboard.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val earliestBeat = clipboard.minOf { it.beat }
        val offset = pasteAtBeat - earliestBeat

        viewModelScope.launch {
            val newNotes = clipboard.map { note ->
                note.copy(
                    id = UUID.randomUUID().toString(),
                    trackId = activeTrack.id,
                    beat = snapBeat(note.beat + offset, snapValue).coerceAtLeast(0f)
                )
            }
            newNotes.forEach { repository.saveNote(it) }
            val current = _allNotes.value.toMutableMap()
            current[activeTrack.id] = (current[activeTrack.id] ?: emptyList()) + newNotes
            _allNotes.value = current
            _selectedNoteIds.value = newNotes.map { it.id }.toSet()
            updateActiveNotes()
        }
    }

    private fun snapBeat(beat: Float, snapValue: Float): Float =
        (Math.round(beat / snapValue) * snapValue)

    // ── Note edits ────────────────────────────────────────────────────────

    fun updateNoteDuration(noteId: String, newDuration: Float) {
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val current = _allNotes.value.toMutableMap()
        val notes = current[activeTrack.id] ?: return
        val updated = notes.map { if (it.id == noteId) it.copy(duration = newDuration) else it }
        current[activeTrack.id] = updated
        _allNotes.value = current
        updateActiveNotes()
        viewModelScope.launch {
            val note = updated.firstOrNull { it.id == noteId } ?: return@launch
            repository.saveNote(note)
        }
    }

    fun updateNoteVelocity(noteId: String, newVelocity: Int) {
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val current = _allNotes.value.toMutableMap()
        val notes = current[activeTrack.id] ?: return
        val updated = notes.map {
            if (it.id == noteId) it.copy(velocity = newVelocity.coerceIn(1, 127)) else it
        }
        current[activeTrack.id] = updated
        _allNotes.value = current
        updateActiveNotes()
        viewModelScope.launch {
            val note = updated.firstOrNull { it.id == noteId } ?: return@launch
            repository.saveNote(note)
        }
    }

    fun auditionNote(pitch: Int, velocity: Int = 100) {
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val midi = (108 - pitch).coerceIn(0, 127)
        if (isDrumTrack(activeTrack)) {
            drumSamplePlayer.play(midi, velocity)
        } else {
            sf2Player.playNote(midi, 0.8f, velocity, gmProgramForTrack(activeTrack))
        }
    }

    fun bounceDown(sourceIndices: List<Int>, destIndex: Int, clearSources: Boolean) {
        viewModelScope.launch {
            val trackList = tracks.value
            val destTrack = trackList.getOrNull(destIndex) ?: return@launch
            val mergedNotes = sourceIndices.flatMap { i ->
                val track = trackList.getOrNull(i) ?: return@flatMap emptyList()
                (_allNotes.value[track.id] ?: emptyList()).map {
                    it.copy(id = UUID.randomUUID().toString(), trackId = destTrack.id)
                }
            }
            repository.saveNotes(mergedNotes)
            if (clearSources) {
                sourceIndices.forEach { i ->
                    val track = trackList.getOrNull(i) ?: return@forEach
                    if (i != destIndex) repository.deleteNotesForTrack(track.id)
                }
            }
            loadTracksAndNotes()
        }
    }

    private fun isDrumTrack(track: MidiTrack) =
        track.midiChannel == 9 ||
                track.name.uppercase() in listOf("DRUMS", "DRUM")

    private fun gmProgramForTrack(track: MidiTrack): Int = when (track.name.uppercase()) {
        "BASS"  -> 32
        "LEAD"  -> 80
        "CHORD" -> 25
        "PAD"   -> 88
        else    -> 0
    }

    // If your original implementation did more here, you can keep it.
    // This stub keeps bounceDown() compiling even if tracks/notes are flow-driven.
    private fun loadTracksAndNotes() {
        // No-op if your flows auto-update from Room; keep or extend as needed.
    }
    // -- Playback delegation -----------------------------------------------
    val isPlaying:    StateFlow<Boolean> = playbackController.state
        .map { it.isPlaying }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val playheadBeat: StateFlow<Float> = playbackController.state
        .map { it.playheadBeat }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    val loopEnabled:  StateFlow<Boolean> = playbackController.state
        .map { it.loopEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val loopStart:    StateFlow<Float> = playbackController.state
        .map { it.loopStart }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    val loopEnd:      StateFlow<Float> = playbackController.state
        .map { it.loopEnd }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4f)

    fun toggleLoop() = playbackController.setLoop(!playbackController.state.value.loopEnabled)

    // -- Selection helpers -------------------------------------------------
    fun deleteSelectedNotes() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        viewModelScope.launch {
            ids.forEach { repository.deleteNoteById(it) }
            val current = _allNotes.value.toMutableMap()
            current[activeTrack.id] = (current[activeTrack.id] ?: emptyList())
                .filter { it.id !in ids }
            _allNotes.value = current
            _selectedNoteIds.value = emptySet()
            updateActiveNotes()
        }
    }

        fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun setLoopStart(beat: Float) = playbackController.setLoopStart(beat)
        fun setLoopEnd(beat: Float) = playbackController.setLoopEnd(beat)

    fun selectNote(noteId: String, addToSelection: Boolean = false) {
        _selectedNoteIds.value = if (addToSelection) {
            _selectedNoteIds.value + noteId
        } else {
            setOf(noteId)
        }
    }

    fun setSelection(noteIds: Set<String>) {
        _selectedNoteIds.value = noteIds
    }

        fun setPlayhead(beat: Float, totalBars: Int = 4) {
        val clamped = beat.coerceIn(0f, totalBars * 4f)
        playbackController.seekTo(clamped)
    }
}






