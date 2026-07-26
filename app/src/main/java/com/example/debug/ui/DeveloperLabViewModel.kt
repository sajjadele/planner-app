package com.example.debug.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.core.snapshot.RoomSnapshotRepository
import com.example.core.snapshot.SnapshotAggregator
import com.example.core.snapshot.SnapshotRepository
import com.example.debug.scenarios.GeneratedScenarioResult
import com.example.debug.scenarios.ScenarioGenerator
import com.example.debug.scenarios.ScenarioType
import com.example.debug.validation.ScenarioValidationRunner
import com.example.debug.validation.ValidationResult
import com.example.plugins.notes.data.NoteRepository
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.RoomInsightRepository
import com.example.plugins.planner.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeveloperLabUiState(
    val isRunning: Boolean = false,
    val message: String? = null,
    val lastGenerated: GeneratedScenarioResult? = null,
    val validationResult: ValidationResult? = null,
    val validationScenario: ScenarioType? = null
)

class DeveloperLabViewModel(application: Application) : AndroidViewModel(application) {

    private val goalRepository: GoalRepository
    private val taskRepository: TaskRepository
    private val insightRepository: InsightRepository
    private val noteRepository: NoteRepository
    private val snapshotRepository: SnapshotRepository
    private val snapshotAggregator: SnapshotAggregator
    private val scenarioGenerator: ScenarioGenerator
    private val validationRunner: ScenarioValidationRunner

    private val _state = MutableStateFlow(DeveloperLabUiState())
    val state: StateFlow<DeveloperLabUiState> = _state.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        goalRepository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        taskRepository = TaskRepository(database.taskDao(), database.taskEventDao())
        insightRepository = RoomInsightRepository(database.insightDao())
        noteRepository = NoteRepository(database.noteDao())
        snapshotRepository = RoomSnapshotRepository(database.snapshotDao())
        snapshotAggregator = SnapshotAggregator(insightRepository, snapshotRepository, goalRepository)
        scenarioGenerator = ScenarioGenerator(goalRepository, taskRepository, noteRepository, snapshotAggregator)
        validationRunner = ScenarioValidationRunner(
            scenarioGenerator, taskRepository, insightRepository, goalRepository, snapshotRepository
        )
    }

    fun generate(scenario: ScenarioType) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true, message = "Generating ${scenario.label}...", validationResult = null)
            try {
                val result = scenarioGenerator.generate(scenario)
                _state.value = _state.value.copy(
                    isRunning = false,
                    lastGenerated = result,
                    message = "Generated: ${result.description}"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    message = "Error: ${e.message}"
                )
            }
        }
    }

    fun validate(scenario: ScenarioType) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true, message = "Validating ${scenario.label}...")
            try {
                val result = validationRunner.validate(scenario)
                _state.value = _state.value.copy(
                    isRunning = false,
                    validationResult = result,
                    validationScenario = scenario,
                    message = "Validation: ${if (result.passed) "PASS" else "FAIL"}"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    message = "Validation error: ${e.message}"
                )
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRunning = true, message = "Clearing all developer data...")
            try {
                scenarioGenerator.clearGeneratedScenarios()
                _state.value = DeveloperLabUiState(
                    message = "All developer test data cleared."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    message = "Cleanup error: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}