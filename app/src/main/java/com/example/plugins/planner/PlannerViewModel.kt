package com.example.plugins.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.receiver.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val taskEventDao: TaskEventDao

    private val _lastCompletedTask = MutableStateFlow<TaskEntity?>(null)
    val lastCompletedTask: StateFlow<TaskEntity?> = _lastCompletedTask

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        taskEventDao = database.taskEventDao()
    }

    // Default to today's index (0 = Saturday/شنبه, 6 = Friday/جمعه)
    private val _selectedDayIndex = MutableStateFlow(getTodayIndex())
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> = _selectedDayIndex
        .flatMapLatest { day -> repository.getTasksForDay(day) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectDay(index: Int) {
        _selectedDayIndex.value = index
    }

    fun addTask(title: String, priority: String?, hour: Int?, minute: Int?, goalName: String?, valueTag: String?) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                priority = priority,
                dayIndex = _selectedDayIndex.value,
                reminderHour = hour,
                reminderMinute = minute,
                goalName = goalName,
                valueTag = valueTag
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
            } else {
                taskEventDao.insertEvent(
                    TaskEventEntity(taskId = updatedTask.id, eventType = "reopened")
                )
                if (updatedTask.reminderHour != null && updatedTask.reminderMinute != null) {
                    ReminderScheduler.schedule(getApplication(), updatedTask)
                }
            }
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
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskEventDao.insertEvent(
                TaskEventEntity(taskId = task.id, eventType = "deleted")
            )
            repository.deleteTask(task)
            ReminderScheduler.cancel(getApplication(), task)
        }
    }

    private fun getTodayIndex(): Int {
        val calendar = Calendar.getInstance()
        // Persian week: Saturday=0, Sunday=1, Monday=2, Tuesday=3, Wednesday=4, Thursday=5, Friday=6
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }
}
