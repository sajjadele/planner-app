package com.example.plugins.goals.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.GoalStatus
import com.example.core.goal.RoomGoalRepository
import com.example.core.mirror.MirrorRepository
import com.example.core.mirror.RoomMirrorRepository
import com.example.core.snapshot.RoomSnapshotRepository
import com.example.core.snapshot.SnapshotAggregator
import com.example.domain.goal.GoalProgress
import com.example.domain.goal.GoalProgressCalculator
import com.example.domain.graph.GoalGraph
import com.example.domain.graph.GoalGraphBuilder
import com.example.domain.mirror.MirrorInsight
import com.example.plugins.goals.GraphViewPreferences
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.plugins.planner.data.TaskRepository
import com.example.plugins.planner.data.TaskEntity
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class GoalDetailViewModel(
    application: Application,
    private val goalId: Int
) : AndroidViewModel(application) {

    private val goalRepository: GoalRepository
    private val taskRepository: TaskRepository
    private val insightRepository: InsightRepository
    private val snapshotAggregator: SnapshotAggregator
    private val mirrorRepository: MirrorRepository
    private val graphPreferences: GraphViewPreferences

    private val windowDays = 30

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
        graphPreferences = GraphViewPreferences(application)
    }

    val goal: StateFlow<GoalEntity?> = goalRepository.observeGoalById(goalId)
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksByGoalId(goalId)
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val goalRate: StateFlow<GoalRateResult?> = insightRepository.observeGoalCompletionRate(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Rolling-window mutual activity momentum for progress (Phase 5.2 weighting). */
    private val activeDaysInWindow: StateFlow<Int> = run {
        val (from, to) = windowBounds()
        goalRepository.observeGoalActiveDayCountInWindow(goalId, from, to)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    /** Combined progress: 70% task completion + 30% 30-day window momentum. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val goalProgress: StateFlow<GoalProgress?> = combine(goalRate, activeDaysInWindow) { rate, days ->
        if (rate == null) null
        else GoalProgressCalculator.compute(
            completionRate = rate.completionRate,
            activeDaysInWindow = days,
            windowDays = windowDays
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Lifetime active-day count (card's "X روز فعالیت" line). */
    val activeDays: StateFlow<Int> = goalRepository.observeGoalActiveDayCount(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Latest activity timestamp (card's "آخرین فعالیت" line). */
    val lastActivity: StateFlow<Long?> = goalRepository.observeGoalLastActivity(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _mirrorInsights = MutableStateFlow<List<MirrorInsight>>(emptyList())
    val mirrorInsights: StateFlow<List<MirrorInsight>> = _mirrorInsights

    private val _showMirrorSheet = MutableStateFlow(false)
    val showMirrorSheet: StateFlow<Boolean> = _showMirrorSheet

    fun setMirrorSheetVisible(visible: Boolean) {
        _showMirrorSheet.value = visible
    }

    // ── Behavioral Solar System graph (Phase 6) ──

    /** Live reschedule counts for this goal's tasks (drives the "Boulder" flag). */
    private val rescheduleCounts: StateFlow<Map<Int, Int>> =
        insightRepository.observeRescheduleCountsByGoal(goalId)
            .map { list -> list.associate { it.taskId to it.rescheduleCount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Deterministic Behavioral Solar System graph for this goal, computed on demand from existing
     * reactive sources. Null until the goal loads. UI renders this; it never queries DAOs directly.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val goalGraph: StateFlow<GoalGraph?> = combine(goal, tasks, rescheduleCounts, goalProgress) { g, ts, counts, progress ->
        if (g == null) null
        else withContext(Dispatchers.Default) {
            GoalGraphBuilder.build(
                goalId = g.id,
                goalTitle = g.title,
                tasks = ts.map { task ->
                    GoalGraphBuilder.TaskInput(
                        id = task.id,
                        title = task.title,
                        priority = task.priority,
                        isCompleted = task.isCompleted,
                        deadlineEpochMs = task.deadlineEpochMs
                    )
                },
                rescheduleCounts = counts,
                progress = progress
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showGraphSheet = MutableStateFlow(false)
    val showGraphSheet: StateFlow<Boolean> = _showGraphSheet

    /**
     * One-shot event: open the read-only task preview popup exactly once per satellite tap.
     * Replay-free Channel exposed as a Flow (per README "One-Shot UI Events") — never a sticky
     * StateFlow, so re-collection (config change) does not re-open the popup.
     */
    private val _taskPreviewEvents = Channel<TaskEntity>(Channel.BUFFERED)
    val taskPreviewEvents = _taskPreviewEvents.receiveAsFlow()

    /** Resolve a tapped satellite id to its full [TaskEntity] and emit a one-shot preview event. */
    fun requestTaskPreview(taskId: Int) {
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return
        _taskPreviewEvents.trySend(task)
    }

    fun setGraphSheetVisible(visible: Boolean) {
        _showGraphSheet.value = visible
    }

    /**
     * Phase 5.5 — first-time Graph education gate. True only while the sheet is open AND the user
     * has not yet seen the introduction, so the legend auto-shows on first open and never again.
     * Driven entirely by ViewModel state (survives tab teardown).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val showGraphEducation: StateFlow<Boolean> = combine(showGraphSheet, graphPreferences.hasSeenIntroduction) { open, seen ->
        open && !seen
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Persist that the Graph introduction has been seen (called when the education dialog closes). */
    fun markGraphIntroductionSeen() {
        viewModelScope.launch { graphPreferences.markIntroductionSeen() }
    }

    init {
        refreshMirror()
        // Phase 5.4 (ADR-0009): recompute Mirror only after the goal's signals settle, not on
        // every intermediate emission (e.g. each task toggle fires goalRate). Debounce avoids
        // running the full Mirror aggregation multiple times per burst. Result is identical.
        viewModelScope.launch {
            combine(goalRate, tasks, activeDays) { _, _, _ -> }
                .debounce(250)
                .collect { refreshMirror() }
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

    // ── Status management (Phase 5.3) ──

    /** Valid next statuses for the current goal, derived from GoalStatus.canTransition. */
    fun validNextStatuses(currentStatus: String): List<String> {
        return GoalStatus.ALL.filter { it != currentStatus && GoalStatus.canTransition(currentStatus, it) }
    }

    fun changeStatus(status: String) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(goalId, status)
            refreshMirror()
        }
    }

    fun archiveGoal() {
        viewModelScope.launch {
            goalRepository.archiveGoal(goalId)
            refreshMirror()
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

    fun updateGoal(
        title: String,
        description: String?,
        why: String? = null,
        deadlineEpochMs: Long? = null
    ) {
        viewModelScope.launch {
            val current = goal.value ?: return@launch
            goalRepository.updateGoal(
                current.copy(
                    title = title,
                    description = description,
                    why = why ?: current.why,
                    deadlineEpochMs = deadlineEpochMs ?: current.deadlineEpochMs
                )
            )
            refreshMirror()
        }
    }

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

    private fun todayDateEpochMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
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
