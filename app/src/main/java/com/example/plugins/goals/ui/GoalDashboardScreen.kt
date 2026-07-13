package com.example.plugins.goals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
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
import com.example.core.onboarding.OnboardingDeepLink
import com.example.core.util.isolated
import com.example.ui.theme.*

@Composable
fun GoalDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: GoalViewModel = viewModel()
) {
    val activeGoals by viewModel.activeGoals.collectAsState()
    val allGoals by viewModel.allGoals.collectAsState()

    var selectedGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var showGoalMenu by remember { mutableStateOf(false) }
    var selectedGoalId by remember { mutableStateOf<Int?>(null) }
    var homeHintFor by remember { mutableStateOf(false) }

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
                    text = "اهداف فعال",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                val activeCount = activeGoals.count { it.status == "active" }
                Text(
                    text = "${activeCount.isolated()} هدف فعال",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeGoals.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🎯", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "هنوز هدفی تعریف نشده است",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای شروع، یک هدف بلندمدت اضافه کنید",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("goal_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(activeGoals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onClick = { selectedGoalId = goal.id },
                            onLongClick = {
                                selectedGoal = goal
                                showGoalMenu = true
                            }
                        )
                    }
                }
            }
        }

        // Long-press context menu
        if (showGoalMenu && selectedGoal != null) {
            GoalContextMenu(
                goal = selectedGoal!!,
                onDismiss = {
                    showGoalMenu = false
                    selectedGoal = null
                },
                onComplete = {
                    viewModel.completeGoal(selectedGoal!!.id)
                    showGoalMenu = false
                    selectedGoal = null
                },
                onPause = {
                    viewModel.pauseGoal(selectedGoal!!.id)
                    showGoalMenu = false
                    selectedGoal = null
                },
                onResume = {
                    viewModel.resumeGoal(selectedGoal!!.id)
                    showGoalMenu = false
                    selectedGoal = null
                },
                onAbandon = {
                    viewModel.abandonGoal(selectedGoal!!.id)
                    showGoalMenu = false
                    selectedGoal = null
                },
                onDelete = {
                    viewModel.deleteGoal(selectedGoal!!.id)
                    showGoalMenu = false
                    selectedGoal = null
                }
            )
        }
    }
}

@Composable
private fun GoalContextMenu(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAbandon: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = goal.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                when (goal.status) {
                    "active" -> {
                        TextButton(onClick = onComplete) {
                            Text("✅  تکمیل هدف", color = AccentGreen)
                        }
                        TextButton(onClick = onPause) {
                            Text("⏸  توقف موقت", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    "paused" -> {
                        TextButton(onClick = onResume) {
                            Text("▶  از سرگیری", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = onComplete) {
                            Text("✅  تکمیل هدف", color = AccentGreen)
                        }
                    }
                }
                TextButton(onClick = onAbandon) {
                    Text("🚫  رها کردن هدف", color = Color(0xFFF97316))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                TextButton(onClick = onDelete) {
                    Text("🗑  حذف دائمی", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
