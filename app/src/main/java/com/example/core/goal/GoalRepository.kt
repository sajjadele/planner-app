package com.example.core.goal

import kotlinx.coroutines.flow.Flow

/**
 * Repository boundary for Goal reads/writes and goal-lifecycle event logging.
 *
 * Extracted as an interface (Phase 2) so the concrete [RoomGoalRepository] can be swapped
 * or mocked in tests. UI/ViewModel code depends on this interface, never on the DAO directly.
 */
interface GoalRepository {
    fun getAllGoals(): Flow<List<GoalEntity>>
    fun getActiveGoals(): Flow<List<GoalEntity>>
    suspend fun getGoalById(goalId: Int): GoalEntity?
    fun observeGoalById(goalId: Int): Flow<GoalEntity?>
    suspend fun insertGoal(goal: GoalEntity): Long
    suspend fun updateGoal(goal: GoalEntity)
    suspend fun deleteGoal(goal: GoalEntity)
    suspend fun deleteGoalById(goalId: Int)

    // ── Goal lifecycle events (Phase 2) ──
    suspend fun insertGoalEvent(event: GoalEventEntity)
    fun observeGoalEvents(): Flow<List<GoalEventEntity>>
    fun observeGoalEvents(goalId: Int): Flow<List<GoalEventEntity>>
}
