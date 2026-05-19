package com.reasontouch.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "midi_tracks",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MidiTrack(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val index: Int,
    val name: String,
    val voice: String = "saw",
    val color: Long = 0xFFE84040,
    val midiChannel: Int = 0,
    val muted: Boolean = false,
    val solo: Boolean = false,
    val volume: Float = 1.0f,
    val gmProgram: Int = 33,
    val drumPack: String = "default"
)
