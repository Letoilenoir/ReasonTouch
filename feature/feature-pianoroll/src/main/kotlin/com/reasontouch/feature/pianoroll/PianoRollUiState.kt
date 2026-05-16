package com.reasontouch.feature.pianoroll

import com.reasontouch.core.data.MidiTrack
import com.reasontouch.core.data.NoteEvent

data class PianoRollUiState(
    val tracks:       List<MidiTrack>              = emptyList(),
    val activeIndex:  Int                          = 0,
    val activeNotes:  List<NoteEvent>              = emptyList(),
    val allNotes:     Map<String, List<NoteEvent>> = emptyMap(),
    val currentTool:  PianoRollViewModel.Tool      = PianoRollViewModel.Tool.DRAW,
    val snapIndex:    Int                          = 2,
    val selectedIds:  Set<String>                  = emptySet(),
    val loopEnabled:  Boolean                      = false,
    val loopStart:    Float                        = 0f,
    val loopEnd:      Float                        = 4f,
    val playheadBeat: Float                        = 0f,
    val isPlaying:    Boolean                      = false,
    val totalBars:    Int                          = 4,
    val hasClipboard: Boolean = false,
    val drawDuration: Float = 0.25f
) {
    val snapValues   = listOf(1f, 0.5f, 0.25f, 0.125f, 0.0625f)
    val snapLabels   = listOf("1/4","1/8","1/16","1/32","1/64")
    val snapValue    get() = snapValues[snapIndex]
    val activeTrack  get() = tracks.getOrNull(activeIndex)
    val hasSelection get() = selectedIds.isNotEmpty()
}
