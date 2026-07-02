package com.example.plugins.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.constants.DateConstants
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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
    }

    // Default to today's index (0 = Monday, 6 = Sunday)
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

    fun addTask(title: String, priority: String, hour: Int?, minute: Int?) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                priority = priority,
                dayIndex = _selectedDayIndex.value,
                reminderHour = hour,
                reminderMinute = minute
            )
            val generatedId = repository.insertTask(task)
            
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
            
            // Cancel alarm if task is completed
            if (updatedTask.isCompleted) {
                ReminderScheduler.cancel(getApplication(), updatedTask)
            } else if (updatedTask.reminderHour != null && updatedTask.reminderMinute != null) {
                ReminderScheduler.schedule(getApplication(), updatedTask)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            ReminderScheduler.cancel(getApplication(), task)
        }
    }

    fun getSelectedDayName(): String {
        return DateConstants.persianDayNames.getOrElse(_selectedDayIndex.value) {
            DateConstants.persianDayNames.first()
        }
    }

    private fun getTodayIndex(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }
}
