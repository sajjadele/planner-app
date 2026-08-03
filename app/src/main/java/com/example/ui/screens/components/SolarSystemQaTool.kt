package com.example.ui.screens.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalEventEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.plugins.notes.data.NoteEntity
import com.example.plugins.notes.data.NoteRepository
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityEventRepository
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskEventEntity
import com.example.plugins.planner.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Developer-only tool (gated by BuildConfig.DEBUG) for Solar System QA.
 * Creates REAL goals and tasks through the normal repositories.
 * Every goal is prefixed with [QA_GOAL_PREFIX] for cleanup.
 *
 * 9 scenarios covering all Solar System visual states.
 */
class SolarSystemQaViewModel(application: Application) : AndroidViewModel(application) {

    private val goalRepository: GoalRepository
    private val taskRepository: TaskRepository
    private val noteRepository: NoteRepository
    private val activityEventRepository: ActivityEventRepository

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val generatedGoalIds = mutableListOf<Int>()

    init {
        val database = AppDatabase.getDatabase(application)
        goalRepository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        taskRepository = TaskRepository(database.taskDao(), database.taskEventDao())
        noteRepository = NoteRepository(database.noteDao())
        activityEventRepository = ActivityEventRepository(database.activityEventDao())
    }

    // ── Date helpers ──

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

    private fun timestampHoursAgo(hours: Int): Long =
        System.currentTimeMillis() - hours.toLong() * 60 * 60 * 1000

    private suspend fun createQaGoal(title: String, ageDays: Int = 7): Int {
        val goal = GoalEntity(
            title = "$QA_GOAL_PREFIX $title",
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

    private suspend fun createTask(
        goalId: Int,
        title: String,
        scheduledDaysAgo: Int,
        deadlineDaysAgo: Int? = null,
        createdDaysAgo: Int = scheduledDaysAgo
    ): Int {
        return taskRepository.insertTask(
            TaskEntity(
                title = title,
                dateEpochMs = midnightDaysAgo(scheduledDaysAgo),
                timestamp = timestampDaysAgo(createdDaysAgo),
                goalId = goalId,
                deadlineEpochMs = deadlineDaysAgo?.let { midnightDaysAgo(it) }
            )
        ).toInt()
    }

    private suspend fun completeTask(taskId: Int) {
        val task = taskRepository.getTaskById(taskId) ?: return
        taskRepository.updateTask(task.copy(isCompleted = true))
        taskRepository.insertTaskEvent(
            TaskEventEntity(taskId = taskId, eventType = "completed", timestamp = System.currentTimeMillis())
        )
    }

    private suspend fun rescheduleTask(taskId: Int, daysAgo: Int) {
        taskRepository.insertTaskEvent(
            TaskEventEntity(taskId = taskId, eventType = "rescheduled", timestamp = timestampDaysAgo(daysAgo))
        )
    }

    private suspend fun addNote(taskId: Int, content: String, daysAgo: Int) {
        noteRepository.insert(
            NoteEntity(
                content = content,
                taskId = taskId,
                timestamp = timestampDaysAgo(daysAgo)
            )
        )
    }

    private suspend fun addActivity(taskId: Int, eventType: String, daysAgo: Int) {
        activityEventRepository.addEvent(
            ActivityEventEntity(
                taskId = taskId,
                eventType = eventType,
                timestamp = timestampDaysAgo(daysAgo)
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 1: Empty Goal
    // ═══════════════════════════════════════════════════════════════════════

    fun createEmptyGoal() {
        viewModelScope.launch {
            val goalId = createQaGoal("Empty Goal")
            _status.value = "Goal #$goalId — empty, 0 tasks"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 2: Calm Orbit — recent interactions, no deadlines
    //
    // Expected: low attention (0.0-0.2), all planets far from sun,
    // no boulders, no overdue signals
    // ═══════════════════════════════════════════════════════════════════════

    fun createCalmOrbit() {
        viewModelScope.launch {
            val goalId = createQaGoal("Calm Orbit (5 tasks)")

            for (i in 1..5) {
                val taskId = createTask(goalId, "Calm Task $i", scheduledDaysAgo = 1)
                addNote(taskId, "Working on task $i", daysAgo = 0) // today
                addActivity(taskId, "NOTE_ADDED", daysAgo = 0)
            }

            _status.value = "Goal #$goalId — 5 tasks, recent interactions, no deadlines"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 3: Urgent Tasks — overdue + approaching deadlines
    //
    // Expected: high attention (0.5-0.9), planets close to sun,
    // overdue ring indicators, near-deadline amber signals
    // ═══════════════════════════════════════════════════════════════════════

    fun createUrgentTasks() {
        viewModelScope.launch {
            val goalId = createQaGoal("Urgent Tasks (6 tasks)")

            // 2 overdue tasks (deadline passed)
            val overdue1 = createTask(goalId, "Overdue Critical", scheduledDaysAgo = 5, deadlineDaysAgo = 3)
            rescheduleTask(overdue1, daysAgo = 2)

            val overdue2 = createTask(goalId, "Overdue Feature", scheduledDaysAgo = 4, deadlineDaysAgo = 2)

            // 2 near-deadline tasks (due within 24h)
            createTask(goalId, "Due Today", scheduledDaysAgo = 0, deadlineDaysAgo = 0)
            createTask(goalId, "Due Tomorrow", scheduledDaysAgo = 0, deadlineDaysAgo = -1)

            // 2 tasks with future deadlines (low attention)
            createTask(goalId, "Future Task 1", scheduledDaysAgo = 1, deadlineDaysAgo = -7)
            createTask(goalId, "Future Task 2", scheduledDaysAgo = 1, deadlineDaysAgo = -14)

            _status.value = "Goal #$goalId — 2 overdue, 2 near-deadline, 2 future"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 4: Boulder Field — multiple reschedules (≥2 = boulder)
    //
    // Expected: boulder wobble animation on rescheduled tasks,
    // high avoidance signal, inner orbit positions
    // ═══════════════════════════════════════════════════════════════════════

    fun createBoulderField() {
        viewModelScope.launch {
            val goalId = createQaGoal("Boulder Field (5 tasks)")

            // 3 boulders (3+ reschedules each)
            val boulder1 = createTask(goalId, "Boulder A (3x)", scheduledDaysAgo = 10, deadlineDaysAgo = 5)
            rescheduleTask(boulder1, daysAgo = 8)
            rescheduleTask(boulder1, daysAgo = 5)
            rescheduleTask(boulder1, daysAgo = 2)

            val boulder2 = createTask(goalId, "Boulder B (4x)", scheduledDaysAgo = 12, deadlineDaysAgo = 4)
            rescheduleTask(boulder2, daysAgo = 10)
            rescheduleTask(boulder2, daysAgo = 7)
            rescheduleTask(boulder2, daysAgo = 4)
            rescheduleTask(boulder2, daysAgo = 1)

            val boulder3 = createTask(goalId, "Boulder C (5x)", scheduledDaysAgo = 14, deadlineDaysAgo = 3)
            rescheduleTask(boulder3, daysAgo = 12)
            rescheduleTask(boulder3, daysAgo = 9)
            rescheduleTask(boulder3, daysAgo = 6)
            rescheduleTask(boulder3, daysAgo = 3)
            rescheduleTask(boulder3, daysAgo = 1)

            // 2 non-boulders (1 reschedule each — below threshold)
            val light1 = createTask(goalId, "Light A (1x)", scheduledDaysAgo = 5, deadlineDaysAgo = 2)
            rescheduleTask(light1, daysAgo = 3)

            val light2 = createTask(goalId, "Light B (1x)", scheduledDaysAgo = 4, deadlineDaysAgo = 1)
            rescheduleTask(light2, daysAgo = 2)

            _status.value = "Goal #$goalId — 3 boulders (wobble), 2 light reschedules"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 5: Near Deadline — tasks due within 24h
    //
    // Expected: near-deadline amber ring overlay on those tasks
    // ═══════════════════════════════════════════════════════════════════════

    fun createNearDeadline() {
        viewModelScope.launch {
            val goalId = createQaGoal("Near Deadline (4 tasks)")

            // Due today (midnight tonight)
            createTask(goalId, "Due End of Day", scheduledDaysAgo = 0, deadlineDaysAgo = 0)

            // Due tomorrow
            createTask(goalId, "Due Tomorrow", scheduledDaysAgo = 0, deadlineDaysAgo = -1)

            // Due in 2 days (not near yet)
            createTask(goalId, "Due in 2 Days", scheduledDaysAgo = 0, deadlineDaysAgo = -2)

            // No deadline
            createTask(goalId, "No Deadline", scheduledDaysAgo = 0)

            _status.value = "Goal #$goalId — 1 due today, 1 tomorrow, 1 in 2 days, 1 no deadline"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 6: Priority Mix — HIGH/MEDIUM/LOW with varied attention
    //
    // Expected: different orbit distances based on attention,
    // visual hierarchy through planet proximity
    // ═══════════════════════════════════════════════════════════════════════

    fun createPriorityMix() {
        viewModelScope.launch {
            val goalId = createQaGoal("Priority Mix (7 tasks)")

            // HIGH priority + overdue → highest attention
            val high1 = createTask(goalId, "HIGH Overdue", scheduledDaysAgo = 5, deadlineDaysAgo = 3)
            rescheduleTask(high1, daysAgo = 2)

            // HIGH priority + near deadline
            createTask(goalId, "HIGH Due Soon", scheduledDaysAgo = 1, deadlineDaysAgo = 0)

            // HIGH priority + fresh
            createTask(goalId, "HIGH Fresh", scheduledDaysAgo = 0, deadlineDaysAgo = -5)

            // MEDIUM priority + stale
            val med1 = createTask(goalId, "MEDIUM Stale", scheduledDaysAgo = 8, deadlineDaysAgo = 3)
            // no interactions → staleness signal

            // MEDIUM priority + recent
            val med2 = createTask(goalId, "MEDIUM Recent", scheduledDaysAgo = 2, deadlineDaysAgo = -3)
            addNote(med2, "Active work", daysAgo = 0)

            // LOW priority + old
            createTask(goalId, "LOW Old", scheduledDaysAgo = 10, deadlineDaysAgo = 5)

            // LOW priority + fresh
            val low2 = createTask(goalId, "LOW Fresh", scheduledDaysAgo = 0, deadlineDaysAgo = -10)
            addNote(low2, "Just created", daysAgo = 0)

            _status.value = "Goal #$goalId — 3 HIGH, 2 MEDIUM, 2 LOW with varied attention"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 7: Cluster Stress — 25 tasks to trigger clustering
    //
    // Expected: CLUSTERED density mode, cluster nodes visible,
    // expand/collapse interaction works
    // ═══════════════════════════════════════════════════════════════════════

    fun createClusterStress() {
        viewModelScope.launch {
            val goalId = createQaGoal("Cluster Stress (25 tasks)")

            for (i in 1..25) {
                val createdDaysAgo = 20 - (i % 20)
                val taskId = createTask(goalId, "Cluster Task $i", scheduledDaysAgo = createdDaysAgo)

                // Vary interactions to create attention spread
                when {
                    i <= 5 -> addNote(taskId, "Recent work", daysAgo = 0)
                    i <= 10 -> addNote(taskId, "Older work", daysAgo = 5)
                    i <= 15 -> rescheduleTask(taskId, daysAgo = 3)
                    else -> { /* no interaction — stale */ }
                }
            }

            _status.value = "Goal #$goalId — 25 tasks, CLUSTERED density mode"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 8: All Completed — every task done
    //
    // Expected: "تسک فعالی نیست" empty hint, only sun visible
    // ═══════════════════════════════════════════════════════════════════════

    fun createAllCompleted() {
        viewModelScope.launch {
            val goalId = createQaGoal("All Completed (5 tasks)")

            for (i in 1..5) {
                val taskId = createTask(goalId, "Done Task $i", scheduledDaysAgo = 5)
                addNote(taskId, "Finished task $i", daysAgo = 3)
                completeTask(taskId)
            }

            _status.value = "Goal #$goalId — 5 tasks, all completed"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Scenario 9: Activity Rich — images, files, manual activities
    //
    // Expected: meaningful interaction signal from activity_events
    // (not just notes), staleness signal correctly updated
    // ═══════════════════════════════════════════════════════════════════════

    fun createActivityRich() {
        viewModelScope.launch {
            val goalId = createQaGoal("Activity Rich (4 tasks)")

            // Task with image activity
            val imgTask = createTask(goalId, "Image Task", scheduledDaysAgo = 2)
            addActivity(imgTask, "IMAGE_ADDED", daysAgo = 1)
            addActivity(imgTask, "NOTE_ADDED", daysAgo = 0)

            // Task with file activity
            val fileTask = createTask(goalId, "File Task", scheduledDaysAgo = 3)
            addActivity(fileTask, "FILE_ADDED", daysAgo = 2)

            // Task with manual activity
            val manualTask = createTask(goalId, "Manual Task", scheduledDaysAgo = 4)
            addActivity(manualTask, "MANUAL_ACTIVITY", daysAgo = 1)

            // Task with only notes (backward compat)
            val noteTask = createTask(goalId, "Note-Only Task", scheduledDaysAgo = 5)
            addNote(noteTask, "Old note", daysAgo = 4)

            _status.value = "Goal #$goalId — 4 tasks: image, file, manual, note-only"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════════════

    fun clearQaData() {
        viewModelScope.launch {
            generatedGoalIds.forEach { goalId ->
                taskRepository.deleteTasksForGoal(goalId)
                goalRepository.getGoalById(goalId)?.let { goalRepository.deleteGoal(it) }
            }
            generatedGoalIds.clear()
            _status.value = "Cleared all QA goals"
        }
    }

    companion object {
        const val QA_GOAL_PREFIX = "\uD83E\uDDEA QA"
    }
}
