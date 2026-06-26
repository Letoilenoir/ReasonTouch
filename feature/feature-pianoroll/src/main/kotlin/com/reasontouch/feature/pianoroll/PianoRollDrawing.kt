package com.reasontouch.feature.pianoroll

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.reasontouch.core.data.NoteEvent
import kotlin.math.abs

fun DrawScope.drawRowBackgrounds(state: PianoRollState) {
    for (p in 0 until state.totalNotes) {
        val y = state.pitchToY(p)
        if (y + state.noteHeight < 0 || y > size.height) continue
        val color = if (state.isBlackKey(p)) Color(0xFF1E1E28) else Color(0xFF22222E)
        drawRect(color = color, topLeft = Offset(0f, y), size = Size(size.width, state.noteHeight - 1))
        if (state.noteName(p).let { it.startsWith("C") && !it.contains("#") }) {
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
            val color  = when {
                isBar  -> Color(0xFF404050)
                isBeat -> Color(0xFF2E2E3E)
                else   -> Color(0xFF252530)
            }
            drawRect(color = color, topLeft = Offset(x, state.headerHeight), size = Size(1f, size.height))
        }
        b += snapValue
    }
}

fun DrawScope.drawRuler(state: PianoRollState, textMeasurer: TextMeasurer) {
    drawRect(color = Color(0xFF1A1A22), topLeft = Offset(0f, 0f), size = Size(size.width, state.headerHeight))
    drawRect(color = Color(0xFF2A2A35), topLeft = Offset(0f, state.headerHeight - 1), size = Size(size.width, 1f))
    for (bar in 1..state.totalBars) {
        val x = state.beatToX(((bar - 1) * state.beatsPerBar).toFloat())
        if (x < -80f || x > size.width) continue
        val measured = textMeasurer.measure("$bar",
            style = TextStyle(color = Color(0xFFE84040), fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
        drawText(measured, topLeft = Offset(x + 4f, state.headerHeight - 16f))
        for (beat in 1 until state.beatsPerBar) {
            val bx = state.beatToX(((bar - 1) * state.beatsPerBar + beat).toFloat()) - state.scrollX
            if (bx in 0f..size.width)
                drawRect(color = Color(0xFF444455), topLeft = Offset(bx, state.headerHeight - 7f), size = Size(1f, 7f))
        }
    }
}

fun DrawScope.drawLoopRegion(state: PianoRollState, loopStart: Float, loopEnd: Float) {
    val lx        = state.beatToX(loopStart)
    val rx        = state.beatToX(loopEnd)
    val clampedLx = lx.coerceAtLeast(0f)
    val clampedRx = rx.coerceAtMost(size.width)
    drawRect(color = Color(0x263DDC84), topLeft = Offset(clampedLx, 0f),
        size = Size(clampedRx - clampedLx, state.headerHeight))
    drawRect(color = Color(0x103DDC84), topLeft = Offset(clampedLx, state.headerHeight),
        size = Size(clampedRx - clampedLx, size.height))
    if (lx > -12f && lx < size.width + 12f) {
        drawRect(color = Color(0xFF3DDC84), topLeft = Offset(lx - 1f, 0f), size = Size(2f, state.headerHeight))
        val path = Path().apply { moveTo(lx, 2f); lineTo(lx + 13f, 2f); lineTo(lx + 13f, 13f); lineTo(lx, 13f); close() }
        drawPath(path, color = Color(0xFF3DDC84))
    }
    if (rx > -12f && rx < size.width + 12f) {
        drawRect(color = Color(0xFF3DDC84), topLeft = Offset(rx - 1f, 0f), size = Size(2f, state.headerHeight))
        val path = Path().apply { moveTo(rx, 2f); lineTo(rx - 13f, 2f); lineTo(rx - 13f, 13f); lineTo(rx, 13f); close() }
        drawPath(path, color = Color(0xFF3DDC84))
    }
}

fun DrawScope.drawNotes(
    state: PianoRollState,
    notes: List<NoteEvent>,
    trackColor: Long,
    ghost: Boolean,
    selectedIds: Set<String> = emptySet(),
    moveDeltaBeat: Float = 0f,
    moveDeltaPitch: Int  = 0
) {
    val baseColor = Color(trackColor)
    notes.forEach { note ->
        // Apply move preview offset for selected notes
        val displayBeat  = if (note.id in selectedIds) note.beat + moveDeltaBeat else note.beat
        val displayPitch = if (note.id in selectedIds) note.pitch + moveDeltaPitch else note.pitch

        val x = state.beatToX(displayBeat)
        val y = state.pitchToY(displayPitch)
        val w = maxOf(4f, note.duration * state.pixelsPerBeat - 1f)
        if (x + w < 0 || x > size.width || y + state.noteHeight < 0 || y > size.height) return@forEach

        val alpha     = if (ghost) 0.2f else 1f
        val isSelected = note.id in selectedIds
        val noteColor = when {
            isSelected -> Color(0xFFFF6B35)
            else       -> baseColor
        }

        drawRect(
            color   = noteColor.copy(alpha = alpha * 0.9f),
            topLeft = Offset(x, y),
            size    = Size(w, state.noteHeight - 2f)
        )
        drawRect(
            color   = Color.White.copy(alpha = alpha * 0.15f),
            topLeft = Offset(x, y),
            size    = Size(w, 3f)
        )

        // Selected note — draw border highlight
        if (isSelected && !ghost) {
            drawRect(
                color   = Color(0xFFFF6B35).copy(alpha = 0.8f),
                topLeft = Offset(x, y),
                size    = Size(w, state.noteHeight - 2f),
                style   = Stroke(width = 2f)
            )
        }

        // Right-edge grab handle
        if (!ghost && w > 12f) {
            val handleW = 10f
            drawRect(
                color   = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(x + w - handleW - 1f, y),
                size    = Size(1f, state.noteHeight - 2f)
            )
            drawRect(
                color   = Color.White.copy(alpha = 0.82f),
                topLeft = Offset(x + w - handleW, y + 1f),
                size    = Size(handleW, state.noteHeight - 3f)
            )
            val cx = x + w - handleW / 2f
            listOf(-3.5f, 0f, 3.5f).forEach { offset ->
                drawRect(
                    color   = Color.Black.copy(alpha = 0.4f),
                    topLeft = Offset(cx - 1f, y + state.noteHeight / 2f + offset - 1f),
                    size    = Size(2f, 2f)
                )
            }
        }
    }
}

// Rubber band selection rectangle
fun DrawScope.drawRubberBand(state: PianoRollState) {
    val rect = state.rubberBandRect ?: return
    drawRect(
        color   = Color(0x33A78BFA),
        topLeft = Offset(rect.left, rect.top),
        size    = Size(rect.width, rect.height)
    )
    drawRect(
        color   = Color(0xFFA78BFA),
        topLeft = Offset(rect.left, rect.top),
        size    = Size(rect.width, rect.height),
        style   = Stroke(width = 1.5f)
    )
}

fun DrawScope.drawPlayhead(state: PianoRollState, beat: Float) {
    val x = state.beatToX(beat)
    if (x < 0f || x > size.width) return
    drawRect(color = Color(0xFF3DDC84), topLeft = Offset(x, 0f), size = Size(2f, size.height))
    val path = Path().apply { moveTo(x - 5f, 0f); lineTo(x + 5f, 0f); lineTo(x, 9f); close() }
    drawPath(path, color = Color(0xFF3DDC84))
}

fun DrawScope.drawPianoKeys(state: PianoRollState, textMeasurer: TextMeasurer) {
    for (p in 0 until state.totalNotes) {
        val y = state.pitchToY(p)
        if (y + state.noteHeight < 0 || y > size.height) continue
        val black = state.isBlackKey(p)
        val name  = state.noteName(p)
        val isC   = name.startsWith("C") && !name.contains("#")
        drawRect(
            color   = if (black) Color(0xFF1C1C26) else Color(0xFFD0D0DC),
            topLeft = Offset(0f, y),
            size    = Size(state.keyWidth - (if (black) 14f else 0f), state.noteHeight - 1f)
        )
        if (isC) {
            drawRect(color = Color(0x22E84040), topLeft = Offset(0f, y),
                size = Size(state.keyWidth, state.noteHeight - 1f))
            val measured = textMeasurer.measure(name,
                style = TextStyle(color = Color(0xFFE84040), fontSize = 9.sp, fontFamily = FontFamily.Monospace))
            drawText(measured, topLeft = Offset(state.keyWidth - measured.size.width - 2f, y + state.noteHeight - 13f))
        }
        if (black) drawRect(color = Color(0xFF12121A),
            topLeft = Offset(state.keyWidth - 14f, y), size = Size(14f, state.noteHeight - 1f))
        drawRect(color = Color(0xFF111118),
            topLeft = Offset(0f, y + state.noteHeight - 1f), size = Size(state.keyWidth, 1f))
    }
}

fun DrawScope.drawVelocityBars(state: PianoRollState, notes: List<NoteEvent>) {
    drawRect(color = Color(0xFF1A1A22))
    notes.forEach { note ->
        val x    = state.beatToX(note.beat)
        val barW = maxOf(3f, state.pixelsPerBeat * 0.25f - 1f)
        if (x < -barW || x > size.width) return@forEach
        val velH = (note.velocity / 127f) * (size.height - 14f)
        val y    = size.height - velH
        drawRect(color = Color(0xFFE84040), topLeft = Offset(x, y),
            size = Size(minOf(barW, state.pixelsPerBeat - 1f), velH))
        drawRect(color = Color.White.copy(alpha = 0.3f), topLeft = Offset(x, y),
            size = Size(minOf(barW, state.pixelsPerBeat - 1f), 2f))
    }
}

fun DrawScope.drawHorizontalScrollBar(state: PianoRollState) {
    val totalW = state.totalBeats * state.pixelsPerBeat
    if (totalW <= state.gridWidth) return
    val thumbW    = (state.gridWidth / totalW * size.width).coerceAtLeast(40f)
    val maxScroll = totalW - state.gridWidth
    val thumbX    = (state.scrollX / maxScroll) * (size.width - thumbW)
    drawRect(color = Color(0xFF2A2A35), topLeft = Offset(0f, 0f), size = Size(size.width, size.height))
    drawRect(color = Color(0xFF555568), topLeft = Offset(thumbX, 2f), size = Size(thumbW, size.height - 4f))
}

fun DrawScope.drawVerticalScrollBar(state: PianoRollState) {
    val totalH = state.totalNotes * state.noteHeight
    if (totalH <= state.gridHeight) return
    val thumbH    = (state.gridHeight / totalH * size.height).coerceAtLeast(40f)
    val maxScroll = totalH - state.gridHeight
    val thumbY    = (state.scrollY / maxScroll) * (size.height - thumbH)
    drawRect(color = Color(0xFF2A2A35), topLeft = Offset(0f, 0f), size = Size(size.width, size.height))
    drawRect(color = Color(0xFF555568), topLeft = Offset(2f, thumbY), size = Size(size.width - 4f, thumbH))
}