package com.reasontouch.core.playback

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val controller: PlaybackController
) : ViewModel() {

    val transport = controller.state

    fun play(sessionId: String) = controller.play(sessionId)
    fun stop() = controller.stop()
    fun rewind() = controller.rewind()
}
