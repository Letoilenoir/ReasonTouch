package com.reasontouch.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChordEventDao {

    @Query("SELECT * FROM chord_events WHERE sessionId = :sessionId ORDER BY barIndex ASC")
    fun getChordsForSession(sessionId: String): Flow<List<ChordEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChord(chord: ChordEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChords(chords: List<ChordEvent>)

    @Update
    suspend fun updateChord(chord: ChordEvent)

    @Delete
    suspend fun deleteChord(chord: ChordEvent)

    @Query("DELETE FROM chord_events WHERE sessionId = :sessionId")
    suspend fun deleteChordsForSession(sessionId: String)
}
