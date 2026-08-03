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
    data object Welcome : OnboardingStep
    data object Goal : OnboardingStep
    data object Task : OnboardingStep
    data object Future : OnboardingStep
}

/**
 * 4-step Goal-first onboarding. Steps are presentation-only; the only
 * persistence is [finish], which commits Goal + Task together on the final CTA.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val goalDao = database.goalDao()
    private val taskDao = database.taskDao()
    private val taskEventDao = database.taskEventDao()
    private val repo = OnboardingRepository(application)

    private val ORDER = listOf(
        OnboardingStep.Welcome,
        OnboardingStep.Goal,
        OnboardingStep.Task,
        OnboardingStep.Future
    )

    private val _step = MutableStateFlow<OnboardingStep>(OnboardingStep.Welcome)
    val step: StateFlow<OnboardingStep> = _step

    private val _goalTitle = MutableStateFlow("")
    val goalTitle: StateFlow<String> = _goalTitle

    private val _taskTitle = MutableStateFlow("")
    val taskTitle: StateFlow<String> = _taskTitle

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    var createdGoalId: Int? = null
        private set

    fun onGoalTitleChange(value: String) { _goalTitle.value = value }
    fun onTaskTitleChange(value: String) { _taskTitle.value = value }

    /** Forward transition. Only advances when the current step's input is valid. */
    fun next() {
        val i = ORDER.indexOf(_step.value)
        if (i < 0 || i >= ORDER.lastIndex) return
        val valid = when (_step.value) {
            OnboardingStep.Goal -> _goalTitle.value.isNotBlank()
            OnboardingStep.Task -> _taskTitle.value.isNotBlank()
            else -> true
        }
        if (valid) _step.value = ORDER[i + 1]
    }

    /** Back navigation; Welcome is the root and has no previous step. */
    fun back() {
        val i = ORDER.indexOf(_step.value)
        if (i > 0) _step.value = ORDER[i - 1]
    }

    /**
     * Sequential writes: Goal -> Task (with FK + created event) -> flip flag LAST.
     * The task is ALWAYS bound to the created goal — the relationship is shown by
     * the UI (animated connector), not selected by the user.
     */
    fun finish(onDone: (Int) -> Unit) {
        val goalTitle = _goalTitle.value.trim()
        val taskTitle = _taskTitle.value.trim()
        if (goalTitle.isBlank() || taskTitle.isBlank()) return

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
