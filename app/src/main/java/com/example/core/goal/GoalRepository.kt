package com.example.core.goal

import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    fun getActiveGoals(): Flow<List<GoalEntity>> = goalDao.getActiveGoals()

    suspend fun getGoalById(goalId: Int): GoalEntity? = goalDao.getGoalById(goalId)

    fun observeGoalById(goalId: Int): Flow<GoalEntity?> = goalDao.observeGoalById(goalId)

    suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: GoalEntity) = goalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: GoalEntity) = goalDao.deleteGoal(goal)

    suspend fun deleteGoalById(goalId: Int) = goalDao.deleteGoalById(goalId)
}
