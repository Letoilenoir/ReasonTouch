package com.reasontouch.feature.chords.pairing

data class ProgressionPair(
    val title: String,
    val description: String,
    val type: PairingType,
    val degrees: List<Int>
)
