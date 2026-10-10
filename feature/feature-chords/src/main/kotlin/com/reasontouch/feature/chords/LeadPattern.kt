package com.reasontouch.feature.chords

data class LeadPattern(
    val name: String,
    val description: String,
    val targetBeats: List<Float>
)

object LeadPresets {
    val ANTHEM = LeadPattern(
        "Anthem / Ballad",
        "Long, sustained notes on strong beats, emphasizing root and 5th",
        listOf(0.0f, 2.0f)
    )

    val DRIVING = LeadPattern(
        "Driving Scalar",
        "Active 8th-note movement with chord tones on downbeats",
        listOf(0.0f, 1.0f, 2.0f, 3.0f)
    )

    val ARPEGGIO = LeadPattern(
        "Arpeggio Hook",
        "Triad sweeps across beats 1, 2, 3, 4",
        listOf(0.0f, 1.0f, 2.0f, 3.0f)
    )

    val CALL_RESPONSE = LeadPattern(
        "Call & Response",
        "Motif in first half of bar, resting space in second half",
        listOf(0.0f, 1.0f)
    )

    val all = mapOf(
        "Anthem" to ANTHEM,
        "Driving" to DRIVING,
        "Arpeggio" to ARPEGGIO,
        "Call & Response" to CALL_RESPONSE
    )
}
