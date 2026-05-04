package com.reasontouch.feature.drums

import androidx.compose.ui.graphics.Color

/**
 * Drum lane definitions.
 * gmNote: General MIDI drum note number (channel 9)
 * pitch:  Piano roll pitch = 108 - gmNote
 * All drum notes write to MIDI channel 9 (index 10).
 */
data class DrumLane(
    val name:    String,
    val gmNote:  Int,
    val color:   Color,
    val shortName: String
) {
    val pitch: Int get() = 108 - gmNote
}

object DrumKit {
    val lanes = listOf(
        DrumLane("Kick",       36, Color(0xFFE84040), "KK"),
        DrumLane("Snare",      38, Color(0xFF3DDC84), "SN"),
        DrumLane("Clap",       39, Color(0xFFF5C518), "CP"),
        DrumLane("Hi-Hat",     42, Color(0xFF38BDF8), "HH"),
        DrumLane("Open HH",    46, Color(0xFFA78BFA), "OH"),
        DrumLane("Ride",       51, Color(0xFFFF6B35), "RD")
    )

    const val DRUM_MIDI_CHANNEL = 9   // GM standard drum channel
    const val STEPS_DEFAULT     = 16
    const val STEPS_EXTENDED    = 32
}