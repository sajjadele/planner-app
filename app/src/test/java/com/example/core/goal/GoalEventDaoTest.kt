package com.example.core.goal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.example.core.database.AppDatabase

@RunWith(RobolectricTestRunner::class)
class GoalEventDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: GoalEventDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.goalEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertEvent_andObserveByGoal_returnsOnlyThatGoal() = runTest {
        dao.insertEvent(GoalEventEntity(goalId = 1, eventType = "created"))
        dao.insertEvent(GoalEventEntity(goalId = 1, eventType = "paused"))
        dao.insertEvent(GoalEventEntity(goalId = 2, eventType = "created"))

        val events = dao.observeEventsByGoal(1).first()
        assertEquals(2, events.size)
        assertEquals(setOf("created", "paused"), events.map { it.eventType }.toSet())
    }

    @Test
    fun insertEvent_andObserveAll_returnsAllEvents() = runTest {
        dao.insertEvent(GoalEventEntity(goalId = 1, eventType = "created"))
        dao.insertEvent(GoalEventEntity(goalId = 2, eventType = "abandoned"))

        val all = dao.observeAllEvents().first()
        assertEquals(2, all.size)
    }

    @Test
    fun observeEventsByGoal_emitsUpdates() = runTest {
        dao.observeEventsByGoal(1).test {
            assertEquals(0, awaitItem().size)
            dao.insertEvent(GoalEventEntity(goalId = 1, eventType = "resumed"))
            val item = awaitItem()
            assertEquals(1, item.size)
            assertEquals("resumed", item.first().eventType)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
