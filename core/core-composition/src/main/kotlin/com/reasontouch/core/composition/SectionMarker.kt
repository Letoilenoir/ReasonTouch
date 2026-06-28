package com.reasontouch.core.composition

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * SectionMarker: A lightweight annotation that marks where sections begin in a progression.
 * 
 * This is NOT a new musical abstraction—it's metadata that enriches the existing
 * List<ChordEvent> progression. Multiple markers can annotate the same progression,
 * allowing the application to understand song structure without duplicating chord data.
 * 
 * Example:
 * startBar=0,  type=VERSE      → Bars 0-7 are the verse
 * startBar=8,  type=CHORUS     → Bars 8-15 are the chorus
 * startBar=16, type=VERSE      → Bars 16-23 are the verse (repeated)
 * startBar=24, type=BRIDGE     → Bars 24-31 are the bridge
 * startBar=32, type=CHORUS     → Bars 32-39 are the chorus (repeated)
 * startBar=40, type=OUTRO      → Bars 40-47 are the outro
 * 
 * The underlying progression remains: Bar 0, Bar 1, ... Bar 47 (48 ChordEvents).
 * Markers simply tell us how to interpret that linear sequence as song structure.
 * 
 * Stage 1 fields (current):
 * - id: unique identifier
 * - sessionId: which session this marker belongs to
 * - startBar: which bar index starts this section
 * - type: SectionType (VERSE, CHORUS, etc.)
 * - customLabel: for SectionType.CUSTOM
 * 
 * Stage 2 fields (coming later):
 * - enteredVia: TransitionType (how we enter this section from the previous one)
 * 
 * Stage 3 fields (coming later):
 * - length: explicit bar count for this section (for UI visualization)
 */
@Entity(tableName = "section_markers")
data class SectionMarker(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val startBar: Int,
    val type: String,
    val intent: String? = null,  // ← NEW: CompositionIntent.name()
    val customLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Convenience function: get the SectionRole for this marker's type.
     * This allows downstream systems (Bass, Drums, etc.) to query musical role
     * without needing the enum deserialization logic everywhere.
     */
    fun role(): SectionRole {
        val sectionType = SectionType.fromString(type)
        return sectionType.defaultRole()
    }

    /**
     * Human-readable label combining type and custom label if present.
     */
    fun displayLabel(): String {
        val typeName = SectionType.fromString(type).label()
        return if (type == "CUSTOM" && !customLabel.isNullOrBlank()) {
            "$typeName: $customLabel"
        } else {
            typeName
        }
    }

    /**
     * Companion factory for easier creation from SectionType enum.
     */
    companion object {
        fun create(
            sessionId: String,
            startBar: Int,
            type: SectionType,
            customLabel: String? = null
        ): SectionMarker = SectionMarker(
            sessionId = sessionId,
            startBar = startBar,
            type = type.name,
            customLabel = customLabel
        )
    }
}
