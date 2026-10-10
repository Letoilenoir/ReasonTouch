package com.reasontouch.feature.drums

enum class FillDuration {
    QUARTER_BAR,   // 1 beat
    HALF_BAR,      // 2 beats
    FULL_BAR       // 4 beats
}

enum class WritingDuration(val label: String, val barCount: Int?) {
    ONE_BAR("1 Bar", 1),
    TWO_BARS("2 Bars", 2),
    FOUR_BARS("4 Bars", 4),
    PHRASE_LENGTH("Phrase", null),
    SESSION_LENGTH("Session", null)
}

data class DrumFillPattern(
    val name: String,
    val description: String,
    val defaultDuration: FillDuration,
    val beats: List<Pair<Int, Float>> // Pair(laneIndex, beatOffset)
)
