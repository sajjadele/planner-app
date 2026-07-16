package com.example.core.goal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.plugins.planner.data.TaskDao
import com.example.plugins.planner.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomGoalRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomGoalRepository
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun updateGoalStatus_activeToCompleted_setsStatusAndLogsEvent() = runTest {
        val id = repository.insertGoal(GoalEntity(title = "G")).toInt()

        repository.updateGoalStatus(id, GoalStatus.COMPLETED)

        val goal = repository.getGoalById(id)!!
        assertEquals(GoalStatus.COMPLETED, goal.status)
        val events = repository.observeGoalEvents(id).first()
        assertEquals(listOf("created", "completed"), events.map { it.eventType })
    }

    @Test
    fun updateGoalStatus_completedToArchived_logsArchivedEvent() = runTest {
        val id = repository.insertGoal(GoalEntity(title = "G")).toInt()
        repository.updateGoalStatus(id, GoalStatus.COMPLETED)

        repository.updateGoalStatus(id, GoalStatus.ARCHIVED)

        assertEquals(GoalStatus.ARCHIVED, repository.getGoalById(id)!!.status)
        val events = repository.observeGoalEvents(id).first()
        assertEquals(listOf("created", "completed", "archived"), events.map { it.eventType })
    }

    @Test
    fun archiveGoal_fromAbandoned_succeeds() = runTest {
        val id = repository.insertGoal(GoalEntity(title = "G")).toInt()
        repository.updateGoalStatus(id, GoalStatus.ABANDONED)

        repository.archiveGoal(id)

        assertEquals(GoalStatus.ARCHIVED, repository.getGoalById(id)!!.status)
    }

    @Test
    fun updateGoalStatus_activeToArchived_isIllegal() = runTest {
        val id = repository.insertGoal(GoalEntity(title = "G")).toInt()

        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.updateGoalStatus(id, GoalStatus.ARCHIVED) }
        }
        // Status must remain unchanged (active).
        assertEquals(GoalStatus.ACTIVE, repository.getGoalById(id)!!.status)
    }

    @Test
    fun updateGoalStatus_pausedToActive_isAllowed() = runTest {
        val id = repository.insertGoal(GoalEntity(title = "G")).toInt()
        repository.updateGoalStatus(id, GoalStatus.PAUSED)

        repository.updateGoalStatus(id, GoalStatus.ACTIVE)

        assertEquals(GoalStatus.ACTIVE, repository.getGoalById(id)!!.status)
    }

    @Test
    fun observeGoalsByStatus_returnsArchivedGoalsOnly() = runTest {
        val a = repository.insertGoal(GoalEntity(title = "A")).toInt()
        val b = repository.insertGoal(GoalEntity(title = "B")).toInt()
        repository.updateGoalStatus(a, GoalStatus.COMPLETED)
        repository.updateGoalStatus(a, GoalStatus.ARCHIVED)
        repository.updateGoalStatus(b, GoalStatus.ARCHIVED)

        val archived = repository.observeGoalsByStatus(GoalStatus.ARCHIVED).first()
        assertEquals(setOf("A", "B"), archived.map { it.title }.toSet())
    }

    @Test
    fun observeGoalActiveDayCount_derivedFromTasks() = runTest {
        val id = repository.insertGoal(GoalEntity(title = "G")).toInt()
        taskDao.insertTask(TaskEntity(title = "t1", dateEpochMs = 1000L, goalId = id))
        taskDao.insertTask(TaskEntity(title = "t2", dateEpochMs = 4000L, goalId = id))

        val count = repository.observeGoalActiveDayCount(id).first()
        assertEquals(2, count)
        val last = repository.observeGoalLastActivity(id).first()
        assertEquals(4000L, last)
    }
}
