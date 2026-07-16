package com.example.plugins.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Focused on raw event persistence only.
 * Analytical/aggregation queries live in InsightDao.
 */
@Dao
interface TaskEventDao {
    @Query("SELECT * FROM task_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<TaskEventEntity>>

    @Insert
    suspend fun insertEvent(event: TaskEventEntity)

    @Query("DELETE FROM task_events WHERE taskId IN (SELECT id FROM tasks WHERE goalId = :goalId)")
    suspend fun deleteEventsForGoal(goalId: Int)
}
