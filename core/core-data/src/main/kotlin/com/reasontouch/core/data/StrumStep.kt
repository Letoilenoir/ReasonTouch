package com.reasontouch.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "strum_steps",
    foreignKeys = [
        ForeignKey(
            entity = StrumPattern::class,
            parentColumns = ["id"],
            childColumns = ["patternId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patternId")]
)
data class StrumStep(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val patternId: String,
    val stepIndex: Int,             // position in the grid 0..((beats*subdivisions)-1)
    val active: Boolean = false,
    val direction: String = "DOWN", // DOWN | UP
    val accented: Boolean = false   // louder hit
)
