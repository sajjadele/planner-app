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
import com.example.domain.goal.GoalProgress
import com.example.domain.goal.GoalProgressCalculator
import com.example.domain.goal.GoalSort
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.plugins.planner.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalRepository
    private val insightRepository: InsightRepository
    private val taskRepository: TaskRepository

    private val windowDays = 30

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        insightRepository = RoomInsightRepository(database.insightDao())
        taskRepository = TaskRepository(database.taskDao(), database.taskEventDao())
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

    // ── Phase 5.2: Dashboard tabs ──
    private val _selectedTab = MutableStateFlow(GoalStatus.ACTIVE)
    val selectedTab: StateFlow<String> = _selectedTab

    fun selectTab(status: String) {
        _selectedTab.value = status
    }

    /**
     * Goals for the selected tab, sorted by deadline → recent activity → engagement via
     * [GoalSort]. Activity maps are collected from the repository so the sort stays out of Compose.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val goalsByTab: StateFlow<List<GoalEntity>> = _selectedTab
        .flatMapLatest { status ->
            repository.observeGoalsByStatus(status)
                .map { goals -> enrichAndSort(goals) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private suspend fun enrichAndSort(goals: List<GoalEntity>): List<GoalEntity> {
        val lastActivity = goals.associate { it.id to repository.observeGoalLastActivity(it.id).first() }
        val activeDays = goals.associate { it.id to repository.observeGoalActiveDayCount(it.id).first() }
        return GoalSort.sort(goals, lastActivity, activeDays)
    }

    /**
     * Goal progress for the detail/card, combining task completion rate with a 30-day activity
     * momentum via [GoalProgressCalculator]. Calculation lives in the domain layer, not Compose.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun progressFor(goalId: Int): StateFlow<GoalProgress?> {
        val (from, to) = windowBounds()
        return combine(
            insightRepository.observeGoalCompletionRate(goalId),
            repository.observeGoalActiveDayCountInWindow(goalId, from, to)
        ) { rate, activeDaysInWindow ->
            if (rate == null) null
            else GoalProgressCalculator.compute(
                completionRate = rate.completionRate,
                activeDaysInWindow = activeDaysInWindow,
                windowDays = windowDays
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    /** Latest activity timestamp for a goal (used by the card's "last activity" line). */
    fun lastActivityFor(goalId: Int): Flow<Long?> = repository.observeGoalLastActivity(goalId)

    /** Lifetime active-day count for a goal (used by the card's "X روز فعالیت" line). */
    fun activeDaysFor(goalId: Int): Flow<Int> = repository.observeGoalActiveDayCount(goalId)

    private fun windowBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.timeInMillis
        val from = today - (windowDays - 1) * 86_400_000L
        val to = today + 86_400_000L - 1
        return from to to
    }

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
     * Generic status change used by the Goal Detail / card status control. Validation and event
     * logging are handled by [com.example.core.goal.GoalRepository.updateGoalStatus].
     */
    fun changeStatus(goalId: Int, status: String) {
        viewModelScope.launch {
            repository.updateGoalStatus(goalId, status)
        }
    }

    /**
     * Delete a goal together with its related data, so nothing is left as a hidden orphan
     * (product decision: do not keep tasks as Inbox when their goal is deleted).
     * Order:
     *   1. task_events for the goal's tasks
     *   2. the goal's tasks
     *   3. the goal itself (its goal_events cascade-delete via FK)
     * The caller is responsible for showing a confirmation dialog before invoking this.
     */
    fun deleteGoalWithRelated(goalId: Int) {
        viewModelScope.launch {
            taskRepository.deleteEventsForGoal(goalId)
            taskRepository.deleteTasksForGoal(goalId)
            repository.deleteGoalById(goalId)
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

    /** Valid next statuses for a goal, used by the card status submenu. */
    fun validNextStatuses(currentStatus: String): List<String> {
        return GoalStatus.ALL.filter { it != currentStatus && GoalStatus.canTransition(currentStatus, it) }
    }
}
