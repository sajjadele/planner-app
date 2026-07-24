package com.example.ui.screens.components

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalEventEntity
import com.example.core.goal.GoalRepository
import com.example.core.goal.RoomGoalRepository
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskEventEntity
import com.example.plugins.planner.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Developer-only tool (gated by BuildConfig.DEBUG) for Solar System QA.
 * Creates REAL goals and tasks through the normal repositories.
 * Every goal is prefixed with [QA_GOAL_PREFIX] for cleanup.
 */
class SolarSystemQaViewModel(application: Application) : AndroidViewModel(application) {

    private val goalRepository: GoalRepository
    private val taskRepository: TaskRepository

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val generatedGoalIds = mutableListOf<Int>()

    init {
        val database = AppDatabase.getDatabase(application)
        goalRepository = RoomGoalRepository(database.goalDao(), database.goalEventDao())
        taskRepository = TaskRepository(database.taskDao(), database.taskEventDao())
    }

    private fun midnightDaysAgo(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun timestampDaysAgo(days: Int): Long =
        System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000

    private suspend fun createQaGoal(title: String, ageDays: Int = 1): Int {
        val goal = GoalEntity(
            title = "${QA_GOAL_PREFIX} $title",
            status = "active",
            createdAt = timestampDaysAgo(ageDays)
        )
        val id = goalRepository.insertGoal(goal).toInt()
        goalRepository.insertGoalEvent(
            GoalEventEntity(goalId = id, eventType = "created", timestamp = timestampDaysAgo(ageDays))
        )
        generatedGoalIds += id
        return id
    }

    fun createScenarioA() {
        // Scenario A: Simple Goal (5 active tasks)
        viewModelScope.launch {
            val goalId = createQaGoal("Scenario A (5 tasks)")
            repeat(5) { i ->
                taskRepository.insertTask(
                    TaskEntity(
                        title = "Task A-$i",
                        dateEpochMs = midnightDaysAgo(1),
                        timestamp = timestampDaysAgo(1),
                        goalId = goalId
                    )
                )
            }
            _status.value = "Created goal #$goalId with 5 active tasks"
        }
    }

    fun createScenarioB() {
        // Scenario B: Expanded Goal (20 active tasks)
        viewModelScope.launch {
            val goalId = createQaGoal("Scenario B (20 tasks)")
            repeat(20) { i ->
                taskRepository.insertTask(
                    TaskEntity(
                        title = "Task B-$i",
                        dateEpochMs = midnightDaysAgo(1),
                        timestamp = timestampDaysAgo(1),
                        goalId = goalId
                    )
                )
            }
            _status.value = "Created goal #$goalId with 20 active tasks"
        }
    }

    fun createScenarioC() {
        // Scenario C: Insight Goal (50+ active tasks)
        viewModelScope.launch {
            val goalId = createQaGoal("Scenario C (50 tasks)")
            repeat(50) { i ->
                taskRepository.insertTask(
                    TaskEntity(
                        title = "Task C-$i",
                        dateEpochMs = midnightDaysAgo(1),
                        timestamp = timestampDaysAgo(1),
                        goalId = goalId
                    )
                )
            }
            // Add overdue, rescheduled, completed for realism
            val overdueTask = taskRepository.insertTask(
                TaskEntity(
                    title = "Overdue Task",
                    dateEpochMs = midnightDaysAgo(3),
                    timestamp = timestampDaysAgo(3),
                    goalId = goalId
                )
            ).toInt()
            taskRepository.insertTaskEvent(
                TaskEventEntity(taskId = overdueTask, eventType = "rescheduled", timestamp = timestampDaysAgo(2))
            )
            _status.value = "Created goal #$goalId with 50+ active tasks (overdue + rescheduled)"
        }
    }

    fun createEmptyGoal() {
        // Empty state: 0 active tasks
        viewModelScope.launch {
            val goalId = createQaGoal("Empty Goal")
            _status.value = "Created goal #$goalId with 0 active tasks"
        }
    }

    fun clearQaData() {
        viewModelScope.launch {
            generatedGoalIds.forEach { goalId ->
                taskRepository.deleteTasksForGoal(goalId)
                goalRepository.getGoalById(goalId)?.let { goalRepository.deleteGoal(it) }
            }
            generatedGoalIds.clear()
            _status.value = "Cleared all QA goals"
        }
    }

    companion object {
        const val QA_GOAL_PREFIX = "🧪 QA"
    }
}

@Composable
fun SolarSystemQaDialog(
    onDismiss: () -> Unit,
    viewModel: SolarSystemQaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val status by viewModel.status.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Solar System QA Tool",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Developer-only — BuildConfig.DEBUG",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val scenarios = listOf(
                    "Scenario A: 5 tasks" to { viewModel.createScenarioA() },
                    "Scenario B: 20 tasks" to { viewModel.createScenarioB() },
                    "Scenario C: 50+ tasks" to { viewModel.createScenarioC() },
                    "Empty Goal: 0 tasks" to { viewModel.createEmptyGoal() }
                )

                scenarios.forEach { (label, action) ->
                    Button(
                        onClick = action,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }

                Button(
                    onClick = { viewModel.clearQaData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear QA Data", color = Color.White, fontWeight = FontWeight.Medium)
                }

                status?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}