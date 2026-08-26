package com.reasontouch.feature.chords

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.data.*
import com.reasontouch.core.audio.Sf2Player
import com.reasontouch.core.audio.SynthEngine
import com.reasontouch.core.midi.StepState
import com.reasontouch.feature.chords.model

.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay

// ------------------------------------------------------------
// UI STATE
// ------------------------------------------------------------

data class ChordUiState(
    val compositionMode: CompositionMode = CompositionMode.ASSISTED,
    val selectedChord: String = "E",
    val selectedPosition: String = "Open",
    val selectedCategory: String = "All",
    val barDuration: Double = 4.0,
    val strumSpeed: Double = 0.02,
    val strumEnabled: Boolean = true,
    val instrument: GmInstrument = GM_GUITARS[1],
    val statusMessage: String? = null,
    val harmony: HarmonyState = HarmonyState(),
    val bassGenerating: Boolean = false
)

@HiltViewModel
class ChordViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val synthEngine: SynthEngine,
    private val sf2Player: Sf2Player,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    // ------------------------------------------------------------
    // DOMAIN FLOWS (Session, Tracks, Progression)
    // ------------------------------------------------------------

    val session = repository.getSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tracks = repository.getTracksForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progression = repository.getChordsForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bpm: StateFlow<Int> = session
        .map { it?.bpm ?: 120 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 120)

    // ------------------------------------------------------------
    // UI STATE (Single Source of Truth)
    // ------------------------------------------------------------

    private val _ui = MutableStateFlow(ChordUiState())
    val ui = _ui.asStateFlow()

    private fun update(reducer: ChordUiState.() -> ChordUiState) {
        _ui.value = _ui.value.reducer()
    }

    // ------------------------------------------------------------
    // DERIVED UI FLOWS
    // ------------------------------------------------------------

    val availablePositions: StateFlow<List<String>> =
        ui.map { state ->
            GuitarVoicings.voicings[state.selectedChord]?.keys?.toList() ?: emptyList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf("Open"))

    val filteredChords: StateFlow<List<String>> =
        ui.map { state ->
            GuitarVoicings.categories[state.selectedCategory]
                ?: GuitarVoicings.voicings.keys.toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, GuitarVoicings.voicings.keys.toList())

    private val _stepStates = MutableStateFlow(List(16) { StepState.OFF })
    val stepStates = _stepStates.asStateFlow()

    /// ------------------------------------------------------------
    // USER ACTIONS -> UI STATE
    // ------------------------------------------------------------

    fun setCompositionMode(mode: CompositionMode) =
        update { copy(compositionMode = mode) }

    fun selectCategory(cat: String) =
        update { copy(selectedCategory = cat) }

    fun selectChord(name: String) {
        val positions = GuitarVoicings.voicings[name]?.keys?.toList().orEmpty()
        val firstPos = positions.firstOrNull() ?: "Open"

        update {
            copy(
                selectedChord = name,
                selectedPosition = firstPos
            )
        }

        auditionChord(name, firstPos)
    }

    fun selectPosition(pos: String) =
        update { copy(selectedPosition = pos) }

    fun setBarDuration(v: Double) =
        update { copy(barDuration = v.coerceIn(0.5, 32.0)) }

    fun setStrumSpeed(v: Double) =
        update { copy(strumSpeed = v.coerceAtLeast(0.0)) }

    fun setStrumEnabled(v: Boolean) =
        update { copy(strumEnabled = v) }

    fun setInstrument(v: GmInstrument) =
        update { copy(instrument = v) }

    fun setBpm(value: Int) {
        viewModelScope.launch {
            val currentSession = session.value ?: return@launch
            val updated = currentSession.copy(bpm = value.coerceIn(20, 300))
            repository.updateSession(updated)
        }
    }

    fun clearStatus() =
        update { copy(statusMessage = null) }

    // ------------------------------------------------------------
    // STEP SEQUENCER
    // ------------------------------------------------------------

    fun cycleStep(index: Int) {
        _stepStates.value = _stepStates.value.toMutableList().also {
            it[index] = when (it[index]) {
                StepState.OFF -> StepState.DOWN
                StepState.DOWN -> StepState.UP
                StepState.UP -> StepState.OFF
            }
        }
    }

    fun applyPreset(pattern: StepPattern) {
        _stepStates.value = pattern.steps
    }

    // ------------------------------------------------------------
    // CHORD ADD / REMOVE
    // ------------------------------------------------------------

    fun addBar() {
        val chord = ui.value.selectedChord
        val pos = ui.value.selectedPosition
        val notes = GuitarVoicings.voicings[chord]?.get(pos) ?: return

        if (_stepStates.value.all { it == StepState.OFF }) {
            update { copy(statusMessage = "Set at least one step before adding a bar") }
            return
        }

        viewModelScope.launch {
            val barIndex = progression.value.size
            val chordEvent = ChordEvent(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                barIndex = barIndex,
                chordName = "$chord $pos",
                rootMidi = notes.firstOrNull { it != null } ?: 0,
                midiNotes = notes.filterNotNull().joinToString(","),
                voicing = pos,
                strumPatternId = StepPattern(_stepStates.value).toChordEventString(),
                strumSpeedValue = ui.value.strumSpeed
            )
            repository.saveChord(chordEvent)
            val updatedSession = session.value?.copy(totalBars = progression.value.size + 1)
            if (updatedSession != null) {
                repository.updateSession(updatedSession)
            }

            update { copy(statusMessage = null) }
        }
    }

    fun removeBar(chordEvent: ChordEvent) {
        viewModelScope.launch { repository.deleteChord(chordEvent) }
    }

    fun clearProgression() {
        viewModelScope.launch {
            repository.deleteChordsForSession(sessionId)
            _stepStates.value = List(16) { StepState.OFF }
            update {
                copy(
                    statusMessage = null,
                    harmony = HarmonyState()
                )
            }
        }
    }

    // ------------------------------------------------------------
    // AUDITION
    // ------------------------------------------------------------

    fun auditionChord(
        chordName: String,
        position: String,
        strumDelayMs: Long = 0L  // ms delay between strummed notes; 0 = simultaneous
    ) {
        val state = ui.value
        val notes = GuitarVoicings.voicings[chordName]?.get(position) ?: return
        val midiNotes = notes.filterNotNull()
        val gmProgram = state.instrument.program

        val beatDurSec = 60f / bpm.value.coerceAtLeast(20).toFloat()
        val ringDur = (beatDurSec * 2f).coerceIn(0.3f, 1.2f)

        android.util.Log.d("CHORD_AUDIO", "auditionChord: $chordName, strumDelayMs=$strumDelayMs")
        sf2Player.playChord(midiNotes, ringDur, 90, gmProgram, strumDelayMs)
    }
    /**
     * Generate progression based on mood + harmonic bias
     *
     * Bias range: -1.0 (dark/minor) to +1.0 (bright/major)
     * Returns 4 chords filtered to only include "Open" position variants
     */
    fun generateProgressionForMood(moodName: String, bias: Float): List<String> {
        // Chord pool: Only chords with "Open" position available
        val availableChords = GuitarVoicings.voicings
            .filter { it.value.containsKey("Open") }
            .keys
            .toList()

        // Major-leaning chords (for positive bias)
        val majorChords = listOf("C", "D", "E", "G", "A", "Cmaj7", "Dmaj7", "Emaj7", "Gmaj7", "Amaj7")
            .filter { it in availableChords }

        // Minor-leaning chords (for negative bias)
        val minorChords = listOf("Am", "Dm", "Em", "Gm", "Em7", "Am7", "Dm7")
            .filter { it in availableChords }

        // Dominant 7 chords (neutral/energetic)
        val dominantChords = listOf("G7", "D7", "A7", "E7", "C7")
            .filter { it in availableChords }

        // Mood-specific base progressions
        val baseProgression = when (moodName) {
            "Dark" -> listOf("Am", "Em", "Dm", "Am")
            "Uplifting" -> listOf("Cmaj7", "Am7", "G7", "Gmaj7")
            "Cinematic" -> listOf("Gmaj7", "Em7", "Asus2", "D")
            "Ambient" -> listOf("Gmaj7", "Asus2", "Emaj7", "Amaj7")
            "Energetic" -> listOf("G7", "D7", "A7", "E7")
            "Melancholic" -> listOf("Em", "Am", "Em", "B7")
            else -> listOf("C", "Am", "F", "G")
        }

        // Filter base progression to available chords
        val filteredBase = baseProgression.filter { it in availableChords }

        // If bias is neutral, return base progression
        if (Math.abs(bias) < 0.15f) {
            return filteredBase.takeIf { it.size >= 3 } ?: baseProgression.take(4)
        }

        // Generate bias-influenced progression
        return when {
            // Bright/Major bias (positive)
            bias > 0.15f -> {
                val intensity = (bias * 100).toInt() // 15-100
                val majorCount = (4 * bias).coerceIn(1f, 3f).toInt()
                val major = majorChords.take(majorCount)
                val minor = minorChords.take(4 - majorCount)
                (major + minor).take(4)
            }

            // Dark/Minor bias (negative)
            else -> {
                val intensity = (Math.abs(bias) * 100).toInt()
                val minorCount = (4 * Math.abs(bias)).coerceIn(1f, 3f).toInt()
                val minor = minorChords.take(minorCount)
                val major = majorChords.take(4 - minorCount)
                (minor + major).take(4)
            }
        }
    }
    /**
    * Play a progression of chords sequentially
    * Automatically uses first available position if specified position not found
    */
    fun playProgression(chords: List<String>, strumPattern: StepPattern? = null) {
        viewModelScope.launch {
            chords.forEach { chord ->
                val voicing = GuitarVoicings.voicings[chord]
                val positionToUse = if (voicing?.containsKey("Open") == true) {
                    "Open"
                } else {
                    voicing?.keys?.firstOrNull() ?: "Open"
                }

                val strumDelay = if (strumPattern != null && strumPattern != StepPattern.EMPTY) {
                    (ui.value.strumSpeed * 1000).toLong()
                } else {
                    0L
                }

                auditionChord(chord, positionToUse, strumDelay)
                delay(1200)
            }
        }
    }

    fun auditionSuggestion(suggestion: ChordSuggestion) {
        val state = ui.value
        val gmProgram = state.instrument.program
        val strumDelay = if (state.strumEnabled) (state.strumSpeed * 1000).toLong() else 0L
        sf2Player.playChord(suggestion.chord.midiNotes, 0.5f, 90, gmProgram, strumDelay)
    }

    fun addSuggestedChord(suggestion: ChordSuggestion) {
        viewModelScope.launch {
            val barIndex = progression.value.size
            val chordEvent = ChordEvent(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                barIndex = barIndex,
                chordName = suggestion.chord.label,
                rootMidi = suggestion.chord.midiNotes.firstOrNull() ?: 0,
                midiNotes = suggestion.chord.midiNotes.joinToString(","),
                voicing = "Open",
                strumPatternId = StepPattern(_stepStates.value).toChordEventString(),
                strumSpeedValue = ui.value.strumSpeed
            )
            repository.saveChord(chordEvent)
        }
    }

    fun addBorrowedChord(borrowed: BorrowedChord) {
        viewModelScope.launch {
            val barIndex = progression.value.size
            val chordEvent = ChordEvent(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                barIndex = barIndex,
                chordName = borrowed.chord.label,
                rootMidi = borrowed.chord.midiNotes.firstOrNull() ?: 0,
                midiNotes = borrowed.chord.midiNotes.joinToString(","),
                voicing = "Open",
                strumPatternId = StepPattern(_stepStates.value).toChordEventString(),
                strumSpeedValue = ui.value.strumSpeed
            )
            repository.saveChord(chordEvent)
        }
    }

    fun auditionBorrowed(borrowed: BorrowedChord) {
        val state = ui.value
        val gmProgram = state.instrument.program
        val strumDelay = if (state.strumEnabled) (state.strumSpeed * 1000).toLong() else 0L
        sf2Player.playChord(borrowed.chord.midiNotes, 0.5f, 90, gmProgram, strumDelay)
    }

    // ------------------------------------------------------------
    // HARMONY ANALYSIS
    // ------------------------------------------------------------

    fun analyseProgression() {
        val bars = progression.value
        if (bars.isEmpty()) {
            update { copy(statusMessage = "Add at least one bar before analysing") }
            return
        }

        update {
            copy(
                harmony = harmony.copy(
                    isAnalysing = true,
                    showPanel = true,
                    keyCandidates = emptyList(),
                    selectedKey = null,
                    suggestions = emptyList()
                )
            )
        }

        viewModelScope.launch {
            val chordNames = bars.map { it.chordName.trim().split(" ").first() }
            val candidates = KeyDetector.detect(chordNames, ui.value.harmony.moodBias)

            update {
                copy(
                    harmony = harmony.copy(
                        isAnalysing = false,
                        keyCandidates = candidates,
                        selectedKey = null,
                        suggestions = emptyList()
                    )
                )
            }
        }
    }

    fun selectKey(candidate: KeyCandidate) {
        val lastChordName = progression.value.lastOrNull()
            ?.chordName?.trim()?.split(" ")?.first()

        val suggestions = if (lastChordName != null) {
            ChordSuggestionEngine.suggest(candidate, lastChordName)
        } else {
            ChordSuggestionEngine.suggestAll(candidate)
        }

        val borrowed = ChordSuggestionEngine.borrowedChords(candidate)

        update {
            copy(
                harmony = harmony.copy(
                    selectedKey = candidate,
                    suggestions = suggestions,
                    borrowedChords = borrowed
                )
            )
        }
    }

    fun setMoodBias(bias: Float) {
        val bars = progression.value
        if (bars.isEmpty()) return

        update {
            copy(
                harmony = harmony.copy(
                    moodBias = bias,
                    isAnalysing = true,
                    selectedKey = null,
                    suggestions = emptyList(),
                    borrowedChords = emptyList()
                )
            )
        }

        viewModelScope.launch {
            val chordNames = bars.map { it.chordName.trim().split(" ").first() }
            val candidates = KeyDetector.detect(chordNames, bias)

            update {
                copy(
                    harmony = harmony.copy(
                        isAnalysing = false,
                        keyCandidates = candidates
                    )
                )
            }
        }
    }

    fun dismissHarmonyPanel() =
        update { copy(harmony = HarmonyState()) }

    // ------------------------------------------------------------
    // BASS GENERATION
    // ------------------------------------------------------------

    fun generateBass(style: BassStyle, appendMode: Boolean) {
        viewModelScope.launch {
            val bars = progression.value
            if (bars.isEmpty()) {
                update { copy(statusMessage = "Add bars to the progression first") }
                return@launch
            }

            val trackList = tracks.value
            val bassTrack = trackList.firstOrNull { it.name.uppercase() == "BASS" }
                ?: trackList.firstOrNull()
                ?: return@launch

            update { copy(bassGenerating = true) }

            val appendOffset = if (appendMode) {
                val lastBeat = getLastBeatOnTrack(bassTrack.id)
                val beatsPerBar = ui.value.barDuration.toFloat()
                if (lastBeat <= 0f) 0f else {
                    val barsUsed = kotlin.math.ceil(lastBeat / beatsPerBar).toInt()
                    barsUsed * beatsPerBar
                }
            } else {
                repository.deleteNotesForTrack(bassTrack.id)
                0f
            }

            val notes = BassGenerator.generate(
                chords = bars,
                style = style,
                targetTrackId = bassTrack.id,
                beatsPerBar = ui.value.barDuration.toFloat(),
                appendOffset = appendOffset,
                snapValue = 0.25f
            )

            repository.saveNotes(notes)

            update {
                copy(
                    bassGenerating = false,
                    statusMessage = "Bass generated: ${style.label} (${notes.size} notes)"
                )
            }
        }
    }

    suspend fun getLastBeatOnTrack(trackId: String): Float {
        val notes = repository.getNotesForTrackOnce(trackId)
        return notes.maxOfOrNull { it.beat + it.duration } ?: 0f
    }
    // Convert mood-generated chord names to NoteEvents and send directly to piano roll
    fun sendProgressionToPianoRoll(
        chordNames: List<String>,
        strumPattern: StepPattern,
        trackIndex: Int,
        appendMode: Boolean,
        onComplete: () -> Unit
    ) {
        android.util.Log.d("SendProgression", "ENTRY: chords=$chordNames, trackIndex=$trackIndex, nonOffSteps=${strumPattern.steps.count { it != StepState.OFF }}")
        viewModelScope.launch {
            if (chordNames.isEmpty()) {
                update { copy(statusMessage = "No chords to send") }
                onComplete()
                return@launch
            }

            val trackList = tracks.value
            if (trackIndex !in trackList.indices) {
                update { copy(statusMessage = "Invalid track selected") }
                onComplete()
                return@launch
            }

            val targetTrack = trackList[trackIndex]
            val beatsPerBar = ui.value.barDuration.toFloat()

            if (!appendMode) {
                repository.deleteNotesForTrack(targetTrack.id)
            }

            val appendOffset = if (appendMode) {
                // Append starts at the beginning of the next bar after the current progression
                // Use progression bar count, not note positions (which may bleed into next bar)
                val currentProgressionBars = progression.value.size
                (currentProgressionBars * beatsPerBar).toFloat()
            } else {
                0f
            }

            val notes = mutableListOf<NoteEvent>()

            chordNames.forEachIndexed { barIndex, chordName ->
                val beatStart = (barIndex * beatsPerBar) + appendOffset

                // Try "Open" first, fallback to first available voicing
                val voicing = GuitarVoicings.voicings[chordName]?.get("Open")
                    ?: GuitarVoicings.voicings[chordName]?.values?.firstOrNull()

                val midiNotes = voicing?.filterNotNull() ?: emptyList()

                android.util.Log.d("SendProgression", "Bar $barIndex: $chordName -> ${midiNotes.size} notes")

                notes.addAll(
                    generateStrumNotes(
                        midiNotes = midiNotes,
                        pattern = strumPattern,
                        useStrum = true,
                        strumSpeedSeconds = ui.value.strumSpeed,
                        bpm = bpm.value,
                        beatStart = beatStart,
                        beatsPerBar = beatsPerBar,
                        trackId = targetTrack.id
                    )
                )
            }

            android.util.Log.d("SendProgression", "Saving ${notes.size} notes to ${targetTrack.name}")
            repository.saveNotes(notes)
            update { copy(statusMessage = "Sent ${notes.size} notes to ${targetTrack.name}") }
            onComplete()
        }
    }


    // ------------------------------------------------------------
    // SEND TO PIANO ROLL
    // ------------------------------------------------------------

    fun sendToPianoRoll(
        trackIndex: Int,
        useStrum: Boolean,
        strumPattern: StepPattern?,
        appendMode: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val bars = progression.value
            android.util.Log.d("SendToPianoRoll", "ENTRY: trackIndex=$trackIndex, useStrum=$useStrum, bars.size=${bars.size}, tracks.size=${tracks.value.size}")
            if (bars.isEmpty()) {
                update { copy(statusMessage = "Add bars to the progression first") }
                onComplete()
                return@launch
            }

            val trackList = tracks.value
            if (trackIndex !in trackList.indices) {
                update { copy(statusMessage = "Invalid track selected") }
                onComplete()
                return@launch

            }

            val targetTrack = trackList[trackIndex]
            val beatsPerBar = ui.value.barDuration.toFloat()

            // Delete existing notes if not appending
            if (!appendMode) {
                repository.deleteNotesForTrack(targetTrack.id)
            }

            // Generate notes from progression
            val appendOffset = if (appendMode) {
                val lastBeat = getLastBeatOnTrack(targetTrack.id)
                if (lastBeat <= 0f) 0f else {
                    val barsUsed = kotlin.math.ceil(lastBeat / beatsPerBar).toInt()
                    barsUsed * beatsPerBar
                }
            } else {
                0f
            }

            val notes = mutableListOf<NoteEvent>()
            bars.forEachIndexed { barIndex, chordEvent ->
                val beatStart = (barIndex * beatsPerBar) + appendOffset
                val midiNotes = chordEvent.midiNotes.split(",").mapNotNull { it.toIntOrNull() }

                notes.addAll(
                    generateStrumNotes(
                        midiNotes = midiNotes,
                        pattern = strumPattern,
                        useStrum = useStrum,
                        strumSpeedSeconds = ui.value.strumSpeed,
                        bpm = bpm.value,
                        beatStart = beatStart,
                        beatsPerBar = beatsPerBar,
                        trackId = targetTrack.id
                    )
                )
            }

            android.util.Log.d("SendToPianoRoll", "Saving ${notes.size} notes to track ${targetTrack.id} (${targetTrack.name})")
            repository.saveNotes(notes)
            update { copy(statusMessage = "Sent ${notes.size} notes to ${targetTrack.name}") }
            android.util.Log.d("SendToPianoRoll", "DONE - statusMessage updated")
            onComplete()
        }
    }

    // STAGE 4: Pairing Engine Integration
    // Delegates to SuggestionWorkflow -- see that file for the shared logic
    // and Task_List.md for why this was consolidated.

    fun suggestNextSection(): PairingDecision =
        SuggestionWorkflow.suggestNextSection(progression.value)

    /**
     * Generates phrase candidates using the planning layer.
     * Returns List<GeneratedProgression> with actual TheoryChord data.
     */
    fun suggestNextPhrases(): List<GeneratedProgression> =
        SuggestionWorkflow.suggestNextPhrases(progression.value)
    // <<< INSERT THIS NEW FUNCTION HERE >>>

    /**
     * Multi-option entry point for the SUGGEST NEXT dialog -- returns every ranked intent
     * paired with its generated phrases, rather than collapsing to a single top pick.
     * See SuggestionWorkflow.suggestNextOptionsWithPhrases().
     */
    fun suggestNextOptions(): List<SuggestionWorkflow.SuggestionOption> =
        SuggestionWorkflow.suggestNextOptionsWithPhrases(progression.value)

    /**
     * Adds a generated phrase to the progression.
     * Converts List<TheoryChord> to List<ChordEvent> and adds bars.
     */
    fun addPhrase(generatedProgression: GeneratedProgression) {
        viewModelScope.launch {
            val currentProgression = repository.getChordsForSessionOnce(sessionId)
            if (currentProgression.isEmpty()) return@launch

            val startBarIndex = currentProgression.size
            val currentSession = repository.getSession(sessionId).first()
            val beatsPerBar = ui.value.barDuration.toFloat()
            val chordTrack = repository.getTracksForSession(sessionId).first()
                .firstOrNull { it.name.uppercase() == "CHORD" }

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
                    strumPatternId = StepPattern(_stepStates.value).toChordEventString(),
                    strumSpeedValue = ui.value.strumSpeed
                )
                repository.saveChord(chordEvent)

                if (chordTrack != null) {
                    val beatStart = barIndex * beatsPerBar
                    newNotes.addAll(
                        generateStrumNotes(
                            midiNotes = theoryChord.midiNotes,
                            pattern = StepPattern(_stepStates.value),
                            useStrum = true,
                            strumSpeedSeconds = ui.value.strumSpeed,
                            bpm = currentSession?.bpm ?: 120,
                            beatStart = beatStart,
                            beatsPerBar = beatsPerBar,
                            trackId = chordTrack.id
                        )
                    )
                }
            }

            if (newNotes.isNotEmpty()) {
                repository.saveNotes(newNotes)
            }

            val updatedSession = currentSession?.copy(
                totalBars = startBarIndex + generatedProgression.chords.size
            )
            if (updatedSession != null) {
                repository.updateSession(updatedSession)
            }

            update { copy(statusMessage = null) }

        }
    }

}
