package com.reasontouch.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "strum_patterns")
data class StrumPattern(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val beats: Int = 4,
    val subdivisions: Int = 4,      // steps per beat — 4 = 16th note grid
    val strumSpeed: String = "NATURAL",
    val isPreset: Boolean = false    // true = built-in preset, false = user saved
)
