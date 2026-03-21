package com.reasontouch.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MidiTrackDao {

    @Query("SELECT * FROM midi_tracks WHERE sessionId = :sessionId ORDER BY `index` ASC")
    fun getTracksForSession(sessionId: String): Flow<List<MidiTrack>>

    @Query("SELECT * FROM midi_tracks WHERE id = :id")
    suspend fun getTrackById(id: String): MidiTrack?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: MidiTrack)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<MidiTrack>)

    @Update
    suspend fun updateTrack(track: MidiTrack)

    @Delete
    suspend fun deleteTrack(track: MidiTrack)

    @Query("DELETE FROM midi_tracks WHERE sessionId = :sessionId")
    suspend fun deleteTracksForSession(sessionId: String)
}
