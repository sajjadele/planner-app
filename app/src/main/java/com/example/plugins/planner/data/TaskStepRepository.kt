package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

class TaskStepRepository(private val dao: TaskStepDao) {

    fun observeSteps(taskId: Int): Flow<List<TaskStepEntity>> =
        dao.observeStepsByTaskId(taskId)

    suspend fun addStep(step: TaskStepEntity): Int = dao.insert(step).toInt()

    suspend fun updateStep(step: TaskStepEntity) =
        dao.update(step)

    suspend fun deleteStep(stepId: Int) =
        dao.delete(stepId)

    suspend fun getStepById(stepId: Int): TaskStepEntity? =
        dao.getById(stepId)

    suspend fun getStepsByTaskId(taskId: Int): List<TaskStepEntity> =
        dao.getByTaskId(taskId)
}
