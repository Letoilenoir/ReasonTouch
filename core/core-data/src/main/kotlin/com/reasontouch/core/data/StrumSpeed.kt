package com.reasontouch.core.data

enum class StrumSpeed(
    val displayName: String,
    val description: String,
    val offsetMs: Long              // ms between each string note
) {
    INSTANT("Instant",    "All strings simultaneously",      0L),
    FLICK("Flick",        "Near-instant snap",               8L),
    FAST("Fast Strum",    "Quick but perceptible",           20L),
    NATURAL("Natural",    "Default acoustic strum",          40L),
    LAZY("Lazy",          "Relaxed roll, laid back",         70L),
    SLOW_ROLL("Slow Roll","Wide arpeggio-like",             120L),
    HARP_SWEEP("Harp Sweep", "Very wide roll, classical",   200L)
}
