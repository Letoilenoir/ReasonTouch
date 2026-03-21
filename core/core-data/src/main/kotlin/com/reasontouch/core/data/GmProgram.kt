package com.reasontouch.core.data

enum class GmProgram(
    val programNumber: Int,
    val displayName: String
) {
    ACOUSTIC_NYLON(25, "Acoustic Nylon"),
    ACOUSTIC_STEEL(26, "Acoustic Steel"),
    ELECTRIC_JAZZ(27, "Electric Jazz"),
    ELECTRIC_CLEAN(28, "Electric Clean"),
    ELECTRIC_MUTED(29, "Electric Muted"),
    OVERDRIVEN(30, "Overdriven")
}
