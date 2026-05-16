package com.reasontouch.core.playback

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    private val sequencer: Sequencer
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(TransportState())
    val state = _state.asStateFlow()

    fun play(sessionId: String) {
        scope.launch { sequencer.playSession(sessionId, _state) }
    }

    fun stop() = sequencer.stop(_state)

    fun rewind() = sequencer.rewind(_state)

    fun setBpm(bpm: Int) = _state.update { it.copy(bpm = bpm) }

        fun setLoop(enabled: Boolean) = _state.update { it.copy(loopEnabled = enabled) }
    fun setLoopStart(beat: Float) = _state.update { it.copy(loopStart = beat) }
        fun setLoopEnd(beat: Float)   = _state.update { it.copy(loopEnd = beat) }
    fun seekTo(beat: Float)       = _state.update { it.copy(playheadBeat = beat) }
}



