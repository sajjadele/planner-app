package com.example.plugins.planner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.core.receiver.ReminderScheduler
import com.example.plugins.notes.data.NoteEntity
import com.example.plugins.notes.data.NoteRepository
import com.example.plugins.planner.data.TaskDao
import com.example.plugins.planner.data.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    application: Application,
    private val taskId: Int
) : AndroidViewModel(application) {

    private val taskDao: TaskDao
    private val goalRepository: GoalRepository
    private val noteRepository: NoteRepository

    init {
        val database = AppDatabase.getDatabase(application)
        taskDao = database.taskDao()
        goalRepository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        noteRepository = NoteRepository(database.noteDao())
    }

    /** The task being viewed — reactive */
    val task: StateFlow<TaskEntity?> = taskDao.observeTaskById(taskId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Active goals for the goal reassignment dropdown */
    val activeGoals: StateFlow<List<GoalEntity>> = goalRepository.getActiveGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** All goals — used to resolve the title of a currently-linked goal
     *  (which may be non-active, e.g. paused/completed). */
    val allGoals: StateFlow<List<GoalEntity>> = goalRepository.getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Notes/logs linked to this task — most recent first */
    val taskLogs: StateFlow<List<NoteEntity>> = noteRepository.getNotesByTaskId(taskId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Update task title only — leaves goal/reassignment fields untouched */
    fun updateTitle(title: String) {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            val updated = current.copy(title = title)
            taskDao.updateTask(updated)
        }
    }

    /** Update goal assignment — goalId is the only source of truth (both null = unlinked) */
    fun updateTaskGoal(goalId: Int?) {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            taskDao.updateTask(current.copy(goalId = goalId))
        }
    }

    /** Update task title (goal linkage is changed via updateTaskGoal) */
    fun updateTask(title: String? = null) {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            taskDao.updateTask(current.copy(title = title ?: current.title))
        }
    }

    /** Add a progress log (note) tied to this task */
    fun addTaskLog(content: String) {
        viewModelScope.launch {
            noteRepository.insert(
                NoteEntity(
                    content = content,
                    taskId = taskId
                )
            )
        }
    }

    /** Toggle task completion */
    fun toggleTaskCompletion() {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            val updated = current.copy(isCompleted = !current.isCompleted)
            taskDao.updateTask(updated)
            if (updated.isCompleted) {
                ReminderScheduler.cancel(getApplication(), current)
            } else if (updated.reminderHour != null && updated.reminderMinute != null) {
                ReminderScheduler.schedule(getApplication(), updated)
            }
        }
    }

    /** Set or update the reminder time for this task */
    fun setReminder(hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            val shouldSchedule = !current.isCompleted
            val updated = current.copy(reminderHour = hour, reminderMinute = minute)
            taskDao.updateTask(updated)
            if (shouldSchedule) {
                ReminderScheduler.cancel(getApplication(), current)
                ReminderScheduler.schedule(getApplication(), updated)
            }
        }
    }

    /** Remove the reminder from this task and cancel any scheduled alarm */
    fun clearReminder() {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            ReminderScheduler.cancel(getApplication(), current)
            val updated = current.copy(reminderHour = null, reminderMinute = null)
            taskDao.updateTask(updated)
        }
    }

    companion object {
        fun factory(application: Application, taskId: Int): ViewModelProvider.Factory {
            return object : ViewModelProvider.AndroidViewModelFactory(application) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return TaskDetailViewModel(application, taskId) as T
                }
            }
        }
    }
}
