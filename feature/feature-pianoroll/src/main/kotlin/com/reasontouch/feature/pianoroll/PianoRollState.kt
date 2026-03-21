package com.reasontouch.feature.pianoroll

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Mutable UI state for the piano roll canvas.
 * Holds scroll, zoom and drag state separately from the ViewModel
 * so canvas redraws don't trigger full recomposition.
 */
class PianoRollState {
    // Scroll
    var scrollX by mutableStateOf(0f)
    var scrollY by mutableStateOf(0f)

    // Zoom
    var pixelsPerBeat by mutableStateOf(80f)

    // Canvas dimensions
    var gridWidth by mutableStateOf(0f)
    var gridHeight by mutableStateOf(0f)

    // Constants
    val noteHeight = 22f
    val keyWidth = 64f
    val headerHeight = 24f
    val totalNotes = 88
    val beatsPerBar = 4
    val totalBars = 4
    val totalBeats get() = totalBars * beatsPerBar

    // Coordinate helpers
    fun beatToX(beat: Float) = beat * pixelsPerBeat - scrollX
    fun xToBeat(x: Float) = (x + scrollX) / pixelsPerBeat
    fun pitchToY(pitch: Int) = pitch * noteHeight - scrollY + headerHeight
    fun yToPitch(y: Float) = ((y + scrollY - headerHeight) / noteHeight).toInt()

    fun snapBeat(beat: Float, snapValue: Float): Float {
        return (Math.round(beat / snapValue) * snapValue)
    }

    fun clampScroll(canvasWidth: Float, canvasHeight: Float) {
        val maxX = maxOf(0f, totalBeats * pixelsPerBeat - canvasWidth + 40f)
        val maxY = maxOf(0f, totalNotes * noteHeight - canvasHeight + headerHeight)
        scrollX = scrollX.coerceIn(0f, maxX)
        scrollY = scrollY.coerceIn(0f, maxY)
    }

    // Note colour by track index
    fun trackColor(trackIndex: Int): Long = when (trackIndex % 8) {
        0 -> 0xFFE84040L
        1 -> 0xFF3DDC84L
        2 -> 0xFF38BDF8L
        3 -> 0xFFA78BFAL
        4 -> 0xFFFF6B35L
        5 -> 0xFFF5C518L
        6 -> 0xFFF472B6L
        else -> 0xFF94A3B8L
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