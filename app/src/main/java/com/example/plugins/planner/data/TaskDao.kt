package com.example.plugins.planner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /** Get recent tasks ordered by timestamp (for search screen) */
    @Query("SELECT * FROM tasks ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTasks(limit: Int = 10): Flow<List<TaskEntity>>

    /** One-shot query for BootReceiver — re-schedule reminders after reboot */
    @Query("SELECT * FROM tasks WHERE reminderHour IS NOT NULL AND reminderMinute IS NOT NULL AND isCompleted = 0")
    suspend fun getActiveReminders(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dateEpochMs = :dateEpochMs ORDER BY id DESC")
    fun getTasksForDay(dateEpochMs: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dateEpochMs BETWEEN :start AND :end ORDER BY id DESC")
    fun getTasksBetween(start: Long, end: Long): Flow<List<TaskEntity>>

    /**
     * Sprint 4 (Phase 4.1): projection variant of [getTasksBetween] for the "days with tasks"
     * indicator. Returns only distinct scheduled-day keys (no full [TaskEntity] materialization),
     * so the ±60-day scan is a tiny cursor instead of thousands of entity rows.
     */
    @Query("SELECT DISTINCT dateEpochMs FROM tasks WHERE dateEpochMs BETWEEN :start AND :end")
    fun getTaskDayKeysBetween(start: Long, end: Long): Flow<List<Long>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: Int): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE goalId = :goalId ORDER BY id DESC")
    fun getTasksByGoalId(goalId: Int): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE goalId = :goalId")
    suspend fun deleteTasksForGoal(goalId: Int)
}
