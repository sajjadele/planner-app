package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getTasksForDay(dateEpochMs: Long): Flow<List<TaskEntity>> = taskDao.getTasksForDay(dateEpochMs)

    fun getTasksBetween(start: Long, end: Long): Flow<List<TaskEntity>> = taskDao.getTasksBetween(start, end)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
}
