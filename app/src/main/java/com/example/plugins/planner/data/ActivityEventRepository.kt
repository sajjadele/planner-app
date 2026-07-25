package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

class ActivityEventRepository(private val dao: ActivityEventDao) {

    fun observeActivities(taskId: Int): Flow<List<ActivityEventEntity>> =
        dao.observeByTaskId(taskId)

    suspend fun addEvent(event: ActivityEventEntity) =
        dao.insert(event)

    suspend fun findLatestEvent(taskId: Int, eventType: String): ActivityEventEntity? =
        dao.findLatestEvent(taskId, eventType)

    suspend fun getEventsByStepId(stepId: Int): List<ActivityEventEntity> =
        dao.getEventsByStepId(stepId)
}
