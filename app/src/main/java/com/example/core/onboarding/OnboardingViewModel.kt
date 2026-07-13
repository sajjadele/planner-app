package com.example.core.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskEventEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface OnboardingStep {
    data object Goal : OnboardingStep
    data object Task : OnboardingStep
}

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val goalDao = database.goalDao()
    private val taskDao = database.taskDao()
    private val taskEventDao = database.taskEventDao()
    private val repo = OnboardingRepository(application)

    private val _step = MutableStateFlow<OnboardingStep>(OnboardingStep.Goal)
    val step: StateFlow<OnboardingStep> = _step

    private val _goalTitle = MutableStateFlow("")
    val goalTitle: StateFlow<String> = _goalTitle

    private val _taskTitle = MutableStateFlow("")
    val taskTitle: StateFlow<String> = _taskTitle

    /**
     * The goal the user explicitly linked in Step 2 (via the picker).
     * Null until they perform the linking micro-action. The submit CTA stays
     * disabled until this is set — teaching that tasks must be bound to a goal.
     */
    private val _selectedGoalTitle = MutableStateFlow<String?>(null)
    val selectedGoalTitle: StateFlow<String?> = _selectedGoalTitle

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    var createdGoalId: Int? = null
        private set

    fun onGoalTitleChange(value: String) { _goalTitle.value = value }
    fun onTaskTitleChange(value: String) { _taskTitle.value = value }

    fun onSelectGoal(title: String?) { _selectedGoalTitle.value = title }

    fun goToTask() {
        if (_goalTitle.value.isNotBlank()) {
            // Reset any prior link so the lesson is re-experienced if the goal changed.
            _selectedGoalTitle.value = null
            _step.value = OnboardingStep.Task
        }
    }

    fun backToGoal() { _step.value = OnboardingStep.Goal }

    /**
     * Sequential writes: Goal -> Task (with FK + created event) -> flip flag LAST.
     * All data is 100% user-created and real; nothing is sample/demo.
     */
    fun finish(onDone: (Int) -> Unit) {
        val goalTitle = _goalTitle.value.trim()
        val taskTitle = _taskTitle.value.trim()
        // Required interaction: the user must have linked the task to the goal.
        if (goalTitle.isBlank() || taskTitle.isBlank() || _selectedGoalTitle.value == null) return

        _isSubmitting.value = true
        viewModelScope.launch {
            val goalId = goalDao.insertGoal(
                GoalEntity(title = goalTitle, status = "active")
            ).toInt()

            val taskId = taskDao.insertTask(
                TaskEntity(
                    title = taskTitle,
                    dateEpochMs = getTodayMidnight(),   // appears on today's planner + under goal
                    goalId = goalId
                )
            )
            taskEventDao.insertEvent(
                TaskEventEntity(taskId = taskId.toInt(), eventType = "created")
            )

            createdGoalId = goalId
            repo.setCompleted()   // flag set ONLY after DB writes succeed
            onDone(goalId)
        }
    }

    private fun getTodayMidnight(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
