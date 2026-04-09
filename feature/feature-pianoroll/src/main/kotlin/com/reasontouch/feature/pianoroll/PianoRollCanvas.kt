package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@Composable
fun PianoRollCanvas(
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(uiState.totalBars) {
        state.totalBars = uiState.totalBars
    }

    Row(modifier = modifier) {

        // Piano keys
        Canvas(
            modifier = Modifier
                .width(state.keyWidth.dp)
                .fillMaxHeight()
                .background(Color(0xFF1A1A22))
                .pianoKeyGestures(state) { pitch -> viewModel.auditionNote(pitch) }
        ) {
            drawPianoKeys(state, textMeasurer)
        }

        // Grid canvas — tap, drag/zoom unified, erase-drag separate
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pianoRollTapGestures(state, uiState, viewModel)
                .pianoRollDragGestures(state, uiState, viewModel)
                .pianoRollEraseDragGestures(state, uiState, viewModel)
        ) {
            state.gridWidth  = size.width
            state.gridHeight = size.height

            drawRowBackgrounds(state)
            drawGridLines(state, uiState.snapValue)
            drawRuler(state, textMeasurer)

            if (uiState.loopEnabled) drawLoopRegion(state, uiState.loopStart, uiState.loopEnd)

            uiState.tracks.forEachIndexed { i, track ->
                if (i != uiState.activeIndex && !track.muted) {
                    val notes = uiState.allNotes[track.id] ?: emptyList()
                    drawNotes(state, notes, state.trackColor(i), ghost = true)
                }
            }

            uiState.activeTrack?.let { track ->
                if (!track.muted) {
                    drawNotes(state, uiState.activeNotes, state.trackColor(uiState.activeIndex),
                        ghost = false, selectedIds = uiState.selectedIds)
                }
            }

            drawPlayhead(state, uiState.playheadBeat)
        }
    }
}

@Composable
fun VelocityStripCanvas(
    state: PianoRollState,
    uiState: PianoRollUiState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawVelocityBars(state, uiState.activeNotes)
    }
}