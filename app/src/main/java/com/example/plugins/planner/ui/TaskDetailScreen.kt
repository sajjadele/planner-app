package com.example.plugins.planner.ui

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.util.JalaliDate
import com.example.core.util.RTL
import androidx.compose.material3.AlertDialog
import com.example.plugins.planner.data.ActivityCreationAction
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityFeedFilterState
import com.example.plugins.planner.data.ActivityMessageAction
import com.example.plugins.planner.data.ActivityMessageModel
import com.example.plugins.planner.data.StepDraft
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskStepEntity
import com.example.plugins.planner.ui.components.NeumorphicSurface
import com.example.plugins.planner.ui.components.ActivityMessageCard
import com.example.plugins.planner.ui.components.FullScreenImageDialog
import com.example.plugins.planner.ui.components.ReminderSection
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
    val activityMessages by viewModel.activityMessages.collectAsState()
    val filteredActivityMessages by viewModel.filteredActivityMessages.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
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
    var initialComposerDuration by remember { mutableStateOf<Int?>(null) }

    var editingMessage: ActivityMessageModel? by remember { mutableStateOf(null) }
    var deletingMessageId: Long? by remember { mutableStateOf(null) }
    var replyingToMessage: ActivityMessageModel? by remember { mutableStateOf(null) }
    var selectedMessageId: Long? by remember { mutableStateOf(null) }
    var scrollToMessageId: Long? by remember { mutableStateOf(null) }

    // ── Image picker for direct FAB action ──
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                // Persist read permission for content:// URIs
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                // Create activity directly with the selected image
                viewModel.createActivity(
                    ActivityDraft(
                        attachments = listOf(
                            ActivityAttachment.Image(uri.toString())
                        )
                    )
                )
            }
        }
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                // Derive file name from URI
                val fileName = uri.lastPathSegment ?: "فایل"
                // Create activity directly with the selected file
                viewModel.createActivity(
                    ActivityDraft(
                        attachments = listOf(
                            ActivityAttachment.File(uri.toString(), fileName)
                        )
                    )
                )
            }
        }
    )

    // ── Handle creation actions ──
    val handleCreationAction: (ActivityCreationAction) -> Unit = remember(viewModel, context) { { action ->
        when (action) {
            ActivityCreationAction.Image -> {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            ActivityCreationAction.File -> {
                filePickerLauncher.launch("*/*")
            }
            ActivityCreationAction.ManualActivity -> {
                // Open composer with duration pre-enabled
                initialComposerDuration = 30
                showActivityComposer = true
            }
            else -> {
                // Note opens composer
                initialComposerDuration = null
                showActivityComposer = true
            }
        }
    } }

    // ── Scroll to message on ReplyNavigation ──
    // Uses filteredActivityMessages for correct LazyColumn index.
    // If target is hidden by an active filter, clears filter and retries.
    LaunchedEffect(scrollToMessageId, filteredActivityMessages) {
        val targetId = scrollToMessageId ?: return@LaunchedEffect
        val index = filteredActivityMessages.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            // Target is visible in the current filtered view
            activityListState.animateScrollToItem(index)
            selectedMessageId = targetId
            scrollToMessageId = null
        } else if (filterState.selectedStepId != null) {
            // Target is hidden by a step filter — clear it to reveal all messages
            viewModel.showAllActivities()
            // Keep scrollToMessageId set; the next LaunchedEffect emission will find
            // the target now that the filter is cleared (filteredActivityMessages updated)
        }
        // If target not found and no filter active, the target may not exist — silently ignore
    }

    // ── Scroll to bottom on initial load (Telegram-style: oldest at top, newest at bottom) ──
    val initialLoadDone = remember { mutableStateOf(false) }
    LaunchedEffect(filteredActivityMessages) {
        if (!initialLoadDone.value && filteredActivityMessages.isNotEmpty()) {
            initialLoadDone.value = true
            // Delay slightly to ensure layout is measured
            kotlinx.coroutines.delay(100)
            activityListState.animateScrollToItem(filteredActivityMessages.size - 1)
        }
    }

    val handleMessageAction: (ActivityMessageAction) -> Unit = remember(viewModel, activityMessages) { { action ->
        when (action) {
            is ActivityMessageAction.Edit -> {
                val message = activityMessages.find { it.id == action.messageId }
                if (message != null) {
                    editingMessage = message
                    showActivityComposer = true
                }
                selectedMessageId = null
            }
            is ActivityMessageAction.Delete -> {
                deletingMessageId = action.messageId
            }
            is ActivityMessageAction.Reply -> {
                val message = activityMessages.find { it.id == action.messageId }
                if (message != null) {
                    replyingToMessage = message
                    showActivityComposer = true
                }
                selectedMessageId = null
            }
            is ActivityMessageAction.ReplyNavigation -> {
                scrollToMessageId = action.messageId
            }
        }
    } }

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
                    messages = filteredActivityMessages,
                    allMessages = activityMessages,
                    steps = steps,
                    filterState = filterState,
                    selectedMessageId = selectedMessageId,
                    onTapComposer = { showActivityComposer = true },
                    onSelectAction = handleCreationAction,
                    onMessageAction = handleMessageAction,
                    onShowAll = { viewModel.showAllActivities() },
                    onFilterByStep = { stepId -> viewModel.filterByStep(stepId) },
                    listState = activityListState
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
            onMoveDate = { viewModel.moveActivityDate(it) },
            onMessageAction = handleMessageAction
        )

        if (showActivityComposer) {
            ActivityComposerBottomSheet(
                onDismiss = { 
                    showActivityComposer = false
                    editingMessage = null
                    replyingToMessage = null
                    initialComposerDuration = null
                },
                onCreateActivity = { draft ->
                    viewModel.createActivity(draft)
                    showActivityComposer = false
                    editingMessage = null
                    replyingToMessage = null
                    initialComposerDuration = null
                    focusManager.clearFocus()
                },
                onUpdateActivity = { messageId, draft ->
                    viewModel.updateActivity(messageId, draft)
                    showActivityComposer = false
                    editingMessage = null
                    replyingToMessage = null
                    initialComposerDuration = null
                    focusManager.clearFocus()
                },
                initialMessage = editingMessage,
                replyToMessage = replyingToMessage,
                initialDurationMinutes = initialComposerDuration
            )
        }

        deletingMessageId?.let { msgId ->
            AlertDialog(
                onDismissRequest = { deletingMessageId = null },
                title = {
                    Text(
                        "${RTL}حذف پیام",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "${RTL}این پیام حذف خواهد شد.",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${RTL}این عملیات قابل بازگشت نیست.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteActivity(msgId)
                            deletingMessageId = null
                        }
                    ) {
                        Text(
                            "${RTL}حذف",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingMessageId = null }) {
                        Text("${RTL}لغو")
                    }
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
// ACTIVITY FEED — Phase 5.2.2 UX Layer
// ════════════════════════════════════════════════════════════════

@Composable
private fun TaskDetailActivityContent(
    modifier: Modifier = Modifier,
    messages: List<ActivityMessageModel>,
    allMessages: List<ActivityMessageModel>,
    steps: List<TaskStepEntity>,
    filterState: ActivityFeedFilterState,
    selectedMessageId: Long? = null,
    onTapComposer: () -> Unit,
    onSelectAction: (ActivityCreationAction) -> Unit,
    onMessageAction: ((ActivityMessageAction) -> Unit)? = null,
    onShowAll: () -> Unit = {},
    onFilterByStep: (Long) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val groups = remember(messages) { groupActivityMessagesByDay(messages) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    // ── Fullscreen Image Viewer ──
    fullScreenImageUrl?.let { url ->
        FullScreenImageDialog(
            imageUri = url,
            onDismiss = { fullScreenImageUrl = null }
        )
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Feed Header ──
            item(key = "feed_header") {
                ActivityFeedHeader(onSelectAction = onSelectAction)
            }

            // ── Filter Chips ──
            if (steps.isNotEmpty()) {
                item(key = "filter_chips") {
                    ActivityFeedFilterChips(
                        steps = steps,
                        selectedStepId = filterState.selectedStepId,
                        onShowAll = onShowAll,
                        onFilterByStep = onFilterByStep
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── Empty State ──
            if (groups.isEmpty()) {
                item(key = "empty_feed") {
                    ActivityFeedEmptyState(onTapComposer = onTapComposer)
                }
            }

            // ── Message Groups ──
            groups.forEach { group ->
                item(key = "date_${group.dateKey}") {
                    ActivityFeedDayHeader(dateKey = group.dateKey)
                }

                items(group.messages, key = { it.id }) { message ->
                    val repliedTo = if (message.replyToMessageId != null) {
                        messages.find { it.id == message.replyToMessageId }
                    } else null

                    ActivityMessageCard(
                        message = message,
                        repliedToMessage = repliedTo,
                        isSelected = message.id == selectedMessageId,
                        onAction = onMessageAction,
                        onAttachmentClick = { attachment ->
                            if (attachment is ActivityAttachment.Image) {
                                fullScreenImageUrl = attachment.uri
                            }
                        },
                        onReplyReferenceClick = { targetId ->
                            onMessageAction?.invoke(
                                ActivityMessageAction.ReplyNavigation(targetId)
                            )
                        }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// FEED HEADER with dropdown quick action menu
// ════════════════════════════════════════════════════════════════

@Composable
private fun ActivityFeedHeader(
    onSelectAction: (ActivityCreationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${RTL}فعالیت\u200Cها",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(28.dp)
            ) {
                Text(
                    text = "✚",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("${RTL}📝  یادداشت", fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onSelectAction(ActivityCreationAction.Note)
                    }
                )
                DropdownMenuItem(
                    text = { Text("${RTL}📷  تصویر", fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onSelectAction(ActivityCreationAction.Image)
                    }
                )
                DropdownMenuItem(
                    text = { Text("${RTL}📎  فایل", fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onSelectAction(ActivityCreationAction.File)
                    }
                )
                DropdownMenuItem(
                    text = { Text("${RTL}⏱️  فعالیت دستی", fontSize = 13.sp) },
                    onClick = {
                        showMenu = false
                        onSelectAction(ActivityCreationAction.ManualActivity)
                    }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// FILTER CHIPS
// ════════════════════════════════════════════════════════════════

@Composable
private fun ActivityFeedFilterChips(
    steps: List<TaskStepEntity>,
    selectedStepId: Long?,
    onShowAll: () -> Unit,
    onFilterByStep: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All filter chip
        FilterChip(
            selected = selectedStepId == null,
            onClick = onShowAll,
            label = {
                Text(
                    text = "${RTL}همه",
                    fontSize = 12.sp,
                    fontWeight = if (selectedStepId == null) FontWeight.Bold else FontWeight.Normal
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Step filter chips
        steps.forEach { step ->
            val stepId = step.id.toLong()
            FilterChip(
                selected = selectedStepId == stepId,
                onClick = { onFilterByStep(stepId) },
                label = {
                    Text(
                        text = "${RTL}${step.title}",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selectedStepId == stepId) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// ENHANCED EMPTY STATE
// ════════════════════════════════════════════════════════════════

@Composable
private fun ActivityFeedEmptyState(
    onTapComposer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = "✨",
                fontSize = 36.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${RTL}هنوز فعالیتی ثبت نکردی",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${RTL}یادداشت\u200Cها، تصاویر، فایل\u200Cها و\nاتفاقات مسیر اینجا ذخیره می\u200Cشوند.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            FilledTonalButton(
                onClick = onTapComposer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "${RTL}➕ ثبت فعالیت",
                    fontSize = 13.sp
                )
            }
        }
    }
}


private data class ActivityMessageGroup(
    val dateKey: Long,
    val messages: List<ActivityMessageModel>
)

private fun groupActivityMessagesByDay(messages: List<ActivityMessageModel>): List<ActivityMessageGroup> {
    val grouped = messages.groupBy {
        JalaliDate.toEpochMs(JalaliDate.fromEpochMs(it.createdAt))
    }
    return grouped.entries
        .map { ActivityMessageGroup(it.key, it.value.sortedBy { m -> m.createdAt }) }
        .sortedBy { it.dateKey }
}

@Composable
private fun ActivityFeedDayHeader(dateKey: Long) {
    val todayKey = remember { JalaliDate.toEpochMs(JalaliDate.today()) }
    val yesterdayDate = remember {
        val t = JalaliDate.today()
        JalaliDate.toEpochMs(JalaliDate(t.year, t.month, t.day - 1))
    }

    val label = when (dateKey) {
        todayKey -> "${RTL}امروز"
        yesterdayDate -> "${RTL}دیروز"
        else -> {
            val jDate = JalaliDate.fromEpochMs(dateKey)
            "${RTL}${jDate.day} ${JalaliDate.MONTH_NAMES[jDate.month - 1]} ${jDate.year}"
        }
    }

    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
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

    }
}
