package com.example.core.snapshot

import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalRepository
import com.example.plugins.planner.data.GoalDayCount
import com.example.plugins.planner.data.InsightRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SnapshotAggregatorTest {

    private fun at(year: Int, month: Int, day: Int): Long {
        val c = Calendar.getInstance()
        c.set(year, month, day, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Test
    fun recordDay_persistsBehaviorAndGoalProgress() = runTest {
        val day = at(2026, Calendar.JULY, 13)

        val insightRepo = mockk<InsightRepository>()
        io.mockk.every { insightRepo.observeCompletedCount(any(), any()) } returns flowOf(5)
        io.mockk.every { insightRepo.observeCreatedCount(any(), any()) } returns flowOf(10)
        io.mockk.every { insightRepo.observeCompletedTimestamps() } returns flowOf(listOf(day))
        io.mockk.every { insightRepo.observeRescheduleCountBetween(any(), any()) } returns flowOf(2)
        io.mockk.coEvery { insightRepo.getGoalDayCounts(any(), any(), any()) } returns GoalDayCount(total = 10, completed = 4)

        val goalRepo = mockk<GoalRepository>()
        io.mockk.every { goalRepo.getActiveGoals() } returns flowOf(
            listOf(GoalEntity(id = 1, title = "G", status = "active"))
        )

        val snapshotRepo = mockk<SnapshotRepository>(relaxed = true)
        val behaviorSlot = slot<BehaviorSnapshotEntity>()
        val goalSlot = slot<GoalProgressSnapshotEntity>()
        coEvery { snapshotRepo.recordBehavior(capture(behaviorSlot)) } returns Unit
        coEvery { snapshotRepo.recordGoalProgress(capture(goalSlot)) } returns Unit

        SnapshotAggregator(insightRepo, snapshotRepo, goalRepo).recordDay(day)

        val behavior = behaviorSlot.captured
        assertEquals(day, behavior.dateEpochMs)
        assertEquals(5, behavior.completed)
        assertEquals(10, behavior.created)
        assertEquals(1, behavior.streak)            // only `day` in timestamps
        assertEquals("STABLE", behavior.velocity)   // 50% vs 50% (prev mocked same)
        assertEquals(0.2f, behavior.rescheduleRate) // 2 / 10

        val goal = goalSlot.captured
        assertEquals(day, goal.dateEpochMs)
        assertEquals(1, goal.goalId)
        assertEquals(4, goal.completed)
        assertEquals(10, goal.total)
        assertEquals(40f, goal.rate)
    }

    @Test
    fun backfillIfNeeded_skipsWhenTodayAlreadyCovered() = runTest {
        val insightRepo = mockk<InsightRepository>()
        val goalRepo = mockk<GoalRepository>(relaxed = true)
        val snapshotRepo = mockk<SnapshotRepository>(relaxed = true)

        // Latest snapshot is already today → backfill should be a no-op (no recordDay writes).
        coEvery { snapshotRepo.getLatestBehavior() } returns BehaviorSnapshotEntity(
            dateEpochMs = at(2026, Calendar.JULY, 13),
            completed = 0, created = 0, streak = 0, velocity = "STABLE", rescheduleRate = 0f
        )

        var recordBehaviorCalls = 0
        coEvery { snapshotRepo.recordBehavior(any()) } answers { recordBehaviorCalls++ }

        SnapshotAggregator(insightRepo, snapshotRepo, goalRepo).backfillIfNeeded()

        assertEquals(0, recordBehaviorCalls)
    }
}
