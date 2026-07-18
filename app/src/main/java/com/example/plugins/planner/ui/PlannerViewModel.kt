package com.example.plugins.planner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** All goals (any status) used to resolve task.goalId → GoalEntity for Home grouping. */
    private val allGoals: StateFlow<List<GoalEntity>> = goalDao.getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Today's (selected day's) tasks grouped by their owning goal. Tasks without a goal land in a
     * trailing "بدون هدف" group (goal == null). Pure grouping via [groupTasksByGoal].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val goalTaskGroups: StateFlow<List<GoalTaskGroup>> = _selectedDateEpochMs
        .flatMapLatest { date ->
            combine(repository.getTasksForDay(date), allGoals) { dayTasks, goals ->
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
        repository.getTasksBetween(start, end)
            .map { taskDayKeys(it) }
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

    fun addTask(title: String, priority: String?, hour: Int?, minute: Int?, goalId: Int?, valueTag: String?, lifeAreaId: Int? = null, deadlineEpochMs: Long? = null) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                priority = priority,
                dateEpochMs = _selectedDateEpochMs.value,
                reminderHour = hour,
                reminderMinute = minute,
                goalId = goalId,
                valueTag = valueTag,
                lifeAreaId = lifeAreaId,
                deadlineEpochMs = deadlineEpochMs
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
 * Pure grouping of tasks by goal. Tasks with a known goal are grouped under that goal; tasks whose
 * `goalId` is null (or whose goal is missing from [goals]) are collected into a trailing "بدون هدف"
 * group (goal == null). The "بدون هدف" group is omitted entirely when no such tasks exist.
 *
 * Pure Kotlin (no Android/flow) — host-JVM testable.
 */
fun groupTasksByGoal(tasks: List<TaskEntity>, goals: List<GoalEntity>): List<GoalTaskGroup> {
    val goalMap = goals.associateBy { it.id }

    val withGoal = tasks
        .filter { it.goalId != null }
        .groupBy { it.goalId!! }
        .mapNotNull { (goalId, grouped) -> goalMap[goalId]?.let { GoalTaskGroup(it, grouped) } }

    val withoutGoal = tasks.filter { it.goalId == null }

    return if (withoutGoal.isNotEmpty()) {
        withGoal + GoalTaskGroup(null, withoutGoal)
    } else {
        withGoal
    }
}

/**
 * Pure reduction of tasks to the set of distinct midnight-epoch day keys that contain at least one
 * task. Uses [TaskEntity.dateEpochMs] directly (already midnight-aligned). Pure Kotlin — host-JVM
 * testable.
 */
fun taskDayKeys(tasks: List<TaskEntity>): Set<Long> =
    tasks.map { it.dateEpochMs }.toSet()
