package com.reasontouch.feature.pianoroll

import android.util.Log
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
import com.reasontouch.core.data.SessionSnapshot
import com.reasontouch.core.data.UndoHistoryManager
import com.reasontouch.core.playback.PlaybackController
import com.reasontouch.core.playback.GridSubdivision
import com.reasontouch.core.playback.TransportState
import com.reasontouch.feature.chords.CompositionIntent
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.KeyDetector
import com.reasontouch.feature.chords.PairingDecision
import com.reasontouch.feature.chords.PairingEngine
import com.reasontouch.feature.chords.PairingType
import com.reasontouch.feature.chords.ProgressionAnalyzer
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.ProgressionGenerator
import com.reasontouch.feature.chords.SuggestionWorkflow
import com.reasontouch.feature.chords.guitarLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PianoRollViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val sf2Player: Sf2Player,
    private val drumSamplePlayer: DrumSamplePlayer,
    private val playbackController: PlaybackController,
    private val undoHistoryManager: UndoHistoryManager,
    private val soundPackManager: com.reasontouch.core.audio.SoundPackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String =
        checkNotNull(savedStateHandle["sessionId"])

    val session: StateFlow<Session?> =
        repository.getSession(sessionId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    /**
     * The currently active sound pack's manifest, re-derived whenever
     * SoundPackManager.activePackId changes. Phase 1 of the Instrument
     * Panel sound-pack display work -- see Phase 2 for where this gets
     * consumed in InstrumentPanel. null means "default" (no custom pack).
     */
    val activeManifest: StateFlow<com.reasontouch.core.audio.SoundPackManifest?> =
        soundPackManager.activePackIdFlow
            .map { packId -> soundPackManager.getManifest(packId) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    val tracks: StateFlow<List<MidiTrack>> =
        repository.getTracksForSession(sessionId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val chords: StateFlow<List<ChordEvent>> =
        repository.getChordsForSession(sessionId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun recordSnapshot() {
        val currentChords = chords.value
        val currentNotes = _allNotes.value
        undoHistoryManager.push(sessionId, SessionSnapshot(currentChords, currentNotes))
    }

    fun undo() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = undoHistoryManager.pop(sessionId) ?: return@launch
            repository.deleteChordsForSession(sessionId)
            repository.saveChords(snapshot.chords)
            tracks.value.forEach { track ->
                repository.deleteNotesForTrack(track.id)
                snapshot.notesMap[track.id]?.let { repository.saveNotes(it) }
            }
            _allNotes.update { snapshot.notesMap }
        }
    }

    private val _activeTrackIndex = MutableStateFlow(0)
    val activeTrackIndex: StateFlow<Int> =
        _activeTrackIndex.asStateFlow()

    private val _gridSubdivision = MutableStateFlow(GridSubdivision.STRAIGHT_16TH)
    val gridSubdivision: StateFlow<GridSubdivision> =
        _gridSubdivision.asStateFlow()

    fun setGridSubdivision(subdivision: GridSubdivision) {
        _gridSubdivision.value = subdivision
    }

    private val _allNotes =
        MutableStateFlow<Map<String, List<NoteEvent>>>(emptyMap())

    val allNotes: StateFlow<Map<String, List<NoteEvent>>> =
        _allNotes.asStateFlow()

    val activeNotes: StateFlow<List<NoteEvent>> =
        combine(_allNotes, tracks, _activeTrackIndex) { all, trackList, activeIndex ->
            val activeTrack = trackList.getOrNull(activeIndex)
            activeTrack?.let { all[it.id] } ?: emptyList()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    enum class Tool {
        DRAW,
        SELECT,
        ERASE
    }

    private val _currentTool =
        MutableStateFlow(Tool.DRAW)

    val currentTool: StateFlow<Tool> =
        _currentTool.asStateFlow()

    val snapValues =
        listOf(1f, 0.5f, 0.25f, 0.125f, 0.0625f)

    val snapLabels =
        listOf("1/4", "1/8", "1/16", "1/32", "1/64")

    private val _snapIndex = MutableStateFlow(2)

    val snapIndex: StateFlow<Int> =
        _snapIndex.asStateFlow()

    private val _selectedNoteIds =
        MutableStateFlow<Set<String>>(emptySet())

    val selectedNoteIds: StateFlow<Set<String>> =
        _selectedNoteIds.asStateFlow()

    private var clipboard: List<NoteEvent> = emptyList()

    private val _hasClipboard =
        MutableStateFlow(false)

    val hasClipboard: StateFlow<Boolean> =
        _hasClipboard.asStateFlow()

    val transport: StateFlow<TransportState> = playbackController.state

    // GLOBAL DRAW DURATION
    val drawDuration: StateFlow<Float> =
        playbackController.drawDuration

    init {

        // Resize selected notes whenever transport duration changes
        viewModelScope.launch {
            playbackController.drawDuration.collect { newDuration ->

                if (_selectedNoteIds.value.isNotEmpty()) {
                    resizeSelectedNotes(newDuration)
                }
            }
        }

        viewModelScope.launch {
            tracks.collect { trackList ->
                val fetched = fetchAllNotes(trackList)
                _allNotes.update { fetched }
            }
        }
    }

    private suspend fun fetchAllNotes(
        trackList: List<MidiTrack>
    ): Map<String, List<NoteEvent>> = withContext(Dispatchers.IO) {
        trackList.associate { track ->
            track.id to repository.getNotesForTrackOnce(track.id)
        }
    }

    /**
     * Force a re-fetch of all track notes from the repository. Needed because
     * _allNotes only auto-updates when the track list itself changes (see init{}),
     * not when notes are written by a separate ViewModel instance -- e.g. SUGGEST
     * continuations via ChordViewModel.addPhrase() (Phase 3's addPhrase consolidation).
     * Confirmed bug 2026-08-27: grid stayed stale after SUGGEST until navigating
     * away and back, which recreated this ViewModel and re-ran init{}.
     */
    fun refreshNotes() {
        viewModelScope.launch {
            val fetched = fetchAllNotes(tracks.value)
            _allNotes.update { fetched }
        }
    }

    fun setActiveTrack(index: Int) {
        _activeTrackIndex.value = index
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = tracks.value.firstOrNull { it.id == trackId } ?: return@launch
            repository.updateTrack(
                track.copy(
                    volume = volume.coerceIn(0f, 1f)
                )
            )
        }
    }

    fun setTrackGmProgram(trackId: String, gmProgram: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = tracks.value.firstOrNull { it.id == trackId } ?: return@launch
            repository.updateTrack(
                track.copy(gmProgram = gmProgram)
            )
        }
    }

    fun muteTrack(trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = tracks.value.firstOrNull { it.id == trackId } ?: return@launch
            repository.updateTrack(
                track.copy(muted = !track.muted)
            )
        }
    }

    fun setTool(tool: Tool) {

        _currentTool.value = tool
        _selectedNoteIds.value = emptySet()
    }

    fun setSnapIndex(index: Int) {
        _snapIndex.value = index
    }

    fun increaseSnapIndex() {

        _snapIndex.value =
            (_snapIndex.value + 1)
                .coerceAtMost(snapValues.lastIndex)
    }

    fun decreaseSnapIndex() {

        _snapIndex.value =
            (_snapIndex.value - 1)
                .coerceAtLeast(0)
    }

    fun currentSnapValue(): Float =
        snapValues[_snapIndex.value]

    fun currentSnapLabel(): String =
        snapLabels[_snapIndex.value]

    // ---------------------------------------------------------------------
    // DRAW NOTE
    // ---------------------------------------------------------------------

    // -- Draw duration state ------------------------------------

      fun addNote(
        pitch: Int,
        beat: Float,
        duration: Float = playbackController.drawDuration.value,
        velocity: Int = 100
    ) {
        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val snap = currentSnapValue()

        val note = NoteEvent(
            id = UUID.randomUUID().toString(),
            trackId = activeTrack.id,
            pitch = pitch,
            beat = (Math.round(beat / snap) * snap),
            duration = duration.coerceAtLeast(snap),
            velocity = velocity
        )

        _allNotes.update { current ->
            val trackNotes = (current[activeTrack.id] ?: emptyList()) + note
            current + (activeTrack.id to trackNotes)
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNote(note)
        }
    }

    /**
     * Saves a batch of already-generated notes (potentially across multiple
     * tracks) directly, without going through the draw-tool/snap logic
     * addNote() uses. Added for Phase 8b's Full Groove preview -- Apply
     * writes both the previewed Bass and Drum notes in one call.
     */
    fun saveGeneratedNotes(notes: List<NoteEvent>) {
        if (notes.isEmpty()) return

        _allNotes.update { current ->
            val updated = current.toMutableMap()
            notes.groupBy { it.trackId }.forEach { (trackId, trackNotes) ->
                updated[trackId] = (updated[trackId] ?: emptyList()) + trackNotes
            }
            updated
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNotes(notes)
        }
    }

    // ---------------------------------------------------------------------
    // DURATION STEPPER
    // --------------------------------------------------------------------------------------------------------------------------------------

    fun stepDurationStepper(direction: Int) {

        val durations = listOf(
            0.0625f, // 1/64
            0.125f,  // 1/32
            0.25f,   // 1/16
            0.5f,    // 1/8
            1f,      // 1/4
            2f,      // 1/2
            4f,      // 1 bar
            8f       // 2 bars
        )

        val current =
            playbackController.drawDuration.value

        val currentIndex =
            durations.indexOf(current)
                .coerceAtLeast(0)

        val nextIndex =
            (currentIndex + direction)
                .coerceIn(0, durations.lastIndex)

        val nextDuration =
            durations[nextIndex]

        // ALWAYS update transport duration
        playbackController.setDrawDuration(nextDuration)

        // IF notes selected -> resize them
        if (_selectedNoteIds.value.isNotEmpty()) {
            resizeSelectedNotes(nextDuration)
        }
    }

    private fun resizeSelectedNotes(newDuration: Float) {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        var updatedNotesToSave: List<NoteEvent> = emptyList()

        _allNotes.update { current ->
            val notes = current[activeTrack.id] ?: return@update current
            val updated = notes.map { note ->
                if (note.id in ids) {
                    note.copy(duration = newDuration)
                } else {
                    note
                }
            }
            updatedNotesToSave = updated.filter { it.id in ids }
            current + (activeTrack.id to updated)
        }

        if (updatedNotesToSave.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveNotes(updatedNotesToSave)
            }
        }
    }

    // ---------------------------------------------------------------------
    // NOTE DELETE
    // ---------------------------------------------------------------------

    fun deleteNote(noteId: String) {
        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)

        _allNotes.update { current ->
            if (activeTrack != null) {
                val trackNotes = (current[activeTrack.id] ?: emptyList()).filter { it.id != noteId }
                current + (activeTrack.id to trackNotes)
            } else {
                current.mapValues { (_, notes) -> notes.filter { it.id != noteId } }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNoteById(noteId)
        }
    }

    // ---------------------------------------------------------------------
    // NOTE MOVE
    // ---------------------------------------------------------------------

    fun moveSelectedNotes(
        deltaBeats: Float,
        deltaPitch: Int,
        snapValue: Float
    ) {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        _allNotes.update { current ->
            val notes = current[activeTrack.id] ?: return@update current
            val updated = notes.map { note ->
                if (note.id in ids) {
                    val newBeat =
                        snapBeat(
                            note.beat + deltaBeats,
                            snapValue
                        ).coerceAtLeast(0f)

                    val newPitch =
                        (note.pitch + deltaPitch)
                            .coerceIn(0, 127)

                    note.copy(
                        beat = newBeat,
                        pitch = newPitch
                    )
                } else {
                    note
                }
            }
            current + (activeTrack.id to updated)
        }
    }

    fun commitMove() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val notesToSave = _allNotes.value[activeTrack.id]?.filter { it.id in ids } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNotes(notesToSave)
        }
    }

    // ---------------------------------------------------------------------

    fun updateNoteDuration(
        noteId: String,
        newDuration: Float
    ) {
        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val minDuration = currentSnapValue()
        var updatedNote: NoteEvent? = null

        _allNotes.update { current ->
            val notes = current[activeTrack.id] ?: return@update current
            val updated = notes.map { note ->
                if (note.id == noteId) {
                    val noteUpdated = note.copy(
                        duration = newDuration.coerceAtLeast(minDuration)
                    )
                    updatedNote = noteUpdated
                    noteUpdated
                } else {
                    note
                }
            }
            current + (activeTrack.id to updated)
        }

        val note = updatedNote ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNote(note)
        }
    }

    // ---------------------------------------------------------------------
    // NOTE SELECTION
    // ---------------------------------------------------------------------

    fun selectNote(
        noteId: String,
        addToSelection: Boolean = false
    ) {
        _selectedNoteIds.value =
            if (addToSelection) {
                _selectedNoteIds.value + noteId
            } else {
                setOf(noteId)
            }
    }

    fun setSelection(noteIds: Set<String>) {
        _selectedNoteIds.value = noteIds
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    // ---------------------------------------------------------------------
    // COPY / PASTE
    // ---------------------------------------------------------------------

    fun copySelectedNotes() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val notes =
            _allNotes.value[activeTrack.id]
                ?: return

        clipboard =
            notes.filter { it.id in ids }

        _hasClipboard.value =
            clipboard.isNotEmpty()
    }

    fun pasteNotes(
        pasteAtBeat: Float,
        snapValue: Float
    ) {
        if (clipboard.isEmpty()) return

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val earliestBeat =
            clipboard.minOf { it.beat }

        val offset =
            pasteAtBeat - earliestBeat

        val newNotes = clipboard.map { note ->
            note.copy(
                id = UUID.randomUUID().toString(),
                trackId = activeTrack.id,
                beat = snapBeat(
                    note.beat + offset,
                    snapValue
                ).coerceAtLeast(0f)
            )
        }

        recordSnapshot()

        _allNotes.update { current ->
            val trackNotes = (current[activeTrack.id] ?: emptyList()) + newNotes
            current + (activeTrack.id to trackNotes)
        }

        _selectedNoteIds.value =
            newNotes.map { it.id }.toSet()

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNotes(newNotes)
        }
    }

    fun deleteSelectedNotes() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        recordSnapshot()

        _allNotes.update { current ->
            val trackNotes = (current[activeTrack.id] ?: emptyList())
                .filter { it.id !in ids }
            current + (activeTrack.id to trackNotes)
        }

        _selectedNoteIds.value = emptySet()

        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach {
                repository.deleteNoteById(it)
            }
        }
    }

    fun transposeSelectedNotes(delta: Int) {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return

        recordSnapshot()

        var notesToSave: List<NoteEvent> = emptyList()

        _allNotes.update { current ->
            val notes = current[activeTrack.id] ?: emptyList()
            val updatedNotes = notes.map { note ->
                if (note.id in ids) {
                    note.copy(pitch = (note.pitch + delta).coerceIn(0, 87))
                } else note
            }
            notesToSave = updatedNotes
            current + (activeTrack.id to updatedNotes)
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotesForTrack(activeTrack.id)
            repository.saveNotes(notesToSave)
        }
    }

    fun invertSelectedNotes() {
        val ids = _selectedNoteIds.value
        if (ids.isEmpty()) return
        val activeTrack = tracks.value.getOrNull(_activeTrackIndex.value) ?: return

        val notes = _allNotes.value[activeTrack.id] ?: emptyList()
        val selectedNotes = notes.filter { it.id in ids }
        if (selectedNotes.isEmpty()) return
        val avgPitch = selectedNotes.map { it.pitch }.average().toInt()

        recordSnapshot()

        var notesToSave: List<NoteEvent> = emptyList()

        _allNotes.update { current ->
            val currentNotes = current[activeTrack.id] ?: emptyList()
            val updatedNotes = currentNotes.map { note ->
                if (note.id in ids) {
                    val reflected = (2 * avgPitch - note.pitch).coerceIn(0, 87)
                    note.copy(pitch = reflected)
                } else note
            }
            notesToSave = updatedNotes
            current + (activeTrack.id to updatedNotes)
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotesForTrack(activeTrack.id)
            repository.saveNotes(notesToSave)
        }
    }

    // ---------------------------------------------------------------------
    // PLAYBACK
    // ---------------------------------------------------------------------

    val isPlaying: StateFlow<Boolean> =
        playbackController.state
            .map { it.isPlaying }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false
            )

    val playheadBeat: StateFlow<Float> =
        playbackController.state
            .map { it.playheadBeat }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0f
            )
    val loopEnabled: StateFlow<Boolean> =
        playbackController.state
            .map { it.loopEnabled }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false
            )

    val loopStart: StateFlow<Float> =
        playbackController.state
            .map { it.loopStart }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0f
            )

    val loopEnd: StateFlow<Float> =
        playbackController.state
            .map { it.loopEnd }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                4f
            )

    fun setPlayhead(
        beat: Float,
        totalBars: Int = 4
    ) {

        val clamped =
            beat.coerceIn(0f, totalBars * 4f)

        playbackController.seekTo(clamped)
    }

    fun toggleLoop() {

        playbackController.setLoop(
            !playbackController.state.value.loopEnabled
        )
    }

    fun setLoopStart(beat: Float) =
        playbackController.setLoopStart(beat)

    fun setLoopEnd(beat: Float) =
        playbackController.setLoopEnd(beat)

    // ---------------------------------------------------------------------
    // NOTE AUDITION
    // ---------------------------------------------------------------------

    fun auditionNote(
        pitch: Int,
        velocity: Int = 100
    ) {

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val midi =
            (108 - pitch).coerceIn(0, 127)

        if (isDrumTrack(activeTrack)) {

            drumSamplePlayer.play(
                midi,
                velocity
            )

        } else {

            sf2Player.playNote(
                midi,
                0.8f,
                velocity,
                gmProgramForTrack(activeTrack)
            )
        }
    }

    // ---------------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------------

    private fun isDrumTrack(track: MidiTrack): Boolean {

        return track.midiChannel == 9 ||
                track.name.uppercase() in listOf(
            "DRUMS",
            "DRUM"
        )
    }

    private fun gmProgramForTrack(track: MidiTrack): Int =
        track.gmProgram

    private fun snapBeat(
        beat: Float,
        snapValue: Float
    ): Float {

        return (
                Math.round(beat / snapValue) * snapValue
                )
    }

    // Delegates to SuggestionWorkflow (feature-chords module) -- see that
    // file for the shared logic and Task_List.md for why this was
    // consolidated. Still suspend: the progression is fetched fresh from
    // the repository rather than via the chords StateFlow, unchanged from
    // the original behavior.

    suspend fun suggestNextPhrases(): List<GeneratedProgression> = withContext(Dispatchers.Default) {
        val currentProgression = repository.getChordsForSessionOnce(sessionId)
        SuggestionWorkflow.suggestNextPhrases(currentProgression)
    }

    suspend fun suggestNextSection(): PairingDecision = withContext(Dispatchers.Default) {
        val currentProgression = repository.getChordsForSessionOnce(sessionId)
        SuggestionWorkflow.suggestNextSection(currentProgression)
    }

    /**
     * Multi-option entry point for the SUGGEST NEXT dialog -- returns every ranked intent
     * paired with its generated phrases, rather than collapsing to a single top pick.
     * See SuggestionWorkflow.suggestNextOptionsWithPhrases().
     */
    suspend fun suggestNextOptions(): List<SuggestionWorkflow.SuggestionOption> = withContext(Dispatchers.Default) {
        val currentProgression = repository.getChordsForSessionOnce(sessionId)
        SuggestionWorkflow.suggestNextOptionsWithPhrases(currentProgression)
    }

    // NEW:
    fun addPhrase(generatedProgression: GeneratedProgression) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentProgression = repository.getChordsForSessionOnce(sessionId)
            if (currentProgression.isEmpty()) return@launch

            val startBarIndex = currentProgression.size
            val beatsPerBar = (session.value?.timeSignatureNumerator ?: 4).toFloat()
            val chordTrack = tracks.value.firstOrNull { it.name == "CHORD" }

            val newNotes = mutableListOf<NoteEvent>()

            generatedProgression.chords.forEachIndexed { index, theoryChord ->
                val barIndex = startBarIndex + index
                val chordEvent = ChordEvent(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    barIndex = barIndex,
                    chordName = theoryChord.guitarLabel(),
                    rootMidi = 60,
                    midiNotes = theoryChord.midiNotes.joinToString(","),
                    voicing = "Open",
                    strumPatternId = null
                )
                repository.saveChord(chordEvent)

                if (chordTrack != null) {
                    val beatStart = barIndex * beatsPerBar
                    theoryChord.midiNotes.forEach { midiNote ->
                        newNotes.add(
                            NoteEvent(
                                id = UUID.randomUUID().toString(),
                                trackId = chordTrack.id,
                                pitch = 108 - midiNote,
                                beat = beatStart,
                                duration = beatsPerBar,
                                velocity = 80
                            )
                        )
                    }
                }
            }

            if (newNotes.isNotEmpty()) {
                repository.saveNotes(newNotes)
                if (chordTrack != null) {
                    _allNotes.update { current ->
                        val trackNotes = (current[chordTrack.id] ?: emptyList()) + newNotes
                        current + (chordTrack.id to trackNotes)
                    }
                }
            }

            val updatedSession = session.value?.copy(
                totalBars = startBarIndex + generatedProgression.chords.size
            )
            if (updatedSession != null) {
                repository.updateSession(updatedSession)
            }
        }
    }

}
