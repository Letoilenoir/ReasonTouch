package com.reasontouch.feature.chords

/**
 * Bass generation styles.
 * Each style produces a different rhythmic and melodic pattern
 * from the chord root and scale context.
 */
enum class BassStyle(val label: String, val description: String) {
    ROOT(
        "Root",
        "One note per bar on the root"
    ),
    ROOT_FIFTH(
        "Root + 5th",
        "Root on beat 1, fifth on beat 3"
    ),
    OCTAVE(
        "Octave",
        "Root with octave jump on beat 3"
    ),
    WALKING(
        "Walking Bass",
        "Chromatic approach notes between roots"
    ),
    ARPEGGIO(
        "Arpeggio",
        "Chord tones in sequence"
    ),
    GROOVE(
        "Groove",
        "Syncopated root pattern"
    ),
    PEDAL(
        "Pedal",
        "Sustained root throughout"
    )
}