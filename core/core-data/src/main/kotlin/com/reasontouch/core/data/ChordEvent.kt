package com.reasontouch.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "chord_events",
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
data class ChordEvent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val barIndex: Int,              // which bar in the progression
    val chordName: String,
    val rootMidi: Int,
    val midiNotes: String = "",     // comma-separated MIDI note numbers
    val voicing: String = "OPEN",
    val strumPatternId: String? = null,  // 16-char encoded step string (D/U/. per step) — see StrumEncoding.kt
    val strumSpeedValue: Double? = null, // raw beatsPerString value at time of write
    val phraseId: String? = null,        // UUID shared by every bar generated in one addPhrase() call; null for bars added via addBar()/addSuggestedChord()/addBorrowedChord()
    val phraseLength: Int? = null        // total bar count of the phrase this bar belongs to; null when phraseId is null
)
