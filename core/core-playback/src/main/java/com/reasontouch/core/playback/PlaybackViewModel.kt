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
    fun rewind() = controller.rewind()

    // -- Draw duration stepper ------------------------------------
    private val _drawDuration = kotlinx.coroutines.flow.MutableStateFlow(0.25f)
    val drawDuration: kotlinx.coroutines.flow.StateFlow<Float> = _drawDuration.asStateFlow()

    val snapValues = listOf(1f, 0.5f, 0.25f, 0.125f, 0.0625f)

    fun stepDuration(delta: Float, snapValue: Float) {
        _drawDuration.value = (_drawDuration.value + delta)
            .coerceIn(snapValue, 16f)
    }
}


