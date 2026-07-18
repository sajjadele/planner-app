package com.example.core.goal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.plugins.planner.data.GoalRateResult
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = 'active' ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = :status ORDER BY createdAt DESC")
    fun getGoalsByStatus(status: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Int): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun observeGoalById(goalId: Int): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Int)

    @Query("SELECT COUNT(*) FROM goals WHERE status = 'active'")
    fun observeActiveGoalCount(): Flow<Int>

    /**
     * Latest scheduled activity day for a goal's tasks, derived from `tasks.dateEpochMs`.
     *
     * NOTE: this reflects *scheduled* activity days, not necessarily executed actions. A future
     * refinement may migrate the signal to `task_events`. Kept derived (never stored) so the
     * single source of truth remains `tasks`/`task_events`.
     */
    @Query("SELECT MAX(t.dateEpochMs) FROM tasks t WHERE t.goalId = :goalId")
    fun observeGoalLastActivity(goalId: Int): Flow<Long?>

    /**
     * Count of distinct scheduled days on which the goal has tasks, derived from `tasks.dateEpochMs`.
     *
     * NOTE: "active day" here means a day with at least one task scheduled for the goal. See the
     * note on [observeGoalLastActivity] — may later migrate to `task_events`. Derived, not stored.
     */
    @Query("""
        SELECT COUNT(DISTINCT CAST(t.dateEpochMs / 86400000 AS INTEGER))
        FROM tasks t WHERE t.goalId = :goalId
    """)
    fun observeGoalActiveDayCount(goalId: Int): Flow<Int>

    /**
     * Count of distinct scheduled activity days for a goal within a rolling window
     * [fromEpochMs, toEpochMs]. Used by Goal Progress momentum (Phase 5.2) so a goal's momentum
     * reflects *recent* behavior, not a burst of activity long ago.
     *
     * Derived from `tasks.dateEpochMs` (see the note on [observeGoalLastActivity]); not stored.
     */
    @Query("""
        SELECT COUNT(DISTINCT CAST(t.dateEpochMs / 86400000 AS INTEGER))
        FROM tasks t WHERE t.goalId = :goalId AND t.dateEpochMs BETWEEN :fromEpochMs AND :toEpochMs
    """)
    fun observeGoalActiveDayCountInWindow(
        goalId: Int,
        fromEpochMs: Long,
        toEpochMs: Long
    ): Flow<Int>

    /**
     * Bulk activity aggregates for ALL goals of a given status, in a single `GROUP BY` pass.
     *
     * Replaces the per-goal N+1 loop previously run inside the dashboard's `enrichAndSort`
     * (each goal issued its own `observeGoalLastActivity` / `observeGoalActiveDayCount` query).
     * Returns one row per goal with its `lastActivity` (MAX scheduled day) and `activeDays`
     * (count of distinct scheduled days). Derived from `tasks.dateEpochMs`; never stored.
     *
     * Phase 5.4 performance optimization — see ADR-0009.
     */
    @Query("""
        SELECT t.goalId AS goalId,
               MAX(t.dateEpochMs) AS lastActivity,
               COUNT(DISTINCT CAST(t.dateEpochMs / 86400000 AS INTEGER)) AS activeDays
        FROM tasks t
        INNER JOIN goals g ON g.id = t.goalId
        WHERE g.status = :status
        GROUP BY t.goalId
    """)
    fun observeGoalActivityBulk(status: String): Flow<List<GoalActivityBulk>>

    /**
     * Windowed variant of [observeGoalActivityBulk]: distinct scheduled activity days within
     * [fromEpochMs, toEpochMs], used for the rolling progress momentum. Single `GROUP BY` pass
     * over all goals of [status]. Phase 5.4 — see ADR-0009.
     */
    @Query("""
        SELECT t.goalId AS goalId,
               COUNT(DISTINCT CAST(t.dateEpochMs / 86400000 AS INTEGER)) AS activeDaysInWindow
        FROM tasks t
        INNER JOIN goals g ON g.id = t.goalId
        WHERE g.status = :status AND t.dateEpochMs BETWEEN :fromEpochMs AND :toEpochMs
        GROUP BY t.goalId
    """)
    fun observeGoalActivityBulkInWindow(
        status: String,
        fromEpochMs: Long,
        toEpochMs: Long
    ): Flow<List<GoalActivityBulkInWindow>>

    /**
     * Bulk task completion rates for ALL goals of a given status, in a single `GROUP BY` pass.
     * Mirrors [com.example.plugins.planner.data.InsightDao.observeGoalCompletionRates] but
     * parameterized by status (that query is hardcoded to `active`). Phase 5.4 — see ADR-0009.
     */
    @Query("""
        SELECT g.id as goalId,
               g.title as goalTitle,
               COUNT(t.id) as totalTasks,
               SUM(CASE WHEN t.isCompleted = 1 THEN 1 ELSE 0 END) as completedTasks,
               CASE
                   WHEN COUNT(t.id) > 0
                   THEN CAST(SUM(CASE WHEN t.isCompleted = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(t.id) * 100
                   ELSE 0
               END as completionRate
        FROM goals g
        LEFT JOIN tasks t ON t.goalId = g.id
        WHERE g.status = :status
        GROUP BY g.id, g.title
    """)
    fun observeGoalCompletionRatesByStatus(status: String): Flow<List<GoalRateResult>>
}

/** Per-goal bulk activity aggregate returned by [observeGoalActivityBulk]. */
data class GoalActivityBulk(
    val goalId: Int,
    val lastActivity: Long?,
    val activeDays: Int
)

/** Per-goal windowed active-day aggregate returned by [observeGoalActivityBulkInWindow]. */
data class GoalActivityBulkInWindow(
    val goalId: Int,
    val activeDaysInWindow: Int
)
