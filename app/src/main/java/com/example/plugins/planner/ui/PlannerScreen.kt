package com.example.plugins.planner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.goal.GoalEntity
import com.example.plugins.planner.ui.GoalTaskGroup
import com.example.core.util.isolated
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
    val goalTaskGroups by viewModel.goalTaskGroups.collectAsState()
    val daysWithTasks by viewModel.daysWithTasks.collectAsState()
    val insightState by insightViewModel.insightState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showInsightSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        viewModel.completionEvents.collect { task ->
            val result = snackbarHostState.showSnackbar(
                message = "تسک انجام شد",
                actionLabel = "بازگردانی",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastComplete()
            }
        }
    }

    // ── Calendar Popup ──
    if (showCalendarPopup) {
            CalendarPopup(
                selectedDateEpochMs = selectedDateEpochMs,
                daysWithTasks = daysWithTasks,
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
                    daysWithTasks = daysWithTasks,
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
                            text = "برنامه‌های $headerDayName، ${selectedJalali.day.isolated()} ${JalaliDate.MONTH_NAMES[selectedJalali.month - 1]}",
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
                    goalTaskGroups.forEach { group ->
                        item(key = "header_${group.goal?.id ?: "none"}") {
                            GoalSectionHeader(goal = group.goal)
                        }
                        items(group.tasks, key = { it.id }) { task ->
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
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

    }
}

/**
 * Small goal header for the goal-centric Home list. Shows the goal title (or "بدون هدف" when the
 * group has no goal). Home stays execution-focused — no progress, mirror, or extra metadata.
 */
@Composable
private fun GoalSectionHeader(goal: GoalEntity?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (goal != null) Icons.Default.Flag else Icons.Default.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = goal?.title ?: "بدون هدف",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
