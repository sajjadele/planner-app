package com.example.ui.screens.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalEventEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.core.mirror.MirrorRepository
import com.example.core.mirror.RoomMirrorRepository
import com.example.domain.mirror.MirrorInsight
import com.example.core.snapshot.RoomSnapshotRepository
import com.example.core.snapshot.SnapshotAggregator
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskEventEntity
import com.example.plugins.planner.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Developer-only tool (gated by BuildConfig.DEBUG at the UI layer).
 *
 * Generates synthetic but REAL data through the normal repositories so the genuine
 * Mirror pipeline (Events -> Snapshots -> MirrorRepository -> MirrorEngine) processes it.
 * It does NOT fake Mirror responses, bypass heuristics, or touch Mirror thresholds.
 *
 * Every generated goal is prefixed with [TEST_GOAL_PREFIX] so cleanup stays scoped.
 */
class DebugMirrorViewModel(application: Application) : AndroidViewModel(application) {

    private val goalRepository: GoalRepository
    private val taskRepository: TaskRepository
    private val insightRepository: InsightRepository
    private val snapshotAggregator: SnapshotAggregator
    private val mirrorRepository: MirrorRepository

    /** Goal IDs created by this tool, so Clear deletes only debug data. */
    private val generatedGoalIds = mutableListOf<Int>()

    private val _lastResult = MutableStateFlow<List<MirrorInsight>>(emptyList())
    val lastResult: StateFlow<List<MirrorInsight>> = _lastResult.asStateFlow()

    private val _lastScenario = MutableStateFlow<String?>(null)
    val lastScenario: StateFlow<String?> = _lastScenario.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        goalRepository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        taskRepository = TaskRepository(database.taskDao(), database.taskEventDao())
        insightRepository = RoomInsightRepository(database.insightDao())
        snapshotAggregator = SnapshotAggregator(
            insightRepository,
            RoomSnapshotRepository(database.snapshotDao()),
            goalRepository
        )
        mirrorRepository = RoomMirrorRepository(
            insightRepository,
            goalRepository,
            RoomSnapshotRepository(database.snapshotDao())
        )
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun midnightDaysAgo(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun timestampDaysAgo(days: Int): Long =
        System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000

    /** Insert a goal (active) with its `created` event dated in the past; returns goal id. */
    private suspend fun createTestGoal(ageDays: Int, title: String): Int {
        val goal = GoalEntity(
            title = "$TEST_GOAL_PREFIX $title",
            status = "active",
            createdAt = timestampDaysAgo(ageDays)
        )
        val id = goalRepository.insertGoal(goal).toInt()
        goalRepository.insertGoalEvent(
            GoalEventEntity(goalId = id, eventType = "created", timestamp = timestampDaysAgo(ageDays))
        )
        generatedGoalIds += id
        return id
    }

    /** Rebuild snapshot projections so time-window heuristics see the synthetic history. */
    private suspend fun rebuildSnapshots() {
        snapshotAggregator.backfillMissing()
    }

    private suspend fun runMirror(goalId: Int, scenario: String) {
        val signals = mirrorRepository.evaluate(goalId)
        _lastResult.value = mirrorRepository.render(signals)
        _lastScenario.value = scenario
    }

    // ── actions ──────────────────────────────────────────────────────────────

    fun simulateHistory() {
        viewModelScope.launch {
            val goalId = createTestGoal(ageDays = 8, title = "History")
            taskRepository.insertTask(
                TaskEntity(
                    title = "Task", dateEpochMs = midnightDaysAgo(8),
                    timestamp = timestampDaysAgo(8), goalId = goalId
                )
            )
            rebuildSnapshots()
            runMirror(goalId, "Simulate History")
            _status.value = "Simulated 7+ day history for goal #$goalId"
        }
    }

    fun createBoulderScenario() {
        viewModelScope.launch {
            val goalId = createTestGoal(ageDays = 10, title = "Boulder")
            val taskId = taskRepository.insertTask(
                TaskEntity(
                    title = "Stuck Task", dateEpochMs = midnightDaysAgo(10),
                    timestamp = timestampDaysAgo(10), goalId = goalId
                )
            ).toInt()
            repeat(3) {
                taskRepository.insertTaskEvent(
                    TaskEventEntity(taskId = taskId, eventType = "rescheduled", timestamp = timestampDaysAgo(9))
                )
            }
            rebuildSnapshots()
            runMirror(goalId, "Boulder")
            _status.value = "Boulder scenario built for goal #$goalId"
        }
    }

    fun createGoalAttentionScenario() {
        viewModelScope.launch {
            // Goal created 30 days ago, last activity (created event) 15+ days ago.
            val goalId = createTestGoal(ageDays = 30, title = "Attention")
            taskRepository.insertTask(
                TaskEntity(
                    title = "Old Task", dateEpochMs = midnightDaysAgo(30),
                    timestamp = timestampDaysAgo(30), goalId = goalId
                )
            )
            rebuildSnapshots()
            runMirror(goalId, "Goal Attention")
            _status.value = "Goal Attention scenario built for goal #$goalId (30d old, inactive 15d+)"
        }
    }

    fun createInitiatorFinisherScenario() {
        viewModelScope.launch {
            val goalId = createTestGoal(ageDays = 12, title = "InitiatorFinisher")
            repeat(10) { i ->
                val taskId = taskRepository.insertTask(
                    TaskEntity(
                        title = "Task $i", dateEpochMs = midnightDaysAgo(11),
                        timestamp = timestampDaysAgo(11), goalId = goalId
                    )
                ).toInt()
                taskRepository.insertTaskEvent(
                    TaskEventEntity(taskId = taskId, eventType = "created", timestamp = timestampDaysAgo(11))
                )
            }
            // Complete only 3 of them.
            taskRepository.getTasksByGoalId(goalId).first().take(3).forEach { task ->
                taskRepository.updateTask(task.copy(isCompleted = true))
                taskRepository.insertTaskEvent(
                    TaskEventEntity(taskId = task.id, eventType = "completed", timestamp = timestampDaysAgo(2))
                )
            }
            rebuildSnapshots()
            runMirror(goalId, "Initiator/Finisher")
            _status.value = "Initiator/Finisher scenario built for goal #$goalId"
        }
    }

    fun createConsistencyDecayScenario() {
        viewModelScope.launch {
            val goalId = createTestGoal(ageDays = 14, title = "ConsistencyDecay")
            // Previous week: many completions. Current week: few.
            repeat(8) { i ->
                val taskId = taskRepository.insertTask(
                    TaskEntity(
                        title = "Prev $i", dateEpochMs = midnightDaysAgo(10),
                        timestamp = timestampDaysAgo(10), goalId = goalId
                    )
                ).toInt()
                taskRepository.insertTaskEvent(
                    TaskEventEntity(taskId = taskId, eventType = "completed", timestamp = timestampDaysAgo(10))
                )
            }
            repeat(1) { i ->
                val taskId = taskRepository.insertTask(
                    TaskEntity(
                        title = "Now $i", dateEpochMs = midnightDaysAgo(1),
                        timestamp = timestampDaysAgo(1), goalId = goalId
                    )
                ).toInt()
                taskRepository.insertTaskEvent(
                    TaskEventEntity(taskId = taskId, eventType = "completed", timestamp = timestampDaysAgo(1))
                )
            }
            rebuildSnapshots()
            runMirror(goalId, "Consistency Decay")
            _status.value = "Consistency Decay scenario built for goal #$goalId"
        }
    }

    fun clearTestData() {
        viewModelScope.launch {
            val ids = generatedGoalIds.toList()
            ids.forEach { goalId ->
                // Remove task events and tasks first so they don't orphan and pollute
                // global Mirror queries (e.g. leftover reschedules re-triggering Boulder).
                taskRepository.deleteEventsForGoal(goalId)
                taskRepository.deleteTasksForGoal(goalId)
                goalRepository.getGoalById(goalId)?.let { goalRepository.deleteGoal(it) }
            }
            generatedGoalIds.clear()
            rebuildSnapshots()
            _lastResult.value = emptyList()
            _lastScenario.value = null
            _status.value = "Cleared ${ids.size} debug goal(s)"
        }
    }

    companion object {
        const val TEST_GOAL_PREFIX = "🧪 Mirror Test"
    }
}
