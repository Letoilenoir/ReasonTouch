package com.reasontouch.feature.pianoroll

import androidx.compose.ui.geometry.Offset
import com.reasontouch.core.data.NoteEvent

fun handleGridTap(
    offset: Offset,
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel
) {
    if (uiState.activeTrack == null) return

    if (offset.y < state.headerHeight) {
        viewModel.setPlayhead(state.xToBeat(offset.x), uiState.totalBars)
        return
    }

    val beat  = state.snapBeat(state.xToBeat(offset.x), uiState.snapValue)
        .coerceIn(0f, uiState.totalBars * 4f)
    val pitch = state.yToPitch(offset.y).coerceIn(0, state.totalNotes - 1)

    val tolerance = if (uiState.currentTool == PianoRollViewModel.Tool.ERASE) 8f else 0f
    val hitNote   = findHitNote(offset, state, uiState.activeNotes, tolerance)

    when (uiState.currentTool) {
        PianoRollViewModel.Tool.DRAW -> {
            if (hitNote == null) {
                viewModel.addNote(pitch, beat, uiState.snapValue)
                viewModel.auditionNote(pitch)
            } else {
                viewModel.selectNote(hitNote.id)
                viewModel.auditionNote(hitNote.pitch)
            }
        }
        PianoRollViewModel.Tool.ERASE  -> hitNote?.let { viewModel.deleteNote(it.id) }
        PianoRollViewModel.Tool.SELECT -> {
            if (hitNote != null) viewModel.selectNote(hitNote.id)
            else viewModel.clearSelection()
        }
    }
}

fun findHitNote(
    offset: Offset,
    state: PianoRollState,
    notes: List<NoteEvent>,
    tolerance: Float = 0f
): NoteEvent? {
    return notes.lastOrNull { note ->
        val nx = state.beatToX(note.beat)
        val ny = state.pitchToY(note.pitch)
        val nw = maxOf(4f, note.duration * state.pixelsPerBeat)
        offset.x >= nx - tolerance && offset.x <= nx + nw + tolerance &&
        offset.y >= ny - tolerance && offset.y <= ny + state.noteHeight - 2f + tolerance
    }
}

fun handleLoopDragStart(
    offset: Offset,
    state: PianoRollState,
    uiState: PianoRollUiState
) {
    state.loopDragTarget = null
    if (!uiState.loopEnabled) return
    val hit = state.loopHandleHit(offset.x, offset.y, uiState.loopStart, uiState.loopEnd)
    if (hit != null) {
        state.loopDragTarget    = hit
        state.loopDragStartX    = offset.x
        state.loopDragOrigStart = uiState.loopStart
        state.loopDragOrigEnd   = uiState.loopEnd
    }
}

fun handleLoopDrag(
    position: Offset,
    state: PianoRollState,
    snapValue: Float,
    viewModel: PianoRollViewModel
) {
    val target = state.loopDragTarget ?: return
    val dx     = position.x - state.loopDragStartX
    val dBeats = dx / state.pixelsPerBeat
    when (target) {
        "L" -> viewModel.setLoopStart(
            state.snapBeat(state.loopDragOrigStart + dBeats, snapValue))
        "R" -> viewModel.setLoopEnd(
            state.snapBeat(state.loopDragOrigEnd + dBeats, snapValue))
        "BODY" -> {
            val span     = state.loopDragOrigEnd - state.loopDragOrigStart
            val newStart = state.snapBeat(state.loopDragOrigStart + dBeats, snapValue)
            viewModel.setLoopStart(newStart)
            viewModel.setLoopEnd(newStart + span)
        }
    }
}

fun handleDragStart(
    offset: Offset,
    state: PianoRollState,
    uiState: PianoRollUiState
) {
    state.durationDragNoteId = null
    if (uiState.currentTool == PianoRollViewModel.Tool.DRAW ||
        uiState.currentTool == PianoRollViewModel.Tool.SELECT) {
        val edgeHit = state.noteRightEdgeHit(offset.x, offset.y, uiState.activeNotes)
        if (edgeHit != null) {
            state.durationDragNoteId     = edgeHit.id
            // anchorX already set inside noteRightEdgeHit
            state.durationDragOrigDuration = edgeHit.duration
            return
        }
    }
    handleLoopDragStart(offset, state, uiState)
}

fun handleDrag(
    position: Offset,
    dragAmount: Offset,
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel
) {
    when {
        state.durationDragNoteId != null -> {
            // Delta measured from note right-edge pixel, not finger start.
            // This means leftward drag immediately reduces duration.
            val dx     = position.x - state.durationDragAnchorX
            val dBeats = dx / state.pixelsPerBeat
            val newDur = state.snapBeat((state.durationDragOrigDuration + dBeats).coerceAtLeast(state.durationDragOrigDuration), uiState.snapValue)
            viewModel.updateNoteDuration(state.durationDragNoteId!!, newDur)
        }
        state.loopDragTarget != null -> {
            handleLoopDrag(position, state, uiState.snapValue, viewModel)
        }
    }
}

fun handleDragEnd(state: PianoRollState) {
    state.durationDragNoteId = null
    state.loopDragTarget     = null
}