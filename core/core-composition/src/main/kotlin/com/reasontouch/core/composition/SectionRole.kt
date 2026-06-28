package com.reasontouch.core.composition

enum class SectionRole {
    PRIMARY,      // Verse, Chorus - the main narrative
    CONTRAST,     // Bridge, Solo - provides variation
    TRANSITION,   // Intro, Pre-Chorus - moves between sections
    ENDING;       // Outro - conclusion

    /**
     * Human-readable label for UI display
     */
    fun label(): String = when (this) {
        PRIMARY -> "Primary"
        CONTRAST -> "Contrast"
        TRANSITION -> "Transition"
        ENDING -> "Ending"
    }

    companion object {
        fun fromString(value: String?): SectionRole =
            values().find { it.name.equals(value, ignoreCase = true) } ?: PRIMARY
    }
}

/**
 * Maps SectionType to its default SectionRole.
 * Allows customization in UI (user can override).
 */
fun SectionType.defaultRole(): SectionRole = when (this) {
    SectionType.VERSE -> SectionRole.PRIMARY
    SectionType.CHORUS -> SectionRole.PRIMARY
    SectionType.BRIDGE -> SectionRole.CONTRAST
    SectionType.SOLO -> SectionRole.CONTRAST
    SectionType.PRE_CHORUS -> SectionRole.TRANSITION
    SectionType.INTRO -> SectionRole.TRANSITION
    SectionType.OUTRO -> SectionRole.ENDING
    SectionType.CUSTOM -> SectionRole.PRIMARY
}
