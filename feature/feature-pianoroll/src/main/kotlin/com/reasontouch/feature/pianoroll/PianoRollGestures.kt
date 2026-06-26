package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.roundToInt

fun Modifier.pianoRollGestures(
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel
): Modifier = this.pointerInput(
    uiState.currentTool, uiState.snapValue, uiState.activeTrack,
    uiState.loopEnabled, uiState.activeNotes, uiState.selectedIds
) {
    awaitEachGesture {
        val down    = awaitFirstDown(requireUnconsumed = false)
        val downPos = down.position
        var lastPos = downPos
        var totalMoved = 0f

        // ── Classify intent on finger-down ────────────────────────────────
        val isDurationDrag = (uiState.currentTool == PianoRollViewModel.Tool.DRAW ||
                              uiState.currentTool == PianoRollViewModel.Tool.SELECT) &&
                             state.noteRightEdgeHit(downPos.x, downPos.y,
                                 uiState.activeNotes) != null

        val isLoopDrag = !isDurationDrag && uiState.loopEnabled &&
                         state.loopHandleHit(downPos.x, downPos.y,
                             uiState.loopStart, uiState.loopEnd) != null

        // In SELECT mode — check if touching a selected note body for move
        val hitNote = findHitNote(downPos, state, uiState.activeNotes)
        val isMoveDrag = !isDurationDrag && !isLoopDrag &&
                         uiState.currentTool == PianoRollViewModel.Tool.SELECT &&
                         hitNote != null &&
                         hitNote.id in uiState.selectedIds

        // Rubber band: SELECT tool, empty space, below header
        val isRubberBand = !isDurationDrag && !isLoopDrag && !isMoveDrag &&
                           uiState.currentTool == PianoRollViewModel.Tool.SELECT &&
                           downPos.y > state.headerHeight

        if (isDurationDrag || isLoopDrag) down.consume()

        if (isDurationDrag) handleDragStart(downPos, state, uiState)
        if (isLoopDrag)     handleLoopDragStart(downPos, state, uiState)

        if (isMoveDrag) {
            state.moveDragActive    = true
            state.moveDragStartX    = downPos.x
            state.moveDragStartY    = downPos.y
            state.moveDragDeltaBeat = 0f
            state.moveDragDeltaPitch = 0
            down.consume()
        }

        if (isRubberBand) {
            state.rubberBandStart = downPos
            state.rubberBandEnd   = downPos
        }

        do {
            val event    = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }

            if (pointers.size >= 2) {
                // Pinch zoom — cancel all drags
                state.durationDragNoteId = null
                state.loopDragTarget     = null
                state.moveDragActive     = false
                state.rubberBandStart    = null
                state.rubberBandEnd      = null
                totalMoved = Float.MAX_VALUE

                val zoom = event.calculateZoom()
                if (zoom != 1f) {

                    // Horizontal anchor
                    val cx = event.changes.map { it.position.x }.average().toFloat()
                    val beatAtCx = state.xToBeat(cx)

                    // Vertical anchor
                    val cy = event.changes.map { it.position.y }.average().toFloat()
                    val pitchAtCy =
                        (cy + state.scrollY - state.headerHeight) / state.noteHeight

                    // Horizontal zoom
                    state.pixelsPerBeat =
                        (state.pixelsPerBeat * zoom).coerceIn(20f, 400f)

                    // Vertical zoom
                    state.noteHeight =
                        (state.noteHeight * zoom).coerceIn(12f, 64f)

                    // Preserve horizontal focus
                    state.scrollX =
                        (beatAtCx * state.pixelsPerBeat - cx)
                            .coerceAtLeast(0f)

                    // Preserve vertical focus
                    state.scrollY =
                        (pitchAtCy * state.noteHeight - cy + state.headerHeight)
                            .coerceAtLeast(0f)
                    state.clampScroll(state.gridWidth, state.gridHeight)
                }
                event.changes.forEach { it.consume() }

            } else if (pointers.size == 1) {
                val change = pointers[0]
                val delta  = Offset(change.position.x - lastPos.x, change.position.y - lastPos.y)
                totalMoved += abs(delta.x) + abs(delta.y)
                lastPos = change.position

                when {
                    isDurationDrag || state.durationDragNoteId != null -> {
                        handleDrag(change.position, delta, state, uiState, viewModel)
                        change.consume()
                    }
                    isLoopDrag || state.loopDragTarget != null -> {
                        handleLoopDrag(change.position, state, uiState.snapValue, viewModel)
                        change.consume()
                    }
                    isMoveDrag && state.moveDragActive -> {
                        // Calculate beat and pitch delta from drag start
                        val totalDx = change.position.x - state.moveDragStartX
                        val totalDy = change.position.y - state.moveDragStartY
                        val rawDeltaBeat  = totalDx / state.pixelsPerBeat
                        val rawDeltaPitch = (totalDy / state.noteHeight).roundToInt()
                        val snappedDelta  = state.snapBeat(rawDeltaBeat, uiState.snapValue)

                        // Only update if changed to avoid excess recompose
                        if (snappedDelta != state.moveDragDeltaBeat ||
                            rawDeltaPitch != state.moveDragDeltaPitch) {
                            state.moveDragDeltaBeat  = snappedDelta
                            state.moveDragDeltaPitch = rawDeltaPitch
                        }
                        change.consume()
                    }
                    isRubberBand && state.rubberBandStart != null -> {
                        state.rubberBandEnd = change.position
                        change.consume()
                    }
                    uiState.currentTool == PianoRollViewModel.Tool.ERASE && totalMoved > 4f -> {
                        handleGridTap(change.position, state, uiState, viewModel)
                        change.consume()
                    }
                }
            }
        } while (pointers.isNotEmpty())

        // ── Finger up — finalise gesture ──────────────────────────────────

        // Commit move
        if (isMoveDrag && state.moveDragActive &&
            (state.moveDragDeltaBeat != 0f || state.moveDragDeltaPitch != 0)) {
            viewModel.moveSelectedNotes(
                state.moveDragDeltaBeat, state.moveDragDeltaPitch, uiState.snapValue)
            viewModel.commitMove()
        }
        state.moveDragActive     = false
        state.moveDragDeltaBeat  = 0f
        state.moveDragDeltaPitch = 0

        // Commit rubber band selection
        if (isRubberBand && state.rubberBandStart != null) {
            val selected = state.notesInRubberBand(uiState.activeNotes)
            if (selected.isNotEmpty()) {
                viewModel.setSelection(selected.map { it.id }.toSet())
            } else {
                viewModel.clearSelection()
            }
            state.rubberBandStart = null
            state.rubberBandEnd   = null
        }

        handleDragEnd(state)

        // Fire tap only on clean short press
        if (totalMoved <= 8f && !isDurationDrag && !isLoopDrag &&
            !isMoveDrag && !isRubberBand) {
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
fun Modifier.gridScrollGestures(state: PianoRollState): Modifier =
    this.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()

            // Horizontal scroll
            val totalW = state.totalBeats * state.pixelsPerBeat
            val ratioX = if (totalW > 0f) totalW / state.gridWidth else 1f
            state.scrollX = (state.scrollX + dragAmount.x * ratioX).coerceAtLeast(0f)

            // Vertical scroll
            val totalH = state.totalNotes * state.noteHeight
            val ratioY = if (totalH > 0f) totalH / state.gridHeight else 1f
            state.scrollY = (state.scrollY + dragAmount.y * ratioY).coerceAtLeast(0f)

            state.clampScroll(state.gridWidth, state.gridHeight)
        }
    }
fun Modifier.velocityStripGestures(
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel
): Modifier = this.pointerInput(uiState.activeNotes) {
    detectDragGestures(
        onDragStart = { offset ->
            val hit = state.velocityBarHit(offset.x, uiState.activeNotes)
            if (hit != null) {
                state.velocityDragNoteId      = hit.id
                state.velocityDragOrigVelocity = hit.velocity
            }
        },
        onDragEnd    = { state.velocityDragNoteId = null },
        onDragCancel = { state.velocityDragNoteId = null },
        onDrag = { change, _ ->
            change.consume()
            val noteId = state.velocityDragNoteId ?: return@detectDragGestures
            val newVel = state.yToVelocity(change.position.y, size.height.toFloat())
            viewModel.updateNoteVelocity(noteId, newVel)
        }
    )
}

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
        onKeyTap(state.yToPitch(down.position.y).coerceIn(0, state.totalNotes - 1))
        do {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                if (change.pressed) {
                    if (abs(change.position.y - lastPos.y) > 2f) {
                        lastPos = change.position
                        onKeyTap(state.yToPitch(change.position.y).coerceIn(0, state.totalNotes - 1))
                    }
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}