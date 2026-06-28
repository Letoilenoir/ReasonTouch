package com.reasontouch.core.composition

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SectionMarker entities.
 * 
 * Provides reactive (Flow-based) queries for section markers,
 * allowing ViewModels and UI to observe changes in real-time.
 */
@Dao
interface SectionMarkerDao {

    /**
     * Get all section markers for a given session, ordered by startBar.
     * Returns a Flow for reactive observation.
     */
    @Query("SELECT * FROM section_markers WHERE sessionId = :sessionId ORDER BY startBar ASC")
    fun getMarkersForSession(sessionId: String): Flow<List<SectionMarker>>

    /**
     * Get all section markers for a session as a one-time read (not reactive).
     * Useful for synchronous operations that don't need live updates.
     */
    @Query("SELECT * FROM section_markers WHERE sessionId = :sessionId ORDER BY startBar ASC")
    suspend fun getMarkersForSessionOnce(sessionId: String): List<SectionMarker>

    /**
     * Get a specific marker by ID.
     */
    @Query("SELECT * FROM section_markers WHERE id = :markerId")
    suspend fun getMarkerById(markerId: String): SectionMarker?

    /**
     * Insert a new section marker.
     */
    @Insert
    suspend fun insertMarker(marker: SectionMarker)

    /**
     * Insert multiple markers at once (useful for bulk operations).
     */
    @Insert
    suspend fun insertMarkers(markers: List<SectionMarker>)

    /**
     * Update an existing marker.
     */
    @Update
    suspend fun updateMarker(marker: SectionMarker)

    /**
     * Delete a specific marker.
     */
    @Delete
    suspend fun deleteMarker(marker: SectionMarker)

    /**
     * Delete all markers for a session.
     * Useful when deleting a session.
     */
    @Query("DELETE FROM section_markers WHERE sessionId = :sessionId")
    suspend fun deleteMarkersForSession(sessionId: String)

    /**
     * Check if a session has any markers.
     */
    @Query("SELECT COUNT(*) > 0 FROM section_markers WHERE sessionId = :sessionId")
    suspend fun sessionHasMarkers(sessionId: String): Boolean

    /**
     * Get the number of sections in a session.
     */
    @Query("SELECT COUNT(*) FROM section_markers WHERE sessionId = :sessionId")
    suspend fun countMarkersForSession(sessionId: String): Int
}
