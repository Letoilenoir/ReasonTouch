package com.reasontouch.feature.chords

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.SessionRepository
import com.reasontouch.core.midi.MidiProgressionBar
import com.reasontouch.core.midi.StepState
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
class ChordViewModel @Inject constructor(
    private val repository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    // -- Session -----------------------------------------------------------
    val session = repository.getSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // -- Chord progression from Room ----------------------------------------
    val progression = repository.getChordsForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -- Chord selection ----------------------------------------------------
    private val _selectedChord    = MutableStateFlow("E")
    private val _selectedPosition = MutableStateFlow("Open")
    private val _selectedCategory = MutableStateFlow("All")

    val selectedChord:    StateFlow<String> = _selectedChord.asStateFlow()
    val selectedPosition: StateFlow<String> = _selectedPosition.asStateFlow()
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val availablePositions: StateFlow<List<String>> = _selectedChord
        .map { chord -> GuitarVoicings.voicings[chord]?.keys?.toList() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf("Open"))

    val filteredChords: StateFlow<List<String>> = _selectedCategory
        .map { cat -> GuitarVoicings.categories[cat] ?: GuitarVoicings.voicings.keys.toList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GuitarVoicings.voicings.keys.toList())

    // -- Step sequencer -----------------------------------------------------
    private val _stepStates = MutableStateFlow(List(16) { StepState.OFF })
    val stepStates: StateFlow<List<StepState>> = _stepStates.asStateFlow()

    // -- Settings -----------------------------------------------------------
    private val _tempo        = MutableStateFlow(120)
    private val _barDuration  = MutableStateFlow(4.0)
    private val _strumSpeed   = MutableStateFlow(0.02)
    private val _strumEnabled = MutableStateFlow(true)
    private val _instrument   = MutableStateFlow(GM_GUITARS[1])

    val tempo:        StateFlow<Int>          = _tempo.asStateFlow()
    val barDuration:  StateFlow<Double>       = _barDuration.asStateFlow()
    val strumSpeed:   StateFlow<Double>       = _strumSpeed.asStateFlow()
    val strumEnabled: StateFlow<Boolean>      = _strumEnabled.asStateFlow()
    val instrument:   StateFlow<GmInstrument> = _instrument.asStateFlow()

    // -- Status -------------------------------------------------------------
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // -- Actions ------------------------------------------------------------
    fun selectCategory(cat: String) { _selectedCategory.value = cat }

    fun selectChord(name: String) {
        _selectedChord.value = name
        val positions = GuitarVoicings.voicings[name]?.keys?.toList() ?: return
        _selectedPosition.value = positions.firstOrNull() ?: ""
    }

    fun selectPosition(pos: String) { _selectedPosition.value = pos }

    fun cycleStep(index: Int) {
        _stepStates.value = _stepStates.value.toMutableList().also {
            it[index] = when (it[index]) {
                StepState.OFF  -> StepState.DOWN
                StepState.DOWN -> StepState.UP
                StepState.UP   -> StepState.OFF
            }
        }
    }

    fun applyPreset(pattern: StepPattern) { _stepStates.value = pattern.steps }

    fun setTempo(bpm: Int)            { _tempo.value = bpm.coerceIn(20, 300) }
    fun setBarDuration(beats: Double) { _barDuration.value = beats.coerceIn(0.5, 32.0) }
    fun setStrumSpeed(v: Double)      { _strumSpeed.value = v.coerceAtLeast(0.0) }
    fun setStrumEnabled(v: Boolean)   { _strumEnabled.value = v }
    fun setInstrument(v: GmInstrument){ _instrument.value = v }

    fun addBar() {
        if (_stepStates.value.all { it == StepState.OFF }) {
            _statusMessage.value = "Set at least one step before adding a bar"
            return
        }
        val chord = _selectedChord.value
        val pos   = _selectedPosition.value
        val notes = GuitarVoicings.voicings[chord]?.get(pos) ?: return

        viewModelScope.launch {
            val barIndex = progression.value.size
            val chordEvent = ChordEvent(
                id           = UUID.randomUUID().toString(),
                sessionId    = sessionId,
                barIndex     = barIndex,
                chordName    = "$chord $pos",
                rootMidi     = notes.firstOrNull { it != null } ?: 0,
                midiNotes    = notes.filterNotNull().joinToString(","),
                voicing      = pos,
                strumPatternId = null
            )
            repository.saveChord(chordEvent)
            _statusMessage.value = null
        }
    }

    fun removeBar(chordEvent: ChordEvent) {
        viewModelScope.launch {
            repository.deleteChord(chordEvent)
        }
    }

    fun clearProgression() {
        viewModelScope.launch {
            repository.deleteChordsForSession(sessionId)
            _stepStates.value = List(16) { StepState.OFF }
            _statusMessage.value = null
        }
    }

    fun clearStatus() { _statusMessage.value = null }

    // -- Build MidiProgressionBars for export/playback ----------------------
    fun buildMidiBars(): List<MidiProgressionBar> {
        return progression.value.map { chord ->
            val notes = chord.midiNotes.split(",").mapIndexed { i, s ->
                s.trim().toIntOrNull()
            }.let { parsed ->
                Array<Int?>(6) { i -> parsed.getOrNull(i) }
            }
            MidiProgressionBar(
                chordName    = chord.chordName,
                notes        = notes,
                steps        = _stepStates.value,
                durationBeats = _barDuration.value,
                tempoBpm     = _tempo.value,
                strumSpeed   = if (_strumEnabled.value) _strumSpeed.value else 0.0,
                gmProgram    = _instrument.value.program
            )
        }
    }
}
