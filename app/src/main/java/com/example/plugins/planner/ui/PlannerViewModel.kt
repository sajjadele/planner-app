package com.example.plugins.planner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalStatus
import com.example.core.receiver.ReminderScheduler
import com.example.core.snapshot.RoomSnapshotRepository
import com.example.core.snapshot.SnapshotAggregator
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskEventDao
import com.example.plugins.planner.data.TaskEventEntity
import com.example.plugins.planner.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val taskEventDao: TaskEventDao
    private val snapshotAggregator: SnapshotAggregator
    private val goalDao = AppDatabase.getDatabase(application).goalDao()

    private val _lastCompletedTask = MutableStateFlow<TaskEntity?>(null)
    val lastCompletedTask: StateFlow<TaskEntity?> = _lastCompletedTask

    /** One-shot event used to trigger the completion snackbar exactly once. */
    private val _completionEvents = Channel<TaskEntity>(Channel.BUFFERED)
    val completionEvents = _completionEvents.receiveAsFlow()

    /** Active goals for the Goal Picker in AddTaskDialog */
    val activeGoals: StateFlow<List<GoalEntity>> = goalDao.getActiveGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao(), database.taskEventDao())
        taskEventDao = database.taskEventDao()
        snapshotAggregator = SnapshotAggregator(
            RoomInsightRepository(database.insightDao()),
            RoomSnapshotRepository(database.snapshotDao()),
            com.example.core.goal.RoomGoalRepository(database.goalDao(), database.goalEventDao())
        )
    }

    /** Midnight epoch ms of the currently-selected day (local timezone). */
    private val _selectedDateEpochMs = MutableStateFlow(getTodayDateEpochMs())
    val selectedDateEpochMs: StateFlow<Long> = _selectedDateEpochMs

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> = _selectedDateEpochMs
        .flatMapLatest { date -> repository.getTasksForDay(date) }
        // Flip the loading flag on the FIRST genuine emission (empty OR non-empty) — never on a
        // change. Doing this in `init` via `tasks.drop(1).first()` hangs forever on an empty day,
        // because Room emits exactly one value and nothing changes afterwards, so `drop(1)` waits
        // for a second emission that never arrives and the skeleton stays stuck. `onEach` is
        // replay-safe across movableContentOf re-entry (the ViewModel is built once).
        .onEach { _isTasksLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Explicit loading flag for the task list. `tasks` is a StateFlow seeded with emptyList() and
     * replays its current value on re-subscription, so the UI must NOT infer loading from
     * `tasks.isEmpty()`. This is flipped true by `.onEach` on the `tasks` flow on the FIRST genuine
     * Room emission (empty OR non-empty) — never in an `init { tasks.drop(1).first() }` block, which
     * hangs forever on an empty day (Room emits once, nothing changes, `drop(1)` waits for a second
     * emission that never arrives). Once true it stays true — making the three states
     * (Loading / Loaded+Empty / Loaded+Content) independent and replay-safe.
     */
    private val _isTasksLoaded = MutableStateFlow(false)
    val isTasksLoaded: StateFlow<Boolean> = _isTasksLoaded.asStateFlow()

    /**
     * Sprint 5.2 (F2): `goalTaskGroups` resolves task.goalId → title using the already-loaded
     * [activeGoals] instead of a second full `getAllGoals()` scan. This removes one cold-start Room
     * query. Tasks whose goal is non-active (paused/completed/archived) are kept and rendered under a
     * placeholder "هدف غیرفعال" group (see [groupTasksByGoal]) so they are never dropped — only the
     * header label differs from active goals.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val goalTaskGroups: StateFlow<List<GoalTaskGroup>> = _selectedDateEpochMs
        .flatMapLatest { date ->
            combine(repository.getTasksForDay(date), activeGoals) { dayTasks, goals ->
                groupTasksByGoal(dayTasks, goals)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Set of midnight-epoch day keys (matching [TaskEntity.dateEpochMs]) that have at least one
     * task, within a bounded window (±[TASK_DAY_WINDOW_DAYS] days around today). Powers the
     * "has tasks" dot in the week bar and calendar without loading the full infinite history.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val daysWithTasks: StateFlow<Set<Long>> = run {
        val today = getTodayDateEpochMs()
        val start = today - TASK_DAY_WINDOW_DAYS * DAY_MS
        val end = today + TASK_DAY_WINDOW_DAYS * DAY_MS
        repository.getTaskDayKeysBetween(start, end)
            .map { it.toSet() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )
    }

    /** @param dateEpochMs midnight epoch ms of the day to select */
    fun selectDate(dateEpochMs: Long) {
        _selectedDateEpochMs.value = dateEpochMs
    }

    /** Select a task by ID — navigates to its day and sets selectedTaskId */
    fun selectTask(taskId: Int) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                _selectedDateEpochMs.value = task.dateEpochMs
            }
        }
    }

    fun addTask(title: String, priority: String?, hour: Int?, minute: Int?, goalId: Int?, valueTag: String?, lifeAreaId: Int? = null, dateEpochMs: Long? = null) {
        viewModelScope.launch {
            val actualDateEpochMs = dateEpochMs ?: _selectedDateEpochMs.value
            val task = TaskEntity(
                title = title,
                priority = priority,
                dateEpochMs = actualDateEpochMs,
                reminderHour = hour,
                reminderMinute = minute,
                goalId = goalId,
                valueTag = valueTag,
                lifeAreaId = lifeAreaId
            )
            val generatedId = repository.insertTask(task)

            taskEventDao.insertEvent(
                TaskEventEntity(
                    taskId = generatedId.toInt(),
                    eventType = "created"
                )
            )

            // If reminder scheduled, schedule with updated ID
            if (hour != null && minute != null) {
                val finalTask = task.copy(id = generatedId.toInt())
                ReminderScheduler.schedule(getApplication(), finalTask)
            }
            snapshotAggregator.recordDay(getTodayDateEpochMs())
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updatedTask)

            if (updatedTask.isCompleted) {
                taskEventDao.insertEvent(
                    TaskEventEntity(taskId = updatedTask.id, eventType = "completed")
                )
                ReminderScheduler.cancel(getApplication(), updatedTask)
                _lastCompletedTask.value = updatedTask
                _completionEvents.trySend(updatedTask)
            } else {
                taskEventDao.insertEvent(
                    TaskEventEntity(taskId = updatedTask.id, eventType = "reopened")
                )
                if (updatedTask.reminderHour != null && updatedTask.reminderMinute != null) {
                    ReminderScheduler.schedule(getApplication(), updatedTask)
                }
            }
            snapshotAggregator.recordDay(getTodayDateEpochMs())
        }
    }

    fun undoLastComplete() {
        val completed = _lastCompletedTask.value ?: return
        _lastCompletedTask.value = null
        viewModelScope.launch {
            val reopened = completed.copy(isCompleted = false)
            repository.updateTask(reopened)
            taskEventDao.insertEvent(
                TaskEventEntity(taskId = reopened.id, eventType = "reopened")
            )
            if (reopened.reminderHour != null && reopened.reminderMinute != null) {
                ReminderScheduler.schedule(getApplication(), reopened)
            }
            snapshotAggregator.recordDay(getTodayDateEpochMs())
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskEventDao.insertEvent(
                TaskEventEntity(taskId = task.id, eventType = "deleted")
            )
            repository.deleteTask(task)
            ReminderScheduler.cancel(getApplication(), task)
            snapshotAggregator.recordDay(getTodayDateEpochMs())
        }
    }

    /** @param newDateEpochMs midnight epoch ms of the new scheduled day */
    fun rescheduleTask(task: TaskEntity, newDateEpochMs: Long) {
        viewModelScope.launch {
            val updated = task.copy(dateEpochMs = newDateEpochMs)
            repository.updateTask(updated)
            taskEventDao.insertEvent(
                TaskEventEntity(
                    taskId = task.id,
                    eventType = "rescheduled"
                )
            )
            ReminderScheduler.cancel(getApplication(), task)
            if (updated.reminderHour != null && updated.reminderMinute != null && !updated.isCompleted) {
                ReminderScheduler.schedule(getApplication(), updated)
            }
            snapshotAggregator.recordDay(getTodayDateEpochMs())
        }
    }

    fun changePriority(task: TaskEntity, newPriority: String?) {
        viewModelScope.launch {
            val updated = task.copy(priority = newPriority)
            repository.updateTask(updated)
            taskEventDao.insertEvent(
                TaskEventEntity(
                    taskId = task.id,
                    eventType = "priority_changed"
                )
            )
        }
    }

    /** Midnight epoch ms of today (local timezone, 00:00:00.000). */
    private fun getTodayDateEpochMs(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

/** Bounded window (days each side of today) for the "days with tasks" indicator. */
private const val TASK_DAY_WINDOW_DAYS = 60
private const val DAY_MS = 86_400_000L

/**
 * Pure grouping of tasks by goal. Tasks with a known goal are grouped under that goal; tasks with a
 * null `goalId` land in a trailing "بدون هدف" group (goal == null). Tasks whose `goalId` points to a
 * goal **not present in [goals]** (e.g. a non-active/paused/completed/archived goal, since the caller
 * passes only [activeGoals]) are NOT dropped — they are kept under a placeholder "هدف غیرفعال" goal so
 * the task stays visible. The "بدون هدف" group is omitted entirely when no such tasks exist.
 *
 * Sprint 5.2 (F2): previously a second full `getAllGoals()` scan supplied every status; now the
 * caller reuses [activeGoals], and non-active goals are represented by the placeholder instead of a
 * heavy extra query.
 *
 * Pure Kotlin (no Android/flow) — host-JVM testable.
 */
fun groupTasksByGoal(tasks: List<TaskEntity>, goals: List<GoalEntity>): List<GoalTaskGroup> {
    val goalMap = goals.associateBy { it.id }

    val withGoal = tasks
        .filter { it.goalId != null }
        .groupBy { it.goalId!! }
        .map { (goalId, grouped) ->
            val goal = goalMap[goalId]
                ?: GoalEntity(id = goalId, title = INACTIVE_GOAL_PLACEHOLDER, status = GoalStatus.ARCHIVED)
            GoalTaskGroup(goal, grouped)
        }

    val withoutGoal = tasks.filter { it.goalId == null }

    return if (withoutGoal.isNotEmpty()) {
        withGoal + GoalTaskGroup(null, withoutGoal)
    } else {
        withGoal
    }
}

/** Sprint 5.2 (F2): placeholder goal title for tasks whose goal is non-active (not in [activeGoals]). */
private const val INACTIVE_GOAL_PLACEHOLDER = "هدف غیرفعال"

/**
 * Pure reduction of tasks to the set of distinct midnight-epoch day keys that contain at least one
 * task. Retained for host-JVM testing; the production path now uses the DAO projection
 * [com.example.plugins.planner.data.TaskDao.getTaskDayKeysBetween] (Sprint 4, Phase 4.1).
 */
fun taskDayKeys(tasks: List<TaskEntity>): Set<Long> =
    tasks.map { it.dateEpochMs }.toSet()
