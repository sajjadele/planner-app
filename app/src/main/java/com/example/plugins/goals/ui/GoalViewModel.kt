package com.example.plugins.goals.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GoalRepository(database.goalDao())
    }

    /** All active goals — drives the GoalDashboardScreen list */
    val activeGoals: StateFlow<List<GoalEntity>> = repository.getActiveGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** All goals (including completed/archived) — for future stats */
    val allGoals: StateFlow<List<GoalEntity>> = repository.getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addGoal(title: String, description: String?) {
        viewModelScope.launch {
            repository.insertGoal(
                GoalEntity(
                    title = title,
                    description = description,
                    status = "active"
                )
            )
        }
    }

    fun completeGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(
                goal.copy(
                    status = "completed",
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun pauseGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(goal.copy(status = "paused"))
        }
    }

    fun resumeGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(goal.copy(status = "active", completedAt = null))
        }
    }

    fun abandonGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(goal.copy(status = "abandoned"))
        }
    }

    fun updateGoal(goalId: Int, title: String, description: String?) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(
                goal.copy(
                    title = title,
                    description = description
                )
            )
        }
    }

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            repository.deleteGoalById(goalId)
        }
    }
}
