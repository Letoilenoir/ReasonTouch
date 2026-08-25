package com.reasontouch.feature.chords

import com.reasontouch.core.midi.StepState

/**
 * Encodes/decodes StepPattern <-> the 16-char string stored in
 * ChordEvent.strumPatternId (see Roadmap: Strum Persistence & Continuation
 * Inheritance, Section 2, Decision #2).
 *
 * Encoding: one char per step, in order — "D" (DOWN), "U" (UP), "." (OFF).
 * Always exactly 16 characters for a valid pattern.
 */

fun StepPattern.toChordEventString(): String =
    steps.joinToString(separator = "") { step ->
        when (step) {
            StepState.DOWN -> "D"
            StepState.UP -> "U"
            StepState.OFF -> "."
        }
    }

/**
 * Tolerant decode: null, wrong length, or any unrecognized character
 * falls back to StepPattern.EMPTY rather than throwing. This is intentional —
 * a malformed/legacy value here must never crash generation or playback.
 */
fun String?.toStepPattern(): StepPattern {
    if (this == null || this.length != 16) return StepPattern.EMPTY

    val steps = this.map { char ->
        when (char) {
            'D' -> StepState.DOWN
            'U' -> StepState.UP
            '.' -> StepState.OFF
            else -> return StepPattern.EMPTY
        }
    }

    return StepPattern(steps)
}