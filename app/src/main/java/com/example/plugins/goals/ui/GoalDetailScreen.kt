package com.example.plugins.goals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.util.toPersianDigits
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.ui.components.NeumorphicSurface
import com.example.ui.theme.*

/** RTL mark — forces paragraph direction to RTL */
private const val RTL = "‏"

@Composable
fun GoalDetailScreen(
    goalId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalDetailViewModel = viewModel(
        factory = GoalDetailViewModel.factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
            goalId
        )
    )
) {
    val goal by viewModel.goal.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val goalRate by viewModel.goalRate.collectAsState()

    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var showEditGoalDialog by remember { mutableStateOf(false) }

    // Navigate to task detail if selected
    selectedTaskId?.let { taskId ->
        com.example.plugins.planner.ui.TaskDetailScreen(
            taskId = taskId,
            onBack = { selectedTaskId = null }
        )
        return
    }

    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size
    val rate = goalRate?.completionRate ?: 0f

    val statusColor = when (goal?.status) {
        "active" -> MaterialTheme.colorScheme.primary
        "completed" -> AccentGreen
        "paused" -> MaterialTheme.colorScheme.onSurfaceVariant
        "abandoned" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusLabel = when (goal?.status) {
        "active" -> "فعال"
        "completed" -> "تکمیل شده"
        "paused" -> "متوقف"
        "abandoned" -> "رها شده"
        else -> ""
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
            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
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

                // Edit action — opens EditGoalDialog
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

            // ── Goal info card ──
            NeumorphicSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = 6
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Description
                    goal?.description?.let { desc ->
                        if (desc.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = desc,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "نرخ تکمیل",
                            value = "${rate.toInt().toPersianDigits()}٪",
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            label = "انجام شده",
                            value = "${RTL}${completedCount.toPersianDigits()} از ${totalCount.toPersianDigits()}",
                            color = AccentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Section header ──
            Text(
                text = "${RTL}تسک‌های مرتبط",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tasks list ──
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📝", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هنوز تسکی برای این هدف ثبت نشده",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        GoalDetailTaskRow(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompletion(task) },
                            onEdit = { selectedTaskId = task.id }
                        )
                    }
                }
            }
        }
    }

    // ── Edit Goal Dialog ──
    if (showEditGoalDialog && goal != null) {
        EditGoalDialog(
            goal = goal!!,
            onDismiss = { showEditGoalDialog = false },
            onUpdateGoal = { title, description ->
                viewModel.updateGoal(title, description)
                showEditGoalDialog = false
            }
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalDetailTaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit = {}
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
            // Checkbox — always full opacity
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Content — faded when completed
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

            // Priority badge + Edit — faded when completed
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

            // Edit action
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
        }
    }
}
