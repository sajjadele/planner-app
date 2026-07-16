package com.example.plugins.goals.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalEventEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.GoalStatus
import com.example.core.goal.RoomGoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
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

    fun addGoal(
        title: String,
        description: String?,
        why: String? = null,
        deadlineEpochMs: Long? = null
    ) {
        viewModelScope.launch {
            val id = repository.insertGoal(
                GoalEntity(
                    title = title,
                    description = description,
                    status = GoalStatus.ACTIVE,
                    why = why,
                    deadlineEpochMs = deadlineEpochMs
                )
            )
            repository.insertGoalEvent(
                GoalEventEntity(goalId = id.toInt(), eventType = GoalStatus.ACTIVE)
            )
        }
    }

    fun completeGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(
                goal.copy(
                    status = GoalStatus.COMPLETED,
                    completedAt = System.currentTimeMillis()
                )
            )
            repository.insertGoalEvent(
                GoalEventEntity(goalId = goalId, eventType = GoalStatus.COMPLETED)
            )
        }
    }

    fun pauseGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(goal.copy(status = GoalStatus.PAUSED))
            repository.insertGoalEvent(
                GoalEventEntity(goalId = goalId, eventType = GoalStatus.PAUSED)
            )
        }
    }

    fun resumeGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(goal.copy(status = GoalStatus.ACTIVE, completedAt = null))
            repository.insertGoalEvent(
                GoalEventEntity(goalId = goalId, eventType = GoalStatus.ACTIVE)
            )
        }
    }

    fun abandonGoal(goalId: Int) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(goal.copy(status = GoalStatus.ABANDONED))
            repository.insertGoalEvent(
                GoalEventEntity(goalId = goalId, eventType = GoalStatus.ABANDONED)
            )
        }
    }

    /**
     * Archive a goal (terminal status). Delegates to the repository, which validates the
     * transition (only valid from completed/abandoned) and logs a `archived` lifecycle event.
     */
    fun archiveGoal(goalId: Int) {
        viewModelScope.launch {
            repository.archiveGoal(goalId)
        }
    }

    /**
     * Generic status change used by the Goal Detail status control. Validation and event
     * logging are handled by [com.example.core.goal.GoalRepository.updateGoalStatus].
     */
    fun updateGoalStatus(goalId: Int, status: String) {
        viewModelScope.launch {
            repository.updateGoalStatus(goalId, status)
        }
    }

    fun updateGoal(
        goalId: Int,
        title: String,
        description: String?,
        why: String? = null,
        deadlineEpochMs: Long? = null
    ) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            repository.updateGoal(
                goal.copy(
                    title = title,
                    description = description,
                    why = why,
                    deadlineEpochMs = deadlineEpochMs
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
