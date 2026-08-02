package com.example.plugins.planner.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Dedicated DAO for analytical and aggregation queries.
 *
 * Separated from TaskEventDao to maintain single-responsibility:
 * - TaskEventDao → raw event persistence (create, complete, delete, etc.)
 * - InsightDao   → computed insights (weekly trends, life area breakdown, etc.)
 *
 * All queries here are reactive Flows that re-emit when underlying data changes.
 *
 * All BETWEEN filters use dateEpochMs (the task's scheduled day) rather than
 * timestamp (creation moment). This correctly assigns a task to the week it
 * was planned for, not the week it was created in.
 */
@Dao
interface InsightDao {
    // ──────────────────────────────────────────────
    // Weekly completion metrics
    // ──────────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE isCompleted = 1 AND dateEpochMs BETWEEN :start AND :end
    """)
    fun observeCompletedCount(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE dateEpochMs BETWEEN :start AND :end
    """)
    fun observeCreatedCount(start: Long, end: Long): Flow<Int>

    /**
     * Raw timestamps of 'completed' events.
     * Uses task_events.timestamp (actual completion time), NOT dateEpochMs.
     * A streak is about user action on a calendar day, not scheduled dates.
     *
     * @param fromEpochMs lower bound — only completions after this timestamp are returned.
     *                    Caps streak calculation at ~400 days (any realistic streak length).
     */
    @Query(
        """
        SELECT timestamp FROM task_events
        WHERE eventType = 'completed' AND timestamp >= :fromEpochMs
        """
    )
    fun observeCompletedTimestamps(fromEpochMs: Long): Flow<List<Long>>
    // ──────────────────────────────────────────────
    // Life area breakdown
    // ──────────────────────────────────────────────

    @Query("""
        SELECT t.lifeAreaId as lifeAreaId, COUNT(*) as count
        FROM tasks t
        WHERE t.isCompleted = 1 AND t.dateEpochMs BETWEEN :start AND :end
        AND t.lifeAreaId IS NOT NULL
        GROUP BY t.lifeAreaId
        ORDER BY count DESC
    """)
    fun observeCompletionByLifeArea(start: Long, end: Long): Flow<List<LifeAreaCompletion>>

    // ──────────────────────────────────────────────
    // Unorganized tasks (no life area, no goal)
    // ──────────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE dateEpochMs BETWEEN :start AND :end
        AND lifeAreaId IS NULL AND goalId IS NULL
    """)
    fun observeUnorganizedCount(start: Long, end: Long): Flow<Int>

    // ──────────────────────────────────────────────
    // Best day of week
    // Derives Persian day index (0=Saturday … 6=Friday) from dateEpochMs.
    // 1970-01-01 (epoch day 0) was a Thursday = Persian index 5.
    // Formula: (dateEpochMs / 86400000 + 5) % 7
    // ──────────────────────────────────────────────

    @Query("""
        SELECT CAST((dateEpochMs / 86400000 + 5) AS INTEGER) % 7 as dayIndex, COUNT(*) as count
        FROM tasks
        WHERE isCompleted = 1 AND dateEpochMs BETWEEN :start AND :end
        GROUP BY dayIndex
        ORDER BY count DESC
    """)
    fun observeCompletionByDay(start: Long, end: Long): Flow<List<DayCompletion>>

    // ──────────────────────────────────────────────
    // Goal breakdown is derived by FK join in observeGoalCompletionRates
    // (see below). The legacy goalName-based grouping was removed:
    // goal linkage is now resolved exclusively via goalId.
    // ──────────────────────────────────────────────

    // ──────────────────────────────────────────────
    // Phase 3: Procrastination Detection
    // Tracks how many times each task has been rescheduled.
    // Tasks with ≥ 3 reschedules signal procrastination patterns.
    // Joins tasks to resolve the title without a blocking lookup.
    //
    // @param fromEpochMs lower bound — only tasks created after this timestamp are returned.
    //                     Caps procrastination alerts at ~90 days (active concern window).
    // ──────────────────────────────────────────────

    @Query("""
        SELECT te.taskId AS taskId, t.title AS taskTitle, t.timestamp AS taskCreatedAt, COUNT(*) AS rescheduleCount
        FROM task_events te
        JOIN tasks t ON t.id = te.taskId
        WHERE te.eventType = 'rescheduled' AND t.timestamp >= :fromEpochMs
        GROUP BY te.taskId
    """)
    fun observeRescheduleCounts(fromEpochMs: Long): Flow<List<TaskRescheduleWithTitle>>

    @Query("""
        SELECT te.taskId AS taskId, t.title AS taskTitle, t.timestamp AS taskCreatedAt, COUNT(*) AS rescheduleCount
        FROM task_events te
        JOIN tasks t ON t.id = te.taskId
        WHERE te.eventType = 'rescheduled' AND t.goalId = :goalId
        GROUP BY te.taskId
    """)
    fun observeRescheduleCountsByGoal(goalId: Int): Flow<List<TaskRescheduleWithTitle>>

    // ──────────────────────────────────────────────
    // Phase 3: Goal Completion Rates
    // Uses FK join between tasks.goalId → goals.id
    // to calculate completed/total per goal.
    // ──────────────────────────────────────────────

    @Query("""
        SELECT
            g.id as goalId,
            g.title as goalTitle,
            COUNT(t.id) as totalTasks,
            SUM(CASE WHEN t.isCompleted = 1 THEN 1 ELSE 0 END) as completedTasks,
            CASE
                WHEN COUNT(t.id) > 0
                THEN CAST(SUM(CASE WHEN t.isCompleted = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(t.id) * 100
                ELSE 0
            END as completionRate
        FROM goals g
        INNER JOIN tasks t ON t.goalId = g.id
        WHERE g.status = 'active'
        GROUP BY g.id, g.title
        ORDER BY completionRate ASC
    """)
    fun observeGoalCompletionRates(): Flow<List<GoalRateResult>>

    @Query("""
        SELECT
            g.id as goalId,
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
        WHERE g.id = :goalId
        GROUP BY g.id, g.title
    """)
    fun observeGoalCompletionRate(goalId: Int): Flow<GoalRateResult?>

    @Query("""
        SELECT COUNT(*) FROM tasks t
        WHERE t.isCompleted = 1
        AND t.goalId IS NOT NULL
        AND t.dateEpochMs BETWEEN :start AND :end
    """)
    fun observeGoalTaskCompletedCount(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM tasks t
        WHERE t.goalId IS NOT NULL
        AND t.dateEpochMs BETWEEN :start AND :end
    """)
    fun observeGoalTaskCreatedCount(start: Long, end: Long): Flow<Int>

    // ──────────────────────────────────────────────
    // Phase 3: Weekly Velocity
    // ──────────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE isCompleted = 1 AND dateEpochMs BETWEEN :start AND :end
    """)
    fun observePreviousWeekCompletedCount(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE dateEpochMs BETWEEN :start AND :end
    """)
    fun observePreviousWeekCreatedCount(start: Long, end: Long): Flow<Int>

    // ──────────────────────────────────────────────
    // Phase 3: Snapshot aggregation inputs
    // ──────────────────────────────────────────────

    /** Reschedule events whose timestamp falls within [start, end] (day-scoped). */
    @Query("""
        SELECT COUNT(*) FROM task_events
        WHERE eventType = 'rescheduled'
        AND timestamp BETWEEN :start AND :end
    """)
    fun observeRescheduleCountBetween(start: Long, end: Long): Flow<Int>

    /** Completed/total task counts for a single goal within [start, end] (day-scoped). */
    @Query("""
        SELECT COUNT(*) as total,
               COALESCE(SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END), 0) as completed
        FROM tasks
        WHERE goalId = :goalId AND dateEpochMs BETWEEN :start AND :end
    """)
    suspend fun getGoalDayCounts(goalId: Int, start: Long, end: Long): GoalDayCount

    /** Earliest scheduled task day — backfill start anchor (null when no tasks exist). */
    @Query("SELECT MIN(dateEpochMs) FROM tasks")
    suspend fun getEarliestTaskDateEpochMs(): Long?

    // ──────────────────────────────────────────────
    // Uncategorized tasks list (goalId IS NULL)
    // ──────────────────────────────────────────────

    @Query("""
        SELECT id, title, priority, isCompleted, dateEpochMs, timestamp
        FROM tasks
        WHERE goalId IS NULL AND lifeAreaId IS NULL
        ORDER BY dateEpochMs DESC
    """)
    fun observeUnorganizedTasks(): Flow<List<UncategorizedTask>>

    // ──────────────────────────────────────────────
    // Phase 2A: Attention — meaningful interaction timestamps
    //
    // Sources: notes (user-authored logs linked to tasks).
    // Future expansion: UNION ALL with subtask_events, photo timestamps, etc.
    // The Attention algorithm receives a single (taskId → timestamp) map and
    // does not know the source — this query is the only place that decides
    // which events count as "meaningful".
    // ──────────────────────────────────────────────

    @Query("""
        SELECT taskId, MAX(timestamp) AS lastMeaningfulMs
        FROM notes
        WHERE taskId IS NOT NULL
        AND taskId IN (SELECT id FROM tasks WHERE goalId = :goalId)
        GROUP BY taskId
    """)
    suspend fun getLastMeaningfulInteractionPerTask(goalId: Int): List<TaskLastInteraction>
}

/** Result of the meaningful-interaction aggregation query. */
data class TaskLastInteraction(val taskId: Int, val lastMeaningfulMs: Long)

/** Per-goal task counts for a day, returned by [getGoalDayCounts]. */
data class GoalDayCount(val total: Int, val completed: Int)

// ──────────────────────────────────────────────
// Result types for aggregation queries
// ──────────────────────────────────────────────

data class LifeAreaCompletion(val lifeAreaId: Int, val count: Int)
data class DayCompletion(val dayIndex: Int, val count: Int)

data class TaskRescheduleWithTitle(val taskId: Int, val taskTitle: String?, val taskCreatedAt: Long, val rescheduleCount: Int)

data class GoalRateResult(
    val goalId: Int,
    val goalTitle: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val completionRate: Float
)

data class UncategorizedTask(
    val id: Int,
    val title: String,
    val priority: String?,
    val isCompleted: Boolean,
    val dateEpochMs: Long,
    val timestamp: Long
)
