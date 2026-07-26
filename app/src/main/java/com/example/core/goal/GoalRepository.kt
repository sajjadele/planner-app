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
    fun observeGoalsByStatus(status: String): Flow<List<GoalEntity>>
    suspend fun getGoalById(goalId: Int): GoalEntity?
    fun observeGoalById(goalId: Int): Flow<GoalEntity?>
    suspend fun insertGoal(goal: GoalEntity): Long
    suspend fun updateGoal(goal: GoalEntity)
    suspend fun deleteGoal(goal: GoalEntity)
    suspend fun deleteGoalById(goalId: Int)

    // ── Goal lifecycle status management (Phase 5.1) ──
    /**
     * Change a goal's status, validating the transition and logging a matching lifecycle event.
     * Transition rules live in [com.example.core.goal.GoalStatus].
     */
    suspend fun updateGoalStatus(goalId: Int, status: String)

    /**
     * Archive a goal. Archive is a terminal status; delegates to [updateGoalStatus] with
     * [com.example.core.goal.GoalStatus.ARCHIVED] (only valid from completed/abandoned).
     */
    suspend fun archiveGoal(goalId: Int)

    // ── Goal activity foundation (Phase 5.1) — derived, never stored ──
    fun observeGoalLastActivity(goalId: Int): Flow<Long?>
    fun observeGoalActiveDayCount(goalId: Int): Flow<Int>

    /**
     * Rolling-window distinct active-day count for a goal, used by Goal Progress momentum (Phase 5.2).
     * Derived from `tasks.dateEpochMs`; never stored.
     */
    fun observeGoalActiveDayCountInWindow(goalId: Int, fromEpochMs: Long, toEpochMs: Long): Flow<Int>

    /**
     * Bulk activity aggregates (last activity + active days) for every goal of [status], in a
     * single query. Phase 5.4 — replaces per-goal N+1 in the dashboard sort. See ADR-0009.
     */
    fun observeGoalActivityBulk(status: String): Flow<List<com.example.core.goal.GoalActivityBulk>>

    /**
     * Windowed bulk active-day counts (rolling momentum) for every goal of [status]. Phase 5.4.
     */
    fun observeGoalActivityBulkInWindow(
        status: String,
        fromEpochMs: Long,
        toEpochMs: Long
    ): Flow<List<com.example.core.goal.GoalActivityBulkInWindow>>

    /**
     * Bulk task completion rates for every goal of [status]. Phase 5.4 — replaces per-goal
     * progress recompute in the dashboard. See ADR-0009.
     */
    fun observeGoalCompletionRatesByStatus(status: String): Flow<List<com.example.plugins.planner.data.GoalRateResult>>

    // ── Goal lifecycle events (Phase 2) ──
    suspend fun insertGoalEvent(event: GoalEventEntity)
    fun observeGoalEvents(): Flow<List<GoalEventEntity>>
    fun observeGoalEvents(goalId: Int): Flow<List<GoalEventEntity>>
}
