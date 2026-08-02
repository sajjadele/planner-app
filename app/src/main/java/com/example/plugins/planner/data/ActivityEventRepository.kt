package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

class ActivityEventRepository(private val dao: ActivityEventDao) {

    fun observeActivities(taskId: Int): Flow<List<ActivityEventEntity>> =
        dao.observeByTaskId(taskId)

    /** Windowed reactive feed anchor — bounded to [limit] rows (P0 windowed loading). */
    fun observeFeedWindow(
        taskId: Int,
        dayStart: Long?,
        dayEnd: Long?,
        stepId: Int?,
        limit: Int
    ): Flow<List<ActivityEventEntity>> =
        dao.observeFeedWindow(taskId, dayStart, dayEnd, stepId, limit)

    /** One-shot cursor-based older-page fetch for scroll-back pagination (P0). */
    suspend fun getFeedPageBefore(
        taskId: Int,
        dayStart: Long?,
        dayEnd: Long?,
        stepId: Int?,
        beforeTs: Long,
        beforeId: Int,
        limit: Int
    ): List<ActivityEventEntity> =
        dao.getFeedPageBefore(taskId, dayStart, dayEnd, stepId, beforeTs, beforeId, limit)

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
