package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

fun Modifier.pianoRollGestures(
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel
): Modifier = this.pointerInput(
    uiState.currentTool, uiState.snapValue, uiState.activeTrack,
    uiState.loopEnabled, uiState.activeNotes
) {
    awaitEachGesture {
        val down    = awaitFirstDown(requireUnconsumed = false)
        val downPos = down.position
        var lastPos = downPos
        var totalMoved = 0f

        // Classify on finger-down
        val isDurationDrag = (uiState.currentTool == PianoRollViewModel.Tool.DRAW ||
                              uiState.currentTool == PianoRollViewModel.Tool.SELECT) &&
                             state.noteRightEdgeHit(downPos.x, downPos.y,
                                 uiState.activeNotes) != null

        val isLoopDrag = !isDurationDrag && uiState.loopEnabled &&
                         state.loopHandleHit(downPos.x, downPos.y,
                             uiState.loopStart, uiState.loopEnd) != null

        // Initialise drag state immediately on finger-down
        if (isDurationDrag || isLoopDrag) {
            down.consume()
            handleDragStart(downPos, state, uiState)
        }

        do {
            val event    = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }

            if (pointers.size >= 2) {
                // Pinch zoom
                state.durationDragNoteId = null
                state.loopDragTarget     = null
                totalMoved = Float.MAX_VALUE  // never fire tap
                val zoom = event.calculateZoom()
                if (zoom != 1f) {
                    val cx = event.changes.map { it.position.x }.average().toFloat()
                    val beatAtCx = state.xToBeat(cx)
                    state.pixelsPerBeat = (state.pixelsPerBeat * zoom).coerceIn(20f, 400f)
                    state.scrollX = (beatAtCx * state.pixelsPerBeat - cx).coerceAtLeast(0f)
                }
                event.changes.forEach { it.consume() }

            } else if (pointers.size == 1) {
                val change = pointers[0]
                val delta  = Offset(
                    change.position.x - lastPos.x,
                    change.position.y - lastPos.y
                )
                totalMoved += abs(delta.x) + abs(delta.y)
                lastPos = change.position

                when {
                    // Duration or loop drag — always handle regardless of movement
                    isDurationDrag || isLoopDrag -> {
                        handleDrag(change.position, delta, state, uiState, viewModel)
                        change.consume()
                    }
                    // Erase — drag erases continuously
                    uiState.currentTool == PianoRollViewModel.Tool.ERASE && totalMoved > 4f -> {
                        handleGridTap(change.position, state, uiState, viewModel)
                        change.consume()
                    }
                    // Movement without a specific target — not a tap, not our drag
                    totalMoved > 8f -> {
                        // consumed by scrollbars — just mark as moved
                    }
                }
            }
        } while (pointers.isNotEmpty())

        handleDragEnd(state)

        // Fire tap only on clean short press — no significant movement,
        // not already handled as a drag type
        if (totalMoved <= 8f && !isDurationDrag && !isLoopDrag) {
            handleGridTap(downPos, state, uiState, viewModel)
        }
    }
}

fun Modifier.horizontalScrollBarGestures(
    state: PianoRollState
): Modifier = this.pointerInput(Unit) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        val totalW = state.totalBeats * state.pixelsPerBeat
        val ratio  = if (totalW > 0f) totalW / state.gridWidth else 1f
        state.scrollX = (state.scrollX + dragAmount.x * ratio).coerceAtLeast(0f)
        state.clampScroll(state.gridWidth, state.gridHeight)
    }
}

fun Modifier.verticalScrollBarGestures(
    state: PianoRollState
): Modifier = this.pointerInput(Unit) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        val totalH = state.totalNotes * state.noteHeight
        val ratio  = if (totalH > 0f) totalH / state.gridHeight else 1f
        state.scrollY = (state.scrollY + dragAmount.y * ratio).coerceAtLeast(0f)
        state.clampScroll(state.gridWidth, state.gridHeight)
    }
}

// These are now no-ops — everything runs through pianoRollGestures
fun Modifier.pianoRollZoomGestures(state: PianoRollState): Modifier = this
fun Modifier.pianoRollTapGestures(state: PianoRollState, uiState: PianoRollUiState,
    viewModel: PianoRollViewModel): Modifier = this
fun Modifier.pianoRollDragGestures(state: PianoRollState, uiState: PianoRollUiState,
    viewModel: PianoRollViewModel): Modifier = this
fun Modifier.pianoRollEraseDragGestures(state: PianoRollState, uiState: PianoRollUiState,
    viewModel: PianoRollViewModel): Modifier = this

fun Modifier.pianoKeyGestures(
    state: PianoRollState,
    onKeyTap: (Int) -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastPos = down.position
        var moved = false
        onKeyTap(state.yToPitch(down.position.y).coerceIn(0, state.totalNotes - 1))
        do {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                if (change.pressed) {
                    if (abs(change.position.y - lastPos.y) > 2f) {
                        moved = true
                        lastPos = change.position
                        onKeyTap(state.yToPitch(change.position.y).coerceIn(0, state.totalNotes - 1))
                    }
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}