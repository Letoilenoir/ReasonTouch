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
import com.reasontouch.core.playback.PlaybackController
import com.reasontouch.feature.chords.CompositionIntent
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.KeyDetector
import com.reasontouch.feature.chords.PairingDecision
import com.reasontouch.feature.chords.PairingEngine
import com.reasontouch.feature.chords.PairingType
import com.reasontouch.feature.chords.ProgressionAnalyzer
import com.reasontouch.feature.chords.ProgressionGenerationRequest
import com.reasontouch.feature.chords.ProgressionGenerator
import com.reasontouch.feature.chords.guitarLabel
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

    private val sessionId: String =
        checkNotNull(savedStateHandle["sessionId"])

    val session: StateFlow<Session?> =
        repository.getSession(sessionId)
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

    private val _activeTrackIndex = MutableStateFlow(0)
    val activeTrackIndex: StateFlow<Int> =
        _activeTrackIndex.asStateFlow()

    private val _activeNotes =
        MutableStateFlow<List<NoteEvent>>(emptyList())

    val activeNotes: StateFlow<List<NoteEvent>> =
        _activeNotes.asStateFlow()

    private val _allNotes =
        MutableStateFlow<Map<String, List<NoteEvent>>>(emptyMap())

    val allNotes: StateFlow<Map<String, List<NoteEvent>>> =
        _allNotes.asStateFlow()

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

    val transport = playbackController.state

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

                val notes = trackList.associate { track ->
                    track.id to repository.getNotesForTrackOnce(track.id)
                }

                _allNotes.value = notes
                updateActiveNotes()
            }
        }
    }

    private fun updateActiveNotes() {

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        _activeNotes.value =
            _allNotes.value[activeTrack.id]
                ?: emptyList()
    }

    fun setActiveTrack(index: Int) {

        _activeTrackIndex.value = index
        updateActiveNotes()
    }

    fun setTrackVolume(index: Int, volume: Float) {

        viewModelScope.launch {

            val track =
                tracks.value.getOrNull(index)
                    ?: return@launch

            repository.updateTrack(
                track.copy(
                    volume = volume.coerceIn(0f, 1f)
                )
            )
        }
    }

    fun setTrackGmProgram(index: Int, gmProgram: Int) {

        viewModelScope.launch {

            val track =
                tracks.value.getOrNull(index)
                    ?: return@launch

            repository.updateTrack(
                track.copy(gmProgram = gmProgram)
            )
        }
    }

    fun muteTrack(index: Int) {

        viewModelScope.launch {

            val track =
                tracks.value.getOrNull(index)
                    ?: return@launch

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

        viewModelScope.launch {

            repository.saveNote(note)

            val current = _allNotes.value.toMutableMap()

            current[activeTrack.id] =
                (current[activeTrack.id] ?: emptyList()) + note

            _allNotes.value = current

            updateActiveNotes()
        }
    }

    // ---------------------------------------------------------------------
    // DURATION STEPPER
    // ---------------------------------------------------------------------

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

        val current =
            _allNotes.value.toMutableMap()

        val notes =
            current[activeTrack.id]
                ?: return

        val updated = notes.map { note ->

            if (note.id in ids) {
                note.copy(duration = newDuration)
            } else {
                note
            }
        }

        current[activeTrack.id] = updated
        _allNotes.value = current

        updateActiveNotes()

        viewModelScope.launch {

            updated
                .filter { it.id in ids }
                .forEach { repository.saveNote(it) }
        }
    }

    // ---------------------------------------------------------------------
    // NOTE DELETE
    // ---------------------------------------------------------------------

    fun deleteNote(noteId: String) {

        viewModelScope.launch {

            repository.deleteNoteById(noteId)

            val activeTrack =
                tracks.value.getOrNull(_activeTrackIndex.value)
                    ?: return@launch

            val current =
                _allNotes.value.toMutableMap()

            current[activeTrack.id] =
                (current[activeTrack.id] ?: emptyList())
                    .filter { it.id != noteId }

            _allNotes.value = current

            updateActiveNotes()
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

        val current =
            _allNotes.value.toMutableMap()

        val notes =
            current[activeTrack.id]
                ?: return

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

        current[activeTrack.id] = updated
        _allNotes.value = current

        updateActiveNotes()
    }

    fun commitMove() {

        val ids = _selectedNoteIds.value

        if (ids.isEmpty()) return

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val notes =
            _allNotes.value[activeTrack.id]
                ?: return

        viewModelScope.launch {

            notes
                .filter { it.id in ids }
                .forEach {
                    repository.saveNote(it)
                }
        }
    }
// ---------------------------------------------------------------------
// NOTE VELOCITY
// ---------------------------------------------------------------------

    fun updateNoteVelocity(
        noteId: String,
        newVelocity: Int
    ) {

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val current =
            _allNotes.value.toMutableMap()

        val notes =
            current[activeTrack.id]
                ?: return

        val updated = notes.map { note ->

            if (note.id == noteId) {

                note.copy(
                    velocity = newVelocity.coerceIn(1, 127)
                )

            } else {
                note
            }
        }

        current[activeTrack.id] = updated
        _allNotes.value = current

        updateActiveNotes()

        viewModelScope.launch {

            val note =
                updated.firstOrNull { it.id == noteId }
                    ?: return@launch

            repository.saveNote(note)
        }
    }
    // ---------------------------------------------------------------------
// NOTE DURATION
// ---------------------------------------------------------------------

    fun updateNoteDuration(
        noteId: String,
        newDuration: Float
    ) {

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        val current =
            _allNotes.value.toMutableMap()

        val notes =
            current[activeTrack.id]
                ?: return

        val updated = notes.map { note ->

            if (note.id == noteId) {

                note.copy(
                    duration = newDuration.coerceAtLeast(
                        currentSnapValue()
                    )
                )

            } else {
                note
            }
        }

        current[activeTrack.id] = updated
        _allNotes.value = current

        updateActiveNotes()

        viewModelScope.launch {

            val note =
                updated.firstOrNull { it.id == noteId }
                    ?: return@launch

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

        viewModelScope.launch {

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

            newNotes.forEach {
                repository.saveNote(it)
            }

            val current =
                _allNotes.value.toMutableMap()

            current[activeTrack.id] =
                (current[activeTrack.id] ?: emptyList()) + newNotes

            _allNotes.value = current

            _selectedNoteIds.value =
                newNotes.map { it.id }.toSet()

            updateActiveNotes()
        }
    }
    fun deleteSelectedNotes() {

        val ids = _selectedNoteIds.value

        if (ids.isEmpty()) return

        val activeTrack =
            tracks.value.getOrNull(_activeTrackIndex.value)
                ?: return

        viewModelScope.launch {

            ids.forEach {
                repository.deleteNoteById(it)
            }

            val current =
                _allNotes.value.toMutableMap()

            current[activeTrack.id] =
                (current[activeTrack.id] ?: emptyList())
                    .filter { it.id !in ids }

            _allNotes.value = current

            _selectedNoteIds.value = emptySet()

            updateActiveNotes()
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

    suspend fun suggestNextPhrases(): List<GeneratedProgression> {
        val currentProgression = repository.getChordsForSessionOnce(sessionId)
        if (currentProgression.isEmpty()) {
            return emptyList()
        }
        val chordNames = currentProgression.map { it.chordName.substringBefore(" ") }
        val detectedKeys = KeyDetector.detect(chordNames)
        val detectedKey = detectedKeys.firstOrNull() ?: return emptyList()
        val analysis = ProgressionAnalyzer.analyze(currentProgression, detectedKey)
        val pairingDecision = PairingEngine.suggestNext(analysis)
        val intent = pairingDecision.type.toCompositionIntent()
        val request = ProgressionGenerationRequest(
            sourceAnalysis = analysis,
            sourceProgression = currentProgression,
            primaryIntent = intent,
            targetSection = null,
            targetEnergy = null,
            preferredLength = pairingDecision.suggestedBars
        )
        return ProgressionGenerator.generate(request)
    }

    // NEW:
    suspend fun suggestNextSection(): PairingDecision {
        val currentProgression = repository.getChordsForSessionOnce(sessionId)
        if (currentProgression.isEmpty()) {
            return PairingDecision(
                type = PairingType.CONTINUE,
                suggestedBars = 4,
                confidence = 0.3f,
                rationale = "Could not detect key"
            )
        }
        val chordNames = currentProgression.map { it.chordName.substringBefore(" ") }
        val detectedKeys = KeyDetector.detect(chordNames)
        val detectedKey = detectedKeys.firstOrNull() ?: return PairingDecision(
            type = PairingType.CONTINUE,
            suggestedBars = 4,
            confidence = 0.3f,
            rationale = "Could not detect key"
        )
        val analysis = ProgressionAnalyzer.analyze(currentProgression, detectedKey)
        return PairingEngine.suggestNext(analysis)
    }
    // NEW:
    fun addPhrase(generatedProgression: GeneratedProgression) {
        viewModelScope.launch {
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
                val current = _allNotes.value.toMutableMap()
                if (chordTrack != null) {
                    current[chordTrack.id] =
                        (current[chordTrack.id] ?: emptyList()) + newNotes
                }
                _allNotes.value = current
                updateActiveNotes()
            }

            val updatedSession = session.value?.copy(
                totalBars = startBarIndex + generatedProgression.chords.size
            )
            if (updatedSession != null) {
                repository.updateSession(updatedSession)
            }
        }
    }

    private fun PairingType.toCompositionIntent(): CompositionIntent = when (this) {
        PairingType.CONTINUE  -> CompositionIntent.CONTINUE
        PairingType.LIFT      -> CompositionIntent.LIFT
        PairingType.CONTRAST  -> CompositionIntent.CONTRAST
        PairingType.RESOLVE   -> CompositionIntent.RESOLVE
        PairingType.EXPAND    -> CompositionIntent.EXPAND
        PairingType.SURPRISE  -> CompositionIntent.SURPRISE
        PairingType.SIMPLIFY  -> CompositionIntent.SIMPLIFY
        PairingType.MODULATE  -> CompositionIntent.DEVELOP
    }
}
