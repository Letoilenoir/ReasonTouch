package com.reasontouch.core.playback

import com.reasontouch.core.audio.DrumSamplePlayer
import com.reasontouch.core.audio.Sf2Player
import com.reasontouch.core.data.MidiTrack
import com.reasontouch.core.data.NoteEvent
import com.reasontouch.core.data.SessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlin.math.ceil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Sequencer @Inject constructor(
    private val repository: SessionRepository,
    private val sf2Player: Sf2Player,
    private val drumPlayer: DrumSamplePlayer
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    private val CHORD_AUDITION_DUR_SEC = 0.5f

    suspend fun playSession(sessionId: String, transport: MutableStateFlow<TransportState>) {
        if (transport.value.isPlaying) return

        val tracks = repository.getTracksForSession(sessionId).first()
        val allNotes = tracks.associate { it.id to repository.getNotesForTrackOnce(it.id) }

        val bpm = transport.value.bpm
        val beatDurMs = 60000.0 / bpm
        val loopMode = transport.value.loopEnabled
        val loopS = transport.value.loopStart
        val loopE = transport.value.loopEnd
        val endBeat = if (loopMode) loopE else computeEndBeat(allNotes)

        transport.update { it.copy(isPlaying = true) }

        fun isDrumTrack(track: MidiTrack) =
            track.midiChannel == 9 || track.name.uppercase() in listOf("DRUM", "DRUMS")

        fun gmProgram(track: MidiTrack) = when (track.name.uppercase()) {
            "BASS" -> 32
            "LEAD" -> 80
            "CHORD" -> 25
            "PAD" -> 88
            else -> 0
        }

        fun buildClusters(notes: List<NoteEvent>): List<Float> {
            val sorted = notes.sortedBy { it.beat }
            val clusters = mutableListOf<Float>()
            var last = Float.MIN_VALUE
            sorted.forEach { n ->
                if (n.beat - last > 0.5f) {
                    last = n.beat
                    clusters.add(last)
                }
            }
            return clusters
        }

        fun ringDur(note: NoteEvent, clusters: List<Float>): Float {
            val myCluster = clusters.lastOrNull { it <= note.beat + 0.001f } ?: note.beat
            val next = clusters.firstOrNull { it > myCluster + 0.001f }
            return if (next != null) {
                val gapSec = ((next - myCluster) * beatDurMs / 1000.0).toFloat()
                minOf(CHORD_AUDITION_DUR_SEC, gapSec).coerceAtLeast(0.1f)
            } else CHORD_AUDITION_DUR_SEC
        }

        fun schedulePass(fromBeat: Float, originMs: Long) {
            tracks.forEach { track ->
                if (track.muted) return@forEach
                val notes = allNotes[track.id] ?: emptyList()
                val clusters = buildClusters(notes)
                val program = gmProgram(track)
                val isDrum = isDrumTrack(track)

                notes.filter { it.beat >= fromBeat && it.beat < endBeat }
                    .forEach { note ->
                        val delayMs = ((note.beat - fromBeat) * beatDurMs).toLong()
                        val dur = ringDur(note, clusters)
                        val trackId = track.id

                        scope.launch(Dispatchers.IO) {
                            val wait = originMs + delayMs - System.currentTimeMillis()
                            if (wait > 0) delay(wait)
                            if (!transport.value.isPlaying) return@launch

                            val liveTracks = repository.getTracksForSession(sessionId).first()
                            val vol = liveTracks.firstOrNull { it.id == trackId }?.volume ?: 1f
                            val vel = (note.velocity * vol).toInt().coerceIn(1, 127)
                            val midi = (108 - note.pitch).coerceIn(0, 127)

                            if (isDrum) drumPlayer.play(midi, vel)
                            else sf2Player.playNote(midi, dur, vel, program)
                        }
                    }
            }
        }

        job?.cancel()
        job = scope.launch(Dispatchers.Main) {
            var origin = System.currentTimeMillis()
            var passStart = transport.value.playheadBeat

            schedulePass(passStart, origin)

            while (transport.value.isPlaying) {
                val elapsed = System.currentTimeMillis() - origin
                val beat = passStart + (elapsed / beatDurMs).toFloat()

                when {
                    beat >= endBeat && loopMode -> {
                        origin = System.currentTimeMillis()
                        passStart = loopS
                        transport.update { it.copy(playheadBeat = loopS) }
                        schedulePass(loopS, origin)
                    }
                    beat >= endBeat -> {
                        transport.update { it.copy(playheadBeat = endBeat, isPlaying = false) }
                        break
                    }
                    else -> transport.update { it.copy(playheadBeat = beat) }
                }

                delay(16)
            }
        }
    }

    fun stop(transport: MutableStateFlow<TransportState>) {
        job?.cancel()
        job = null
        transport.update { it.copy(isPlaying = false) }
    }

    fun rewind(transport: MutableStateFlow<TransportState>) {
        stop(transport)
        transport.update { it.copy(playheadBeat = 0f) }
    }

    private fun computeEndBeat(allNotes: Map<String, List<NoteEvent>>): Float {
        val last = allNotes.values.flatten().maxOfOrNull { it.beat + it.duration } ?: 0f
        val bars = ceil(last / 4f).toInt().coerceAtLeast(1)
        return bars * 4f
    }
}


