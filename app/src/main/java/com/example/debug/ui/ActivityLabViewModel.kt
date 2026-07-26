package com.example.debug.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityEventRepository
import com.example.plugins.planner.data.ActivityEventType
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskStepEntity
import com.example.plugins.planner.data.TaskStepRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ActivityLabUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val selectedTask: TaskEntity? = null,
    val steps: List<TaskStepEntity> = emptyList(),
    val activities: List<ActivityEventEntity> = emptyList(),
    val stepInput: String = "",
    val status: String? = null,
    val isRunning: Boolean = false
)

class ActivityLabViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val taskStepRepository = TaskStepRepository(database.taskStepDao(), database.activityEventDao())
    private val activityEventRepository = ActivityEventRepository(database.activityEventDao())

    private val _state = MutableStateFlow(ActivityLabUiState())
    val state: StateFlow<ActivityLabUiState> = _state.asStateFlow()

    private var stepsJob: Job? = null
    private var activitiesJob: Job? = null

    init {
        taskDao.getAllTasks()
            .onEach { tasks -> _state.value = _state.value.copy(tasks = tasks) }
            .launchIn(viewModelScope)
    }

    fun selectTask(task: TaskEntity) {
        _state.value = _state.value.copy(
            selectedTask = task,
            status = null
        )
        stepsJob?.cancel()
        activitiesJob?.cancel()
        stepsJob = taskStepRepository.observeSteps(task.id)
            .onEach { steps -> _state.value = _state.value.copy(steps = steps) }
            .launchIn(viewModelScope)
        activitiesJob = activityEventRepository.observeActivities(task.id)
            .onEach { activities -> _state.value = _state.value.copy(activities = activities) }
            .launchIn(viewModelScope)
    }

    fun updateStepInput(input: String) {
        _state.value = _state.value.copy(stepInput = input)
    }

    fun addStep() {
        val task = _state.value.selectedTask ?: return
        val title = _state.value.stepInput.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true)
            try {
                val stepId = taskStepRepository.addStep(
                    TaskStepEntity(taskId = task.id, title = title)
                )
                _state.value = _state.value.copy(
                    stepInput = "",
                    status = "Step created: $title (id=$stepId)",
                    isRunning = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = "Error: ${e.message}",
                    isRunning = false
                )
            }
        }
    }

    fun toggleStepCompletion(step: TaskStepEntity) {
        val task = _state.value.selectedTask ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true)
            try {
                val nowCompleted = !step.isCompleted
                val updated = step.copy(
                    isCompleted = nowCompleted,
                    completedAt = if (nowCompleted) System.currentTimeMillis() else null
                )
                taskStepRepository.updateStep(updated)
                if (nowCompleted) {
                    activityEventRepository.addEvent(
                        ActivityEventEntity(
                            taskId = task.id,
                            stepId = step.id,
                            eventType = ActivityEventType.STEP_COMPLETED.name,
                            description = step.title
                        )
                    )
                }
                _state.value = _state.value.copy(
                    status = if (nowCompleted) "Step completed: ${step.title}" else "Step reopened: ${step.title}",
                    isRunning = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = "Error: ${e.message}",
                    isRunning = false
                )
            }
        }
    }

    fun deleteStep(step: TaskStepEntity) {
        val task = _state.value.selectedTask ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true)
            try {
                activityEventRepository.addEvent(
                    ActivityEventEntity(
                        taskId = task.id,
                        stepId = step.id,
                        eventType = ActivityEventType.STEP_DELETED.name,
                        description = step.title
                    )
                )
                taskStepRepository.deleteStep(step.id)
                _state.value = _state.value.copy(
                    status = "Step deleted: ${step.title}",
                    isRunning = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = "Error: ${e.message}",
                    isRunning = false
                )
            }
        }
    }

    fun runVerification() {
        val task = _state.value.selectedTask ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true, status = "Running verification...")
            val results = mutableListOf<String>()

            try {
                // Scenario 1: Create step
                val testTitle = "Lab Test Step ${System.currentTimeMillis()}"
                val stepId = taskStepRepository.addStep(
                    TaskStepEntity(taskId = task.id, title = testTitle)
                )
                activityEventRepository.addEvent(
                    ActivityEventEntity(
                        taskId = task.id,
                        stepId = stepId,
                        eventType = ActivityEventType.STEP_CREATED.name,
                        description = testTitle
                    )
                )

                val createdStep = taskStepRepository.getStepById(stepId)
                val createdEvent = activityEventRepository.findLatestEvent(task.id, ActivityEventType.STEP_CREATED.name)
                if (createdStep != null && createdStep.title == testTitle && createdEvent != null && createdEvent.stepId == stepId) {
                    results.add("Scenario 1 (Create): PASS — stepId=$stepId")
                } else {
                    results.add("Scenario 1 (Create): FAIL — step or event not found")
                }

                // Scenario 2: Complete step
                val stepBefore = taskStepRepository.getStepById(stepId)
                if (stepBefore != null) {
                    val completedStep = stepBefore.copy(
                        isCompleted = true,
                        completedAt = System.currentTimeMillis()
                    )
                    taskStepRepository.updateStep(completedStep)
                    activityEventRepository.addEvent(
                        ActivityEventEntity(
                            taskId = task.id,
                            stepId = stepId,
                            eventType = ActivityEventType.STEP_COMPLETED.name,
                            description = testTitle
                        )
                    )

                    val stepAfter = taskStepRepository.getStepById(stepId)
                    val completedEvent = activityEventRepository.findLatestEvent(task.id, ActivityEventType.STEP_COMPLETED.name)
                    if (stepAfter != null && stepAfter.isCompleted && stepAfter.completedAt != null && completedEvent != null && completedEvent.stepId == stepId) {
                        results.add("Scenario 2 (Complete): PASS")
                    } else {
                        results.add("Scenario 2 (Complete): FAIL — step state or event mismatch")
                    }
                } else {
                    results.add("Scenario 2 (Complete): FAIL — step not found")
                }

                // Scenario 3: Delete step
                val stepToDelete = taskStepRepository.getStepById(stepId)
                val originalTitle = stepToDelete?.title ?: testTitle
                activityEventRepository.addEvent(
                    ActivityEventEntity(
                        taskId = task.id,
                        stepId = stepId,
                        eventType = ActivityEventType.STEP_DELETED.name,
                        description = originalTitle
                    )
                )
                taskStepRepository.deleteStep(stepId)

                val stepAfterDelete = taskStepRepository.getStepById(stepId)
                val deletedEvent = activityEventRepository.findLatestEvent(task.id, ActivityEventType.STEP_DELETED.name)
                if (stepAfterDelete == null && deletedEvent != null && deletedEvent.description == originalTitle) {
                    results.add("Scenario 3 (Delete): PASS")
                } else {
                    results.add("Scenario 3 (Delete): FAIL — step still exists or event missing")
                }

                // Scenario 4: Persistence — verify step is gone but events remain
                val stepGone = taskStepRepository.getStepById(stepId) == null
                val eventsForStep = activityEventRepository.getEventsByStepId(stepId)
                val hasCreated = eventsForStep.any { it.eventType == ActivityEventType.STEP_CREATED.name }
                val hasCompleted = eventsForStep.any { it.eventType == ActivityEventType.STEP_COMPLETED.name }
                val hasDeleted = eventsForStep.any { it.eventType == ActivityEventType.STEP_DELETED.name }

                if (stepGone && hasCreated && hasCompleted && hasDeleted) {
                    results.add("Scenario 4 (Persistence): PASS — step removed, ${eventsForStep.size} events retained")
                } else if (!stepGone) {
                    results.add("Scenario 4 (Persistence): FAIL — step still exists after deletion")
                } else {
                    results.add("Scenario 4 (Persistence): FAIL — missing events (created=$hasCreated, completed=$hasCompleted, deleted=$hasDeleted)")
                }

                _state.value = _state.value.copy(
                    status = results.joinToString("\n"),
                    isRunning = false
                )
            } catch (e: Exception) {
                results.add("ERROR: ${e.message}")
                _state.value = _state.value.copy(
                    status = results.joinToString("\n"),
                    isRunning = false
                )
            }
        }
    }

    fun clearStatus() {
        _state.value = _state.value.copy(status = null)
    }
}
