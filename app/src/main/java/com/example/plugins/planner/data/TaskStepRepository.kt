package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

class TaskStepRepository(
    private val dao: TaskStepDao,
    private val activityEventDao: ActivityEventDao
) {

    fun observeSteps(taskId: Int): Flow<List<TaskStepEntity>> =
        dao.observeStepsByTaskId(taskId)

    suspend fun addStep(step: TaskStepEntity): Int {
        val id = dao.insert(step).toInt()
        check(id > 0) { "Failed to generate step ID for task ${step.taskId}" }
        activityEventDao.insert(
            ActivityEventEntity(
                taskId = step.taskId,
                stepId = id,
                eventType = ActivityEventType.STEP_CREATED.name,
                description = step.title
            )
        )
        return id
    }

    suspend fun updateStep(step: TaskStepEntity) =
        dao.update(step)

    suspend fun deleteStep(stepId: Int) =
        dao.delete(stepId)

    suspend fun getStepById(stepId: Int): TaskStepEntity? =
        dao.getById(stepId)

    suspend fun getStepsByTaskId(taskId: Int): List<TaskStepEntity> =
        dao.getByTaskId(taskId)
}
