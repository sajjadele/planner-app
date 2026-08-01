package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val taskEventDao: TaskEventDao
) {
    fun getTasksForDay(dateEpochMs: Long): Flow<List<TaskEntity>> = taskDao.getTasksForDay(dateEpochMs)

    fun getTasksBetween(start: Long, end: Long): Flow<List<TaskEntity>> = taskDao.getTasksBetween(start, end)

    /** Sprint 4 (Phase 4.1): distinct day-key projection for the "days with tasks" indicator. */
    fun getTaskDayKeysBetween(start: Long, end: Long): Flow<List<Long>> =
        taskDao.getTaskDayKeysBetween(start, end)

    fun getTasksByGoalId(goalId: Int): Flow<List<TaskEntity>> = taskDao.getTasksByGoalId(goalId)

    suspend fun getTaskById(taskId: Int): TaskEntity? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun insertTaskEvent(event: TaskEventEntity) = taskEventDao.insertEvent(event)

    suspend fun deleteEventsForGoal(goalId: Int) = taskEventDao.deleteEventsForGoal(goalId)

    suspend fun deleteTasksForGoal(goalId: Int) = taskDao.deleteTasksForGoal(goalId)
}
