package com.reasontouch.core.playback

data class GridSubdivision(
    val stepsPerBeat: Int,
    val totalBeats: Int = 16 // 4 bars * 4 beats
) {
    val stepDuration: Float
        get() = 1.0f / stepsPerBeat

    val totalSteps: Int
        get() = totalBeats * stepsPerBeat

    companion object {
        val STRAIGHT_16TH = GridSubdivision(stepsPerBeat = 4, totalBeats = 16)
        val TRIPLET_16TH = GridSubdivision(stepsPerBeat = 6, totalBeats = 16)
        val TRIPLET_8TH = GridSubdivision(stepsPerBeat = 3, totalBeats = 16)
    }
}
