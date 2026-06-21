package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Spacer
// GmInstrument defined locally to avoid cross-module dependency
data class TrackInstrument(val label: String, val program: Int)

private val BG       = Color(0xFF1A1A1E)
private val RACK     = Color(0xFF222228)
private val PANEL    = Color(0xFF2A2A32)
private val BORDER   = Color(0xFF3A3A45)
private val ACCENT   = Color(0xFFE84040)
private val ACCENT2  = Color(0xFFFF6B35)
private val GREEN    = Color(0xFF3DDC84)
private val TEXT     = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val PURPLE   = Color(0xFFA78BFA)
private val BLUE     = Color(0xFF38BDF8)

// -- Per-track instrument lists ------------------------------------------------

private val BASS_INSTRUMENTS = listOf(
    TrackInstrument("Acoustic Bass",    32),
    TrackInstrument("Finger Bass",      33),
    TrackInstrument("Synth Bass 1",     38),
    TrackInstrument("Synth Bass 2",     39)
)

private val LEAD_INSTRUMENTS = listOf(
    TrackInstrument("Grand Piano",      0),
    TrackInstrument("Electric Piano",   4),
    TrackInstrument("Overdriven",       29),
    TrackInstrument("Square Lead",      80),
    TrackInstrument("Saw Lead",         81)
)

private val CHORD_INSTRUMENTS = listOf(
    TrackInstrument("Nylon Guitar",     24),
    TrackInstrument("Steel Guitar",     25),
    TrackInstrument("Jazz Guitar",      26),
    TrackInstrument("Clean Guitar",     27),
    TrackInstrument("Overdriven",       29),
    TrackInstrument("Distortion",       30)
)

private val PAD_INSTRUMENTS = listOf(
    TrackInstrument("Electric Piano",   4),
    TrackInstrument("Strings",          48),
    TrackInstrument("Strings 2",        49),
    TrackInstrument("Synth Strings",    51),
    TrackInstrument("Fantasia",         88),
    TrackInstrument("Warm Pad",         89)
)

private fun instrumentsForTrack(trackName: String): List<TrackInstrument>? = when (trackName.uppercase()) {
    "BASS"  -> BASS_INSTRUMENTS
    "LEAD"  -> LEAD_INSTRUMENTS
    "CHORD" -> CHORD_INSTRUMENTS
    "PAD"   -> PAD_INSTRUMENTS
    else    -> null  // DRUMS � no picker
}

private fun emojiForTrack(trackName: String): String = when (trackName.uppercase()) {
    "BASS"  -> "\uD83C\uDFB8"
    "LEAD"  -> "\uD83C\uDFB9"
    "CHORD" -> "\uD83C\uDFB8"
    "PAD"   -> "\uD83C\uDFB9"
    "DRUMS" -> "\uD83E\uDD41"
    else    -> "\uD83C\uDFB5"
}

@Composable
fun PianoRollScreen(
    sessionId: String,
    viewModel: PianoRollViewModel = hiltViewModel(),
    onSetBackAction: (((() -> Unit)?) -> Unit)? = null,
    onNavigateToChords: () -> Unit = {}
) {
    val state = remember { PianoRollState() }

    // Register back action immediately
    LaunchedEffect(Unit) {
        onSetBackAction?.invoke {
            onNavigateToChords()
        }
    }

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
    val hasClipboard by viewModel.hasClipboard.collectAsState()
    val drawDuration by viewModel.drawDuration.collectAsState()

    var showInstrumentPanel by remember { mutableStateOf(false) }

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
        totalBars    = totalBars,
        hasClipboard = hasClipboard,
        drawDuration = drawDuration
    )

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

    LaunchedEffect(isPlaying, playheadBeat) {
        if (!isPlaying && playheadBeat == 0f) state.scrollX = 0f
    }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {

        PianoRollToolbar(
            uiState           = uiState,
            onTool            = viewModel::setTool,
            onSnapCycle       = { viewModel.setSnapIndex((snapIndex + 1) % viewModel.snapValues.size) },
            onLoopToggle      = viewModel::toggleLoop,
            onInstrumentClick = { showInstrumentPanel = !showInstrumentPanel }
        )

        if (uiState.hasSelection) {
            SelectionActionBar(
                selectedCount = selectedIds.size,
                hasClipboard  = hasClipboard,
                pasteAtBeat   = playheadBeat,
                onCopy        = { viewModel.copySelectedNotes() },
                onPaste       = { viewModel.pasteNotes(playheadBeat, uiState.snapValue) },
                onDelete      = { viewModel.deleteSelectedNotes() },
                onClear       = { viewModel.clearSelection() }
            )
        }

        // Floating instrument panel � appears above track row when open
        if (showInstrumentPanel) {
            val activeTrack = tracks.getOrNull(activeIndex)
            if (activeTrack != null) {
                InstrumentPanel(
                    trackName      = activeTrack.name,
                    currentProgram = activeTrack.gmProgram,
                    onSelect       = { program ->
                        viewModel.setTrackGmProgram(activeIndex, program)
                        showInstrumentPanel = false
                    },
                    onDismiss = { showInstrumentPanel = false }
                )
            }
        } else {
            NeoTrackCardRow(
                tracks         = tracks,
                activeIndex    = activeIndex,
                onSelect       = viewModel::setActiveTrack,
                onMuteToggle   = viewModel::muteTrack,
                onVolumeChange = viewModel::setTrackVolume
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PianoRollCanvas(
                state     = state,
                uiState   = uiState,
                viewModel = viewModel,
                modifier  = Modifier.fillMaxSize()
            )

            VelocityOverlayTray(
                state     = state,
                uiState   = uiState,
                viewModel = viewModel,
                modifier  = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// -- Instrument panel ----------------------------------------------------------

@Composable
fun InstrumentPanel(
    trackName:      String,
    currentProgram: Int,
    onSelect:       (Int) -> Unit,
    onDismiss:      () -> Unit
) {
    val instruments = instrumentsForTrack(trackName)
    val emoji       = emojiForTrack(trackName)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2A))
            .border(1.dp, BLUE.copy(alpha = 0.4f), RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$emoji  ${trackName.uppercase()} SOUND",
                color = BLUE, fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("X", color = TEXT_DIM, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (instruments == null) {
            // DRUMS � WAV engine, no picker
            Text(
                text = "WAV Kit � sound managed by Drum Machine",
                color = TEXT_DIM, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            // Instrument chips in a wrapping row
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                instruments.forEach { inst ->
                    val isSelected = inst.program == currentProgram
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) BLUE.copy(alpha = 0.2f)
                                else Color(0xFF16161A)
                            )
                            .border(
                                1.dp,
                                if (isSelected) BLUE else BORDER,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onSelect(inst.program) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = inst.label,
                                color = if (isSelected) BLUE else TEXT,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "GM ${inst.program}",
                                color = TEXT_DIM, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// -- Selection action bar ------------------------------------------------------

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    hasClipboard:  Boolean,
    pasteAtBeat:   Float = 0f,
    onCopy:   () -> Unit,
    onPaste:  () -> Unit,
    onDelete: () -> Unit,
    onClear:  () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF1E1A2E))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$selectedCount selected",
            color = PURPLE, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        SelectionChip("COPY",   PURPLE, onCopy)
        if (hasClipboard) {
            SelectionChip(
                "PASTE \u2192 bar ${(pasteAtBeat / 4).toInt() + 1}",
                GREEN, onPaste
            )
        }
        SelectionChip("DELETE", ACCENT,   onDelete)
        SelectionChip("X",      TEXT_DIM, onClear)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp)
        .background(PURPLE.copy(alpha = 0.3f)))
}

@Composable
fun SelectionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// -- Toolbar -------------------------------------------------------------------

@Composable
fun PianoRollToolbar(
    uiState:           PianoRollUiState,
    onTool:            (PianoRollViewModel.Tool) -> Unit,
    onSnapCycle:       () -> Unit,
    onLoopToggle:      () -> Unit,
    onInstrumentClick: () -> Unit
) {
    val activeTrack = uiState.tracks.getOrNull(uiState.activeIndex)
    val emoji       = if (activeTrack != null) emojiForTrack(activeTrack.name) else "\uD83C\uDFB5"
    val instLabel   = if (activeTrack != null) {
        val instruments = instrumentsForTrack(activeTrack.name)
        instruments?.firstOrNull { it.program == activeTrack.gmProgram }?.label
            ?: if (activeTrack.name.uppercase() == "DRUMS") "WAV Kit" else "GM ${activeTrack.gmProgram}"
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth().height(40.dp)
            .background(PANEL).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        PianoRollViewModel.Tool.entries.forEach { tool ->
            ToolChip(
                label    = tool.name,
                selected = tool == uiState.currentTool,
                onClick  = { onTool(tool) }
            )
        }

        Divider()
        ToolChip(
            label          = "LOOP",
            selected       = uiState.loopEnabled,
            selectedColor  = Color(0xFF1A4A2E),
            selectedBorder = GREEN,
            selectedText   = GREEN,
            onClick        = onLoopToggle
        )
        Divider()
        // Instrument selector button � right side, weight fills remaining space
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF16161A))
                .border(1.dp, BLUE.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                .clickable(onClick = onInstrumentClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$emoji $instLabel \u25BE",
                color = BLUE, fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BORDER))
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BORDER))
}

@Composable
fun ToolChip(
    label:          String,
    selected:       Boolean,
    onClick:        () -> Unit,
    monospace:      Boolean = false,
    selectedColor:  Color   = ACCENT,
    selectedBorder: Color   = ACCENT,
    selectedText:   Color   = Color.White
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
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}