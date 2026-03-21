package com.reasontouch.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "note_events",
    foreignKeys = [
        ForeignKey(
            entity = MidiTrack::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class NoteEvent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val pitch: Int,
    val beat: Float,
    val duration: Float,
    val velocity: Int = 100
)
