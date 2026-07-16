package com.example.plugins.goals.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.core.mirror.MirrorRepository
import com.example.core.mirror.RoomMirrorRepository
import com.example.core.snapshot.RoomSnapshotRepository
import com.example.core.snapshot.SnapshotAggregator
import com.example.domain.mirror.MirrorInsight
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.plugins.planner.data.TaskRepository
import com.example.plugins.planner.data.TaskEntity
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val mirrorRepository: MirrorRepository

    init {
        val database = AppDatabase.getDatabase(application)
        goalRepository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        taskRepository = TaskRepository(database.taskDao(), database.taskEventDao())
        insightRepository = RoomInsightRepository(database.insightDao())
        snapshotAggregator = SnapshotAggregator(
            insightRepository,
            RoomSnapshotRepository(database.snapshotDao()),
            goalRepository
        )
        mirrorRepository = RoomMirrorRepository(
            insightRepository,
            goalRepository,
            RoomSnapshotRepository(database.snapshotDao())
        )
    }

    val goal: StateFlow<GoalEntity?> = goalRepository.observeGoalById(goalId)
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksByGoalId(goalId)
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val goalRate: StateFlow<GoalRateResult?> = insightRepository.observeGoalCompletionRate(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _mirrorInsights = MutableStateFlow<List<MirrorInsight>>(emptyList())
    val mirrorInsights: StateFlow<List<MirrorInsight>> = _mirrorInsights

    init {
        refreshMirror()
        viewModelScope.launch {
            goalRate.collect { refreshMirror() }
        }
    }

    fun refreshMirror() {
        viewModelScope.launch {
            runCatching {
                val signals = mirrorRepository.evaluate(goalId)
                _mirrorInsights.value = mirrorRepository.render(signals)
            }
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskRepository.updateTask(updated)
            snapshotAggregator.recordDay(todayDateEpochMs())
            refreshMirror()
        }
    }

    fun updateGoal(title: String, description: String?) {
        viewModelScope.launch {
            val current = goal.value ?: return@launch
            goalRepository.updateGoal(current.copy(title = title, description = description))
            refreshMirror()
        }
    }

    private fun todayDateEpochMs(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
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
