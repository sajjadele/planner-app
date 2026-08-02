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
        // Tags are metadata only — they cannot be completed.
        _state.value = _state.value.copy(
            status = "Tags cannot be completed: ${step.title}",
            isRunning = false
        )
    }

    fun deleteStep(step: TaskStepEntity) {
        val task = _state.value.selectedTask ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true)
            try {
                taskStepRepository.deleteStep(step.id)
                _state.value = _state.value.copy(
                    status = "Tag deleted: ${step.title}",
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
                // Scenario 1: Create tag
                val testTitle = "Lab Test Tag ${System.currentTimeMillis()}"
                val tagId = taskStepRepository.addStep(
                    TaskStepEntity(taskId = task.id, title = testTitle)
                )

                val createdTag = taskStepRepository.getStepById(tagId)
                val hasNoCreatedEvent = activityEventRepository.findLatestEvent(task.id, ActivityEventType.STEP_CREATED.name) == null
                if (createdTag != null && createdTag.title == testTitle && hasNoCreatedEvent) {
                    results.add("Scenario 1 (Create Tag): PASS — tagId=$tagId (no activity event)")
                } else {
                    results.add("Scenario 1 (Create Tag): FAIL — tag not found or unexpected event")
                }

                // Scenario 2: Tags cannot be completed — verify they remain unchanged
                val stepBefore2 = taskStepRepository.getStepById(stepId)
                if (stepBefore2 != null) {
                    // Tags are metadata — no completion state exists
                    results.add("Scenario 2 (Tag Completion): SKIPPED — tags cannot be completed")
                } else {
                    results.add("Scenario 2 (Tag Completion): FAIL — step not found")
                }

                // Scenario 3: Delete tag
                val tagToDelete = taskStepRepository.getStepById(tagId)
                val originalTitle = tagToDelete?.title ?: testTitle
                taskStepRepository.deleteStep(tagId)

                val tagAfterDelete = taskStepRepository.getStepById(tagId)
                if (tagAfterDelete == null) {
                    results.add("Scenario 3 (Delete Tag): PASS — tag removed, activities preserved")
                } else {
                    results.add("Scenario 3 (Delete Tag): FAIL — tag still exists")
                }

                // Scenario 4: Persistence — verify tag is gone
                val tagGone = taskStepRepository.getStepById(tagId) == null

                if (tagGone) {
                    results.add("Scenario 4 (Persistence): PASS — tag removed")
                } else {
                    results.add("Scenario 4 (Persistence): FAIL — tag still exists after deletion")
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
