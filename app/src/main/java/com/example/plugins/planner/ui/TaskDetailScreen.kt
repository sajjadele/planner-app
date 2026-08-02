package com.example.plugins.planner.ui

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.calendar.PersianCalendarDialog
import com.example.core.util.JalaliDate
import com.example.core.util.RTL
import androidx.compose.material3.AlertDialog
import com.example.plugins.planner.data.ActivityCreationAction
import com.example.ui.screens.components.VisionPopupMenu
import com.example.ui.screens.components.VisionMenuItem
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
import com.example.plugins.planner.ui.components.AddTagDialog
import com.example.plugins.planner.ui.components.defaultTagColor
import com.example.plugins.planner.ui.components.parseColorHex
import com.example.plugins.planner.ui.components.FullScreenImageDialog
import com.example.plugins.planner.ui.components.ReminderSection
import com.example.plugins.planner.ui.components.skeletonShimmerBrush
import com.example.ui.components.VisionConfirmDialog
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

    val editableTitle by viewModel.editableTitle.collectAsState()
    var showGoalDropdown by remember { mutableStateOf(false) }
    var logInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val activityListState = rememberLazyListState()
    var showActivityDateCalendar by remember { mutableStateOf(false) }
    var showActivityComposer by remember { mutableStateOf(false) }
    var initialComposerDuration by remember { mutableStateOf<Int?>(null) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showTagActionMenu by remember { mutableStateOf(false) }
    var selectedTagForAction: TaskStepEntity? by remember { mutableStateOf(null) }
    var editingTag: TaskStepEntity? by remember { mutableStateOf(null) }
    var deletingTag: TaskStepEntity? by remember { mutableStateOf(null) }

    var editingMessage: ActivityMessageModel? by remember { mutableStateOf(null) }
    var deletingMessageId: Long? by remember { mutableStateOf(null) }
    var replyingToMessage: ActivityMessageModel? by remember { mutableStateOf(null) }
    var selectedMessageId: Long? by remember { mutableStateOf(null) }
    var scrollToMessageId: Long? by remember { mutableStateOf(null) }

    // ── Multi-select state (Telegram-style) ──
    var selectedMessageIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedMessageIds.isNotEmpty()
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    // ── Image picker for direct FAB action (multi-select) ──
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                val attachments = uris.map { uri ->
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    ActivityAttachment.Image(uri.toString())
                }
                viewModel.createActivity(
                    ActivityDraft(attachments = attachments)
                )
            }
        }
    )

    // ── File picker (multi-select) ──
    @Suppress("DEPRECATION")
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val fileName = context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                } ?: uri.lastPathSegment ?: "فایل"
                viewModel.createActivity(
                    ActivityDraft(attachments = listOf(ActivityAttachment.File(uri.toString(), fileName)))
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
                filePickerLauncher.launch(arrayOf("*/*"))
            }
            ActivityCreationAction.ManualActivity -> {
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

    // ── Scroll to top when date filter changes (Phase 6.0.6) ──
    LaunchedEffect(selectedActivityDate) {
        if (selectedActivityDate != null && initialLoadDone.value) {
            kotlinx.coroutines.delay(150)
            activityListState.animateScrollToItem(0)
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
            is ActivityMessageAction.Select -> {
                selectedMessageIds = if (action.messageId in selectedMessageIds) {
                    selectedMessageIds - action.messageId
                } else {
                    selectedMessageIds + action.messageId
                }
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
            // ── Top Bar: Selection mode or normal ──
            if (isSelectionMode) {
                SelectionModeTopBar(
                    selectedCount = selectedMessageIds.size,
                    onClose = { selectedMessageIds = emptySet() },
                    onDelete = { showBatchDeleteConfirm = true }
                )
            } else {
                TaskDetailTopBar(onBack = onBack)
            }

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
                    onEditableTitleChange = { viewModel.updateEditableTitle(it) },
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
                    isSelectionMode = isSelectionMode,
                    selectedMessageIds = selectedMessageIds,
                    selectedDate = selectedActivityDate,
                    timelineStartDate = timelineStartDate,
                    timelineEndDate = timelineEndDate,
                    onTapComposer = { showActivityComposer = true },
                    onSelectAction = handleCreationAction,
                    onMessageAction = handleMessageAction,
                    onLongPress = { messageId ->
                        selectedMessageIds = if (messageId in selectedMessageIds) {
                            selectedMessageIds - messageId
                        } else {
                            selectedMessageIds + messageId
                        }
                    },
                    onShowAll = { viewModel.showAllActivities() },
                    onFilterByStep = { stepId -> viewModel.filterByStep(stepId) },
                    onAddTag = { showAddTagDialog = true },
                    onTagLongPress = { tag ->
                        selectedTagForAction = tag
                        showTagActionMenu = true
                    },
                    onOpenCalendar = { showActivityDateCalendar = true },
                    onGoToToday = { viewModel.goToToday() },
                    onMoveDate = { days -> viewModel.moveActivityDate(days) },
                    onClearDate = { viewModel.clearDateFilter() },
                    listState = activityListState
                )
            }
        }

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
            VisionConfirmDialog(
                onDismissRequest = { deletingMessageId = null },
                title = "${RTL}حذف پیام",
                message = "${RTL}آیا از حذف این پیام اطمینان دارید؟\n${RTL}این عملیات قابل بازگشت نیست.",
                confirmText = "${RTL}حذف",
                dismissText = "${RTL}لغو",
                onConfirm = { viewModel.deleteActivity(msgId) },
                isDestructive = true
            )
        }

        // ── Batch Delete Confirmation ──
        if (showBatchDeleteConfirm) {
            VisionConfirmDialog(
                onDismissRequest = { showBatchDeleteConfirm = false },
                title = "${RTL}حذف پیام‌ها",
                message = "${RTL}${selectedMessageIds.size} پیام حذف خواهد شد.\n${RTL}این عملیات قابل بازگشت نیست.",
                confirmText = "${RTL}حذف",
                dismissText = "${RTL}لغو",
                onConfirm = {
                    selectedMessageIds.forEach { viewModel.deleteActivity(it) }
                    selectedMessageIds = emptySet()
                },
                isDestructive = true
            )
        }

        // ── Tag Creation Dialog ──
        if (showAddTagDialog) {
            AddTagDialog(
                onDismiss = {
                    showAddTagDialog = false
                    editingTag = null
                },
                onCreateTag = { name, colorHex ->
                    if (editingTag != null) {
                        // Editing existing tag — in-place update, preserve stepId
                        val tag = editingTag!!
                        val newColor = colorHex ?: tag.colorHex
                        viewModel.updateTag(tag, name, newColor)
                    } else {
                        viewModel.createTag(name, colorHex)
                    }
                },
                initialName = editingTag?.title,
                initialColorHex = editingTag?.colorHex,
                isEditing = editingTag != null
            )
        }

        // ── Tag Action Menu (Edit / Delete) ──
        if (showTagActionMenu && selectedTagForAction != null) {
            val tag = selectedTagForAction!!
            AlertDialog(
                onDismissRequest = {
                    showTagActionMenu = false
                    selectedTagForAction = null
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "${RTL}${tag.title}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Edit button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showTagActionMenu = false
                                    editingTag = tag
                                    showAddTagDialog = true
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${RTL}ویرایش اسم",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        // Delete button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showTagActionMenu = false
                                    deletingTag = tag
                                },
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${RTL}حذف دسته",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showTagActionMenu = false
                            selectedTagForAction = null
                        }
                    ) {
                        Text(
                            "${RTL}لغو",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        // ── Tag Delete Confirmation Dialog ──
        deletingTag?.let { tag ->
            AlertDialog(
                onDismissRequest = { deletingTag = null },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "${RTL}حذف دسته؟",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${RTL}با حذف این دسته، فعالیت‌های قبلی حذف نمی‌شوند",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${RTL}اما این دسته‌بندی از آن‌ها حذف خواهد شد.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteStep(tag)
                            deletingTag = null
                            selectedTagForAction = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "${RTL}حذف",
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { deletingTag = null },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${RTL}لغو",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        // ── Activity Date Calendar Dialog (Phase 6.0.6) ──
        if (showActivityDateCalendar) {
            PersianCalendarDialog(
                selectedDateEpochMs = selectedActivityDate ?: timelineStartDate,
                onDateSelected = { date ->
                    viewModel.selectActivityDate(date)
                    showActivityDateCalendar = false
                },
                onDismiss = { showActivityDateCalendar = false },
                minSelectableDate = timelineStartDate,
                maxSelectableDate = timelineEndDate,
                confirmButtonText = "${RTL}انتخاب",
                showConfirmButton = true
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
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

@Composable
private fun SelectionModeTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "${RTL}بستن",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = "${RTL}${selectedCount}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "${RTL}حذف",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// TAB SELECTOR
// ══════════════════════════════════════════════════════════════

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
    isSelectionMode: Boolean = false,
    selectedMessageIds: Set<Long> = emptySet(),
    selectedDate: Long? = null,
    timelineStartDate: Long = 0L,
    timelineEndDate: Long = 0L,
    onTapComposer: () -> Unit,
    onSelectAction: (ActivityCreationAction) -> Unit,
    onMessageAction: ((ActivityMessageAction) -> Unit)? = null,
    onLongPress: ((Long) -> Unit)? = null,
    onShowAll: () -> Unit = {},
    onFilterByStep: (Long) -> Unit = {},
    onAddTag: () -> Unit = {},
    onTagLongPress: (TaskStepEntity) -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onGoToToday: () -> Unit = {},
    onMoveDate: (Int) -> Unit = {},
    onClearDate: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
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
                ActivityFeedHeader(
                    onSelectAction = onSelectAction
                )
            }

            // ── Date Control (Phase 6.1: always visible, above tags) ──
            item(key = "date_control") {
                ActivityFeedDateNavigator(
                    selectedDate = selectedDate,
                    timelineStartDate = timelineStartDate,
                    timelineEndDate = timelineEndDate,
                    onOpenCalendar = onOpenCalendar,
                    onGoToToday = onGoToToday,
                    onMoveDate = onMoveDate,
                    onClearDate = onClearDate,
                    onSelectDate = { /* handled by calendar dialog */ }
                )
            }

            // ── Filter Chips (always visible — shows [+] and [همه] even with zero tags) ──
            item(key = "filter_chips") {
                ActivityFeedFilterChips(
                    steps = steps,
                    selectedStepId = filterState.selectedStepId,
                    onShowAll = onShowAll,
                    onFilterByStep = onFilterByStep,
                    onAddTag = onAddTag,
                    onTagLongPress = onTagLongPress
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                        isSelected = message.id in selectedMessageIds,
                        isSelectionMode = isSelectionMode,
                        onAction = onMessageAction,
                        onLongPress = { onLongPress?.invoke(message.id) },
                        onAttachmentClick = { attachment ->
                            when (attachment) {
                                is ActivityAttachment.Image -> {
                                    fullScreenImageUrl = attachment.uri
                                }
                                is ActivityAttachment.File -> {
                                    // Open file with external app
                                    val uri = android.net.Uri.parse(attachment.uri)
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW
                                    ).apply {
                                        setDataAndType(uri, context.contentResolver.getType(uri))
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        // No app can handle this file type
                                    }
                                }
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

// ══════════════════════════════════════════════════════════════
// ENHANCED EMPTY STATE
// ══════════════════════════════════════════════════════════════

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
            VisionPopupMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                VisionMenuItem(
                    text = "${RTL}📝  یادداشت",
                    leadingIcon = null,
                    onClick = {
                        showMenu = false
                        onSelectAction(ActivityCreationAction.Note)
                    }
                )
                VisionMenuItem(
                    text = "${RTL}📷  تصویر",
                    leadingIcon = null,
                    onClick = {
                        showMenu = false
                        onSelectAction(ActivityCreationAction.Image)
                    }
                )
                VisionMenuItem(
                    text = "${RTL}📎  فایل",
                    leadingIcon = null,
                    onClick = {
                        showMenu = false
                        onSelectAction(ActivityCreationAction.File)
                    }
                )
                VisionMenuItem(
                    text = "${RTL}⏱️  فعالیت دستی",
                    leadingIcon = null,
                    onClick = {
                        showMenu = false
                        onSelectAction(ActivityCreationAction.ManualActivity)
                    }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// FILTER CHIPS
// ══════════════════════════════════════════════════════════════

/**
 * ActivityFeedFilterChips — Horizontal filter chips for tag-based filtering.
 *
 * Phase 5.9.3: "+" action at index 0 + colored dots.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityFeedFilterChips(
    steps: List<TaskStepEntity>,
    selectedStepId: Long?,
    onShowAll: () -> Unit,
    onFilterByStep: (Long) -> Unit,
    onAddTag: () -> Unit,
    onTagLongPress: (TaskStepEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onAddTag),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        FilterChip(
            selected = selectedStepId == null,
            onClick = onShowAll,
            label = {
                Text(
                    text = "${RTL}همه",
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (selectedStepId == null) FontWeight.Bold else FontWeight.Normal
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
            )
        )

        steps.forEach { step ->
            val stepId = step.id.toLong()
            val isSelected = selectedStepId == stepId
            val chipColor = parseColorHex(step.colorHex) ?: defaultTagColor
            Surface(
                modifier = Modifier.combinedClickable(
                    onClick = { onFilterByStep(stepId) },
                    onLongClick = { onTagLongPress(step) }
                ),
                shape = RoundedCornerShape(16.dp),
                color = chipColor.copy(alpha = if (isSelected) 0.25f else 0.08f),
                border = if (isSelected) BorderStroke(1.dp, chipColor.copy(alpha = 0.4f)) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelected) {
                        Text(
                            text = "✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipColor
                        )
                    }
                    Text(
                        text = "${RTL}${step.title}",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = chipColor
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// DATE CONTROL — Phase 6.1 Always-Visible Compact Design
// ══════════════════════════════════════════════════════════════

/**
 * ActivityFeedDateNavigator — Compact date control for the activity feed.
 *
 * Phase 6.1: Always-visible design.
 * - Always renders (date defaults to today)
 * - Collapsed: single-row chip (📅 date ▼)
 * - Expanded: reveals prev/next/today navigation controls
 *
 * Target heights:
 *   Collapsed: ~36dp (single row)
 *   Expanded:  ~80dp max (row + navigation controls)
 */
@Composable
private fun ActivityFeedDateNavigator(
    selectedDate: Long?,
    timelineStartDate: Long,
    timelineEndDate: Long,
    onOpenCalendar: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onGoToToday: () -> Unit,
    onMoveDate: (Int) -> Unit,
    onClearDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val dateToShow = selectedDate ?: timelineStartDate
    val jalali = remember(dateToShow) { JalaliDate.fromEpochMs(dateToShow) }
    val dateText = remember(jalali) {
        "${RTL}${jalali.day} ${JalaliDate.MONTH_NAMES[jalali.month - 1]} ${jalali.year}"
    }

    val canMovePrevious = dateToShow > timelineStartDate
    val canMoveNext = dateToShow < timelineEndDate

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Collapsed Row: date chip + expand/collapse arrow ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: calendar icon + date text (tappable → open calendar)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenCalendar)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "${RTL}انتخاب تاریخ",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Right: expand/collapse arrow
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "${RTL}بستن" else "${RTL}باز کردن",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = if (isExpanded) 180f else 0f
                        }
                )
            }
        }

        // ── Expanded Navigation Controls ──
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous day
                IconButton(
                    onClick = { if (canMovePrevious) onMoveDate(-1) },
                    modifier = Modifier.size(32.dp),
                    enabled = canMovePrevious
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "${RTL}روز قبل",
                        tint = if (canMovePrevious)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = 90f }
                    )
                }

                // Today button
                AssistChip(
                    onClick = onGoToToday,
                    label = {
                        Text(
                            text = "${RTL}امروز",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                // Next day
                IconButton(
                    onClick = { if (canMoveNext) onMoveDate(1) },
                    modifier = Modifier.size(32.dp),
                    enabled = canMoveNext
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "${RTL}روز بعد",
                        tint = if (canMoveNext)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = -90f }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// ENHANCED EMPTY STATE
// ══════════════════════════════════════════════════════════════

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
