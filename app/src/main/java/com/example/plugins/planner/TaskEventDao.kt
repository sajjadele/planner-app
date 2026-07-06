package com.example.plugins.planner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskEventDao {
    @Query("SELECT * FROM task_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<TaskEventEntity>>

    @Insert
    suspend fun insertEvent(event: TaskEventEntity)
}
