package com.reasontouch.feature.chords.generation.phrase

import com.reasontouch.feature.chords.TheoryChord

data class PhraseOption(
    val title: String,
    val chords: List<TheoryChord>,
    val explanation: String,
    val confidence: Float
)