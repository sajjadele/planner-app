package com.example.core.goal

import kotlinx.coroutines.flow.Flow

/**
 * Room-backed implementation of [GoalRepository]. Thin delegation over [GoalDao] and
 * [GoalEventDao]; all SQL lives in the DAOs, this class owns only the boundary.
 */
class RoomGoalRepository(
    private val goalDao: GoalDao,
    private val goalEventDao: GoalEventDao
) : GoalRepository {
    override fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()
    override fun getActiveGoals(): Flow<List<GoalEntity>> = goalDao.getActiveGoals()
    override suspend fun getGoalById(goalId: Int): GoalEntity? = goalDao.getGoalById(goalId)
    override fun observeGoalById(goalId: Int): Flow<GoalEntity?> = goalDao.observeGoalById(goalId)
    override suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insertGoal(goal)
    override suspend fun updateGoal(goal: GoalEntity) = goalDao.updateGoal(goal)
    override suspend fun deleteGoal(goal: GoalEntity) = goalDao.deleteGoal(goal)
    override suspend fun deleteGoalById(goalId: Int) = goalDao.deleteGoalById(goalId)

    override suspend fun insertGoalEvent(event: GoalEventEntity) = goalEventDao.insertEvent(event)
    override fun observeGoalEvents(): Flow<List<GoalEventEntity>> = goalEventDao.observeAllEvents()
    override fun observeGoalEvents(goalId: Int): Flow<List<GoalEventEntity>> =
        goalEventDao.observeEventsByGoal(goalId)
}
