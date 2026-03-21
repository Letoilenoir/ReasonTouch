package com.reasontouch.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StrumPatternDao {

    @Query("SELECT * FROM strum_patterns ORDER BY isPreset DESC, name ASC")
    fun getAllPatterns(): Flow<List<StrumPattern>>

    @Query("SELECT * FROM strum_patterns WHERE isPreset = 1 ORDER BY name ASC")
    fun getPresetPatterns(): Flow<List<StrumPattern>>

    @Query("SELECT * FROM strum_patterns WHERE isPreset = 0 ORDER BY name ASC")
    fun getUserPatterns(): Flow<List<StrumPattern>>

    @Query("SELECT * FROM strum_patterns WHERE id = :id")
    suspend fun getPatternById(id: String): StrumPattern?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: StrumPattern)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatterns(patterns: List<StrumPattern>)

    @Update
    suspend fun updatePattern(pattern: StrumPattern)

    @Delete
    suspend fun deletePattern(pattern: StrumPattern)
}
