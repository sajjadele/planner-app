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
import com.example.domain.attention.AttentionProvider
import com.example.domain.attention.AttentionResult
import com.example.domain.attention.TaskAttentionInput
import com.example.domain.graph.TaskMetadata
import com.example.domain.graph.VisibilityInput
import com.example.domain.graph.VisibilityLevel
import com.example.domain.graph.VisibilityResolver
import com.example.domain.graph.VisibleGraphModel
import com.example.domain.mirror.MirrorInsight
import com.example.plugins.goals.GraphViewPreferences
import com.example.plugins.planner.data.GoalRateResult
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.plugins.planner.data.TaskRepository
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskEventEntity
import com.example.core.util.JalaliDate
import com.example.core.receiver.ReminderScheduler
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Performance Sprint 1 — lazy Behavioral Solar System computation.
 *
 * Phase 2B+: [Ready] carries attention-driven [VisibleGraphModel] plus goal metadata
 * (title/progress). Visibility decisions never live in Compose.
 */
sealed interface GraphState {
    data object NotRequested : GraphState
    data object Loading : GraphState
    data class Ready(
        val visibleGraph: VisibleGraphModel,
        val goalTitle: String,
        val goalProgressOverall: Float
    ) : GraphState
    data class Error(val throwable: Throwable) : GraphState
}

/**
 * Performance Sprint 1 — lazy Mirror / Insight aggregation.
 *
 * Mirror `evaluate`/`render` is heavy and only meaningful while the sheet is visible. `Ready` retains
 * the last result as a cache after the sheet closes.
 */
sealed interface MirrorState {
    data object NotRequested : MirrorState
    data object Loading : MirrorState
    data class Ready(val insights: List<MirrorInsight>) : MirrorState
    data class Error(val throwable: Throwable) : MirrorState
}

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

    /** Debounced snapshot refresh — coalesces rapid user actions into one recordDay() call. */
    private var recordDayJob: Job? = null

    private fun recordDayDebounced(dateEpochMs: Long) {
        recordDayJob?.cancel()
        recordDayJob = viewModelScope.launch {
            delay(RECORD_DAY_DEBOUNCE_MS)
            snapshotAggregator.recordDay(dateEpochMs)
        }
    }

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

    /**
     * Phase 3 — full task list for this goal (date-unfiltered). Used by the Solar System graph,
     * Mirror insights, and task preview (which need ALL tasks), plus as the source for the
     * date-filtered [filteredTasks] and the Future-Hint counts.
     */
    private val allTasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksByGoalId(goalId)
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    /**
     * Phase 3 — goal-scoped set of midnight-epoch day keys that have at least one task for THIS goal.
     * Derived from [allTasks] (already filtered by goalId) so the Goal Detail calendar shows markers
     * only for this goal's tasks, not the global Planner set. Reused by all Goal Detail calendar consumers.
     */
    val goalTaskDays: StateFlow<Set<Long>> = allTasks.map { tasks ->
        tasks.mapNotNull { it.dateEpochMs }.toSet()
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptySet())

    /**
     * Phase 3 — selected display date (local-midnight epoch ms). Drives the Goal Detail task
     * timeline: only tasks whose [TaskEntity.dateEpochMs] matches this day are shown.
     * Initialized to today so the screen opens on "امروز".
     */
    private val _selectedTaskDateEpochMs = MutableStateFlow(todayDateEpochMs())
    val selectedTaskDateEpochMs: StateFlow<Long> = _selectedTaskDateEpochMs.asStateFlow()

    fun selectTaskDate(epochMs: Long) {
        _selectedTaskDateEpochMs.value = epochMs
    }

    /**
     * Phase 3 — tasks for the [selectedTaskDateEpochMs] day only. Derived from [allTasks] so
     * graph/mirror keep the full list while the UI shows a single day (default: today).
     */
    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _selectedTaskDateEpochMs
    ) { tasks, date ->
        tasks.filter { it.dateEpochMs == date }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    /**
     * Phase 3 — compact "near future" hint counts for this goal. Non-intrusive nudge showing how
     * many open tasks are scheduled tomorrow and later this week. Click targets are wired but the
     * popup preview is deferred to Phase 4.
     */
    data class FutureHintState(
        val tomorrowCount: Int = 0,
        val thisWeekCount: Int = 0
    )

    val futureHintState: StateFlow<FutureHintState> = allTasks.map { tasks ->
        val today = todayDateEpochMs()
        val tomorrow = today + 86_400_000L
        val saturday = JalaliDate.saturdayOfWeek(today)
        val weekEnd = saturday + 7 * 86_400_000L - 1 // Friday midnight (end of Persian week)
        FutureHintState(
            tomorrowCount = tasks.count { it.dateEpochMs == tomorrow && !it.isCompleted },
            thisWeekCount = tasks.count {
                it.dateEpochMs in (tomorrow + 86_400_000L)..weekEnd && !it.isCompleted
            }
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = FutureHintState())

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

    /**
     * Explicit readiness for [goalProgress]. Independent of [metricsState] — it resolves from the
     * same upstreams (goalRate + activeDaysInWindow) and flips true as soon as they first emit, even
     * when the goal has no tasks (rate == null → progress stays null but IS resolved). This gives the
     * UI a replay-safe loading signal without relying on `progress == null` or emission counting.
     */
    private val _goalProgressLoaded = MutableStateFlow(false)
    val goalProgressLoaded: StateFlow<Boolean> = _goalProgressLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            combine(goalRate, activeDaysInWindow) { _, _ -> }
                .first()
            _goalProgressLoaded.value = true
        }
    }

    /**
     * Active-day count for the card's "X روز فعالیت" line. Sprint 5.3 (F3): bounded to the rolling
     * 30-day window via [activeDaysInWindow] instead of the all-time [observeGoalActiveDayCount] scan,
     * removing one cold-open DB query. The windowed count is already produced for [goalProgress].
     */
    val activeDays: StateFlow<Int> = activeDaysInWindow

    /** Latest activity timestamp (card's "آخرین فعالیت" line). */
    val lastActivity: StateFlow<Long?> = goalRepository.observeGoalLastActivity(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Sprint 4 (Phase 4.3) — metrics-only state for the Goal Detail progress card. `goal` and `tasks`
     * remain independent fast flows (collected directly in the screen) so the header and task list
     * render immediately. `progress` (the combined ring) is intentionally NOT here — it is collected
     * separately (see [goalProgress]) so the card's completion % / active-days / last-activity render
     * independently of the combined ring. Sprint 5.3 (F3): `activeDays` is now the bounded windowed
     * count, not the all-time scan. Graph/Mirror stay separate (lazy, event-driven — see ADR-0012/0013).
     */
    data class GoalDetailMetricsState(
        val completionRate: Float? = null,
        val activeDays: Int = 0,
        val lastActivity: Long? = null,
        val loaded: Boolean = false
    )

    private val _metricsState = MutableStateFlow(GoalDetailMetricsState())
    val metricsState: StateFlow<GoalDetailMetricsState> = _metricsState

    init {
        viewModelScope.launch {
            combine(
                listOf(
                    goalRate,
                    activeDaysInWindow,
                    lastActivity
                )
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                val rate = array[0] as GoalRateResult?
                val days = array[1] as Int
                val last = array[2] as Long?
                GoalDetailMetricsState(
                    completionRate = rate?.completionRate,
                    activeDays = days,
                    lastActivity = last,
                    loaded = true
                )
            }.collect { _metricsState.value = it }
        }
    }

    /**
     * Lazy Mirror / Insight state (Sprint 1). Starts [MirrorState.NotRequested]; becomes
     * [MirrorState.Loading] then [MirrorState.Ready] only when the Mirror sheet is open. The last
     * [MirrorState.Ready] is retained as a cache after the sheet closes.
     */
    private val _mirrorState = MutableStateFlow<MirrorState>(MirrorState.NotRequested)
    val mirrorState: StateFlow<MirrorState> = _mirrorState

    private val _showMirrorSheet = MutableStateFlow(false)
    val showMirrorSheet: StateFlow<Boolean> = _showMirrorSheet

    fun setMirrorSheetVisible(visible: Boolean) {
        _showMirrorSheet.value = visible
        if (visible) refreshMirror() else stopMirrorComputation()
    }

    /** Cancel background Mirror work on close; retain the last [MirrorState.Ready] as a cache. */
    private fun stopMirrorComputation() {
        // No long-running collector for Mirror; nothing to cancel. Cache is retained implicitly.
    }

    // ── Behavioral Solar System graph (Phase 6 / 2B+) ──

    // ── Reactive attention signals (Phase 1) ──
    // rescheduleCountsFlow and meaningfulInteractionsFlow are defined as
    // StateFlow properties near _attentionResults. No one-shot loading needed.

    /**
     * Reactive graph pipeline (Phase 2B+ / Phase 1 reactive signals):
     *
     * allTasks + rescheduleCounts (reactive) + meaningfulInteractions (reactive)
     *   → AttentionProvider (recomputed on every emission)
     *   → VisibilityResolver
     *   → VisibleGraphModel
     *
     * Visibility decisions are never made in GoalGraphBuilder or Compose.
     * All 6 inputs are reactive Flows — no one-shot loading.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun graphSource(): Flow<Triple<VisibleGraphModel, String, Float>> {
        // Nest combines to handle 6 flows with type safety.
        val taskInputs = combine(goal, allTasks, _visibilityLevel) { g, ts, level ->
            Triple(g, ts, level)
        }
        val signalInputs = combine(goalProgress, rescheduleCountsFlow, meaningfulInteractionsFlow) { p, r, i ->
            Triple(p, r, i)
        }

        return combine(taskInputs, signalInputs) { tasks, signals ->
            val (g, ts, level) = tasks
            val (progress, rescheduleCounts, meaningfulInteractions) = signals

            if (g == null) return@combine null

            val nowMillis = System.currentTimeMillis()
            val activeTasks = ts.filter { !it.isCompleted }

            // Reactive attention: recompute on every task list emission while sheet is open.
            val attentionInputs = activeTasks.map { task ->
                TaskAttentionInput(
                    id = task.id,
                    title = task.title,
                    dateEpochMs = task.dateEpochMs,
                    deadlineEpochMs = task.deadlineEpochMs,
                    lastMeaningfulInteractionMs = meaningfulInteractions[task.id],
                    rescheduleCount = rescheduleCounts[task.id] ?: 0
                )
            }
            val attentionLookup = AttentionProvider.compute(attentionInputs, nowMillis)
            _attentionResults.value = attentionLookup

            val taskMetadata = activeTasks.associate { task ->
                task.id to TaskMetadata(
                    id = task.id,
                    title = task.title,
                    priority = task.priority,
                    dateEpochMs = task.dateEpochMs,
                    deadlineEpochMs = task.deadlineEpochMs,
                    rescheduleCount = rescheduleCounts[task.id] ?: 0
                )
            }

            val visibleGraph = VisibilityResolver.resolve(
                VisibilityInput(
                    activeTaskIds = activeTasks.map { it.id },
                    attentionResults = attentionLookup,
                    taskMetadata = taskMetadata,
                    level = level,
                    nowMillis = nowMillis
                )
            )

            Triple(visibleGraph, g.title, progress?.overall ?: 0f)
        }.filterNotNull()
    }

    /**
     * Lazy Behavioral Solar System graph state. Starts as [GraphState.NotRequested]; becomes
     * [GraphState.Loading] then [GraphState.Ready] only while the Graph sheet is open. On sheet close
     * the last [GraphState.Ready] is retained as a cache (NOT reset to [GraphState.NotRequested]).
     */
    private val _goalGraphState = MutableStateFlow<GraphState>(GraphState.NotRequested)
    val goalGraphState: StateFlow<GraphState> = _goalGraphState

    private var graphCollectionJob: kotlinx.coroutines.Job? = null

    private val _showGraphSheet = MutableStateFlow(false)
    val showGraphSheet: StateFlow<Boolean> = _showGraphSheet

    /**
     * Phase 2B — progressive disclosure level owned by the ViewModel.
     * VisibilityResolver receives this level and returns one resolved VisibleGraphModel.
     */
    private val _visibilityLevel = MutableStateFlow(VisibilityLevel.OVERVIEW)
    val visibilityLevel: StateFlow<VisibilityLevel> = _visibilityLevel

    fun setVisibilityLevel(level: VisibilityLevel) {
        _visibilityLevel.value = level
    }

    /**
     * Progressive disclosure from graph chrome ("N more"): OVERVIEW → EXPANDED → INSIGHT.
     * INSIGHT only when active density exceeds the domain cluster threshold. No mode picker.
     */
    fun revealMoreTasks() {
        when (_visibilityLevel.value) {
            VisibilityLevel.OVERVIEW -> {
                _visibilityLevel.value = VisibilityLevel.EXPANDED
                _expandedClusterId.value = null
            }
            VisibilityLevel.EXPANDED -> {
                val activeCount = allTasks.value.count { !it.isCompleted }
                if (activeCount > VisibilityResolver.CLUSTER_THRESHOLD) {
                    _visibilityLevel.value = VisibilityLevel.INSIGHT
                    _expandedClusterId.value = null
                }
            }
            VisibilityLevel.INSIGHT -> Unit
        }
    }

    /**
     * Phase 2B — expanded cluster id owned by ViewModel because it affects visible content.
     * Null means overview (no cluster expanded).
     */
    private val _expandedClusterId = MutableStateFlow<Int?>(null)
    val expandedClusterId: StateFlow<Int?> = _expandedClusterId

    fun setExpandedClusterId(clusterId: Int?) {
        _expandedClusterId.value = clusterId
    }

    /** Toggle cluster expansion in place (re-tap collapses). */
    fun toggleExpandedCluster(clusterId: Int) {
        _expandedClusterId.value =
            if (_expandedClusterId.value == clusterId) null else clusterId
    }

    /**
     * One-shot event: open the read-only task preview popup exactly once per satellite tap.
     * Replay-free Channel exposed as a Flow (per README "One-Shot UI Events") — never a sticky
     * StateFlow, so re-collection (config change) does not re-open the popup.
     */
    private val _taskPreviewEvents = Channel<TaskEntity>(Channel.BUFFERED)
    val taskPreviewEvents = _taskPreviewEvents.receiveAsFlow()

    /** Resolve a tapped satellite id to its full [TaskEntity] and emit a one-shot preview event. */
    fun requestTaskPreview(taskId: Int) {
        val task = allTasks.value.firstOrNull { it.id == taskId } ?: return
        _taskPreviewEvents.trySend(task)
    }

    fun setGraphSheetVisible(visible: Boolean) {
        _showGraphSheet.value = visible
        if (visible) {
            startGraphComputation()
        } else {
            stopGraphComputation()
            _expandedClusterId.value = null
            _visibilityLevel.value = VisibilityLevel.OVERVIEW
        }
    }

    /**
     * Begin lazily collecting [graphSource]. Re-emits on every source change while open so
     * attention + visibility stay live during task toggles, reschedules, and note additions.
     * All attention signals are now reactive — no one-shot loading.
     */
    private fun startGraphComputation() {
        if (graphCollectionJob?.isActive == true) return
        if (_goalGraphState.value !is GraphState.Ready) {
            _goalGraphState.value = GraphState.Loading
        }
        graphCollectionJob = viewModelScope.launch {
            graphSource().collect { (visible, title, progress) ->
                _goalGraphState.value = GraphState.Ready(
                    visibleGraph = visible,
                    goalTitle = title,
                    goalProgressOverall = progress
                )
            }
        }
    }

    // ── Phase 2A: Attention — meaningful interaction timestamps ──
    // Now reactive via meaningfulInteractionsFlow (StateFlow property).

    /**
     * Attention results for this goal's active tasks. Updated reactively while the Graph sheet
     * is open (every allTasks emission). Completed tasks are excluded (completion gate).
     */
    private val _attentionResults = MutableStateFlow<Map<Int, AttentionResult>>(emptyMap())
    val attentionResults: StateFlow<Map<Int, AttentionResult>> = _attentionResults

    // ── Reactive attention signals (Phase 1: stale-signal fix) ──

    /** Reactive reschedule counts — re-emits when task_events change. */
    private val rescheduleCountsFlow: StateFlow<Map<Int, Int>> =
        insightRepository.observeRescheduleCountsByGoal(goalId)
            .map { list -> list.associate { it.taskId to it.rescheduleCount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Reactive meaningful-interaction timestamps — re-emits when notes change. */
    private val meaningfulInteractionsFlow: StateFlow<Map<Int, Long>> =
        insightRepository.observeLastMeaningfulInteractionPerTask(goalId)
            .map { list -> list.associate { it.taskId to it.lastMeaningfulMs } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Stop graph collection on sheet close (Sprint 1). The last [GraphState.Ready] is intentionally
     * kept as a cache — NOT reset to [GraphState.NotRequested] — so re-opening is instant.
     */
    private fun stopGraphComputation() {
        graphCollectionJob?.cancel()
        graphCollectionJob = null
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
        // Sprint 1 (lazy): Mirror is NOT computed on open. It is computed only when the sheet opens
        // (see setMirrorSheetVisible) and while open via the debounced collector below.
        // Phase 5.4 (ADR-0009): recompute Mirror only after the goal's signals settle, not on
        // every intermediate emission (e.g. each task toggle fires goalRate). Debounce avoids
        // running the full Mirror aggregation multiple times per burst. Gated by _showMirrorSheet
        // so it never runs while the Mirror sheet is closed.
        viewModelScope.launch {
            combine(goalRate, allTasks, activeDays) { _, _, _ -> }
                .debounce(250)
                .collect { if (_showMirrorSheet.value) refreshMirror() }
        }
    }

    /**
     * Compute Mirror insights (Sprint 1 lazy). Non-blocking: emits [MirrorState.Loading] then
     * [MirrorState.Ready] (or [MirrorState.Error]). Safe to call only when the sheet is open; callers
     * gate on [_showMirrorSheet].
     */
    fun refreshMirror() {
        if (_showMirrorSheet.value.not() && _mirrorState.value is MirrorState.NotRequested) return
        _mirrorState.value = MirrorState.Loading
        viewModelScope.launch {
            // Sprint 4 (Phase 4.2): the App-Launch Backfill Engine was moved off cold start and is
            // now triggered lazily here — the first time Mirror/Insight is opened. This keeps the
            // historical behavior_snapshot projection current (Mirror reads it) without stalling
            // first-screen queries. Runs on IO so the (potentially large) historical fill never
            // blocks the UI thread. `recordDay(today)` on task events keeps today fresh regardless.
            withContext(Dispatchers.IO) {
                snapshotAggregator.backfillIfNeeded()
            }
            runCatching {
                val signals = mirrorRepository.evaluate(goalId)
                mirrorRepository.render(signals)
            }.onSuccess { insights ->
                _mirrorState.value = MirrorState.Ready(insights)
            }.onFailure { throwable ->
                _mirrorState.value = MirrorState.Error(throwable)
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
            if (_showMirrorSheet.value) refreshMirror()
        }
    }

    fun archiveGoal() {
        viewModelScope.launch {
            goalRepository.archiveGoal(goalId)
            if (_showMirrorSheet.value) refreshMirror()
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskRepository.updateTask(updated)
            taskRepository.insertTaskEvent(
                TaskEventEntity(
                    taskId = updated.id,
                    eventType = if (updated.isCompleted) "completed" else "reopened"
                )
            )
            if (updated.isCompleted) {
                ReminderScheduler.cancel(getApplication(), updated)
            } else if (updated.reminderHour != null && updated.reminderMinute != null) {
                ReminderScheduler.schedule(getApplication(), updated)
            }
            recordDayDebounced(todayDateEpochMs())
            if (_showMirrorSheet.value) refreshMirror()
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
            if (_showMirrorSheet.value) refreshMirror()
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

/** Debounce window for snapshot refresh — coalesces rapid user actions. */
private const val RECORD_DAY_DEBOUNCE_MS = 300L
