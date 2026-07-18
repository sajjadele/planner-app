package com.example.plugins.goals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.goal.GoalEntity
import com.example.plugins.goals.ui.AddGoalDialog
import com.example.core.goal.GoalStatus
import com.example.core.onboarding.OnboardingDeepLink
import com.example.core.util.isolated
import com.example.ui.theme.*

@Composable
fun GoalDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: GoalViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val goalsByTab by viewModel.goalsByTab.collectAsState()

    var selectedGoalId by remember { mutableStateOf<Int?>(null) }
    var showGoalMenu by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var homeHintFor by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    // Deep-link from onboarding: land directly on the new goal's detail screen.
    LaunchedEffect(Unit) {
        OnboardingDeepLink.consume()?.let { (id, hint) ->
            homeHintFor = hint
            selectedGoalId = id
        }
    }

    // Navigate to detail screen if a goal is selected
    selectedGoalId?.let { goalId ->
        GoalDetailScreen(
            goalId = goalId,
            onBack = { selectedGoalId = null },
            isOnboarding = homeHintFor
        )
        return
    }

    if (showEditGoalDialog && selectedGoal != null) {
        EditGoalDialog(
            goal = selectedGoal!!,
            onDismiss = { showEditGoalDialog = false },
            onUpdateGoal = { title, description, why, deadlineEpochMs ->
                viewModel.updateGoal(selectedGoal!!.id, title, description, why, deadlineEpochMs)
                showEditGoalDialog = false
            }
        )
    }

    if (showDeleteConfirm && selectedGoal != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف هدف", fontWeight = FontWeight.Bold) },
            text = { Text("حذف این هدف تمام تسک‌ها و رویدادهای مربوط به آن را نیز حذف می‌کند. ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoalWithRelated(selectedGoal!!.id)
                        showDeleteConfirm = false
                        selectedGoal = null
                    }
                ) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("انصراف") }
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onAddGoal = { title, description, _, _ ->
                viewModel.addGoal(title, description, null, null)
                showAddGoalDialog = false
            }
        )
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

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "اهداف",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Segmented status selector: Active / Completed / Archived
            GoalStatusTabs(
                selected = selectedTab,
                onSelect = { viewModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (goalsByTab.isEmpty()) {
                GoalDashboardEmptyState(
                    isActiveTab = selectedTab == GoalStatus.ACTIVE,
                    onCreateGoal = { showAddGoalDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("goal_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(goalsByTab, key = { it.goal.id }) { item ->
                        val goalId = item.goal.id
                        // Cache the per-item action lambdas once per goal so GoalCard stays skippable
                        // (Phase 5.4 / ADR-0009). Lambdas close over stable ids, not the item object.
                        val onOpen = remember(goalId) { { selectedGoalId = goalId } }
                        val onEdit = remember(goalId) {
                            {
                                selectedGoal = item.goal
                                showEditGoalDialog = true
                            }
                        }
                        val onChangeStatus = remember(goalId) { { next: String -> viewModel.changeStatus(goalId, next) } }
                        val onArchive = remember(goalId) { { viewModel.archiveGoal(goalId) } }
                        val onDelete = remember(goalId) {
                            {
                                selectedGoal = item.goal
                                showDeleteConfirm = true
                            }
                        }
                        GoalCard(
                            item = item,
                            onClick = onOpen,
                            onOpen = onOpen,
                            onEdit = onEdit,
                            onChangeStatus = onChangeStatus,
                            onArchive = onArchive,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalStatusTabs(
    selected: String,
    onSelect: (String) -> Unit
) {
    val tabs = listOf(
        GoalStatus.ACTIVE to "فعال",
        GoalStatus.COMPLETED to "انجام شده",
        GoalStatus.ARCHIVED to "آرشیو"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, (status, label) ->
            SegmentedButton(
                selected = selected == status,
                onClick = { onSelect(status) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                label = { Text(label, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun GoalDashboardEmptyState(
    isActiveTab: Boolean,
    onCreateGoal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🎯", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isActiveTab) "هنوز هدف فعالی نداری" else "موردی یافت نشد",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (isActiveTab) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "اولین هدف خودت را بساز و شروع به ساختن پیشرفت کن.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateGoal,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("ساخت هدف", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
