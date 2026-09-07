package com.reasontouch.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New Session",
    val bpm: Int = 120,
    val keyRoot: String = "C",
    val keyQuality: String = "MAJOR",
    val timeSignatureNumerator: Int = 4,
    val timeSignatureDenominator: Int = 4,
    val totalBars: Int = 4,
    val defaultStrumSpeed: String = "NATURAL",
    val strumSimulationEnabled: Boolean = true,
    val gmProgram: Int = 26,        // Acoustic Steel default
    val compositionMode: String = "ASSISTED",  // MANUAL, ASSISTED, or GUIDED -- see CompositionMode enum in feature-chords
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
