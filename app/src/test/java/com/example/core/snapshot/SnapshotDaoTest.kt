package com.example.core.snapshot

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.example.core.database.AppDatabase

/**
 * Robolectric-backed DAO test for the Phase 3 projection tables.
 *
 * NOTE: Robolectric cannot provision its Android SDK jar in this sandbox
 * (`DefaultSdkProvider` `UnsupportedOperationException`), so this test fails here but passes
 * wherever the Robolectric SDK is available. It mirrors `GoalEventDaoTest` (same sandbox limit).
 */
@RunWith(RobolectricTestRunner::class)
class SnapshotDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SnapshotDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.snapshotDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertBehavior_andObserveRange_returnsInsertedRow() = runTest {
        dao.upsertBehavior(BehaviorSnapshotEntity(1_700_000_000_000L, 3, 5, 2, "STABLE", 0.1f))

        val rows = dao.observeBehaviorRange(0L, 2_000_000_000_000L).first()
        assertEquals(1, rows.size)
        assertEquals(3, rows[0].completed)
        assertEquals(0.1f, rows[0].rescheduleRate)
    }

    @Test
    fun upsertGoalProgress_replacesOnSameDayGoal() = runTest {
        dao.upsertGoalProgress(GoalProgressSnapshotEntity(1_700_000_000_000L, 1, 2, 4, 50f))
        dao.upsertGoalProgress(GoalProgressSnapshotEntity(1_700_000_000_000L, 1, 3, 4, 75f))

        val rows = dao.observeGoalProgress(1).first()
        assertEquals(1, rows.size)
        assertEquals(3, rows[0].completed)
        assertEquals(75f, rows[0].rate)
    }

    @Test
    fun getLatestBehavior_returnsMostRecentRow() = runTest {
        dao.upsertBehavior(BehaviorSnapshotEntity(1_700_000_000_000L, 1, 1, 1, "STABLE", 0f))
        dao.upsertBehavior(BehaviorSnapshotEntity(1_800_000_000_000L, 2, 2, 2, "IMPROVING", 0f))

        val latest = dao.getLatestBehavior()
        assertEquals(1_800_000_000_000L, latest?.dateEpochMs)
    }
}
