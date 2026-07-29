package com.example.plugins.planner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.core.receiver.ReminderScheduler
import com.example.core.util.JalaliDate
import com.example.plugins.notes.data.NoteEntity
import com.example.plugins.notes.data.NoteRepository
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.ActivityDraftResolver
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityCreationContext
import com.example.plugins.planner.data.ActivityMessageMapper
import com.example.plugins.planner.data.ActivityMessageModel
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityEventRepository
import com.example.plugins.planner.data.ActivityEventType
import com.example.plugins.planner.data.ActivityFeedFilterState
import com.example.plugins.planner.data.ImageEventParser
import com.example.plugins.planner.data.CreateStepUseCase
import com.example.plugins.planner.data.StepDraft
import com.example.plugins.planner.data.StepDraftResolver
import com.example.plugins.planner.data.TaskDao
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskStepEntity
import com.example.plugins.planner.data.TaskStepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    application: Application,
    private val taskId: Int
) : AndroidViewModel(application) {

    private val taskDao: TaskDao
    private val goalRepository: GoalRepository
    private val noteRepository: NoteRepository
    private val taskStepRepository: TaskStepRepository
    private val activityEventRepository: ActivityEventRepository
    private val createStepUseCase: CreateStepUseCase

    init {
        val database = AppDatabase.getDatabase(application)
        taskDao = database.taskDao()
        goalRepository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        noteRepository = NoteRepository(database.noteDao())
        taskStepRepository = TaskStepRepository(database.taskStepDao(), database.activityEventDao())
        activityEventRepository = ActivityEventRepository(database.activityEventDao())
        createStepUseCase = CreateStepUseCase(database)
    }

    /** Loading gate: true once the first Room emission arrives for this task. */
    private val _isTaskLoaded = MutableStateFlow(false)
    val isTaskLoaded: StateFlow<Boolean> = _isTaskLoaded.asStateFlow()

    /** The task being viewed — reactive */
    val task: StateFlow<TaskEntity?> = taskDao.observeTaskById(taskId)
        .onEach { _isTaskLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Active goals for the goal reassignment dropdown */
    val activeGoals: StateFlow<List<GoalEntity>> = goalRepository.getActiveGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** All goals — used to resolve the title of a currently-linked goal
     *  (which may be non-active, e.g. paused/completed). */
    val allGoals: StateFlow<List<GoalEntity>> = goalRepository.getAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Notes/logs linked to this task — most recent first */
    val taskLogs: StateFlow<List<NoteEntity>> = noteRepository.getNotesByTaskId(taskId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Steps for this task — ordered by creation time */
    val steps: StateFlow<List<TaskStepEntity>> = taskStepRepository.observeSteps(taskId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Activity timeline for this task — most recent first */
    val activities: StateFlow<List<ActivityEventEntity>> = activityEventRepository.observeActivities(taskId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Pre-mapped activity messages — system events filtered out */
    val activityMessages: StateFlow<List<ActivityMessageModel>> = activities
        .map { ActivityMessageMapper.toMessages(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ════════════════════════════════════════════════════════════════
    // Phase 5.2.2: Activity Feed Filtering
    // ════════════════════════════════════════════════════════════════

    /** Current filter state for the activity feed */
    private val _filterState = MutableStateFlow(ActivityFeedFilterState.DEFAULT)
    val filterState: StateFlow<ActivityFeedFilterState> = _filterState.asStateFlow()

    /** Filtered activity messages — combines raw messages with current filter (tag + date) */
    val filteredActivityMessages: StateFlow<List<ActivityMessageModel>> = combine(
        activityMessages, _filterState, _selectedActivityDate
    ) { messages, filter, selectedDate ->
        applyFilter(messages, filter, selectedDate)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Apply the current filter to a list of messages.
     *
     * - No filter (همه): shows everything
     * - Step filter: only messages matching stepId
     * - Date filter: only messages on the selected day (epoch ms, normalized)
     * - Image/file filters: for future use
     */
    private fun applyFilter(
        messages: List<ActivityMessageModel>,
        filter: ActivityFeedFilterState,
        selectedDate: Long?
    ): List<ActivityMessageModel> {
        var result = messages

        // Step filter
        if (filter.selectedStepId != null) {
            result = result.filter { it.stepId == filter.selectedStepId }
        }

        // Date filter — match messages whose createdAt falls on the same Jalali day
        if (selectedDate != null) {
            result = result.filter { msg ->
                normalizeToDayStart(msg.createdAt) == selectedDate
            }
        }

        // Media type filters (future-ready)
        if (filter.showImagesOnly) {
            result = result.filter { it.attachments.any { a -> a is ActivityAttachment.Image } }
        }
        if (filter.showFilesOnly) {
            result = result.filter { it.attachments.any { a -> a is ActivityAttachment.File } }
        }

        return result
    }

    /** Set the filter to show all activities */
    fun showAllActivities() {
        _filterState.value = _filterState.value.copy(selectedStepId = null)
    }

    /** Filter activities by step ID */
    fun filterByStep(stepId: Long) {
        _filterState.value = _filterState.value.copy(selectedStepId = stepId)
    }

    /** Set image-only filter */
    fun setImageFilter(enabled: Boolean) {
        _filterState.value = _filterState.value.copy(
            showImagesOnly = enabled,
            showFilesOnly = if (enabled) false else _filterState.value.showFilesOnly
        )
    }

    /** Set file-only filter */
    fun setFileFilter(enabled: Boolean) {
        _filterState.value = _filterState.value.copy(
            showFilesOnly = enabled,
            showImagesOnly = if (enabled) false else _filterState.value.showImagesOnly
        )
    }

    /** Check if a step filter is currently active */
    fun isStepFilterActive(stepId: Long): Boolean =
        _filterState.value.selectedStepId == stepId

    /** Check if "همه" filter is active */
    fun isAllFilterActive(): Boolean =
        _filterState.value.selectedStepId == null

    // ════════════════════════════════════════════════════════════════
    // Phase 5.5b: Context-Aware Activity Creation
    // ════════════════════════════════════════════════════════════════

    /** Current creation context derived from the active filter state. */
    val creationContext: StateFlow<ActivityCreationContext> = _filterState
        .map { filter ->
            if (filter.selectedStepId != null) {
                val step = steps.value.find { it.id.toLong() == filter.selectedStepId }
                ActivityCreationContext(
                    stepId = filter.selectedStepId,
                    stepName = step?.title
                )
            } else {
                ActivityCreationContext.DEFAULT
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActivityCreationContext.DEFAULT
        )

    /** Timeline navigation range — today's start in epoch ms */
    private val _timelineStartDate = MutableStateFlow(JalaliDate.toEpochMs(JalaliDate.today()))
    val timelineStartDate: StateFlow<Long> = _timelineStartDate.asStateFlow()

    /** Timeline navigation range — task date normalized to day start */
    private val _timelineEndDate = MutableStateFlow(_timelineStartDate.value)
    val timelineEndDate: StateFlow<Long> = _timelineEndDate.asStateFlow()

    /** Currently selected date in the Activity timeline — null means "show all dates". */
    private val _selectedActivityDate = MutableStateFlow<Long?>(null)
    val selectedActivityDate: StateFlow<Long?> = _selectedActivityDate.asStateFlow()

    private fun normalizeToDayStart(epochMs: Long): Long =
        JalaliDate.toEpochMs(JalaliDate.fromEpochMs(epochMs))

    private fun clampToTimelineRange(date: Long): Long {
        val end = _timelineEndDate.value
        return maxOf(_timelineStartDate.value, minOf(date, end))
    }

    init {
        viewModelScope.launch {
            task.collect { t ->
                if (t != null) {
                    val end = normalizeToDayStart(t.dateEpochMs)
                    _timelineEndDate.value = end
                    _selectedActivityDate.value?.let { sel ->
                        _selectedActivityDate.value = clampToTimelineRange(sel)
                    }
                }
            }
        }
    }

    fun selectActivityDate(dateEpochMs: Long) {
        _selectedActivityDate.value = clampToTimelineRange(dateEpochMs)
    }

    /** Clear the date filter — show all activities regardless of date. */
    fun clearDateFilter() {
        _selectedActivityDate.value = null
    }

    fun goToToday() {
        _selectedActivityDate.value = _timelineStartDate.value
    }

    fun moveActivityDate(days: Int) {
        val current = _selectedActivityDate.value
        val next = current + days * DAY_MILLIS
        _selectedActivityDate.value = clampToTimelineRange(next)
    }

    /** Update task title only — leaves goal/reassignment fields untouched */
    fun updateTitle(title: String) {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            val updated = current.copy(title = title)
            taskDao.updateTask(updated)
        }
    }

    /** Update goal assignment — goalId is the only source of truth (both null = unlinked) */
    fun updateTaskGoal(goalId: Int?) {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            taskDao.updateTask(current.copy(goalId = goalId))
        }
    }

    /** Update task title (goal linkage is changed via updateTaskGoal) */
    fun updateTask(title: String? = null) {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            taskDao.updateTask(current.copy(title = title ?: current.title))
        }
    }

    /** Add a progress log (note) tied to this task */
    fun addTaskLog(content: String) {
        viewModelScope.launch {
            noteRepository.insert(
                NoteEntity(
                    content = content,
                    taskId = taskId
                )
            )
        }
    }

    // ════════════════════════════════════════════════════════════════
    // NEW: Unified Activity Creation (Phase 4.7.1)
    // ════════════════════════════════════════════════════════════════

    /**
     * Create an activity in the current context (Phase 5.5b).
     *
     * Derives stepId from the active filter/tag selection.
     * - Tag filter active → new activity inherits that tag
     * - All/No filter → task-level activity (stepId = null)
     *
     * The Composer remains step-agnostic — it never sees stepId.
     * This is the primary entry point for UI creation flows.
     */
    fun createActivity(draft: ActivityDraft) {
        val contextStepId = _filterState.value.selectedStepId?.toInt()
        createActivity(draft, contextStepId)
    }

    /**
     * Create an activity from an ActivityDraft.
     * Accepts optional explicit stepId for internal use.
     *
     * Phase 4.10.1: Domain Separation
     * - ActivityDraft no longer has intent or stepId
     * - stepId is passed as a separate parameter
     * - This method ONLY creates activities, not steps
     */
    fun createActivity(draft: ActivityDraft, stepId: Int? = null) {
        viewModelScope.launch {
            val eventType = ActivityDraftResolver.resolveEventType(draft)
            val description = ActivityDraftResolver.encodeDescription(draft)

            activityEventRepository.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = stepId,
                    eventType = eventType.name,
                    description = description
                )
            )
        }
    }

    /**
     * Create a Tag (دسته/برچسب) — standalone tag creation.
     *
     * Phase 5.9.2: Dedicated Tag Creation UX.
     * Phase 5.9.3: Optional colorHex for tag display.
     * Creates TaskStepEntity + STEP_CREATED event only.
     * No ActivityEventEntity for activities — tags are metadata only.
     */
    fun createTag(name: String, colorHex: String? = null) {
        viewModelScope.launch {
            createStepUseCase.execute(taskId, StepDraft(title = name, colorHex = colorHex))
        }
    }

    /**
     * Rename an existing tag (Step). Only title changed — color preserved.
     */
    fun renameStep(step: TaskStepEntity, newTitle: String) {
        viewModelScope.launch {
            val updated = step.copy(title = newTitle)
            taskStepRepository.updateStep(updated)
        }
    }

    /**
     * Create a Step (Tag) from a StepDraft.
     *
     * Phase 5.5d: Pure tag creation — no initial activities.
     * Creates TaskStepEntity + STEP_CREATED event only.
     */
    fun createStep(draft: StepDraft) {
        viewModelScope.launch {
            createStepUseCase.execute(taskId, draft)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Phase 5.1: Activity Message Interactions (Edit/Delete/Reply)
    // ════════════════════════════════════════════════════════════════

    /**
     * Delete an activity message by its ID.
     * Removes the entity from the database.
     * Flow automatically updates the UI.
     */
    fun deleteActivity(messageId: Long) {
        viewModelScope.launch {
            activityEventRepository.deleteEvent(messageId)
        }
    }

    /**
     * Update an existing activity message with new content.
     * Updates the description field of the existing entity.
     * Event type and timestamp are preserved.
     */
    fun updateActivity(messageId: Long, draft: ActivityDraft) {
        viewModelScope.launch {
            val entity = activityEventRepository.getEventById(messageId) ?: return@launch
            val description = ActivityDraftResolver.encodeDescription(draft)
            val updated = entity.copy(description = description)
            activityEventRepository.updateEvent(updated)
        }
    }

    /**
     * Get a single activity message model by entity ID.
     * Used for pre-filling the composer during edit.
     */
    suspend fun getActivityById(messageId: Long): ActivityMessageModel? {
        val entity = activityEventRepository.getEventById(messageId) ?: return null
        return ActivityMessageMapper.toMessage(entity)
    }

    // ════════════════════════════════════════════════════════════════
    // LEGACY: Keep for backward compatibility (can be removed later)
    // ════════════════════════════════════════════════════════════════

    /** Create a step — STEP_CREATED event is handled by the repository */
    @Deprecated(
        "Step creation now uses createStep(StepDraft) via composer. " +
        "Plain addStep creates orphan steps with no UI path.",
        replaceWith = ReplaceWith("createStep(StepDraft(title = title))")
    )
    fun addStep(title: String) {
        viewModelScope.launch {
            taskStepRepository.addStep(
                TaskStepEntity(taskId = taskId, title = title)
            )
        }
    }

    /** Create a note — NOTE_ADDED event, independent of any step */
    @Deprecated(
        "Use createActivity(ActivityDraft(text = ...)) instead. " +
        "This method does not support the context-aware creation flow.",
        replaceWith = ReplaceWith("createActivity(ActivityDraft(text = text))")
    )
    fun addNote(text: String) {
        viewModelScope.launch {
            activityEventRepository.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = null,
                    eventType = ActivityEventType.NOTE_ADDED.name,
                    description = text
                )
            )
        }
    }

    /** Create a manual activity — MANUAL_ACTIVITY event, independent of any step */
    @Deprecated(
        "Use createActivity(ActivityDraft(text = ..., durationMinutes = ...)) instead. " +
        "This method does not support the context-aware creation flow.",
        replaceWith = ReplaceWith(
            "createActivity(ActivityDraft(text = title, durationMinutes = durationMinutes))"
        )
    )
    fun addManualActivity(title: String, durationMinutes: Int?) {
        viewModelScope.launch {
            val description = if (durationMinutes != null) {
                "$title|$durationMinutes"
            } else {
                title
            }
            activityEventRepository.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = null,
                    eventType = ActivityEventType.MANUAL_ACTIVITY.name,
                    description = description
                )
            )
        }
    }

    /** Create an image activity — IMAGE_ADDED event, independent of any step */
    @Deprecated(
        "Use createActivity(ActivityDraft(attachments = listOf(ActivityAttachment.Image(uri)))) instead. " +
        "This method does not support the context-aware creation flow.",
        replaceWith = ReplaceWith(
            "createActivity(ActivityDraft(attachments = listOf(ActivityAttachment.Image(uri))))"
        )
    )
    fun addImage(uri: String, description: String?) {
        viewModelScope.launch {
            activityEventRepository.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = null,
                    eventType = ActivityEventType.IMAGE_ADDED.name,
                    description = ImageEventParser.encode(uri, description)
                )
            )
        }
    }

    /** Toggle step completion status and log the activity event */
    @Deprecated(
        "Step completion as container behavior is removed. " +
        "Steps are now tags/metadata only. " +
        "Use the Activity Feed for tracking progress.",
        level = DeprecationLevel.WARNING
    )
    fun toggleStepCompletion(step: TaskStepEntity) {
        viewModelScope.launch {
            val nowCompleted = !step.isCompleted
            val updated = step.copy(
                isCompleted = nowCompleted,
                completedAt = if (nowCompleted) System.currentTimeMillis() else null
            )
            taskStepRepository.updateStep(updated)

            val eventType = if (nowCompleted)
                ActivityEventType.STEP_COMPLETED
            else
                ActivityEventType.STEP_REOPENED

            activityEventRepository.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = step.id,
                    eventType = eventType.name,
                    description = step.title
                )
            )
        }
    }

    /** Delete a step and log the activity event */
    @Deprecated(
        "Step deletion as container behavior is removed. " +
        "Steps are now tags/metadata only. " +
        "Use the Activity Feed for managing activities.",
        level = DeprecationLevel.WARNING
    )
    fun deleteStep(step: TaskStepEntity) {
        viewModelScope.launch {
            activityEventRepository.addEvent(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = step.id,
                    eventType = ActivityEventType.STEP_DELETED.name,
                    description = step.title
                )
            )
            taskStepRepository.deleteStep(step.id)
        }
    }

    /** Toggle task completion */
    fun toggleTaskCompletion() {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            val updated = current.copy(isCompleted = !current.isCompleted)
            taskDao.updateTask(updated)
            if (updated.isCompleted) {
                ReminderScheduler.cancel(getApplication(), current)
            } else if (updated.reminderHour != null && updated.reminderMinute != null) {
                ReminderScheduler.schedule(getApplication(), updated)
            }
        }
    }

    /** Set or update the reminder time for this task */
    fun setReminder(hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            val shouldSchedule = !current.isCompleted
            val updated = current.copy(reminderHour = hour, reminderMinute = minute)
            taskDao.updateTask(updated)
            if (shouldSchedule) {
                ReminderScheduler.cancel(getApplication(), current)
                ReminderScheduler.schedule(getApplication(), updated)
            }
        }
    }

    /** Remove the reminder from this task and cancel any scheduled alarm */
    fun clearReminder() {
        viewModelScope.launch {
            val current = task.value ?: return@launch
            ReminderScheduler.cancel(getApplication(), current)
            val updated = current.copy(reminderHour = null, reminderMinute = null)
            taskDao.updateTask(updated)
        }
    }

    companion object {
        private const val DAY_MILLIS = 86_400_000L

        fun factory(application: Application, taskId: Int): ViewModelProvider.Factory {
            return object : ViewModelProvider.AndroidViewModelFactory(application) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return TaskDetailViewModel(application, taskId) as T
                }
            }
        }
    }
}
