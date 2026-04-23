package com.reasontouch.feature.chords

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.NoteEvent
import com.reasontouch.core.data.SessionRepository
import com.reasontouch.core.midi.StepState
import com.reasontouch.core.audio.Sf2Player
import com.reasontouch.core.audio.SynthEngine
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
    private val synthEngine: SynthEngine,
    private val sf2Player: Sf2Player,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val session = repository.getSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val tracks = repository.getTracksForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val progression = repository.getChordsForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _stepStates = MutableStateFlow(List(16) { StepState.OFF })
    val stepStates: StateFlow<List<StepState>> = _stepStates.asStateFlow()

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

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // ── Harmony suggestion state ──────────────────────────────────────────
    private val _harmonyState = MutableStateFlow(HarmonyState())
    val harmonyState: StateFlow<HarmonyState> = _harmonyState.asStateFlow()

    fun selectCategory(cat: String) { _selectedCategory.value = cat }

    fun selectChord(name: String) {
        _selectedChord.value = name
        val positions = GuitarVoicings.voicings[name]?.keys?.toList() ?: return
        val pos = positions.firstOrNull() ?: ""
        _selectedPosition.value = pos
        auditionChord(name, pos)
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
                id             = UUID.randomUUID().toString(),
                sessionId      = sessionId,
                barIndex       = barIndex,
                chordName      = "$chord $pos",
                rootMidi       = notes.firstOrNull { it != null } ?: 0,
                midiNotes      = notes.filterNotNull().joinToString(","),
                voicing        = pos,
                strumPatternId = null
            )
            repository.saveChord(chordEvent)
            _statusMessage.value = null
        }
    }

    fun removeBar(chordEvent: ChordEvent) {
        viewModelScope.launch { repository.deleteChord(chordEvent) }
    }

    fun clearProgression() {
        viewModelScope.launch {
            repository.deleteChordsForSession(sessionId)
            _stepStates.value = List(16) { StepState.OFF }
            _statusMessage.value = null
            _harmonyState.value = HarmonyState()
        }
    }

    fun clearStatus() { _statusMessage.value = null }

    fun auditionChord(chordName: String, position: String) {
        val notes      = GuitarVoicings.voicings[chordName]?.get(position) ?: return
        val midiNotes  = notes.filterNotNull()
        val gmProgram  = _instrument.value.program
        val strumDelay = if (_strumEnabled.value) (_strumSpeed.value * 1000).toLong() else 0L
        sf2Player.playChord(midiNotes, 0.5f, 90, gmProgram, strumDelay)
    }

    /** Audition a suggestion chord directly from its MIDI notes */
    fun auditionSuggestion(suggestion: ChordSuggestion) {
        val gmProgram  = _instrument.value.program
        val strumDelay = if (_strumEnabled.value) (_strumSpeed.value * 1000).toLong() else 0L
        sf2Player.playChord(suggestion.chord.midiNotes, 0.5f, 90, gmProgram, strumDelay)
    }

    private fun strumVelocity(strIdx: Int, stringCount: Int, isDownstroke: Boolean): Int {
        val baseVel  = if (isDownstroke) 105 else 90
        val taperVel = if (isDownstroke) 72  else 65
        val t        = strIdx.toFloat() / (stringCount - 1).coerceAtLeast(1)
        return (baseVel - t * (baseVel - taperVel)).toInt().coerceIn(40, 127)
    }

    // ── Harmony analysis ──────────────────────────────────────────────────

    /**
     * Analyses the current progression and detects candidate keys.
     * Called when user taps SUGGEST in the toolbar.
     * Option B: shows key candidates for user selection before generating suggestions.
     */
    fun analyseProgression() {
        val bars = progression.value
        if (bars.isEmpty()) {
            _statusMessage.value = "Add at least one bar before analysing"
            return
        }

        _harmonyState.value = _harmonyState.value.copy(
            isAnalysing   = true,
            showPanel     = true,
            keyCandidates = emptyList(),
            selectedKey   = null,
            suggestions   = emptyList()
        )

        viewModelScope.launch {
            // Extract chord names from progression (just the chord part, not voicing)
            val chordNames = bars.map { it.chordName.trim().split(" ").first() }
            val candidates = KeyDetector.detect(chordNames, _harmonyState.value.moodBias)

            _harmonyState.value = _harmonyState.value.copy(
                isAnalysing   = false,
                keyCandidates = candidates,
                selectedKey   = null,
                suggestions   = emptyList()
            )
        }
    }

    /**
     * User selects a key from the candidates.
     * Immediately generates chord suggestions for that key.
     */
    fun selectKey(candidate: KeyCandidate) {
        val lastChordName = progression.value.lastOrNull()?.chordName
            ?.trim()?.split(" ")?.first()

        val suggestions = if (lastChordName != null) {
            ChordSuggestionEngine.suggest(candidate, lastChordName)
        } else {
            ChordSuggestionEngine.suggestAll(candidate)
        }

        val borrowed = ChordSuggestionEngine.borrowedChords(candidate)

        _harmonyState.value = _harmonyState.value.copy(
            selectedKey    = candidate,
            suggestions    = suggestions,
            borrowedChords = borrowed
        )
    }

    /**
     * User taps a suggestion — adds it as the next bar in the progression.
     * Finds the best matching chord in GuitarVoicings or falls back to
     * the first available voicing for that root.
     */
    private val SHARP_TO_FLAT = mapOf(
        "C#" to "Db", "D#" to "Eb", "F#" to "Gb", "G#" to "Ab", "A#" to "Bb"
    )

    fun addSuggestedChord(suggestion: ChordSuggestion) {
        val rootLabel    = suggestion.chord.root.label
        val qualitySuffix = when (suggestion.chord.quality) {
            ChordQuality.MIN  -> "m"
            ChordQuality.DIM  -> "dim"
            ChordQuality.AUG  -> "aug"
            ChordQuality.DOM7 -> "7"
            ChordQuality.MAJ7 -> "maj7"
            ChordQuality.MIN7 -> "m7"
            ChordQuality.SUS2 -> "sus2"
            ChordQuality.SUS4 -> "sus4"
            ChordQuality.MAJ  -> ""
        }
        val chordKey = "$rootLabel$qualitySuffix"

        // Try sharp form first, then flat enharmonic equivalent
        val flatRoot    = SHARP_TO_FLAT[rootLabel] ?: rootLabel
        val flatChordKey = "$flatRoot$qualitySuffix"

        // Find voicing � try sharp, then flat, then prefix match
        // Also track which key was actually matched for correct display name
        val voicingEntry = when {
            GuitarVoicings.voicings.containsKey(chordKey)     ->
                chordKey to GuitarVoicings.voicings[chordKey]!!
            GuitarVoicings.voicings.containsKey(flatChordKey) ->
                flatChordKey to GuitarVoicings.voicings[flatChordKey]!!
            else -> GuitarVoicings.voicings.entries
                .firstOrNull {
                    it.key.startsWith(rootLabel) || it.key.startsWith(flatRoot)
                }?.let { it.key to it.value }
        }
        val resolvedKey = voicingEntry?.first
        val voicingMap  = voicingEntry?.second

        if (voicingMap == null) {
            _statusMessage.value = "No voicing found for ${suggestion.chord.label} — tap to add manually"
            return
        }

        val pos   = voicingMap.keys.first()
        val notes = voicingMap[pos] ?: return

        viewModelScope.launch {
            val barIndex = progression.value.size
            val chordEvent = ChordEvent(
                id        = UUID.randomUUID().toString(),
                sessionId = sessionId,
                barIndex  = barIndex,
                chordName = "${resolvedKey ?: chordKey} $pos",
                rootMidi       = notes.firstOrNull { it != null } ?: 0,
                midiNotes      = notes.filterNotNull().joinToString(","),
                voicing        = pos,
                strumPatternId = null
            )
            repository.saveChord(chordEvent)

            // Refresh suggestions based on new last chord
            val selectedKey = _harmonyState.value.selectedKey
            if (selectedKey != null) {
                val newSuggestions = ChordSuggestionEngine.suggest(selectedKey, resolvedKey ?: chordKey)
                val newBorrowed    = ChordSuggestionEngine.borrowedChords(selectedKey)
                _harmonyState.value = _harmonyState.value.copy(
                    suggestions    = newSuggestions,
                    borrowedChords = newBorrowed
                )
            }
            _statusMessage.value = null
        }
    }

    private val _bassGenerating = MutableStateFlow(false)
    val bassGenerating: StateFlow<Boolean> = _bassGenerating.asStateFlow()

    /**
     * Generate bass notes from the current progression.
     * Finds the BASS track automatically.
     * appendMode: if true, adds after existing bass content rather than replacing it.
     */
    fun generateBass(style: BassStyle, appendMode: Boolean) {
        viewModelScope.launch {
            val bars = progression.value
            if (bars.isEmpty()) {
                _statusMessage.value = "Add bars to the progression first"
                return@launch
            }

            val trackList  = tracks.value
            val bassTrack  = trackList.firstOrNull { it.name.uppercase() == "BASS" }
                ?: trackList.firstOrNull()
                ?: return@launch

            _bassGenerating.value = true

            val appendOffset = if (appendMode) {
                val lastBeat    = getLastBeatOnTrack(bassTrack.id)
                val beatsPerBar = _barDuration.value.toFloat()
                if (lastBeat <= 0f) 0f
                else {
                    val barsUsed = kotlin.math.ceil(lastBeat / beatsPerBar).toInt()
                    barsUsed * beatsPerBar
                }
            } else {
                // Overwrite � clear existing bass notes first
                repository.deleteNotesForTrack(bassTrack.id)
                0f
            }

            val notes = BassGenerator.generate(
                chords        = bars,
                style         = style,
                targetTrackId = bassTrack.id,
                beatsPerBar   = _barDuration.value.toFloat(),
                appendOffset  = appendOffset,
                snapValue     = 0.25f
            )

            repository.saveNotes(notes)
            _bassGenerating.value = false
            _statusMessage.value  = "Bass generated: ${style.label} (${notes.size} notes)"
        }
    }

    fun auditionBorrowed(borrowed: BorrowedChord) {
        val gmProgram  = _instrument.value.program
        val strumDelay = if (_strumEnabled.value) (_strumSpeed.value * 1000).toLong() else 0L
        sf2Player.playChord(borrowed.chord.midiNotes, 0.5f, 90, gmProgram, strumDelay)
    }

    fun addBorrowedChord(borrowed: BorrowedChord) {
        val rootLabel     = borrowed.chord.root.label
        val flatRoot      = SHARP_TO_FLAT[rootLabel] ?: rootLabel
        val qualitySuffix = when (borrowed.chord.quality) {
            ChordQuality.MIN  -> "m"
            ChordQuality.DIM  -> "dim"
            ChordQuality.AUG  -> "aug"
            ChordQuality.DOM7 -> "7"
            ChordQuality.MAJ7 -> "maj7"
            ChordQuality.MIN7 -> "m7"
            ChordQuality.SUS2 -> "sus2"
            ChordQuality.SUS4 -> "sus4"
            ChordQuality.MAJ  -> ""
        }
        val chordKey     = "$rootLabel$qualitySuffix"
        val flatChordKey = "$flatRoot$qualitySuffix"

        val voicingEntry = when {
            GuitarVoicings.voicings.containsKey(chordKey)     ->
                chordKey to GuitarVoicings.voicings[chordKey]!!
            GuitarVoicings.voicings.containsKey(flatChordKey) ->
                flatChordKey to GuitarVoicings.voicings[flatChordKey]!!
            else -> GuitarVoicings.voicings.entries
                .firstOrNull {
                    it.key.startsWith(rootLabel) || it.key.startsWith(flatRoot)
                }?.let { it.key to it.value }
        }

        val resolvedKey = voicingEntry?.first
        val voicingMap  = voicingEntry?.second

        if (voicingMap == null) {
            _statusMessage.value =
                "No voicing for ${borrowed.chord.guitarLabel()} � add manually"
            return
        }

        val pos   = voicingMap.keys.first()
        val notes = voicingMap[pos] ?: return

        viewModelScope.launch {
            val barIndex = progression.value.size
            val chordEvent = ChordEvent(
                id             = UUID.randomUUID().toString(),
                sessionId      = sessionId,
                barIndex       = barIndex,
                chordName      = "${resolvedKey ?: chordKey} $pos",
                rootMidi       = notes.firstOrNull { it != null } ?: 0,
                midiNotes      = notes.filterNotNull().joinToString(","),
                voicing        = pos,
                strumPatternId = null
            )
            repository.saveChord(chordEvent)

            // Refresh suggestions for the new last chord
            val selectedKey = _harmonyState.value.selectedKey
            if (selectedKey != null) {
                val newSuggestions = ChordSuggestionEngine.suggest(
                    selectedKey, resolvedKey ?: chordKey)
                val newBorrowed = ChordSuggestionEngine.borrowedChords(selectedKey)
                _harmonyState.value = _harmonyState.value.copy(
                    suggestions    = newSuggestions,
                    borrowedChords = newBorrowed
                )
            }
            _statusMessage.value = null
        }
    }

    fun setMoodBias(bias: Float) {
        val bars = progression.value
        if (bars.isEmpty()) return

        _harmonyState.value = _harmonyState.value.copy(
            moodBias      = bias,
            isAnalysing   = true,
            selectedKey   = null,
            suggestions   = emptyList(),
            borrowedChords = emptyList()
        )

        viewModelScope.launch {
            val chordNames = bars.map { it.chordName.trim().split(" ").first() }
            val candidates = KeyDetector.detect(chordNames, bias)
            _harmonyState.value = _harmonyState.value.copy(
                isAnalysing   = false,
                keyCandidates = candidates
            )
        }
    }

    fun dismissHarmonyPanel() {
        _harmonyState.value = HarmonyState()
    }

    suspend fun getLastBeatOnTrack(trackId: String): Float {
        val notes = repository.getNotesForTrackOnce(trackId)
        return notes.maxOfOrNull { it.beat + it.duration } ?: 0f
    }

    fun sendToPianoRoll(
        targetTrackIndex: Int,
        useStrum:         Boolean,
        appendMode:       Boolean,
        onComplete:       () -> Unit
    ) {
        viewModelScope.launch {
            val trackList   = tracks.value
            val targetTrack = trackList.getOrNull(targetTrackIndex) ?: return@launch
            val bars        = progression.value
            if (bars.isEmpty()) {
                _statusMessage.value = "No progression to send"
                return@launch
            }

            val startFromBeat = if (appendMode) {
                val lastBeat    = getLastBeatOnTrack(targetTrack.id)
                val beatsPerBar = _barDuration.value
                if (lastBeat <= 0f) 0f
                else {
                    val barsUsed = kotlin.math.ceil(lastBeat / beatsPerBar).toInt()
                    (barsUsed * beatsPerBar).toFloat()
                }
            } else 0f

            val beatsPerBar   = _barDuration.value
            val strumSpeedVal = if (useStrum && _strumEnabled.value) _strumSpeed.value else 0.0

            bars.forEachIndexed { barIdx, chord ->
                val startBeat  = startFromBeat + (barIdx * beatsPerBar).toFloat()
                val midiNotes  = chord.midiNotes.split(",").mapNotNull { it.trim().toIntOrNull() }
                val notesPairs = midiNotes.mapIndexed { i, midi -> Pair(i, midi) }

                if (!useStrum || strumSpeedVal == 0.0) {
                    notesPairs.forEach { (_, midi) ->
                        val pitch = (108 - midi).coerceIn(0, 87)
                        repository.saveNote(NoteEvent(
                            id       = UUID.randomUUID().toString(),
                            trackId  = targetTrack.id,
                            pitch    = pitch,
                            beat     = startBeat,
                            duration = beatsPerBar.toFloat(),
                            velocity = 90
                        ))
                    }
                } else {
                    val stepStates    = _stepStates.value
                    val beatsPerStep  = beatsPerBar / 16.0
                    val activeSteps   = stepStates.mapIndexedNotNull { i, s ->
                        if (s != StepState.OFF) Pair(i, s) else null
                    }
                    if (activeSteps.isEmpty()) {
                        notesPairs.forEach { (_, midi) ->
                            val pitch = (108 - midi).coerceIn(0, 87)
                            repository.saveNote(NoteEvent(
                                id       = UUID.randomUUID().toString(),
                                trackId  = targetTrack.id,
                                pitch    = pitch,
                                beat     = startBeat,
                                duration = beatsPerBar.toFloat(),
                                velocity = 90
                            ))
                        }
                    } else {
                        activeSteps.forEach { (stepIdx, stepState) ->
                            val stepBeat     = startBeat + (stepIdx * beatsPerStep).toFloat()
                            val isDownstroke = stepState == StepState.DOWN
                            val strOrder     = if (isDownstroke) notesPairs else notesPairs.reversed()
                            val noteDur      = (beatsPerStep * 0.95).toFloat().coerceAtLeast(0.0625f)
                            strOrder.forEachIndexed { strIdx, (_, midi) ->
                                val offset = (strIdx * strumSpeedVal).toFloat()
                                val pitch  = (108 - midi).coerceIn(0, 87)
                                val vel    = strumVelocity(strIdx, strOrder.size, isDownstroke)
                                repository.saveNote(NoteEvent(
                                    id       = UUID.randomUUID().toString(),
                                    trackId  = targetTrack.id,
                                    pitch    = pitch,
                                    beat     = (stepBeat + offset).coerceAtLeast(0f),
                                    duration = noteDur,
                                    velocity = vel
                                ))
                            }
                        }
                    }
                }
            }
            _statusMessage.value = "Sent to ${targetTrack.name}${if (appendMode) " (appended)" else ""}"
            onComplete()
        }
    }

    fun buildMidiBars() = progression.value.map { chord ->
        val notes = chord.midiNotes.split(",").mapNotNull { it.trim().toIntOrNull() }
        com.reasontouch.core.midi.MidiProgressionBar(
            chordName     = chord.chordName,
            notes         = Array<Int?>(6) { i -> notes.getOrNull(i) },
            steps         = _stepStates.value,
            durationBeats = _barDuration.value,
            tempoBpm      = _tempo.value,
            strumSpeed    = if (_strumEnabled.value) _strumSpeed.value else 0.0,
            gmProgram     = _instrument.value.program
        )
    }
}