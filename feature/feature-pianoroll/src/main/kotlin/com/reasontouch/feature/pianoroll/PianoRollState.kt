package com.reasontouch.feature.pianoroll

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class PianoRollState {
    var scrollX by mutableStateOf(0f)
    var scrollY by mutableStateOf(0f)
    var pixelsPerBeat by mutableStateOf(80f)
    var gridWidth by mutableStateOf(0f)
    var gridHeight by mutableStateOf(0f)

    var noteHeight by mutableStateOf(22f)
    val keyWidth = 64f
    val headerHeight = 24f
    val totalNotes = 88
    val beatsPerBar = 4

    var totalBars by mutableStateOf(4)
    val totalBeats get() = totalBars * beatsPerBar

    fun beatToX(beat: Float) = beat * pixelsPerBeat - scrollX
    fun xToBeat(x: Float) = (x + scrollX) / pixelsPerBeat
    fun pitchToY(pitch: Int) = pitch * noteHeight - scrollY + headerHeight
    fun yToPitch(y: Float) = ((y + scrollY - headerHeight) / noteHeight).toInt()

    fun snapBeat(beat: Float, snapValue: Float): Float =
        (Math.round(beat / snapValue) * snapValue)

    // ── Loop drag ─────────────────────────────────────────────────────────
    var loopDragTarget: String? = null
    var loopDragStartX: Float = 0f
    var loopDragOrigStart: Float = 0f
    var loopDragOrigEnd: Float = 0f

    fun loopHandleHit(x: Float, y: Float, loopStart: Float, loopEnd: Float): String? {
        val lx = beatToX(loopStart)
        val rx = beatToX(loopEnd)
        val tolerance = 24f
        return when {
            kotlin.math.abs(x - lx) <= tolerance -> "L"
            kotlin.math.abs(x - rx) <= tolerance -> "R"
            x > lx + tolerance && x < rx - tolerance -> "BODY"
            else -> null
        }
    }

    // ── Duration drag ─────────────────────────────────────────────────────
    var durationDragNoteId: String? = null
    var durationDragAnchorX: Float = 0f
    var durationDragOrigDuration: Float = 0f

    fun noteRightEdgeHit(
        x: Float,
        y: Float,
        notes: List<com.reasontouch.core.data.NoteEvent>
    ): com.reasontouch.core.data.NoteEvent? {
        val edgeZone = 28f
        return notes.lastOrNull { note ->
            val nx = beatToX(note.beat)
            val ny = pitchToY(note.pitch)
            val nw = maxOf(4f, note.duration * pixelsPerBeat - 1f)
            val rightEdge = nx + nw
            if (x >= rightEdge - edgeZone && x <= rightEdge + edgeZone &&
                y >= ny && y <= ny + noteHeight - 2f) {
                durationDragAnchorX = rightEdge
                true
            } else false
        }
    }

    // ── Rubber band selection ─────────────────────────────────────────────
    var rubberBandStart: Offset? by mutableStateOf(null)
    var rubberBandEnd:   Offset? by mutableStateOf(null)

    val rubberBandRect: androidx.compose.ui.geometry.Rect?
        get() {
            val s = rubberBandStart ?: return null
            val e = rubberBandEnd   ?: return null
            return androidx.compose.ui.geometry.Rect(
                left   = minOf(s.x, e.x),
                top    = minOf(s.y, e.y),
                right  = maxOf(s.x, e.x),
                bottom = maxOf(s.y, e.y)
            )
        }

    // Find all notes whose rects intersect the rubber band rectangle
    fun notesInRubberBand(
        notes: List<com.reasontouch.core.data.NoteEvent>
    ): List<com.reasontouch.core.data.NoteEvent> {
        val rect = rubberBandRect ?: return emptyList()
        return notes.filter { note ->
            val nx = beatToX(note.beat)
            val ny = pitchToY(note.pitch)
            val nw = maxOf(4f, note.duration * pixelsPerBeat - 1f)
            val nh = noteHeight - 2f
            // Intersection check
            nx < rect.right && nx + nw > rect.left &&
            ny < rect.bottom && ny + nh > rect.top
        }
    }

    // ── Note move drag ────────────────────────────────────────────────────
    // Dragging selected notes as a group
    var moveDragActive:    Boolean = false
    var moveDragStartX:    Float   = 0f
    var moveDragStartY:    Float   = 0f
    var moveDragDeltaBeat: Float   by mutableStateOf(0f)
    var moveDragDeltaPitch: Int    by mutableStateOf(0)

    // ── Velocity drag ─────────────────────────────────────────────────────
    var velocityDragNoteId: String? = null
    var velocityDragOrigVelocity: Int = 100

    fun velocityBarHit(
        x: Float,
        notes: List<com.reasontouch.core.data.NoteEvent>
    ): com.reasontouch.core.data.NoteEvent? {
        val barW = maxOf(3f, pixelsPerBeat * 0.25f - 1f)
        return notes.lastOrNull { note ->
            val nx = beatToX(note.beat)
            x >= nx - barW && x <= nx + barW
        }
    }

    fun yToVelocity(y: Float, stripHeight: Float): Int {
        val t = 1f - (y / stripHeight).coerceIn(0f, 1f)
        return (t * 127f).toInt().coerceIn(1, 127)
    }

    fun clampScroll(canvasWidth: Float, canvasHeight: Float) {
        val maxX = maxOf(0f, totalBeats * pixelsPerBeat - canvasWidth + 40f)
        val maxY = maxOf(0f, totalNotes * noteHeight - canvasHeight + headerHeight)
        scrollX = scrollX.coerceIn(0f, maxX)
        scrollY = scrollY.coerceIn(0f, maxY)
    }

    fun trackColor(trackIndex: Int): Long = when (trackIndex % 8) {
        0 -> 0xFFE84040L; 1 -> 0xFF3DDC84L; 2 -> 0xFF38BDF8L; 3 -> 0xFFA78BFAL
        4 -> 0xFFFF6B35L; 5 -> 0xFFF5C518L; 6 -> 0xFFF472B6L; else -> 0xFF94A3B8L
    }

    fun isBlackKey(pitch: Int): Boolean {
        val midi = (108 - pitch) % 12
        return midi in listOf(1, 3, 6, 8, 10)
    }

    fun noteName(pitch: Int): String {
        val midi = 108 - pitch
        val octave = midi / 12 - 1
        val names = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
        return "${names[midi % 12]}$octave"
    }
}