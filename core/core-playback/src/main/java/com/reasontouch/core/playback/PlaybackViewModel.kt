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

    fun stepStepper(direction: Int) {
        val snapSteps = listOf(
            0.0625f, // 1/64
            0.125f,  // 1/32
            0.25f,   // 1/16
            0.5f,    // 1/8
            1f,      // 1/4
            2f,      // 1/2
            4f,      // 1 bar
            8f       // 2 bar
        )

        val current = drawDuration.value

        val currentIndex =
            snapSteps.indexOfFirst { it == current }
                .coerceAtLeast(0)

        val nextIndex =
            (currentIndex + direction)
                .coerceIn(0, snapSteps.lastIndex)

        controller.setDrawDuration(
            snapSteps[nextIndex]
        )
    }

    val drawDuration: StateFlow<Float> = controller.drawDuration
    val snapValues   = controller.snapValues

}






