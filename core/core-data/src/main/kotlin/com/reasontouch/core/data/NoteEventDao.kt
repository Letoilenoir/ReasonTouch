package com.reasontouch.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteEventDao {

    @Query("SELECT * FROM note_events WHERE trackId = :trackId ORDER BY beat ASC")
    fun getNotesForTrack(trackId: String): Flow<List<NoteEvent>>

    @Query("SELECT * FROM note_events WHERE trackId = :trackId ORDER BY beat ASC")
    suspend fun getNotesForTrackOnce(trackId: String): List<NoteEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEvent>)

    @Update
    suspend fun updateNote(note: NoteEvent)

    @Delete
    suspend fun deleteNote(note: NoteEvent)

    @Query("DELETE FROM note_events WHERE trackId = :trackId")
    suspend fun deleteNotesForTrack(trackId: String)

    @Query("DELETE FROM note_events WHERE id = :id")
    suspend fun deleteNoteById(id: String)
}
