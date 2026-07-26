package com.example.plugins.planner.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityMessageMapper
import com.example.plugins.planner.data.StepCardMapper
import com.example.plugins.planner.data.StepCardModel
import com.example.plugins.planner.data.StepDraft
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskStepEntity
import com.example.plugins.planner.ui.components.NeumorphicSurface
import com.example.plugins.planner.ui.components.ActivityMessageCard
import com.example.plugins.planner.ui.components.ReminderSection
import com.example.plugins.planner.ui.components.StepCard
import com.example.plugins.planner.ui.components.skeletonShimmerBrush
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TaskDetailScreen(
    taskId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: TaskDetailViewModel = viewModel(
        key = "task_detail_$taskId",
        factory = TaskDetailViewModel.factory(
            LocalContext.current.applicationContext as android.app.Application,
            taskId
        )
    )

    val task by viewModel.task.collectAsState()
    val isTaskLoaded by viewModel.isTaskLoaded.collectAsState()
    val activeGoals by viewModel.activeGoals.collectAsState()
    val allGoals by viewModel.allGoals.collectAsState()
    val taskLogs by viewModel.taskLogs.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val activityMessages by viewModel.activityMessages.collectAsState()
    val selectedActivityDate by viewModel.selectedActivityDate.collectAsState()
    val timelineStartDate by viewModel.timelineStartDate.collectAsState()
    val timelineEndDate by viewModel.timelineEndDate.collectAsState()

    var editableTitle by remember(task) { mutableStateOf(task?.title ?: "") }
    var showGoalDropdown by remember { mutableStateOf(false) }
    var logInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val activityListState = rememberLazyListState()
    var showTimelineSheet by remember { mutableStateOf(false) }
    var showActivityComposer by remember { mutableStateOf(false) }
    var selectedStepIdForActivity by remember { mutableIntStateOf(-1) }

    val currentGoalName = remember(task, allGoals) {
        task?.goalId?.let { gid -> allGoals.firstOrNull { it.id == gid }?.title }
    }

    // ── Skeleton visibility: enforce minimum 250ms for smooth perceived loading ──
    var showSkeleton by remember { mutableStateOf(true) }
    LaunchedEffect(isTaskLoaded) {
        if (isTaskLoaded) {
            delay(250)
            showSkeleton = false
        }
    }

    if (showSkeleton) {
        TaskDetailSkeleton(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TaskDetailTopBar(onBack = onBack)

            TaskDetailTabSelector(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    focusManager.clearFocus()
                }
            )

            when (selectedTab) {
                0 -> TaskDetailOverviewContent(
                    task = task,
                    editableTitle = editableTitle,
                    onEditableTitleChange = { editableTitle = it },
                    showGoalDropdown = showGoalDropdown,
                    onShowGoalDropdownChange = { showGoalDropdown = it },
                    currentGoalName = currentGoalName,
                    activeGoals = activeGoals,
                    onUpdateTitle = { viewModel.updateTitle(it) },
                    onUpdateGoal = { viewModel.updateTaskGoal(it) },
                    onToggleCompletion = { viewModel.toggleTaskCompletion() },
                    onSetReminder = { h, m -> viewModel.setReminder(h, m) },
                    onClearReminder = { viewModel.clearReminder() },
                    focusManager = focusManager
                )

                1 -> TaskDetailActivityContent(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    steps = steps,
                    activities = activities,
                    onTapComposer = { showActivityComposer = true },
                    onToggleStep = { viewModel.toggleStepCompletion(it) },
                    onDeleteStep = { viewModel.deleteStep(it) },
                    onAddActivityToStep = { stepId ->
                        selectedStepIdForActivity = stepId
                        showActivityComposer = true
                    },
                    listState = activityListState,
                    onOpenTimeline = { showTimelineSheet = true }
                )
            }
        }

        TimelineBottomSheet(
            visible = showTimelineSheet,
            selectedDate = selectedActivityDate,
            timelineStartDate = timelineStartDate,
            timelineEndDate = timelineEndDate,
            messages = activityMessages,
            onDismiss = { showTimelineSheet = false },
            onSelectDate = { viewModel.selectActivityDate(it) },
            onGoToToday = { viewModel.goToToday() },
            onMoveDate = { viewModel.moveActivityDate(it) }
        )

        if (showActivityComposer) {
            ActivityComposerBottomSheet(
                onDismiss = { 
                    showActivityComposer = false
                    selectedStepIdForActivity = -1
                },
                onCreateActivity = { draft ->
                    val stepId = if (selectedStepIdForActivity != -1) {
                        selectedStepIdForActivity
                    } else {
                        null
                    }
                    viewModel.createActivity(draft, stepId)
                    showActivityComposer = false
                    selectedStepIdForActivity = -1
                    focusManager.clearFocus()
                },
                onCreateStep = { stepDraft ->
                    viewModel.createStep(stepDraft)
                    showActivityComposer = false
                    selectedStepIdForActivity = -1
                    focusManager.clearFocus()
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// TOP BAR
// ════════════════════════════════════════════════════════════════

@Composable
private fun TaskDetailTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
            text = "${RTL}جزئیات تسک",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

// ════════════════════════════════════════════════════════════════
// TAB SELECTOR
// ════════════════════════════════════════════════════════════════

@Composable
private fun TaskDetailTabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("${RTL}نمای کلی", "${RTL}فعالیت")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                label = { Text(label, fontSize = 12.sp) }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// OVERVIEW — stable task identity and configuration
// ════════════════════════════════════════════════════════════════

@Composable
private fun TaskDetailOverviewContent(
    task: TaskEntity?,
    editableTitle: String,
    onEditableTitleChange: (String) -> Unit,
    showGoalDropdown: Boolean,
    onShowGoalDropdownChange: (Boolean) -> Unit,
    currentGoalName: String?,
    activeGoals: List<com.example.core.goal.GoalEntity>,
    onUpdateTitle: (String) -> Unit,
    onUpdateGoal: (Int?) -> Unit,
    onToggleCompletion: () -> Unit,
    onSetReminder: (Int, Int) -> Unit,
    onClearReminder: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    NeumorphicSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 6
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Editable Title ──
            OutlinedTextField(
                value = editableTitle,
                onValueChange = onEditableTitleChange,
                label = { Text("${RTL}عنوان تسک", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (editableTitle.isNotBlank() && editableTitle != task?.title) {
                        onUpdateTitle(editableTitle.trim())
                    }
                    focusManager.clearFocus()
                })
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Goal Reassignment ──
            Text(
                text = "${RTL}هدف مرتبط",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onShowGoalDropdownChange(true) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentGoalName ?: "بدون هدف",
                        fontSize = 13.sp,
                        color = if (currentGoalName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showGoalDropdown,
                    onDismissRequest = { onShowGoalDropdownChange(false) },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("بدون هدف", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        onClick = {
                            onUpdateGoal(null)
                            onShowGoalDropdownChange(false)
                        }
                    )
                    activeGoals.forEach { goal ->
                        DropdownMenuItem(
                            text = {
                                Text(goal.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            },
                            onClick = {
                                onUpdateGoal(goal.id)
                                onShowGoalDropdownChange(false)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Completion Toggle ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${RTL}وضعیت انجام",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = task?.isCompleted ?: false,
                    onCheckedChange = { onToggleCompletion() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Reminder ──
            ReminderSection(
                reminderHour = task?.reminderHour,
                reminderMinute = task?.reminderMinute,
                onSetReminder = onSetReminder,
                onClearReminder = onClearReminder
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ACTIVITY — steps structure + timeline preview
// ════════════════════════════════════════════════════════════════

@Composable
private fun TaskDetailActivityContent(
    modifier: Modifier = Modifier,
    steps: List<TaskStepEntity>,
    activities: List<ActivityEventEntity>,
    onTapComposer: () -> Unit,
    onToggleStep: (TaskStepEntity) -> Unit,
    onDeleteStep: (TaskStepEntity) -> Unit,
    onAddActivityToStep: (Int) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    onOpenTimeline: () -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // ── Steps Header ──
        item(key = "steps_header") {
            Text(
                text = "${RTL}مراحل (${steps.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // ── Steps List or Empty State ──
        if (steps.isEmpty()) {
            item(key = "steps_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📝", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${RTL}هنوز مرحله‌ای تعریف نشده",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(steps, key = { it.id }) { step ->
                // Convert step to StepCardModel with its activities
                val stepActivities = remember(activities, step.id) {
                    val filtered = activities.filter { it.stepId == step.id }
                    filtered.mapNotNull { ActivityMessageMapper.toMessage(it) }
                }
                val stepCard = remember(step, stepActivities) {
                    StepCardMapper.toCardModel(step, stepActivities)
                }

                StepCard(
                    model = stepCard,
                    onToggle = { onToggleStep(step) },
                    onDelete = { onDeleteStep(step) },
                    onAddActivity = onAddActivityToStep
                )
            }
        }

        // ── Divider ──
        item(key = "divider") {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }

        // ── Activity Header ──
        item(key = "activity_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${RTL}فعالیت‌ها",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onTapComposer) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "${RTL}افزودن فعالیت",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ── Timeline Preview Card ──
        item(key = "timeline_preview") {
            TimelinePreviewCard(
                activities = activities,
                onClick = onOpenTimeline
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// STEP ITEM
// ════════════════════════════════════════════════════════════════

@Composable
private fun TaskStepItem(
    step: TaskStepEntity,
    onToggle: (TaskStepEntity) -> Unit,
    onDelete: (TaskStepEntity) -> Unit
) {
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 3
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = step.isCompleted,
                onCheckedChange = { onToggle(step) },
                modifier = Modifier.size(22.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "${RTL}${step.title}",
                fontSize = 13.sp,
                color = if (step.isCompleted)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = { onDelete(step) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "${RTL}حذف",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// TIMELINE PREVIEW CARD
// ════════════════════════════════════════════════════════════════

@Composable
private fun TimelinePreviewCard(
    activities: List<ActivityEventEntity>,
    onClick: () -> Unit
) {
    NeumorphicSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = 4
    ) {
        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${RTL}هنوز فعالیتی ثبت نشده",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Show latest activity using ActivityMessageCard
            val latestActivity = activities.maxByOrNull { it.timestamp }
            if (latestActivity != null) {
                val message = remember(latestActivity) {
                    ActivityMessageMapper.toMessage(latestActivity)
                }
                if (message != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📅", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${RTL}تاریخچه فعالیت",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    ActivityMessageCard(message = message)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${RTL}مشاهده تاریخچه ←",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelinePreviewLatestEvent(uiModel: TimelineEventUiModel) {
    var showImageViewer by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = uiModel.icon,
                fontSize = 12.sp,
                color = uiModel.color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = uiModel.actionText,
                fontSize = 12.sp,
                color = uiModel.color,
                fontWeight = FontWeight.Medium
            )
        }

        if (uiModel.imageUri != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showImageViewer = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiModel.imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        uiModel.objectText?.let { obj ->
            if (obj.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${RTL}$obj",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        uiModel.supportingText?.let { support ->
            if (support.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = support,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = uiModel.timeText,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 16.dp)
        )
    }

    if (showImageViewer) {
        ImageViewerDialog(
            imageUri = uiModel.imageUri!!,
            onDismiss = { showImageViewer = false }
        )
    }
}

// ════════════════════════════════════════════════════════════════
// SKELETON LOADING — mirrors real TaskDetailScreen layout
// ════════════════════════════════════════════════════════════════

/** A single shimmer bar used by [TaskDetailSkeleton]. */
@Composable
private fun SkeletonBar(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 12.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(skeletonShimmerBrush())
    )
}

/**
 * Full skeleton for [TaskDetailScreen]. Mirrors the real layout:
 * top bar → task edit card (title + goal + switch + reminder) → steps section → timeline preview card.
 */
@Composable
private fun TaskDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 0.dp)
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(skeletonShimmerBrush())
            )
            Spacer(modifier = Modifier.width(8.dp))
            SkeletonBar(modifier = Modifier.weight(1f), height = 16.sp.value.dp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Tab selector placeholder ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonBar(modifier = Modifier.weight(1f), height = 36.dp)
            SkeletonBar(modifier = Modifier.weight(1f), height = 36.dp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Task Edit Card ──
        NeumorphicSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 6
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title field placeholder
                SkeletonBar(modifier = Modifier.fillMaxWidth(), height = 48.dp)

                // Goal relation placeholder
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.3f), height = 10.sp.value.dp)
                    SkeletonBar(modifier = Modifier.fillMaxWidth(), height = 40.dp)
                }

                // Completion toggle placeholder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.35f), height = 10.sp.value.dp)
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(skeletonShimmerBrush())
                    )
                }

                // Reminder section placeholder
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.3f), height = 10.sp.value.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBar(modifier = Modifier.weight(1f), height = 40.dp)
                        SkeletonBar(modifier = Modifier.weight(1f), height = 40.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Steps section header ──
        SkeletonBar(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(0.3f),
            height = 14.sp.value.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Add step input placeholder ──
        SkeletonBar(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(44.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // ── Step items skeleton ──
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(2) {
                NeumorphicSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 3
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(skeletonShimmerBrush())
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        SkeletonBar(modifier = Modifier.weight(1f), height = 13.sp.value.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Divider placeholder ──
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(skeletonShimmerBrush())
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Timeline preview card skeleton ──
        NeumorphicSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            elevation = 4
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBar(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.4f), height = 14.sp.value.dp)
                }
                SkeletonBar(modifier = Modifier.fillMaxWidth(0.5f), height = 12.sp.value.dp)
                SkeletonBar(modifier = Modifier.fillMaxWidth(0.3f), height = 12.sp.value.dp)
            }
        }
    }
}
