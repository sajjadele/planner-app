package com.example.plugins.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.constants.DateConstants
import com.example.plugins.planner.components.AddTaskDialog
import com.example.plugins.planner.components.DaySelector
import com.example.plugins.planner.components.PlannerEmptyState
import com.example.plugins.planner.components.TaskCard
import com.example.plugins.planner.components.WeeklyInsightCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = viewModel(),
    insightViewModel: WeeklyInsightViewModel = viewModel()
) {
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val insightState by insightViewModel.insightState.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val lastCompletedTask by viewModel.lastCompletedTask.collectAsState()

    LaunchedEffect(lastCompletedTask) {
        val task = lastCompletedTask ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "تسک انجام شد",
            actionLabel = "بازگردانی",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastComplete()
        }
    }

    val daysOfWeek = DateConstants.persianDaysOfWeek

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientTop, GradientBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Weekly Insight Card
            WeeklyInsightCard(state = insightState)

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Selection Row
            DaySelector(
                daysOfWeek = DateConstants.persianDaysOfWeek,
                selectedIndex = selectedDayIndex,
                onSelect = { viewModel.selectDay(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section header
            val (currentDayName, _) = daysOfWeek[selectedDayIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "برنامه‌های $currentDayName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                val completedCount = tasks.count { it.isCompleted }
                Text(
                    text = "$completedCount از ${tasks.size} انجام شده",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPurple
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PlannerEmptyState(dayName = currentDayName)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("planner_task_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = AccentPurple,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
                .testTag("add_task_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "افزودن تسک",
                modifier = Modifier.size(24.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onAddTask = { title, priority, hour, minute, goalName, valueTag ->
                    viewModel.addTask(
                        title = title,
                        priority = priority,
                        hour = hour,
                        minute = minute,
                        goalName = goalName,
                        valueTag = valueTag
                    )
                    showAddTaskDialog = false
                }
            )
        }
    }
}
