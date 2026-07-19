package com.example.plugins.goals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalStatus
import com.example.core.util.RTL
import com.example.core.util.toEnglishDigits
import com.example.domain.goal.GoalActivityFormatter
import com.example.domain.mirror.MirrorInsight
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.ui.PlannerViewModel
import com.example.plugins.planner.ui.components.AddTaskDialog
import com.example.plugins.planner.ui.components.NeumorphicSurface
import com.example.ui.onboarding.pressScale
import com.example.ui.screens.components.VisionMenuItem
import com.example.ui.screens.components.VisionPopupMenu
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isOnboarding: Boolean = false,
    viewModel: GoalDetailViewModel = viewModel(
        key = "goal_detail_$goalId",
        factory = GoalDetailViewModel.factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
            goalId
        )
    )
) {
    // Sprint 4 (Phase 4.3): split loading — `goal` (PK read) and `tasks` (goal tasks) are fast,
    // independent flows so the header + task list render immediately; the slower progress/activity
    // metrics arrive separately via `metricsState` and populate the progress card a moment later.
    val goal by viewModel.goal.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    // Sprint 5.3 (F3): the combined progress ring is collected separately from the rest of the
    // metrics, so the card's completion % / active-days / last-activity render as soon as their
    // (independent) queries resolve, without waiting on the combined ring.
    val metrics by viewModel.metricsState.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState()
    val goalRate = metrics.completionRate
    val activeDays = metrics.activeDays
    val lastActivity = metrics.lastActivity
    val mirrorState by viewModel.mirrorState.collectAsState()
    val showMirrorSheet by viewModel.showMirrorSheet.collectAsState()
    val goalGraphState by viewModel.goalGraphState.collectAsState()
    val showGraphSheet by viewModel.showGraphSheet.collectAsState()
    val showGraphEducation by viewModel.showGraphEducation.collectAsState()

    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var previewTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showHomeHint by remember { mutableStateOf(isOnboarding) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // PlannerViewModel for the existing task-creation flow (reused, no new architecture).
    val plannerViewModel: PlannerViewModel = viewModel()
    val daysWithTasks by plannerViewModel.daysWithTasks.collectAsState()

    // One-shot: open the read-only task preview popup exactly once per satellite tap (replay-free).
    LaunchedEffect(Unit) {
        viewModel.taskPreviewEvents.collect { previewTask = it }
    }

    // Navigate to task detail if selected
    selectedTaskId?.let { taskId ->
        com.example.plugins.planner.ui.TaskDetailScreen(
            taskId = taskId,
            onBack = { selectedTaskId = null }
        )
        return
    }

    val g = goal
    if (showEditGoalDialog && g != null) {
        EditGoalDialog(
            goal = g,
            onDismiss = { showEditGoalDialog = false },
            daysWithTasks = daysWithTasks,
            onUpdateGoal = { title, description, why, deadlineEpochMs ->
                viewModel.updateGoal(title, description, why, deadlineEpochMs)
                showEditGoalDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            activeGoals = plannerViewModel.activeGoals,
            initialGoalId = goalId,
            daysWithTasks = daysWithTasks,
            onAddTask = { title, priority, hour, minute, gId, valueTag, lifeAreaId, deadlineEpochMs ->
                plannerViewModel.addTask(
                    title = title,
                    priority = priority,
                    hour = hour,
                    minute = minute,
                    goalId = gId,
                    valueTag = valueTag,
                    lifeAreaId = lifeAreaId,
                    deadlineEpochMs = deadlineEpochMs
                )
                showAddTaskDialog = false
            }
        )
    }

    // ── Mirror bottom sheet (user-controlled) ──
    if (showMirrorSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setMirrorSheetVisible(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val insights = (mirrorState as? com.example.plugins.goals.ui.MirrorState.Ready)?.insights
            if (insights != null) {
                MirrorSheetContent(insights = insights)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // ── Behavioral Solar System graph sheet (user-controlled) ──
    if (showGraphSheet) {
        val graph = (goalGraphState as? com.example.plugins.goals.ui.GraphState.Ready)?.graph
        if (graph != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setGraphSheetVisible(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            GoalGraphSheetContent(
                graph = graph,
                autoShowEducation = showGraphEducation,
                onEducationDismissed = { viewModel.markGraphIntroductionSeen() },
                onTaskTap = { viewModel.requestTaskPreview(it) }
            )
        }
        }
    }

    // ── Read-only task preview popup (satellite tap) ──
    previewTask?.let { task ->
        TaskPreviewDialog(
            task = task,
            onDismiss = { previewTask = null }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // ── Onboarding home hint (dismissible) ──
            if (showHomeHint) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${RTL}بازگشت به خانه",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.weight(1f))
                            val homeHintInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            TextButton(
                                onClick = { showHomeHint = false; onBack() },
                                interactionSource = homeHintInteraction,
                                modifier = Modifier.pressScale(homeHintInteraction)
                            ) {
                                Text("بازگشت", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // ── Top bar: back + title + Mirror icon ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = goal?.title ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Mirror trigger (IconButton; architecture ready for a future unread badge)
                IconButton(
                    onClick = { viewModel.setMirrorSheetVisible(true) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                        contentDescription = "بازتاب هدف",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Behavioral Solar System graph trigger
                IconButton(
                    onClick = { viewModel.setGraphSheetVisible(true) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoGraph,
                        contentDescription = "منظومه رفتاری",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (goal != null) {
                    IconButton(onClick = { showEditGoalDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش هدف",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                }
            }

            // ── Identity block ──
            if (g != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    // Status chip (clickable → valid transitions only)
                    StatusChip(
                        status = g.status,
                        onClick = { showStatusMenu = true }
                    )

                    g.deadlineEpochMs?.let { dl ->
                        val jalali = com.example.core.util.JalaliDate.fromEpochMs(dl)
                        Text(
                            text = "مهلت: ${RTL}${jalali.day.toEnglishDigits()} ${com.example.core.util.JalaliDate.MONTH_NAMES[jalali.month - 1]}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Status dropdown — only valid transitions from GoalStatus.canTransition
                    VisionPopupMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        val valid = viewModel.validNextStatuses(g.status)
                        if (valid.isEmpty()) {
                            VisionMenuItem(
                                text = "بدون تغییر وضعیت",
                                enabled = false,
                                onClick = { showStatusMenu = false }
                            )
                        }
                        valid.forEach { next ->
                            VisionMenuItem(
                                text = statusLabel(next),
                                onClick = {
                                    showStatusMenu = false
                                    viewModel.changeStatus(next)
                                }
                            )
                        }
                    }
                }
                }
            }

            // ── Progress overview card ──
            item {
                GoalProgressCard(
                    progress = goalProgress,
                    completionRate = goalRate,
                    activeDays = activeDays,
                    lastActivity = lastActivity
                )
            }

            // ── Collapsible metadata ──
            if (g != null && (!g.why.isNullOrBlank() || g.deadlineEpochMs != null)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMetadata = !showMetadata }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جزئیات بیشتر",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showMetadata) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (showMetadata) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        g.why?.takeIf { it.isNotBlank() }?.let { why ->
                            Text("چرا این هدف مهم است؟", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(why, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        g.deadlineEpochMs?.let { dl ->
                            val jalali = com.example.core.util.JalaliDate.fromEpochMs(dl)
                            Text("مهلت", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${RTL}${jalali.day.toEnglishDigits()} ${com.example.core.util.JalaliDate.MONTH_NAMES[jalali.month - 1]}",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                }
            }

            // ── Tasks section ──
            item {
                Text(
                    text = "${RTL}کارهای این هدف",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📝", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "هنوز کاری برای این هدف تعریف نکردی",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddTaskDialog = true }) {
                                Text("افزودن Task")
                            }
                        }
                    }
                }
            } else {
                items(
                    tasks,
                    key = { it.id },
                    contentType = { "task" }
                ) { task ->
                    // Sprint 3 (Phase 3.3): remember row callbacks so they are not recreated on every
                    // LazyColumn recomposition; only when the keyed task changes.
                    val onToggle = remember(task.id) { { viewModel.toggleTaskCompletion(task) } }
                    val onEdit = remember(task.id) { { selectedTaskId = task.id } }
                    val onDelete = remember(task.id) { { plannerViewModel.deleteTask(task) } }
                    GoalDetailTaskRow(
                        task = task,
                        onToggle = onToggle,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String, onClick: () -> Unit) {
    val (label, color) = statusLabel(status) to statusColor(status)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun GoalProgressCard(
    progress: com.example.domain.goal.GoalProgress?,
    completionRate: Float?,
    activeDays: Int,
    lastActivity: Long?
) {
    val overall = progress?.overall ?: 0f
    val momentum = progress?.activityMomentum ?: 0f
    val completion = completionRate ?: 0f

    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 6
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Hero: overall progress ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${RTL}پیشرفت کلی",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${RTL}${overall.toInt().toEnglishDigits()}%",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { overall / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Metric cards: completion + momentum ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.TaskAlt,
                    iconTint = AccentGreen,
                    value = "${RTL}${completion.toInt().toEnglishDigits()}%",
                    label = "تکمیل تسک‌ها"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.LocalFireDepartment,
                    iconTint = Color(0xFFF97316),
                    value = "${RTL}${momentum.toInt().toEnglishDigits()}%",
                    label = "ریتم فعالیت"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Recent Activity section ──
            if (activeDays > 0 || GoalActivityFormatter.formatLastActivity(lastActivity).isNotBlank()) {
                Text(
                    text = "${RTL}فعالیت اخیر",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeDays > 0) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.CalendarToday,
                            iconTint = MaterialTheme.colorScheme.primary,
                            value = "${RTL}${activeDays.toEnglishDigits()}",
                            label = "روزهای فعال (۳۰ روز اخیر)"
                        )
                    }
                    val last = GoalActivityFormatter.formatLastActivity(lastActivity)
                    if (last.isNotBlank()) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.AccessTime,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            value = last,
                            label = "آخرین فعالیت"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MirrorSheetContent(insights: List<MirrorInsight>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "بازتاب هدف",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "این بازخورد بر اساس فعالیت‌های اخیر این هدف ایجاد شده است.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (insights.isEmpty()) {
            Text(
                text = "هنوز الگویی برای نمایش وجود ندارد.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            insights.forEach { insight ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = insight.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = insight.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalDetailTaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val rowAlpha = if (task.isCompleted) 0.5f else 1f

    NeumorphicSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(rowAlpha)
            ) {
                Text(
                    text = task.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            task.deadlineEpochMs?.let { dl ->
                val overdue = !task.isCompleted && dl < System.currentTimeMillis() - (System.currentTimeMillis() % 86400000L)
                val jalali = com.example.core.util.JalaliDate.fromEpochMs(dl)
                val dlColor = if (overdue) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    modifier = Modifier.alpha(rowAlpha),
                    shape = RoundedCornerShape(6.dp),
                    color = dlColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "مهلت ${jalali.day.toEnglishDigits()} ${com.example.core.util.JalaliDate.MONTH_NAMES[jalali.month - 1]}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = dlColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            task.priority?.let { priority ->
                val (label, colorV) = when (priority) {
                    "HIGH" -> "بالا" to MaterialTheme.colorScheme.error
                    "MEDIUM" -> "متوسط" to Color(0xFFF97316)
                    "LOW" -> "پایین" to AccentGreen
                    else -> priority to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    modifier = Modifier.alpha(rowAlpha),
                    shape = RoundedCornerShape(6.dp),
                    color = colorV.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorV,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.alpha(rowAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "ویرایش تسک",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.alpha(rowAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "حذف تسک",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun statusLabel(status: String): String = when (status) {
    GoalStatus.ACTIVE -> "فعال"
    GoalStatus.COMPLETED -> "تکمیل‌شده"
    GoalStatus.PAUSED -> "متوقف‌شده"
    GoalStatus.ABANDONED -> "رها‌شده"
    GoalStatus.ARCHIVED -> "بایگانی‌شده"
    else -> status
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    GoalStatus.ACTIVE -> MaterialTheme.colorScheme.primary
    GoalStatus.COMPLETED -> AccentGreen
    GoalStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
    GoalStatus.ABANDONED -> MaterialTheme.colorScheme.error
    GoalStatus.ARCHIVED -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
