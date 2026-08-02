package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

class TaskStepRepository(
    private val dao: TaskStepDao,
    private val activityEventDao: ActivityEventDao
) {

    /** Observe all tags for a task. */
    fun observeSteps(taskId: Int): Flow<List<TaskStepEntity>> =
        dao.observeStepsByTaskId(taskId)

    /**
     * Add a tag. Tags are metadata — no activity event is created.
     * Activities reference tags via stepId but tag creation is not a user action.
     */
    suspend fun addStep(step: TaskStepEntity): Int {
        val id = dao.insert(step).toInt()
        check(id > 0) { "Failed to generate tag ID for task ${step.taskId}" }
        return id
    }

    /** Update a tag in-place (title and/or color). */
    suspend fun updateStep(step: TaskStepEntity) = dao.update(step)

    /** Delete a tag. Activities referencing this tag are unlinked (stepId → null). */
    suspend fun deleteStep(stepId: Int) = dao.delete(stepId)

    suspend fun getStepById(stepId: Int): TaskStepEntity? = dao.getById(stepId)

    /** Get all tags for a task. */
    suspend fun getStepsByTaskId(taskId: Int): List<TaskStepEntity> = dao.getByTaskId(taskId)
}
