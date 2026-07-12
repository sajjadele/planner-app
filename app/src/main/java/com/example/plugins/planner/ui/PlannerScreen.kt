package com.example.plugins.planner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.util.toPersianDigits
import com.example.plugins.planner.ui.components.AddTaskDialog
import com.example.plugins.planner.ui.components.CalendarPopup
import com.example.plugins.planner.ui.components.InfiniteWeekRow
import com.example.plugins.planner.ui.components.InsightDetailsSheetContent
import com.example.plugins.planner.ui.components.PlannerEmptyState
import com.example.plugins.planner.ui.components.TaskCard
import com.example.plugins.planner.ui.components.WeeklyInsightCard
import com.example.plugins.planner.ui.components.persianDayIndex
import com.example.core.util.JalaliDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = viewModel(),
    insightViewModel: WeeklyInsightViewModel = viewModel()
) {
    val selectedDateEpochMs by viewModel.selectedDateEpochMs.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val insightState by insightViewModel.insightState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showInsightSheet by remember { mutableStateOf(false) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var showCalendarPopup by remember { mutableStateOf(false) }

    // Hoisted LazyListState for InfiniteWeekRow — enables programmatic scroll-to-week
    val weekRowListState = rememberLazyListState()

    // Navigate to task detail if selected
    selectedTaskId?.let { taskId ->
        TaskDetailScreen(
            taskId = taskId,
            onBack = { selectedTaskId = null }
        )
        return
    }

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

    // ── Calendar Popup ──
    if (showCalendarPopup) {
        CalendarPopup(
            selectedDateEpochMs = selectedDateEpochMs,
            onDateSelected = { dateMs ->
                viewModel.selectDate(dateMs)
                // Scroll the week row to the selected date's week
                scope.launch {
                    val todaySat = com.example.plugins.planner.ui.components.getSaturdayOfWeek(
                        com.example.plugins.planner.ui.components.todayDateEpochMs()
                    )
                    val targetSat = com.example.plugins.planner.ui.components.getSaturdayOfWeek(dateMs)
                    val offset = ((targetSat - todaySat) / (7L * 86400000L)).toInt() + 1000
                    weekRowListState.animateScrollToItem(index = offset)
                }
            },
            onDismiss = { showCalendarPopup = false }
        )
    }

    // ── Modal Bottom Sheet for insight details ──
    if (showInsightSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInsightSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            InsightDetailsSheetContent(state = insightState)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Weekly Insight Card (compact)
            WeeklyInsightCard(
                state = insightState,
                onShowMore = { showInsightSheet = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Infinite Scrolling Week Row + Calendar trigger ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfiniteWeekRow(
                    selectedDateEpochMs = selectedDateEpochMs,
                    onDateSelected = { viewModel.selectDate(it) },
                    listState = weekRowListState,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showCalendarPopup = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "تقویم",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Dynamic contextual header ──
            val dayIndex = persianDayIndex(selectedDateEpochMs)
            val selectedJalali = remember(selectedDateEpochMs) { JalaliDate.fromEpochMs(selectedDateEpochMs) }
            val headerDayName = JalaliDate.DAY_NAMES[dayIndex]
            val gregMonthNames = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")
            val headerGregDate = remember(selectedDateEpochMs) {
                val (gy, gm, gd) = JalaliDate.toGregorian(selectedJalali)
                "${gd} ${gregMonthNames[gm - 1]}"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "برنامه‌های $headerDayName، ${selectedJalali.day} ${JalaliDate.MONTH_NAMES[selectedJalali.month - 1]}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Gregorian segment — forced LTR so "11 July" reads correctly
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = headerGregDate,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PlannerEmptyState(dayName = headerDayName)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("planner_task_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onEdit = { selectedTaskId = task.id }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 8.dp)
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
                activeGoals = viewModel.activeGoals,
                onAddTask = { title, priority, hour, minute, goalId, goalName, valueTag, lifeAreaId ->
                    viewModel.addTask(
                        title = title,
                        priority = priority,
                        hour = hour,
                        minute = minute,
                        goalId = goalId,
                        goalName = goalName,
                        valueTag = valueTag,
                        lifeAreaId = lifeAreaId
                    )
                    showAddTaskDialog = false
                }
            )
        }
    }
}
