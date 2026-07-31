package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

class ActivityEventRepository(private val dao: ActivityEventDao) {

    fun observeActivities(taskId: Int): Flow<List<ActivityEventEntity>> =
        dao.observeByTaskId(taskId)

    fun observeById(id: Long): Flow<ActivityEventEntity?> =
        dao.observeById(id)

    suspend fun getEventById(id: Long): ActivityEventEntity? =
        dao.getById(id)

    suspend fun addEvent(event: ActivityEventEntity) =
        dao.insert(event)

    suspend fun updateEvent(event: ActivityEventEntity) =
        dao.update(event)

    suspend fun deleteEvent(id: Long) =
        dao.deleteById(id)

    suspend fun findLatestEvent(taskId: Int, eventType: String): ActivityEventEntity? =
        dao.findLatestEvent(taskId, eventType)

    suspend fun getEventsByStepId(stepId: Int): List<ActivityEventEntity> =
        dao.getEventsByStepId(stepId)

    /**
     * Nullify stepId for all activity events referencing a given step.
     * Called before deleting a tag so activities are preserved but unlinked.
     */
    suspend fun clearStepId(stepId: Int) =
        dao.clearStepId(stepId)
}
