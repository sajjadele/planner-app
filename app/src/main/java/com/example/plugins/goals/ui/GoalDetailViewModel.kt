package com.example.plugins.goals.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalRepository
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.InsightDao
import com.example.plugins.planner.data.TaskDao
import com.example.plugins.planner.data.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalDetailViewModel(
    application: Application,
    private val goalId: Int
) : AndroidViewModel(application) {

    private val goalRepository: GoalRepository
    private val taskDao: TaskDao
    private val insightDao: InsightDao

    init {
        val database = AppDatabase.getDatabase(application)
        goalRepository = GoalRepository(database.goalDao())
        taskDao = database.taskDao()
        insightDao = database.insightDao()
    }

    /** The goal being viewed */
    val goal: StateFlow<GoalEntity?> = goalRepository.observeGoalById(goalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Tasks linked to this goal */
    val tasks: StateFlow<List<TaskEntity>> = taskDao.getTasksByGoalId(goalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Goal-specific completion rate */
    val goalRate: StateFlow<GoalRateResult?> = insightDao.observeGoalCompletionRate(goalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Toggle task completion */
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updated)
        }
    }

    /** Update goal title and description */
    fun updateGoal(title: String, description: String?) {
        viewModelScope.launch {
            val current = goal.value ?: return@launch
            goalRepository.updateGoal(
                current.copy(title = title, description = description)
            )
        }
    }

    companion object {
        fun factory(application: Application, goalId: Int): ViewModelProvider.Factory {
            return object : ViewModelProvider.AndroidViewModelFactory(application) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return GoalDetailViewModel(application, goalId) as T
                }
            }
        }
    }
}
