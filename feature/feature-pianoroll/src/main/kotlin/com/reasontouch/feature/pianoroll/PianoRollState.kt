package com.reasontouch.feature.pianoroll

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PianoRollState {
    var scrollX by mutableStateOf(0f)
    var scrollY by mutableStateOf(0f)
    var pixelsPerBeat by mutableStateOf(80f)
    var gridWidth by mutableStateOf(0f)
    var gridHeight by mutableStateOf(0f)

    val noteHeight = 22f
    val keyWidth = 64f
    val headerHeight = 24f
    val totalNotes = 88
    val beatsPerBar = 4

    // Driven by session.totalBars — updated by PianoRollCanvas on each recompose
    var totalBars by mutableStateOf(4)
    val totalBeats get() = totalBars * beatsPerBar

    fun beatToX(beat: Float) = beat * pixelsPerBeat - scrollX
    fun xToBeat(x: Float) = (x + scrollX) / pixelsPerBeat
    fun pitchToY(pitch: Int) = pitch * noteHeight - scrollY + headerHeight
    fun yToPitch(y: Float) = ((y + scrollY - headerHeight) / noteHeight).toInt()

    fun snapBeat(beat: Float, snapValue: Float): Float =
        (Math.round(beat / snapValue) * snapValue)

    // Loop drag state
    var loopDragTarget: String? = null
    var loopDragStartX: Float = 0f
    var loopDragOrigStart: Float = 0f
    var loopDragOrigEnd: Float = 0f

    // Handle hit works across the FULL canvas height, not just the header strip.
    // The header-only restriction was causing all hits to return null from the grid area.
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