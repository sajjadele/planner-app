package com.example.core.goal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.plugins.planner.data.TaskDao
import com.example.plugins.planner.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.example.core.database.AppDatabase

@RunWith(RobolectricTestRunner::class)
class GoalDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var goalDao: GoalDao
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        goalDao = database.goalDao()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertGoal_withoutOptionalFields_readsBackNull() = runTest {
        val id = goalDao.insertGoal(GoalEntity(title = "Learn Arabic"))
        val goal = goalDao.getGoalById(id.toInt())!!

        assertNull(goal.why)
        assertNull(goal.deadlineEpochMs)
        assertEquals(GoalStatus.ACTIVE, goal.status)
    }

    @Test
    fun insertGoal_withOptionalFields_readsBackValues() = runTest {
        val deadline = 1_700_000_000_000L
        val id = goalDao.insertGoal(
            GoalEntity(title = "Fitness", why = "Stay healthy", deadlineEpochMs = deadline)
        )
        val goal = goalDao.getGoalById(id.toInt())!!

        assertEquals("Stay healthy", goal.why)
        assertEquals(deadline, goal.deadlineEpochMs)
    }

    @Test
    fun getGoalsByStatus_returnsOnlyMatchingStatus() = runTest {
        goalDao.insertGoal(GoalEntity(title = "A", status = GoalStatus.ACTIVE))
        goalDao.insertGoal(GoalEntity(title = "B", status = GoalStatus.COMPLETED))
        goalDao.insertGoal(GoalEntity(title = "C", status = GoalStatus.ARCHIVED))
        goalDao.insertGoal(GoalEntity(title = "D", status = GoalStatus.ARCHIVED))

        val archived = goalDao.getGoalsByStatus(GoalStatus.ARCHIVED).first()
        assertEquals(2, archived.size)
        assertEquals(setOf("C", "D"), archived.map { it.title }.toSet())
    }

    @Test
    fun observeGoalLastActivity_returnsLatestTaskDay() = runTest {
        val goalId = goalDao.insertGoal(GoalEntity(title = "G")).toInt()
        taskDao.insertTask(TaskEntity(title = "t1", dateEpochMs = 1000L, goalId = goalId))
        taskDao.insertTask(TaskEntity(title = "t2", dateEpochMs = 5000L, goalId = goalId))
        taskDao.insertTask(TaskEntity(title = "t3", dateEpochMs = 3000L, goalId = goalId))

        val last = goalDao.observeGoalLastActivity(goalId).first()
        assertEquals(5000L, last)
    }

    @Test
    fun observeGoalActiveDayCount_countsDistinctDays() = runTest {
        val goalId = goalDao.insertGoal(GoalEntity(title = "G")).toInt()
        // Day 1000, 1000 (same day), 2000, 3000 -> 3 distinct days
        taskDao.insertTask(TaskEntity(title = "a", dateEpochMs = 1000L, goalId = goalId))
        taskDao.insertTask(TaskEntity(title = "b", dateEpochMs = 1000L, goalId = goalId))
        taskDao.insertTask(TaskEntity(title = "c", dateEpochMs = 2000L, goalId = goalId))
        taskDao.insertTask(TaskEntity(title = "d", dateEpochMs = 3000L, goalId = goalId))
        // Task for a different goal must not be counted
        taskDao.insertTask(TaskEntity(title = "x", dateEpochMs = 9999L, goalId = null))

        val count = goalDao.observeGoalActiveDayCount(goalId).first()
        assertEquals(3, count)
    }

    @Test
    fun observeGoalLastActivity_noTasks_returnsNull() = runTest {
        val goalId = goalDao.insertGoal(GoalEntity(title = "Empty")).toInt()
        assertNull(goalDao.observeGoalLastActivity(goalId).first())
    }
}
