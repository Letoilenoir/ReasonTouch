package com.reasontouch.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StrumStepDao {

    @Query("SELECT * FROM strum_steps WHERE patternId = :patternId ORDER BY stepIndex ASC")
    fun getStepsForPattern(patternId: String): Flow<List<StrumStep>>

    @Query("SELECT * FROM strum_steps WHERE patternId = :patternId ORDER BY stepIndex ASC")
    suspend fun getStepsForPatternOnce(patternId: String): List<StrumStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: StrumStep)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<StrumStep>)

    @Update
    suspend fun updateStep(step: StrumStep)

    @Delete
    suspend fun deleteStep(step: StrumStep)

    @Query("DELETE FROM strum_steps WHERE patternId = :patternId")
    suspend fun deleteStepsForPattern(patternId: String)
}
