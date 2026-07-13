package com.example.plugins.planner.data

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomInsightRepositoryTest {

    @Test
    fun observeCompletedCount_delegatesToDao() = runTest {
        val dao = mockk<InsightDao>()
        every { dao.observeCompletedCount(any(), any()) } returns flowOf(7)
        val repo = RoomInsightRepository(dao)
        assertEquals(7, repo.observeCompletedCount(0L, 1L).first())
    }

    @Test
    fun observeGoalCompletionRate_delegatesToDao() = runTest {
        val dao = mockk<InsightDao>()
        val sample = GoalRateResult(1, "Goal", 3, 1, 33f)
        every { dao.observeGoalCompletionRate(any()) } returns flowOf(sample)
        val repo = RoomInsightRepository(dao)
        assertEquals(sample, repo.observeGoalCompletionRate(1).first())
    }
}
