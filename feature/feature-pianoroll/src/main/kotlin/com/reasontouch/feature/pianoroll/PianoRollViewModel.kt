package com.reasontouch.feature.pianoroll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.audio.Sf2Player
import com.reasontouch.core.audio.SynthEngine
import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.MidiTrack
import com.reasontouch.core.data.NoteEvent
import com.reasontouch.core.data.Session
import com.reasontouch.core.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val synthEngine: SynthEngine,
    private val sf2Player: Sf2Player,
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

    private val _isPlaying    = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _playheadBeat = MutableStateFlow(0f)
    val playheadBeat: StateFlow<Float> = _playheadBeat.asStateFlow()
    private var playbackJob: Job? = null

    private val _loopEnabled = MutableStateFlow(false)
    val loopEnabled: StateFlow<Boolean> = _loopEnabled.asStateFlow()
    private val _loopStart = MutableStateFlow(0f)
    val loopStart: StateFlow<Float> = _loopStart.asStateFlow()
    private val _loopEnd = MutableStateFlow(4f)
    val loopEnd: StateFlow<Float> = _loopEnd.asStateFlow()

    enum class Tool { DRAW, SELECT, ERASE }
    private val _currentTool = MutableStateFlow(Tool.DRAW)
    val currentTool: StateFlow<Tool> = _currentTool.asStateFlow()

    val snapValues = listOf(1f, 0.5f, 0.25f, 0.125f, 0.0625f)
    val snapLabels = listOf("1/4","1/8","1/16","1/32","1/64")
    private val _snapIndex = MutableStateFlow(2)
    val snapIndex: StateFlow<Int> = _snapIndex.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    // Clipboard — stores copies of notes for paste
    private var clipboard: List<NoteEvent> = emptyList()
    private val _hasClipboard = MutableStateFlow(false)
    val hasClipboard: StateFlow<Boolean> = _hasClipboard.asStateFlow()

    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val CHORD_AUDITION_DUR_SEC = 0.5f

    init { loadTracksAndNotes() }

    private fun loadTracksAndNotes() {
        viewModelScope.launch {
            tracks.collect { trackList ->
                val notesMap = mutableMapOf<String, List<NoteEvent>>()
                trackList.forEach { track ->
                    notesMap[track.id] = repository.getNotesForTrackOnce(track.id)
                }
                _allNotes.value = notesMap
                updateActiveNotes()
            }
        }
    }

    private fun updateActiveNotes() {
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        _activeNotes.value = _allNotes.value[activeTrack.id] ?: emptyList()
    }

    fun setActiveTrack(index: Int) { _activeTrackIndex.value = index; updateActiveNotes() }

    fun muteTrack(index: Int) {
        viewModelScope.launch {
            val track = tracks.value.getOrNull(index) ?: return@launch
            repository.updateTrack(track.copy(muted = !track.muted))
        }
    }

    fun setTool(tool: Tool)      { _currentTool.value = tool; _selectedNoteIds.value = emptySet() }
    fun setSnapIndex(index: Int) { _snapIndex.value = index }
    fun toggleLoop()             { _loopEnabled.value = !_loopEnabled.value }

    fun setLoopStart(beat: Float) {
        _loopStart.value = beat.coerceIn(
            0f, (_loopEnd.value - snapValues[_snapIndex.value]).coerceAtLeast(0f))
    }
    fun setLoopEnd(beat: Float, totalBars: Int = 4) {
        _loopEnd.value = beat.coerceIn(
            _loopStart.value + snapValues[_snapIndex.value], (totalBars * 4).toFloat())
    }
    fun setPlayhead(beat: Float, totalBars: Int = 4) {
        _playheadBeat.value = beat.coerceIn(0f, (totalBars * 4).toFloat())
    }
    fun setBpm(bpm: Int) { _bpm.value = bpm.coerceIn(20, 300) }

    private fun computeEndBeat(totalBars: Int): Float {
        val lastNoteBeat = _allNotes.value.values
            .flatten()
            .maxOfOrNull { it.beat + it.duration } ?: 0f
        val barsNeeded = kotlin.math.ceil(lastNoteBeat / 4f).toInt()
        return (maxOf(barsNeeded, totalBars) * 4f)
    }

    private fun gmProgramForTrack(track: MidiTrack): Int = when (track.name.uppercase()) {
        "BASS"  -> 32
        "LEAD"  -> 80
        "CHORD" -> 25
        "PAD"   -> 88
        else    -> 0
    }

    private fun buildChordClusters(
        notes: List<NoteEvent>,
        strumWindow: Float = 0.5f
    ): List<Float> {
        val sorted   = notes.sortedBy { it.beat }
        val clusters = mutableListOf<Float>()
        var clusterStart = Float.MIN_VALUE
        sorted.forEach { note ->
            if (note.beat - clusterStart > strumWindow) {
                clusterStart = note.beat
                clusters.add(clusterStart)
            }
        }
        return clusters
    }

    private fun chordRingDuration(
        note: NoteEvent,
        clusterBeats: List<Float>,
        beatDurMs: Double
    ): Float {
        val myCluster   = clusterBeats.lastOrNull { it <= note.beat + 0.001f } ?: note.beat
        val nextCluster = clusterBeats.firstOrNull { it > myCluster + 0.001f }
        return if (nextCluster != null) {
            val gapSec = ((nextCluster - myCluster) * beatDurMs / 1000.0).toFloat()
            minOf(CHORD_AUDITION_DUR_SEC, gapSec).coerceAtLeast(0.1f)
        } else {
            CHORD_AUDITION_DUR_SEC
        }
    }

    fun play(bpm: Int, totalBars: Int) {
        if (_isPlaying.value) return
        _isPlaying.value = true

        val beatDurMs = 60000.0 / bpm.toDouble()
        val loopMode  = _loopEnabled.value
        val loopS     = _loopStart.value
        val loopE     = _loopEnd.value
        val endBeat   = if (loopMode) loopE else computeEndBeat(totalBars)

        fun schedulePass(fromBeat: Float, timeOriginMs: Long) {
            val trackList    = tracks.value
            val allNotesList = _allNotes.value
            trackList.forEach { track ->
                if (track.muted) return@forEach
                val notes        = allNotesList[track.id] ?: emptyList()
                val gmProgram    = gmProgramForTrack(track)
                val clusterBeats = buildChordClusters(notes)

                notes.filter { it.beat >= fromBeat && it.beat < endBeat }
                    .forEach { note ->
                        val delayMs = ((note.beat - fromBeat) * beatDurMs).toLong()
                        val durSec  = chordRingDuration(note, clusterBeats, beatDurMs)
                        viewModelScope.launch(Dispatchers.IO) {
                            val waitMs = timeOriginMs + delayMs - System.currentTimeMillis()
                            if (waitMs > 0) delay(waitMs)
                            if (_isPlaying.value) {
                                val midi = (108 - note.pitch).coerceIn(0, 127)
                                sf2Player.playNote(midi, durSec, note.velocity, gmProgram)
                            }
                        }
                    }
            }
        }

        playbackJob = viewModelScope.launch(Dispatchers.Main) {
            val startBeat   = _playheadBeat.value
            var loopOrigin  = System.currentTimeMillis()
            var currentPass = startBeat

            schedulePass(currentPass, loopOrigin)

            while (_isPlaying.value) {
                val elapsed     = System.currentTimeMillis() - loopOrigin
                val currentBeat = currentPass + (elapsed / beatDurMs).toFloat()

                when {
                    currentBeat >= endBeat && loopMode -> {
                        loopOrigin  = System.currentTimeMillis()
                        currentPass = loopS
                        _playheadBeat.value = loopS
                        schedulePass(loopS, loopOrigin)
                    }
                    currentBeat >= endBeat -> {
                        _playheadBeat.value = endBeat
                        _isPlaying.value = false
                        break
                    }
                    else -> _playheadBeat.value = currentBeat
                }
                delay(16)
            }
        }
    }

    fun stop()   { playbackJob?.cancel(); playbackJob = null; _isPlaying.value = false }
    fun rewind() { stop(); _playheadBeat.value = 0f }

    fun addNote(pitch: Int, beat: Float, duration: Float, velocity: Int = 100) {
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val snap        = snapValues[_snapIndex.value]
        val note = NoteEvent(
            id = UUID.randomUUID().toString(), trackId = activeTrack.id,
            pitch = pitch, beat = (Math.round(beat / snap) * snap),
            duration = duration.coerceAtLeast(snap), velocity = velocity
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

    // ── Selection operations ──────────────────────────────────────────────

    fun selectNote(noteId: String)    { _selectedNoteIds.value = setOf(noteId) }
    fun addToSelection(noteId: String){ _selectedNoteIds.value = _selectedNoteIds.value + noteId }
    fun setSelection(ids: Set<String>){ _selectedNoteIds.value = ids }
    fun clearSelection()              { _selectedNoteIds.value = emptySet() }

    fun deleteSelectedNotes() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.deleteNoteById(it) }
            val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return@launch
            val current = _allNotes.value.toMutableMap()
            current[activeTrack.id] = (current[activeTrack.id] ?: emptyList())
                .filter { it.id !in ids }
            _allNotes.value = current
            _selectedNoteIds.value = emptySet()
            updateActiveNotes()
        }
    }

    /**
     * Move selected notes by deltaBeats horizontally and deltaPitch vertically.
     * Called repeatedly during drag — updates in-memory immediately,
     * persists to Room on drag end (commitMove).
     */
    fun moveSelectedNotes(deltaBeats: Float, deltaPitch: Int, snapValue: Float) {
        val ids         = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val current     = _allNotes.value.toMutableMap()
        val notes       = current[activeTrack.id] ?: return

        val updated = notes.map { note ->
            if (note.id in ids) {
                val newBeat  = (note.beat + deltaBeats).coerceAtLeast(0f)
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
        val ids         = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val notes       = _allNotes.value[activeTrack.id] ?: return
        viewModelScope.launch {
            notes.filter { it.id in ids }.forEach { repository.saveNote(it) }
        }
    }

    fun copySelectedNotes() {
        val ids         = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return
        val notes       = _allNotes.value[activeTrack.id] ?: return
        clipboard       = notes.filter { it.id in ids }
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
        val offset       = pasteAtBeat - earliestBeat

        viewModelScope.launch {
            val newNotes = clipboard.map { note ->
                note.copy(
                    id      = UUID.randomUUID().toString(),
                    trackId = activeTrack.id,
                    beat    = snapBeat(note.beat + offset, snapValue).coerceAtLeast(0f)
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
        val current     = _allNotes.value.toMutableMap()
        val notes       = current[activeTrack.id] ?: return
        val updated     = notes.map { if (it.id == noteId) it.copy(duration = newDuration) else it }
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
        val current     = _allNotes.value.toMutableMap()
        val notes       = current[activeTrack.id] ?: return
        val updated     = notes.map {
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
        val gmProgram   = gmProgramForTrack(activeTrack)
        sf2Player.playNote((108 - pitch).coerceIn(0, 127), 0.8f, velocity, gmProgram)
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