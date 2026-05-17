package com.reasontouch.core.playback

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val controller: PlaybackController
) : ViewModel() {

    val transport = controller.state

    fun play(sessionId: String) = controller.play(sessionId)
    fun stop() = controller.stop()
    fun rewind()       = controller.rewind()
    fun skipToStart()  = controller.skipToStart()
    fun fastForward()  = controller.fastForward()
    fun skipToEnd()    = controller.skipToEnd()

    val drawDuration: StateFlow<Float> = controller.drawDuration
    val snapValues   = controller.snapValues
    fun stepDuration(delta: Float, snapValue: Float) = controller.stepDuration(delta, snapValue)
}






