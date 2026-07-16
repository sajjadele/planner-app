package com.example.plugins.planner.data

import kotlinx.coroutines.flow.Flow

/**
 * Room-backed implementation of [InsightRepository]. Thin delegation over [InsightDao];
 * the aggregation SQL stays in the DAO, the repository owns only the boundary.
 */
class RoomInsightRepository(private val insightDao: InsightDao) : InsightRepository {
    override fun observeCompletedCount(start: Long, end: Long): Flow<Int> =
        insightDao.observeCompletedCount(start, end)

    override fun observeCreatedCount(start: Long, end: Long): Flow<Int> =
        insightDao.observeCreatedCount(start, end)

    override fun observeCompletionByLifeArea(start: Long, end: Long): Flow<List<LifeAreaCompletion>> =
        insightDao.observeCompletionByLifeArea(start, end)

    override fun observeUnorganizedCount(start: Long, end: Long): Flow<Int> =
        insightDao.observeUnorganizedCount(start, end)

    override fun observeCompletedTimestamps(): Flow<List<Long>> =
        insightDao.observeCompletedTimestamps()

    override fun observeCompletionByDay(start: Long, end: Long): Flow<List<DayCompletion>> =
        insightDao.observeCompletionByDay(start, end)

    override fun observeRescheduleCounts(): Flow<List<TaskRescheduleWithTitle>> =
        insightDao.observeRescheduleCounts()

    override fun observeRescheduleCountsByGoal(goalId: Int): Flow<List<TaskRescheduleWithTitle>> =
        insightDao.observeRescheduleCountsByGoal(goalId)

    override fun observeGoalCompletionRates(): Flow<List<GoalRateResult>> =
        insightDao.observeGoalCompletionRates()

    override fun observeGoalCompletionRate(goalId: Int): Flow<GoalRateResult?> =
        insightDao.observeGoalCompletionRate(goalId)

    override fun observePreviousWeekCompletedCount(start: Long, end: Long): Flow<Int> =
        insightDao.observePreviousWeekCompletedCount(start, end)

    override fun observePreviousWeekCreatedCount(start: Long, end: Long): Flow<Int> =
        insightDao.observePreviousWeekCreatedCount(start, end)

    override fun observeRescheduleCountBetween(start: Long, end: Long): Flow<Int> =
        insightDao.observeRescheduleCountBetween(start, end)

    override suspend fun getGoalDayCounts(goalId: Int, start: Long, end: Long): GoalDayCount =
        insightDao.getGoalDayCounts(goalId, start, end)

    override suspend fun getEarliestTaskDateEpochMs(): Long? =
        insightDao.getEarliestTaskDateEpochMs()
}
