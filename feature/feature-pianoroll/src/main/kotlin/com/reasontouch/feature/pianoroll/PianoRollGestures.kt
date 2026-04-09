package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

// Tap: draw / erase / select / set playhead
fun Modifier.pianoRollTapGestures(
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel
): Modifier = this.pointerInput(uiState.currentTool, uiState.snapValue, uiState.activeTrack) {
    detectTapGestures { offset ->
        handleGridTap(offset, state, uiState, viewModel)
    }
}

// Erase drag handled separately so it doesn't interfere with pan/zoom
fun Modifier.pianoRollEraseDragGestures(
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel
): Modifier = this.pointerInput(uiState.currentTool) {
    if (uiState.currentTool != PianoRollViewModel.Tool.ERASE) return@pointerInput
    detectDragGestures(
        onDragStart = { offset -> handleGridTap(offset, state, uiState, viewModel) },
        onDrag = { change, _ ->
            change.consume()
            handleGridTap(change.position, state, uiState, viewModel)
        }
    )
}

// Pan + zoom + loop-handle drag unified in one pointerInput so they never compete.
// awaitEachGesture lets us inspect pointer count before deciding which mode to use:
//   1 pointer  -> pan or loop-handle drag
//   2 pointers -> pinch zoom anchored at centroid
fun Modifier.pianoRollDragGestures(
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel
): Modifier = this.pointerInput(uiState.currentTool, uiState.loopEnabled) {
    if (uiState.currentTool == PianoRollViewModel.Tool.ERASE) return@pointerInput
    awaitEachGesture {
        // Wait for first finger down
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastPosition = down.position

        // Check for loop handle hit on finger-down
        handleLoopDragStart(down.position, state, uiState)

        do {
            val event = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }

            if (pointers.size >= 2) {
                // ── Pinch zoom ──────────────────────────────────────────
                state.loopDragTarget = null
                val zoom = event.calculateZoom()
                if (zoom != 1f) {
                    // Centroid in canvas coordinates
                    val centroidX = event.changes.map { it.position.x }.average().toFloat()
                    val beatAtCentroid = state.xToBeat(centroidX)
                    state.pixelsPerBeat = (state.pixelsPerBeat * zoom).coerceIn(20f, 400f)
                    state.scrollX = (beatAtCentroid * state.pixelsPerBeat - centroidX)
                        .coerceAtLeast(0f)
                }
                event.changes.forEach { it.consume() }
            } else if (pointers.size == 1) {
                val change = pointers[0]
                val dragAmount = change.position - lastPosition
                lastPosition = change.position

                if (state.loopDragTarget != null) {
                    // ── Loop handle / body drag ─────────────────────────
                    handleLoopDrag(change.position, state, uiState.snapValue,
                        viewModel)
                } else {
                    // ── Pan ─────────────────────────────────────────────
                    state.scrollX = (state.scrollX - dragAmount.x).coerceAtLeast(0f)
                    state.scrollY = (state.scrollY - dragAmount.y).coerceAtLeast(0f)
                    state.clampScroll(state.gridWidth, state.gridHeight)
                }
                if (change.positionChanged()) change.consume()
            }
        } while (pointers.isNotEmpty())

        state.loopDragTarget = null
    }
}

// Zoom-only gesture kept for backwards compatibility but now a no-op —
// zoom is handled inside pianoRollDragGestures above.
fun Modifier.pianoRollZoomGestures(
    state: PianoRollState
): Modifier = this  // intentionally empty — zoom unified into pianoRollDragGestures

fun Modifier.pianoKeyGestures(
    state: PianoRollState,
    onKeyTap: (Int) -> Unit
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures { offset ->
            onKeyTap(state.yToPitch(offset.y).coerceIn(0, state.totalNotes - 1))
        }
    }
    .pointerInput(Unit) {
        detectDragGestures { change, _ ->
            change.consume()
            onKeyTap(state.yToPitch(change.position.y).coerceIn(0, state.totalNotes - 1))
        }
    }