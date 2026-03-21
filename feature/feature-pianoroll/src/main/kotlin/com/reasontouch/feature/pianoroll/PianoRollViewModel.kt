package com.reasontouch.feature.pianoroll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.MidiTrack
import com.reasontouch.core.data.NoteEvent
import com.reasontouch.core.data.Session
import com.reasontouch.core.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PianoRollViewModel @Inject constructor(
    private val repository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    // ── Session ───────────────────────────────────────────────────────────
    val session: StateFlow<Session?> = repository.getSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Tracks ────────────────────────────────────────────────────────────
    val tracks: StateFlow<List<MidiTrack>> = repository.getTracksForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Chord progression (read-only, shown as ghost lane) ────────────────
    val chords: StateFlow<List<ChordEvent>> = repository.getChordsForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Active track ──────────────────────────────────────────────────────
    private val _activeTrackIndex = MutableStateFlow(0)
    val activeTrackIndex: StateFlow<Int> = _activeTrackIndex.asStateFlow()

    // ── Notes per track (cached for active track) ─────────────────────────
    private val _activeNotes = MutableStateFlow<List<NoteEvent>>(emptyList())
    val activeNotes: StateFlow<List<NoteEvent>> = _activeNotes.asStateFlow()

    // ── All notes for ghost rendering ─────────────────────────────────────
    private val _allNotes = MutableStateFlow<Map<String, List<NoteEvent>>>(emptyMap())
    val allNotes: StateFlow<Map<String, List<NoteEvent>>> = _allNotes.asStateFlow()

    // ── Playback ──────────────────────────────────────────────────────────
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playheadBeat = MutableStateFlow(0f)
    val playheadBeat: StateFlow<Float> = _playheadBeat.asStateFlow()

    // ── Loop ──────────────────────────────────────────────────────────────
    private val _loopEnabled = MutableStateFlow(false)
    val loopEnabled: StateFlow<Boolean> = _loopEnabled.asStateFlow()

    private val _loopStart = MutableStateFlow(0f)
    val loopStart: StateFlow<Float> = _loopStart.asStateFlow()

    private val _loopEnd = MutableStateFlow(4f)
    val loopEnd: StateFlow<Float> = _loopEnd.asStateFlow()

    // ── Tool ──────────────────────────────────────────────────────────────
    enum class Tool { DRAW, SELECT, ERASE }
    private val _currentTool = MutableStateFlow(Tool.DRAW)
    val currentTool: StateFlow<Tool> = _currentTool.asStateFlow()

    // ── Snap ──────────────────────────────────────────────────────────────
    val snapValues = listOf(1f, 0.5f, 0.25f, 0.125f, 0.0625f)
    val snapLabels = listOf("1/4","1/8","1/16","1/32","1/64")
    private val _snapIndex = MutableStateFlow(2)
    val snapIndex: StateFlow<Int> = _snapIndex.asStateFlow()

    // ── Selected notes ────────────────────────────────────────────────────
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    // ── BPM ───────────────────────────────────────────────────────────────
    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    init {
        loadTracksAndNotes()
    }

    private fun loadTracksAndNotes() {
        viewModelScope.launch {
            tracks.collect { trackList ->
                val notesMap = mutableMapOf<String, List<NoteEvent>>()
                trackList.forEach { track ->
                    val notes = repository.getNotesForTrackOnce(track.id)
                    notesMap[track.id] = notes
                }
                _allNotes.value = notesMap
                updateActiveNotes()
            }
        }
    }

    private fun updateActiveNotes() {
        val trackList = tracks.value
        val activeTrack = trackList.getOrNull(_activeTrackIndex.value) ?: return
        _activeNotes.value = _allNotes.value[activeTrack.id] ?: emptyList()
    }

    fun setActiveTrack(index: Int) {
        _activeTrackIndex.value = index
        updateActiveNotes()
    }

    fun setTool(tool: Tool) { _currentTool.value = tool }
    fun setSnapIndex(index: Int) { _snapIndex.value = index }
    fun toggleLoop() { _loopEnabled.value = !_loopEnabled.value }
    fun setLoopStart(beat: Float) { _loopStart.value = beat.coerceIn(0f, _loopEnd.value - snapValues[_snapIndex.value]) }
    fun setLoopEnd(beat: Float) { _loopEnd.value = beat.coerceIn(_loopStart.value + snapValues[_snapIndex.value], 16f) }
    fun setPlayhead(beat: Float) { _playheadBeat.value = beat.coerceIn(0f, 16f) }
    fun setBpm(bpm: Int) { _bpm.value = bpm.coerceIn(20, 300) }

    fun addNote(pitch: Int, beat: Float, duration: Float, velocity: Int = 100) {
        val trackList = tracks.value
        val activeTrack = trackList.getOrNull(_activeTrackIndex.value) ?: return
        val snap = snapValues[_snapIndex.value]
        val snappedBeat = (Math.round(beat / snap) * snap)
        val note = NoteEvent(
            id = UUID.randomUUID().toString(),
            trackId = activeTrack.id,
            pitch = pitch,
            beat = snappedBeat,
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

    fun updateNote(note: NoteEvent) {
        viewModelScope.launch {
            repository.updateNote(note)
            val trackList = tracks.value
            val activeTrack = trackList.getOrNull(_activeTrackIndex.value) ?: return@launch
            val current = _allNotes.value.toMutableMap()
            current[activeTrack.id] = (current[activeTrack.id] ?: emptyList()).map {
                if (it.id == note.id) note else it
            }
            _allNotes.value = current
            updateActiveNotes()
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
            val trackList = tracks.value
            val activeTrack = trackList.getOrNull(_activeTrackIndex.value) ?: return@launch
            val current = _allNotes.value.toMutableMap()
            current[activeTrack.id] = (current[activeTrack.id] ?: emptyList()).filter { it.id != noteId }
            _allNotes.value = current
            updateActiveNotes()
        }
    }

    fun selectNote(noteId: String) {
        _selectedNoteIds.value = setOf(noteId)
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
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
}