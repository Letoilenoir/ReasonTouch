package com.reasontouch.core.playback

data class InspectorState(
    val trackId: String = "",
    private val _transposeSemitones: Int = 0,
    val grooveState: GrooveModifierState = GrooveModifierState()
) {
    val transposeSemitones: Int = _transposeSemitones.coerceIn(-12, 12)

    val isPitched: Boolean
        get() = trackId.uppercase() != "DRUMS" && !trackId.uppercase().contains("DRUM")
}
