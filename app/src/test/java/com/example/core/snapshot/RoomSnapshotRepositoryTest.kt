package com.example.core.snapshot

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomSnapshotRepositoryTest {

    @Test
    fun recordBehavior_delegatesToDao() = runTest {
        val dao = mockk<SnapshotDao>(relaxed = true)
        val entity = BehaviorSnapshotEntity(1L, 1, 1, 1, "STABLE", 0f)
        RoomSnapshotRepository(dao).recordBehavior(entity)
        coVerify { dao.upsertBehavior(entity) }
    }

    @Test
    fun recordGoalProgress_delegatesToDao() = runTest {
        val dao = mockk<SnapshotDao>(relaxed = true)
        val entity = GoalProgressSnapshotEntity(1L, 2, 3, 4, 75f)
        RoomSnapshotRepository(dao).recordGoalProgress(entity)
        coVerify { dao.upsertGoalProgress(entity) }
    }

    @Test
    fun observeGoalProgress_delegatesToDao() = runTest {
        val dao = mockk<SnapshotDao>()
        val sample = listOf(GoalProgressSnapshotEntity(1L, 2, 3, 4, 75f))
        io.mockk.every { dao.observeGoalProgress(any()) } returns flowOf(sample)
        assertEquals(sample, RoomSnapshotRepository(dao).observeGoalProgress(2).first())
    }

    @Test
    fun getCoveredDates_delegatesToDao() = runTest {
        val dao = mockk<SnapshotDao>()
        io.mockk.coEvery { dao.getCoveredDates() } returns listOf(1L, 2L)
        assertEquals(listOf(1L, 2L), RoomSnapshotRepository(dao).getCoveredDates())
    }
}
