package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository boundary for analytical/insight reads. Decouples consumers (ViewModels)
 * from the concrete [InsightDao], so the DAO can be swapped or mocked in tests.
 *
 * Only the queries actually consumed by the UI are exposed; unused DAO methods are
 * intentionally omitted to keep the boundary minimal.
 */
interface InsightRepository {
    fun observeCompletedCount(start: Long, end: Long): Flow<Int>
    fun observeCreatedCount(start: Long, end: Long): Flow<Int>
    fun observeCompletionByLifeArea(start: Long, end: Long): Flow<List<LifeAreaCompletion>>
    fun observeUnorganizedCount(start: Long, end: Long): Flow<Int>
    fun observeCompletedTimestamps(): Flow<List<Long>>
    fun observeCompletionByDay(start: Long, end: Long): Flow<List<DayCompletion>>
    fun observeRescheduleCounts(): Flow<List<TaskRescheduleWithTitle>>
    fun observeRescheduleCountsByGoal(goalId: Int): Flow<List<TaskRescheduleWithTitle>>
    fun observeGoalCompletionRates(): Flow<List<GoalRateResult>>
    fun observeGoalCompletionRate(goalId: Int): Flow<GoalRateResult?>
    fun observePreviousWeekCompletedCount(start: Long, end: Long): Flow<Int>
    fun observePreviousWeekCreatedCount(start: Long, end: Long): Flow<Int>

    // ── Phase 3: snapshot aggregation inputs ──
    fun observeRescheduleCountBetween(start: Long, end: Long): Flow<Int>
    suspend fun getGoalDayCounts(goalId: Int, start: Long, end: Long): GoalDayCount
    suspend fun getEarliestTaskDateEpochMs(): Long?
}
