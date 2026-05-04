package com.reasontouch.feature.drums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.audio.Sf2Player
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
class DrumViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val sf2Player:     Sf2Player,
    private val drumPlayer:    com.reasontouch.core.audio.DrumSamplePlayer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val session: StateFlow<Session?> = repository.getSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tracks: StateFlow<List<MidiTrack>> = repository.getTracksForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pattern     = MutableStateFlow(DrumPattern())
    val pattern: StateFlow<DrumPattern> = _pattern.asStateFlow()

    private val _isPlaying   = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStep = MutableStateFlow(-1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private var playbackJob: Job? = null

    fun toggleStep(laneIdx: Int, stepIdx: Int) {
        _pattern.value = _pattern.value.toggle(laneIdx, stepIdx)
        if (_pattern.value.isActive(laneIdx, stepIdx)) auditionLane(laneIdx)
    }

    fun applyPreset(preset: DrumPattern)  { _pattern.value = preset }
    fun clearPattern()                    { _pattern.value = _pattern.value.clear() }

    fun setStepCount(steps: Int) {
        _pattern.value = if (steps >= 32) _pattern.value.extendTo32()
                         else             _pattern.value.trimTo16()
    }

    fun auditionLane(laneIdx: Int, velocity: Int = 100) {
        val lane = DrumKit.lanes.getOrNull(laneIdx) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            drumPlayer.play(lane.gmNote, velocity)
        }
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        val bpm    = session.value?.bpm ?: 120
        val stepMs = (60000.0 / bpm / 4.0).toLong()
        val snap   = _pattern.value

        playbackJob = viewModelScope.launch(Dispatchers.IO) {
            var step = 0
            while (_isPlaying.value) {
                _currentStep.value = step
                DrumKit.lanes.forEachIndexed { laneIdx, lane ->
                    if (snap.isActive(laneIdx, step)) {
                        drumPlayer.play(lane.gmNote, snap.velocity(laneIdx, step))
                    }
                }
                delay(stepMs)
                step = (step + 1) % snap.steps
            }
            _currentStep.value = -1
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob        = null
        _isPlaying.value   = false
        _currentStep.value = -1
    }

    /**
     * Find or create the DRUMS track for this session.
     * Existing sessions created before the DRUMS track was added to
     * createNewSession will get one created on first write.
     */
    private suspend fun getOrCreateDrumsTrack(): MidiTrack {
        val existing = tracks.value.firstOrNull {
            it.name.uppercase() in listOf("DRUMS", "DRUM")
        }
        if (existing != null) return existing

        // Create a new DRUMS track for this session
        val newTrack = MidiTrack(
            sessionId   = sessionId,
            index       = tracks.value.size,
            name        = "DRUMS",
            voice       = "saw",
            color       = 0xFFF5C518,
            midiChannel = 9,
            volume      = 0.90f
        )
        repository.saveTrack(newTrack)
        return newTrack
    }

    fun writeToPianoRoll(appendMode: Boolean, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val drumTrack   = getOrCreateDrumsTrack()
            val beatsPerBar = session.value?.timeSignatureNumerator ?: 4
            val stepDur     = 1f / 4f
            val pat         = _pattern.value

            val patternBeats = pat.steps * stepDur  // total beats in one pattern pass
            val appendOffset = if (appendMode) {
                val last = repository.getNotesForTrackOnce(drumTrack.id)
                    .maxOfOrNull { it.beat + it.duration } ?: 0f
                if (last <= 0f) 0f
                else kotlin.math.ceil(last / patternBeats).toInt() * patternBeats
            } else {
                repository.deleteNotesForTrack(drumTrack.id)
                0f
            }

            val notes = mutableListOf<NoteEvent>()
            DrumKit.lanes.forEachIndexed { li, lane ->
                (0 until pat.steps).forEach { si ->
                    if (pat.isActive(li, si)) {
                        notes.add(NoteEvent(
                            id       = UUID.randomUUID().toString(),
                            trackId  = drumTrack.id,
                            pitch    = lane.pitch,
                            beat     = appendOffset + si * stepDur,
                            duration = stepDur * 0.9f,
                            velocity = pat.velocity(li, si)
                        ))
                    }
                }
            }
            repository.saveNotes(notes)
            onComplete("Written to ${drumTrack.name} (${notes.size} notes)")
        }
    }
}