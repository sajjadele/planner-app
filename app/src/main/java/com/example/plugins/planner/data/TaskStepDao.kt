package com.example.plugins.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskStepDao {

    @Insert
    suspend fun insert(step: TaskStepEntity): Long

    @Update
    suspend fun update(step: TaskStepEntity)

    @Query("DELETE FROM task_steps WHERE id = :stepId")
    suspend fun delete(stepId: Int)

    @Query("DELETE FROM task_steps WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Int)

    @Query(
        """
        SELECT * FROM task_steps
        WHERE taskId = :taskId
        ORDER BY createdAt ASC
        """
    )
    fun observeStepsByTaskId(taskId: Int): Flow<List<TaskStepEntity>>

    @Query("SELECT * FROM task_steps WHERE id = :stepId LIMIT 1")
    suspend fun getById(stepId: Int): TaskStepEntity?

    @Query(
        """
        SELECT * FROM task_steps
        WHERE taskId = :taskId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getByTaskId(taskId: Int): List<TaskStepEntity>
}
