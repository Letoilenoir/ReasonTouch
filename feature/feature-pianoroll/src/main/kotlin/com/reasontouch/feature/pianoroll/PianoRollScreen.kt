package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reasontouch.core.data.MidiTrack
import com.reasontouch.core.data.NoteEvent
import kotlin.math.abs
import kotlin.math.roundToInt

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
    val tracks       by viewModel.tracks.collectAsState()
    val activeIndex  by viewModel.activeTrackIndex.collectAsState()
    val activeNotes  by viewModel.activeNotes.collectAsState()
    val allNotes     by viewModel.allNotes.collectAsState()
    val chords       by viewModel.chords.collectAsState()
    val currentTool  by viewModel.currentTool.collectAsState()
    val snapIndex    by viewModel.snapIndex.collectAsState()
    val selectedIds  by viewModel.selectedNoteIds.collectAsState()
    val loopEnabled  by viewModel.loopEnabled.collectAsState()
    val loopStart    by viewModel.loopStart.collectAsState()
    val loopEnd      by viewModel.loopEnd.collectAsState()
    val playheadBeat by viewModel.playheadBeat.collectAsState()

    val snapValue = viewModel.snapValues[snapIndex]
    val activeTrack = tracks.getOrNull(activeIndex)

    Column(modifier = Modifier.fillMaxSize().background(BG)) {

        // ── Toolbar ───────────────────────────────────────────────────────
        PianoRollToolbar(
            currentTool  = currentTool,
            snapIndex    = snapIndex,
            snapLabels   = viewModel.snapLabels,
            loopEnabled  = loopEnabled,
            onTool       = viewModel::setTool,
            onSnapCycle  = { viewModel.setSnapIndex((snapIndex + 1) % viewModel.snapValues.size) },
            onLoopToggle = viewModel::toggleLoop
        )

        // ── Track selector ────────────────────────────────────────────────
        TrackSelector(
            tracks      = tracks,
            activeIndex = activeIndex,
            onSelect    = viewModel::setActiveTrack
        )

        // ── Main grid area ────────────────────────────────────────────────
        Row(modifier = Modifier.weight(1f)) {

            // Piano keys
            PianoKeys(
                state  = state,
                modifier = Modifier.width(state.keyWidth.dp).fillMaxHeight()
            )

            // Grid canvas
            val textMeasurer = rememberTextMeasurer()

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(currentTool, snapValue, activeTrack) {
                        detectTapGestures { offset ->
                            handleGridTap(offset, state, currentTool, snapValue, activeTrack, viewModel, activeNotes)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                state.scrollX = (state.scrollX - dragAmount.x).coerceAtLeast(0f)
                                state.scrollY = (state.scrollY - dragAmount.y).coerceAtLeast(0f)
                                state.clampScroll(state.gridWidth, state.gridHeight)
                            }
                        )
                    }
            ) {
                state.gridWidth  = size.width
                state.gridHeight = size.height

                // Row backgrounds
                drawRowBackgrounds(state)

                // Vertical grid lines
                drawGridLines(state, snapValue)

                // Ruler
                drawRuler(state, textMeasurer)

                // Loop region
                if (loopEnabled) drawLoopRegion(state, loopStart, loopEnd)

                // Ghost notes from other tracks
                tracks.forEachIndexed { i, track ->
                    if (i != activeIndex) {
                        val notes = allNotes[track.id] ?: emptyList()
                        drawNotes(state, notes, state.trackColor(i), ghost = true)
                    }
                }

                // Active track notes
                if (activeTrack != null) {
                    val color = state.trackColor(activeIndex)
                    drawNotes(state, activeNotes, color, ghost = false, selectedIds = selectedIds)
                }

                // Playhead
                drawPlayhead(state, playheadBeat)
            }
        }

        // ── Velocity strip ────────────────────────────────────────────────
        VelocityStrip(
            notes  = activeNotes,
            state  = state,
            modifier = Modifier.fillMaxWidth().height(56.dp).background(RACK)
        )

        // ── Status bar ────────────────────────────────────────────────────
        StatusBar(
            noteCount   = activeNotes.size,
            trackName   = activeTrack?.name ?: "",
            tool        = currentTool,
            loopEnabled = loopEnabled,
            loopStart   = loopStart,
            loopEnd     = loopEnd
        )
    }
}

// ── Grid drawing functions ────────────────────────────────────────────────────

fun DrawScope.drawRowBackgrounds(state: PianoRollState) {
    for (p in 0 until state.totalNotes) {
        val y = state.pitchToY(p)
        if (y + state.noteHeight < 0 || y > size.height) continue
        val color = if (state.isBlackKey(p)) Color(0xFF1E1E28) else Color(0xFF22222E)
        drawRect(color = color, topLeft = Offset(0f, y), size = Size(size.width, state.noteHeight - 1))
        val name = state.noteName(p)
        if (name.startsWith("C") && !name.contains("#")) {
            drawRect(color = Color(0x0FE84040), topLeft = Offset(0f, y), size = Size(size.width, state.noteHeight - 1))
        }
    }
}

fun DrawScope.drawGridLines(state: PianoRollState, snapValue: Float) {
    val startBeat = (state.xToBeat(0f) / snapValue).toInt() * snapValue
    var b = startBeat
    while (b <= state.totalBeats + 1) {
        val x = state.beatToX(b)
        if (x in 0f..size.width) {
            val isBar  = abs(b % state.beatsPerBar) < 0.001f
            val isBeat = abs(b % 1f) < 0.001f
            val color  = when { isBar -> Color(0xFF404050); isBeat -> Color(0xFF2E2E3E); else -> Color(0xFF252530) }
            drawRect(color = color, topLeft = Offset(x, state.headerHeight), size = Size(1f, size.height))
        }
        b += snapValue
    }
}

fun DrawScope.drawRuler(state: PianoRollState, textMeasurer: androidx.compose.ui.text.TextMeasurer) {
    drawRect(color = Color(0xFF1A1A22), topLeft = Offset(0f, 0f), size = Size(size.width, state.headerHeight))
    drawRect(color = Color(0xFF2A2A35), topLeft = Offset(0f, state.headerHeight - 1), size = Size(size.width, 1f))

    for (bar in 1..state.totalBars) {
        val x = state.beatToX(((bar - 1) * state.beatsPerBar).toFloat())
        if (x < -80f || x > size.width) continue
        val measured = textMeasurer.measure(
            text = "$bar",
            style = TextStyle(color = Color(0xFFE84040), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        )
        drawText(measured, topLeft = Offset(x + 4f, state.headerHeight - 16f))
        for (beat in 1 until state.beatsPerBar) {
            val bx = state.beatToX(((bar - 1) * state.beatsPerBar + beat).toFloat())
            if (bx in 0f..size.width) {
                drawRect(color = Color(0xFF444455), topLeft = Offset(bx, state.headerHeight - 7f), size = Size(1f, 7f))
            }
        }
    }
}

fun DrawScope.drawLoopRegion(state: PianoRollState, loopStart: Float, loopEnd: Float) {
    val lx = state.beatToX(loopStart)
    val rx = state.beatToX(loopEnd)
    val clampedLx = lx.coerceAtLeast(0f)
    val clampedRx = rx.coerceAtMost(size.width)

    drawRect(color = Color(0x263DDC84), topLeft = Offset(clampedLx, 0f), size = Size(clampedRx - clampedLx, state.headerHeight))
    drawRect(color = Color(0x103DDC84), topLeft = Offset(clampedLx, state.headerHeight), size = Size(clampedRx - clampedLx, size.height))

    // L handle
    if (lx > -12f && lx < size.width + 12f) {
        drawRect(color = Color(0xFF3DDC84), topLeft = Offset(lx - 1f, 0f), size = Size(2f, state.headerHeight))
        val path = Path().apply {
            moveTo(lx, 2f); lineTo(lx + 13f, 2f); lineTo(lx + 13f, 13f); lineTo(lx, 13f); close()
        }
        drawPath(path, color = Color(0xFF3DDC84))
    }
    // R handle
    if (rx > -12f && rx < size.width + 12f) {
        drawRect(color = Color(0xFF3DDC84), topLeft = Offset(rx - 1f, 0f), size = Size(2f, state.headerHeight))
        val path = Path().apply {
            moveTo(rx, 2f); lineTo(rx - 13f, 2f); lineTo(rx - 13f, 13f); lineTo(rx, 13f); close()
        }
        drawPath(path, color = Color(0xFF3DDC84))
    }
}

fun DrawScope.drawNotes(
    state: PianoRollState,
    notes: List<NoteEvent>,
    trackColor: Long,
    ghost: Boolean,
    selectedIds: Set<String> = emptySet()
) {
    val baseColor = Color(trackColor)
    notes.forEach { note ->
        val x = state.beatToX(note.beat)
        val y = state.pitchToY(note.pitch)
        val w = maxOf(4f, note.duration * state.pixelsPerBeat - 1f)
        if (x + w < 0 || x > size.width || y + state.noteHeight < 0 || y > size.height) return@forEach

        val alpha = if (ghost) 0.2f else 1f
        val selected = note.id in selectedIds

        val noteColor = if (selected) Color(0xFFFF6B35) else baseColor

        drawRect(
            color = noteColor.copy(alpha = alpha * 0.9f),
            topLeft = Offset(x, y),
            size = Size(w, state.noteHeight - 2f)
        )
        drawRect(
            color = Color.White.copy(alpha = alpha * 0.15f),
            topLeft = Offset(x, y),
            size = Size(w, 3f)
        )
    }
}

fun DrawScope.drawPlayhead(state: PianoRollState, beat: Float) {
    val x = state.beatToX(beat)
    if (x < 0f || x > size.width) return
    drawRect(color = Color(0xFF3DDC84), topLeft = Offset(x, 0f), size = Size(2f, size.height))
    val path = Path().apply {
        moveTo(x - 5f, 0f); lineTo(x + 5f, 0f); lineTo(x, 9f); close()
    }
    drawPath(path, color = Color(0xFF3DDC84))
}

// ── Touch handling ────────────────────────────────────────────────────────────
fun handleGridTap(
    offset: Offset,
    state: PianoRollState,
    tool: PianoRollViewModel.Tool,
    snapValue: Float,
    activeTrack: MidiTrack?,
    viewModel: PianoRollViewModel,
    activeNotes: List<NoteEvent>
) {
    if (activeTrack == null) return
    if (offset.y < state.headerHeight) {
        viewModel.setPlayhead(state.xToBeat(offset.x))
        return
    }

    val beat  = state.snapBeat(state.xToBeat(offset.x), snapValue).coerceIn(0f, state.totalBeats.toFloat())
    val pitch = state.yToPitch(offset.y).coerceIn(0, state.totalNotes - 1)

    val hitNote = activeNotes.lastOrNull { note ->
        val nx = state.beatToX(note.beat)
        val ny = state.pitchToY(note.pitch)
        val nw = maxOf(4f, note.duration * state.pixelsPerBeat)
        offset.x >= nx && offset.x <= nx + nw && offset.y >= ny && offset.y <= ny + state.noteHeight - 2f
    }

    when (tool) {
        PianoRollViewModel.Tool.DRAW -> {
            if (hitNote == null) {
                viewModel.addNote(pitch, beat, snapValue)
            } else {
                viewModel.selectNote(hitNote.id)
            }
        }
        PianoRollViewModel.Tool.ERASE -> {
            hitNote?.let { viewModel.deleteNote(it.id) }
        }
        PianoRollViewModel.Tool.SELECT -> {
            if (hitNote != null) viewModel.selectNote(hitNote.id)
            else viewModel.clearSelection()
        }
    }
}

// ── Piano Keys ────────────────────────────────────────────────────────────────
@Composable
fun PianoKeys(state: PianoRollState, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.background(Color(0xFF1A1A22))) {
        for (p in 0 until state.totalNotes) {
            val y = state.pitchToY(p)
            if (y + state.noteHeight < 0 || y > size.height) continue
            val black = state.isBlackKey(p)
            val name  = state.noteName(p)
            val isC   = name.startsWith("C") && !name.contains("#")

            drawRect(
                color = if (black) Color(0xFF1C1C26) else Color(0xFFD0D0DC),
                topLeft = Offset(0f, y),
                size = Size(size.width - (if (black) 14f else 0f), state.noteHeight - 1f)
            )
            if (isC) {
                drawRect(color = Color(0x22E84040), topLeft = Offset(0f, y), size = Size(size.width, state.noteHeight - 1f))
                val measured = textMeasurer.measure(
                    name,
                    style = TextStyle(color = Color(0xFFE84040), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                )
                drawText(measured, topLeft = Offset(size.width - measured.size.width - 2f, y + state.noteHeight - 13f))
            }
            if (black) {
                drawRect(color = Color(0xFF12121A), topLeft = Offset(size.width - 14f, y), size = Size(14f, state.noteHeight - 1f))
            }
            drawRect(color = Color(0xFF111118), topLeft = Offset(0f, y + state.noteHeight - 1f), size = Size(size.width, 1f))
        }
    }
}

// ── Velocity Strip ────────────────────────────────────────────────────────────
@Composable
fun VelocityStrip(notes: List<NoteEvent>, state: PianoRollState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(color = Color(0xFF1A1A22))
        notes.forEach { note ->
            val x    = state.beatToX(note.beat)
            val barW = maxOf(3f, state.pixelsPerBeat * 0.25f - 1f)
            if (x < -barW || x > size.width) return@forEach
            val velH = (note.velocity / 127f) * (size.height - 14f)
            val y    = size.height - velH
            drawRect(
                color = Color(0xFFE84040),
                topLeft = Offset(x, y),
                size = Size(minOf(barW, state.pixelsPerBeat - 1f), velH)
            )
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(x, y),
                size = Size(minOf(barW, state.pixelsPerBeat - 1f), 2f)
            )
        }
    }
}

// ── Toolbar ───────────────────────────────────────────────────────────────────
@Composable
fun PianoRollToolbar(
    currentTool: PianoRollViewModel.Tool,
    snapIndex: Int,
    snapLabels: List<String>,
    loopEnabled: Boolean,
    onTool: (PianoRollViewModel.Tool) -> Unit,
    onSnapCycle: () -> Unit,
    onLoopToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).background(PANEL).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        PianoRollViewModel.Tool.values().forEach { tool ->
            ToolChip(
                label = tool.name,
                selected = tool == currentTool,
                onClick = { onTool(tool) }
            )
        }
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(BORDER))
        ToolChip(
            label = snapLabels[snapIndex],
            selected = false,
            onClick = onSnapCycle,
            monospace = true
        )
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(BORDER))
        ToolChip(
            label = "LOOP",
            selected = loopEnabled,
            selectedColor = Color(0xFF1A4A2E),
            selectedBorder = GREEN,
            selectedText = GREEN,
            onClick = onLoopToggle
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BORDER))
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
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp).background(RACK).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tracks.forEachIndexed { i, track ->
            val isActive = i == activeIndex
            val trackColor = Color(when (i % 8) {
                0 -> 0xFFE84040L; 1 -> 0xFF3DDC84L; 2 -> 0xFF38BDF8L; 3 -> 0xFFA78BFAL
                4 -> 0xFFFF6B35L; 5 -> 0xFFF5C518L; 6 -> 0xFFF472B6L; else -> 0xFF94A3B8L
            })
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isActive) Color(0xFF28283A) else RACK)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = track.name,
                    color = if (isActive) trackColor else TEXT_DIM,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(BORDER))
}

// ── Status bar ────────────────────────────────────────────────────────────────
@Composable
fun StatusBar(
    noteCount: Int,
    trackName: String,
    tool: PianoRollViewModel.Tool,
    loopEnabled: Boolean,
    loopStart: Float,
    loopEnd: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(22.dp).background(BG).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatusItem(label = "NOTES", value = "$noteCount")
        StatusItem(label = "TRACK", value = trackName)
        StatusItem(label = "TOOL", value = tool.name)
        if (loopEnabled) {
            StatusItem(label = "LOOP", value = "${loopStart.toInt()+1}→${loopEnd.toInt()}", valueColor = GREEN)
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