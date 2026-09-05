package com.reasontouch.feature.drums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reasontouch.core.audio.DrumSamplePlayer
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DrumViewModel @Inject constructor(
    private val repository:  SessionRepository,
    private val sf2Player:   Sf2Player,
    private val drumPlayer:  DrumSamplePlayer,
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

    // ── Pattern editing ───────────────────────────────────────────────────

    fun toggleStep(laneIdx: Int, stepIdx: Int) {
        _pattern.value = _pattern.value.toggle(laneIdx, stepIdx)
        if (_pattern.value.isActive(laneIdx, stepIdx)) auditionLane(laneIdx)
    }

    fun applyPreset(preset: DrumPattern) { _pattern.value = preset }
    fun clearPattern()                   { _pattern.value = _pattern.value.clear() }

    fun setStepCount(steps: Int) {
        _pattern.value = if (steps >= 32) _pattern.value.extendTo32()
                         else             _pattern.value.trimTo16()
    }

    // ── Audition ──────────────────────────────────────────────────────────

    fun auditionLane(laneIdx: Int, velocity: Int = 100) {
        val lane = DrumKit.lanes.getOrNull(laneIdx) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            drumPlayer.play(lane.gmNote, velocity)
        }
    }

    // ── Playback — reads pattern live each step for real-time editing ─────

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        val bpm    = session.value?.bpm ?: 120
        val stepMs = (60000.0 / bpm / 4.0).toLong()

        playbackJob = viewModelScope.launch(Dispatchers.IO) {
            var step = 0
            while (_isPlaying.value) {
                _currentStep.value = step
                // Read live each step — allows real-time pattern changes
                val livePat = _pattern.value
                DrumKit.lanes.forEachIndexed { laneIdx, lane ->
                    if (livePat.isActive(laneIdx, step)) {
                        drumPlayer.play(lane.gmNote, livePat.velocity(laneIdx, step))
                    }
                }
                delay(stepMs)
                step = (step + 1) % livePat.steps
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

    // ── Track management ──────────────────────────────────────────────────

    /**
     * Find the DRUMS track by querying Room directly — avoids stale
     * StateFlow which causes duplicate track creation on repeated writes.
     * Creates the track only if genuinely absent from the DB.
     */
    private suspend fun getOrCreateDrumsTrack(): MidiTrack {
        // Query Room directly for freshest data
        val fresh = repository.getTracksForSession(sessionId).first()

        val existing = fresh.firstOrNull {
            it.midiChannel == 9 ||
            it.name.uppercase() in listOf("DRUMS", "DRUM")
        }
        if (existing != null) return existing

        // Not found — create once
        val newTrack = MidiTrack(
            sessionId   = sessionId,
            index       = fresh.size,
            name        = "DRUMS",
            voice       = "saw",
            color       = 0xFFF5C518,
            midiChannel = 9,
            volume      = 0.90f
        )
        repository.saveTrack(newTrack)
        return newTrack
    }

    // ── Write to piano roll ───────────────────────────────────────────────

    /**
     * Writes the drum pattern to the DRUMS track, repeated for the full
     * session length (totalBars). One pattern = 1 bar (4 beats / 16 steps).
     *
     * appendMode = true  → adds after existing content
     * appendMode = false → clears track then writes from beat 0
     */
    fun writeToPianoRoll(appendMode: Boolean, onComplete: (String) -> Unit) {
        stop()  // always stop playback before writing

        viewModelScope.launch {
            val drumTrack  = getOrCreateDrumsTrack()
            val totalBars  = repository.getSession(sessionId).first()?.totalBars ?: 4
            val pat        = _pattern.value
            val stepDur    = 1f / 4f                    // 16th note = 0.25 beats
            val patternBeats = pat.steps * stepDur      // 16 steps = 4 beats = 1 bar

            val appendOffset = if (appendMode) {
                // Start after last existing note, rounded up to next pattern boundary
                val lastBeat = repository.getNotesForTrackOnce(drumTrack.id)
                    .maxOfOrNull { it.beat + it.duration } ?: 0f
                if (lastBeat <= 0f) 0f
                else kotlin.math.ceil(lastBeat / patternBeats).toInt() * patternBeats
            } else {
                repository.deleteNotesForTrack(drumTrack.id)
                0f
            }

            val notes = mutableListOf<NoteEvent>()

            // Repeat the pattern for every bar in the session
            val barsToWrite = if (appendMode) {
                // In append mode write totalBars worth from the offset
                totalBars
            } else {
                totalBars
            }

            (0 until barsToWrite).forEach { barIdx ->
                val barOffset = appendOffset + barIdx * patternBeats
                DrumKit.lanes.forEachIndexed { li, lane ->
                    (0 until pat.steps).forEach { si ->
                        if (pat.isActive(li, si)) {
                            notes.add(NoteEvent(
                                id       = UUID.randomUUID().toString(),
                                trackId  = drumTrack.id,
                                pitch    = lane.pitch,
                                beat     = barOffset + si * stepDur,
                                duration = stepDur * 0.9f,
                                velocity = pat.velocity(li, si)
                            ))
                        }
                    }
                }
            }

            repository.saveNotes(notes)
            onComplete("Written ${barsToWrite} bars to ${drumTrack.name} (${notes.size} notes)")
        }
    }

    // ── Segment-aware write (Phase 6b) ──────────────────────────────────

    /**
     * Phase 6b of the Bass/Drum Arrangement Roadmap: writes multiple drum
     * patterns across different bar ranges in one call, instead of
     * repeating a single pattern for the whole session (the gap confirmed
     * in docs/design/Drum_Arrangement_Specification.md Section 7 -- the
     * highest-impact finding of that spec).
     *
     * Segments should be contiguous and non-overlapping; this function
     * does not validate that -- the caller (Phase 8's tray integration)
     * is responsible for constructing a sane segment list. Overlapping
     * segments will simply overwrite each other's notes in write order.
     *
     * The original single-pattern writeToPianoRoll() above is left
     * unchanged and still used by DrumScreen's manual write flow -- this
     * is a new, additive capability, not a replacement.
     */
    fun writeSegmentsToPianoRoll(
        segments: List<DrumSegment>,
        appendMode: Boolean,
        onComplete: (String) -> Unit
    ) {
        stop()

        viewModelScope.launch {
            val drumTrack = getOrCreateDrumsTrack()

            val baseOffset = if (appendMode) {
                repository.getNotesForTrackOnce(drumTrack.id)
                    .maxOfOrNull { it.beat + it.duration } ?: 0f
            } else {
                repository.deleteNotesForTrack(drumTrack.id)
                0f
            }

            val notes = mutableListOf<NoteEvent>()

            segments.forEach { segment ->
                val patternBeats = segment.pattern.steps * (1f / 4f)
                notes.addAll(
                    generateDrumNotes(
                        pattern = segment.pattern,
                        barCount = segment.barCount,
                        trackId = drumTrack.id,
                        baseOffset = baseOffset + segment.startBar * patternBeats
                    )
                )
            }

            repository.saveNotes(notes)
            val totalBars = segments.sumOf { it.barCount }
            onComplete("Written $totalBars bars across ${segments.size} segment(s) to ${drumTrack.name} (${notes.size} notes)")
        }
    }
}

data class DrumSegment(
    val pattern:   DrumPattern,
    val startBar:  Int,
    val barCount:  Int
)