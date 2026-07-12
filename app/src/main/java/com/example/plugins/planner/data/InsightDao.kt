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

    // ──────────────────────────────────────────────
    // Streak: raw timestamps of 'completed' events
    // Uses task_events.timestamp (actual completion time), NOT dateEpochMs.
    // A streak is about user action on a calendar day, not scheduled dates.
    // ──────────────────────────────────────────────

    @Query("""
        SELECT timestamp FROM task_events
        WHERE eventType = 'completed'
    """)
    fun observeCompletedTimestamps(): Flow<List<Long>>

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
        AND lifeAreaId IS NULL AND goalId IS NULL AND goalName IS NULL
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
    // Goal breakdown (legacy goalName-based)
    // ──────────────────────────────────────────────

    @Query("""
        SELECT goalName as goalName, COUNT(*) as count
        FROM tasks
        WHERE isCompleted = 1 AND dateEpochMs BETWEEN :start AND :end
        AND goalName IS NOT NULL AND goalName != ''
        GROUP BY goalName
        ORDER BY count DESC
    """)
    fun observeCompletionByGoal(start: Long, end: Long): Flow<List<GoalCompletion>>

    // ──────────────────────────────────────────────
    // Phase 3: Procrastination Detection
    // Tracks how many times each task has been rescheduled.
    // Tasks with ≥ 3 reschedules signal procrastination patterns.
    // ──────────────────────────────────────────────

    @Query("""
        SELECT taskId, COUNT(*) as rescheduleCount
        FROM task_events
        WHERE eventType = 'rescheduled'
        GROUP BY taskId
    """)
    fun observeRescheduleCounts(): Flow<List<TaskRescheduleCount>>

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
}

// ──────────────────────────────────────────────
// Result types for aggregation queries
// ──────────────────────────────────────────────

data class LifeAreaCompletion(val lifeAreaId: Int, val count: Int)
data class DayCompletion(val dayIndex: Int, val count: Int)
data class GoalCompletion(val goalName: String, val count: Int)

data class TaskRescheduleCount(val taskId: Int, val rescheduleCount: Int)

data class GoalRateResult(
    val goalId: Int,
    val goalTitle: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val completionRate: Float
)
