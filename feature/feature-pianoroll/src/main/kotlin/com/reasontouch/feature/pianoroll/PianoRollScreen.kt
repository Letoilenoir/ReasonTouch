package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reasontouch.core.data.MidiTrack

private val BG       = Color(0xFF1A1A1E)
private val RACK     = Color(0xFF222228)
private val PANEL    = Color(0xFF2A2A32)
private val BORDER   = Color(0xFF3A3A45)
private val ACCENT   = Color(0xFFE84040)
private val ACCENT2  = Color(0xFFFF6B35)
private val GREEN    = Color(0xFF3DDC84)
private val TEXT     = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)

@Composable
fun PianoRollScreen(
    sessionId: String,
    viewModel: PianoRollViewModel = hiltViewModel()
) {
    val state = remember { PianoRollState() }

    val session      by viewModel.session.collectAsState()
    val tracks       by viewModel.tracks.collectAsState()
    val activeIndex  by viewModel.activeTrackIndex.collectAsState()
    val activeNotes  by viewModel.activeNotes.collectAsState()
    val allNotes     by viewModel.allNotes.collectAsState()
    val currentTool  by viewModel.currentTool.collectAsState()
    val snapIndex    by viewModel.snapIndex.collectAsState()
    val selectedIds  by viewModel.selectedNoteIds.collectAsState()
    val loopEnabled  by viewModel.loopEnabled.collectAsState()
    val loopStart    by viewModel.loopStart.collectAsState()
    val loopEnd      by viewModel.loopEnd.collectAsState()
    val playheadBeat by viewModel.playheadBeat.collectAsState()
    val isPlaying    by viewModel.isPlaying.collectAsState()

    val bpm       = session?.bpm ?: 120
    val totalBars = session?.totalBars ?: 4

    val uiState = PianoRollUiState(
        tracks       = tracks,
        activeIndex  = activeIndex,
        activeNotes  = activeNotes,
        allNotes     = allNotes,
        currentTool  = currentTool,
        snapIndex    = snapIndex,
        selectedIds  = selectedIds,
        loopEnabled  = loopEnabled,
        loopStart    = loopStart,
        loopEnd      = loopEnd,
        playheadBeat = playheadBeat,
        isPlaying    = isPlaying,
        totalBars    = totalBars
    )

    // Auto-scroll during playback
    LaunchedEffect(playheadBeat, isPlaying) {
        if (isPlaying && state.gridWidth > 0f) {
            val playheadX = state.beatToX(playheadBeat)
            val threshold = state.gridWidth * 0.7f
            if (playheadX > threshold) {
                state.scrollX = (playheadBeat * state.pixelsPerBeat - state.gridWidth * 0.3f)
                    .coerceAtLeast(0f)
            }
        }
    }

    // Scroll back to bar 1 when rewind fires (playheadBeat == 0 and not playing)
    LaunchedEffect(isPlaying, playheadBeat) {
        if (!isPlaying && playheadBeat == 0f) {
            state.scrollX = 0f
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {

        PianoRollToolbar(
            uiState      = uiState,
            onTool       = viewModel::setTool,
            onSnapCycle  = { viewModel.setSnapIndex((snapIndex + 1) % viewModel.snapValues.size) },
            onLoopToggle = viewModel::toggleLoop,
            onPlay       = { viewModel.play(bpm) },
            onStop       = viewModel::stop,
            onRewind     = viewModel::rewind
        )

        TrackSelector(
            tracks       = tracks,
            activeIndex  = activeIndex,
            onSelect     = viewModel::setActiveTrack,
            onMuteToggle = viewModel::muteTrack
        )

        PianoRollCanvas(
            state     = state,
            uiState   = uiState,
            viewModel = viewModel,
            modifier  = Modifier.weight(1f).fillMaxWidth()
        )

        VelocityStripCanvas(
            state    = state,
            uiState  = uiState,
            modifier = Modifier.fillMaxWidth().height(56.dp).background(RACK)
        )

        StatusBar(uiState = uiState, bpm = bpm, totalBars = totalBars)
    }
}

// ── Toolbar ───────────────────────────────────────────────────────────────────

@Composable
fun PianoRollToolbar(
    uiState: PianoRollUiState,
    onTool: (PianoRollViewModel.Tool) -> Unit,
    onSnapCycle: () -> Unit,
    onLoopToggle: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onRewind: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).background(PANEL).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        PianoRollViewModel.Tool.values().forEach { tool ->
            ToolChip(label = tool.name, selected = tool == uiState.currentTool, onClick = { onTool(tool) })
        }
        Divider()
        ToolChip(label = uiState.snapLabels[uiState.snapIndex], selected = false, onClick = onSnapCycle, monospace = true)
        Divider()
        ToolChip(label = "LOOP", selected = uiState.loopEnabled,
            selectedColor = Color(0xFF1A4A2E), selectedBorder = GREEN, selectedText = GREEN,
            onClick = onLoopToggle)
        Divider()
        ToolChip(label = "<<", selected = false, onClick = onRewind)
        ToolChip(
            label = if (uiState.isPlaying) "||" else ">",
            selected = uiState.isPlaying,
            selectedColor = Color(0xFF1A3A2A), selectedBorder = GREEN, selectedText = GREEN,
            onClick = onPlay
        )
        ToolChip(label = "[]", selected = false, onClick = onStop)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BORDER))
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BORDER))
}

@Composable
fun ToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    monospace: Boolean = false,
    selectedColor: Color = ACCENT,
    selectedBorder: Color = ACCENT,
    selectedText: Color = Color.White
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) selectedColor else RACK)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) selectedText else TEXT_DIM,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

// ── Track selector ────────────────────────────────────────────────────────────

@Composable
fun TrackSelector(
    tracks: List<MidiTrack>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onMuteToggle: (Int) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).background(RACK).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tracks.forEachIndexed { i, track ->
            val isActive   = i == activeIndex
            val trackColor = Color(when (i % 8) {
                0 -> 0xFFE84040L; 1 -> 0xFF3DDC84L; 2 -> 0xFF38BDF8L; 3 -> 0xFFA78BFAL
                4 -> 0xFFFF6B35L; 5 -> 0xFFF5C518L; 6 -> 0xFFF472B6L; else -> 0xFF94A3B8L
            })
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(when {
                        track.muted -> Color(0xFF2A1A1A)
                        isActive    -> Color(0xFF28283A)
                        else        -> RACK
                    })
                    .clickable { onSelect(i) }
                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = track.name,
                    color = when {
                        track.muted -> TEXT_DIM.copy(alpha = 0.4f)
                        isActive    -> trackColor
                        else        -> TEXT_DIM
                    },
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (track.muted) Color(0xFF5A1A1A) else Color(0xFF1A1A22))
                        .clickable { onMuteToggle(i) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = "M", color = if (track.muted) ACCENT else TEXT_DIM,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(BORDER))
}

// ── Status bar ────────────────────────────────────────────────────────────────

@Composable
fun StatusBar(uiState: PianoRollUiState, bpm: Int, totalBars: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(22.dp).background(BG).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatusItem("BPM",   "$bpm")
        StatusItem("BARS",  "$totalBars")
        StatusItem("NOTES", "${uiState.activeNotes.size}")
        StatusItem("TRACK", uiState.activeTrack?.name ?: "")
        StatusItem("TOOL",  uiState.currentTool.name)
        if (uiState.loopEnabled) {
            StatusItem("LOOP", "${uiState.loopStart.toInt()+1}>${uiState.loopEnd.toInt()}", GREEN)
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, valueColor: Color = ACCENT2) {
    Row {
        Text(text = "$label: ", color = TEXT_DIM, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = valueColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}