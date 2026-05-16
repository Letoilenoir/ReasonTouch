package com.reasontouch.core.playback

data class TransportState(
    val isPlaying: Boolean = false,
    val playheadBeat: Float = 0f,
    val bpm: Int = 120,
    val loopEnabled: Boolean = false,
    val loopStart: Float = 0f,
    val loopEnd: Float = 4f
)

