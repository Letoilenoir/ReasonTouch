package com.reasontouch.core.composition

enum class SectionType {
    INTRO,
    VERSE,
    PRE_CHORUS,
    CHORUS,
    BRIDGE,
    SOLO,
    OUTRO,
    CUSTOM;

    /**
     * Human-readable label for UI display
     */
    fun label(): String = when (this) {
        INTRO -> "Intro"
        VERSE -> "Verse"
        PRE_CHORUS -> "Pre-Chorus"
        CHORUS -> "Chorus"
        BRIDGE -> "Bridge"
        SOLO -> "Solo"
        OUTRO -> "Outro"
        CUSTOM -> "Custom"
    }

    companion object {
        /**
         * Parse string to SectionType (case-insensitive)
         */
        fun fromString(value: String?): SectionType =
            values().find { it.name.equals(value, ignoreCase = true) } ?: VERSE
    }
}
