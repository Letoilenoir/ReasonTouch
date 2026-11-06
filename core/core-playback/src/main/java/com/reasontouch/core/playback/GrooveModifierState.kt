package com.reasontouch.core.playback

enum class GridSubdivisionEnum {
    STRAIGHT_16,
    TRIPLET_8T,
    TRIPLET_16T
}

enum class ParadiddleModeEnum {
    OFF,
    ACCENT_VELOCITY,
    OCTAVE_BOUNCE,
    ROOT_FIFTH
}

data class GrooveModifierState(
    val gridSubdivision: GridSubdivisionEnum = GridSubdivisionEnum.STRAIGHT_16,
    val paradiddleMode: ParadiddleModeEnum = ParadiddleModeEnum.OFF
)
