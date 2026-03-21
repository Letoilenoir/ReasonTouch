package com.reasontouch.feature.chords

import com.reasontouch.core.midi.StepState

data class StepPattern(val steps: List<StepState>) {
    init { require(steps.size == 16) { "Pattern must have exactly 16 steps" } }
    companion object {
        val EMPTY = StepPattern(List(16) { StepState.OFF })
    }
}

object StrumPatterns {
    private fun p(vararg s: String) = StepPattern(s.map {
        when (it) { "D" -> StepState.DOWN; "U" -> StepState.UP; else -> StepState.OFF }
    })

    val groups: Map<String, Map<String, StepPattern>> = mapOf(
        "Core" to mapOf(
            "All Down"         to p("D","D","D","D","D","D","D","D","D","D","D","D","D","D","D","D"),
            "Down/Up 8ths"     to p("D","_","U","_","D","_","U","_","D","_","U","_","D","_","U","_"),
            "Folk"             to p("D","_","D","U","_","U","D","U","D","_","D","U","_","U","D","U"),
            "Rock D-DU-UDU"    to p("D","_","D","U","_","U","D","U","D","_","D","U","_","U","D","U"),
            "Reggae Skank"     to p("_","_","U","_","_","_","U","_","_","_","U","_","_","_","U","_"),
            "Bossa Nova"       to p("D","_","_","D","_","U","_","D","D","_","_","D","_","U","_","D"),
            "Ballad"           to p("D","_","_","_","D","_","_","_","D","_","_","_","D","_","_","_"),
            "Clear"            to StepPattern.EMPTY
        ),
        "Guitar" to mapOf(
            "Ska Upstroke"     to p("_","U","_","U","_","U","_","U","_","U","_","U","_","U","_","U"),
            "Ska w/ Lead-in"   to p("D","U","_","U","_","U","_","U","_","U","_","U","_","U","_","U"),
            "Funk 16ths"       to p("D","D","_","D","_","D","D","_","D","_","D","D","_","D","_","D"),
            "Funk Chop"        to p("_","_","_","_","D","U","_","_","_","_","_","_","D","U","_","_"),
            "JB Groove"        to p("D","_","_","U","_","_","D","U","D","_","_","U","_","_","D","U"),
            "Country Boom-Chick" to p("D","_","_","_","U","_","_","_","D","_","_","_","U","_","_","_"),
            "Country Walkdown" to p("D","_","_","_","U","_","D","_","D","_","_","_","U","_","D","_"),
            "Soul Ghost"       to p("D","U","_","U","D","U","_","U","D","U","_","U","D","U","_","U"),
            "Philly Soul"      to p("D","_","U","_","_","U","D","U","D","_","U","_","_","U","D","U"),
            "Motown"           to p("D","_","_","U","D","_","_","U","D","_","_","U","D","_","_","U")
        )
    )

    val all: Map<String, StepPattern> = groups.values.fold(mutableMapOf()) { acc, group ->
        acc.also { it.putAll(group) }
    }
}

data class StrumSpeedPreset(
    val label: String,
    val beatsPerString: Double,
    val description: String
)

val STRUM_SPEED_PRESETS = listOf(
    StrumSpeedPreset("Instant",    0.00,  "All strings fire simultaneously"),
    StrumSpeedPreset("Flick",      0.005, "Near-instant snap"),
    StrumSpeedPreset("Fast Strum", 0.01,  "Quick but perceptible roll"),
    StrumSpeedPreset("Natural",    0.02,  "Default acoustic strum"),
    StrumSpeedPreset("Lazy",       0.03,  "Relaxed roll, laid-back feel"),
    StrumSpeedPreset("Slow Roll",  0.05,  "Wide arpeggio-like sweep"),
    StrumSpeedPreset("Harp Sweep", 0.08,  "Very wide roll, classical")
)

data class GmInstrument(val label: String, val program: Int)

val GM_GUITARS = listOf(
    GmInstrument("Acoustic Nylon", 24),
    GmInstrument("Acoustic Steel", 25),
    GmInstrument("Electric Jazz",  26),
    GmInstrument("Electric Clean", 27),
    GmInstrument("Electric Muted", 28),
    GmInstrument("Overdriven",     29),
    GmInstrument("Distortion",     30),
    GmInstrument("Harmonics",      31)
)

data class ProgressionBar(
    val chordName: String,
    val positionName: String,
    val notes: Array<Int?>,
    val pattern: StepPattern,
    val durationBeats: Double,
    val tempoBpm: Int,
    val strumSpeed: Double,
    val instrument: GmInstrument
)
