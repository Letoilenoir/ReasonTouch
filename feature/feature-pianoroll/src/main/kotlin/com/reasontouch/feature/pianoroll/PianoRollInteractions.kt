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

    // Tap in ruler — set playhead
    if (offset.y < state.headerHeight) {
        viewModel.setPlayhead(
            state.xToBeat(offset.x)
        )
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

// Loop dragging handled; clamping is done in ViewModel
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
            state.snapBeat(state.loopDragOrigEnd + dBeats, snapValue)
        )
        "BODY" -> {
            val span     = state.loopDragOrigEnd - state.loopDragOrigStart
            val newStart = state.snapBeat(state.loopDragOrigStart + dBeats, snapValue)
            viewModel.setLoopStart(newStart)
            viewModel.setLoopEnd(newStart + span)

        }
    }
}