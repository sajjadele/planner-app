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
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /** One-shot query for BootReceiver — re-schedule reminders after reboot */
    @Query("SELECT * FROM tasks WHERE reminderHour IS NOT NULL AND reminderMinute IS NOT NULL AND isCompleted = 0")
    suspend fun getActiveReminders(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dateEpochMs = :dateEpochMs ORDER BY isCompleted ASC, id DESC")
    fun getTasksForDay(dateEpochMs: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dateEpochMs BETWEEN :start AND :end ORDER BY isCompleted ASC, id DESC")
    fun getTasksBetween(start: Long, end: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: Int): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE goalId = :goalId ORDER BY isCompleted ASC, id DESC")
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
