package com.example.plugins.goals.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.core.snapshot.RoomSnapshotRepository
import com.example.core.snapshot.SnapshotAggregator
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.plugins.planner.data.TaskRepository
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
    private val taskRepository: TaskRepository
    private val insightRepository: InsightRepository
    private val snapshotAggregator: SnapshotAggregator

    init {
        val database = AppDatabase.getDatabase(application)
        goalRepository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        taskRepository = TaskRepository(database.taskDao())
        insightRepository = RoomInsightRepository(database.insightDao())
        snapshotAggregator = SnapshotAggregator(
            insightRepository,
            RoomSnapshotRepository(database.snapshotDao()),
            goalRepository
        )
    }

    /** The goal being viewed */
    val goal: StateFlow<GoalEntity?> = goalRepository.observeGoalById(goalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Tasks linked to this goal */
    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksByGoalId(goalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Goal-specific completion rate */
    val goalRate: StateFlow<GoalRateResult?> = insightRepository.observeGoalCompletionRate(goalId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Toggle task completion */
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskRepository.updateTask(updated)
            snapshotAggregator.recordDay(todayDateEpochMs())
        }
    }

    /** Midnight epoch ms of today (local timezone) — anchors the real-time snapshot upsert. */
    private fun todayDateEpochMs(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
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
