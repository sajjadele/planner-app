package com.example.plugins.goals.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.goal.GoalEntity
import com.example.core.goal.GoalStatus
import com.example.core.util.toEnglishDigits
import com.example.domain.goal.GoalActivityFormatter
import com.example.plugins.planner.ui.components.NeumorphicSurface
import com.example.ui.screens.components.VisionMenuItem
import com.example.ui.screens.components.VisionMenuDivider
import com.example.ui.screens.components.VisionPopupMenu
import com.example.ui.theme.*

private val STATUS_LABELS = mapOf(
    GoalStatus.ACTIVE to "فعال",
    GoalStatus.COMPLETED to "تکمیل شده",
    GoalStatus.PAUSED to "متوقف",
    GoalStatus.ABANDONED to "رها شده",
    GoalStatus.ARCHIVED to "بایگانی شده"
)

@Composable
fun GoalCard(
    goal: GoalEntity,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onChangeStatus: (String) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    viewModel: GoalViewModel,
    modifier: Modifier = Modifier
) {
    val statusColor = when (goal.status) {
        GoalStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        GoalStatus.COMPLETED -> AccentGreen
        GoalStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
        GoalStatus.ABANDONED -> MaterialTheme.colorScheme.error
        GoalStatus.ARCHIVED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val lastActivity by viewModel.lastActivityFor(goal.id).collectAsState(initial = null)
    val activeDays by viewModel.activeDaysFor(goal.id).collectAsState(initial = 0)
    val progress by viewModel.progressFor(goal.id).collectAsState(initial = null)

    val deadlineText = remember(goal.deadlineEpochMs) {
        GoalActivityFormatter.formatDeadline(goal.deadlineEpochMs)
    }
    val lastActivityText = remember(lastActivity) {
        GoalActivityFormatter.formatLastActivity(lastActivity)
    }
    val activeDaysText = remember(activeDays) {
        GoalActivityFormatter.formatActiveDays(activeDays)
    }

    var showMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    val validStatuses = viewModel.validNextStatuses(goal.status)

    NeumorphicSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 6
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            // ── Section 1: Goal Identity ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = STATUS_LABELS[goal.status] ?: goal.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "گزینه‌ها",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    VisionPopupMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        VisionMenuItem(
                            text = "باز کردن",
                            leadingIcon = Icons.Default.OpenInFull,
                            onClick = { showMenu = false; onOpen() }
                        )
                        VisionMenuItem(
                            text = "ویرایش",
                            leadingIcon = Icons.Default.Edit,
                            onClick = { showMenu = false; onEdit() }
                        )
                        VisionMenuItem(
                            text = "تغییر وضعیت",
                            leadingIcon = if (goal.status == GoalStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                            onClick = { showStatusMenu = true; showMenu = false },
                            enabled = validStatuses.isNotEmpty()
                        )
                        VisionMenuItem(
                            text = "آرشیو",
                            leadingIcon = Icons.Default.Archive,
                            onClick = { showMenu = false; onArchive() },
                            enabled = GoalStatus.canTransition(goal.status, GoalStatus.ARCHIVED)
                        )
                        VisionMenuDivider()
                        VisionMenuItem(
                            text = "حذف",
                            leadingIcon = Icons.Default.DeleteOutline,
                            textColor = MaterialTheme.colorScheme.error,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showMenu = false; onDelete() }
                        )
                    }

                    // Status submenu — only valid transitions from GoalStatus.canTransition
                    VisionPopupMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        validStatuses.forEach { status ->
                            VisionMenuItem(
                                text = STATUS_LABELS[status] ?: status,
                                onClick = {
                                    showStatusMenu = false
                                    onChangeStatus(status)
                                }
                            )
                        }
                    }
                }
            }

            // Description (if present)
            if (!goal.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = goal.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Section 2: Compact Progress (percentage stays the main element) ──
            val overall = progress?.overall ?: 0f
            Text(
                text = "پیشرفت کلی",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${overall.toInt().toEnglishDigits()}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { overall / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Section 3: Minimal Activity ──
            if (activeDaysText.isNotBlank()) {
                Text(
                    text = activeDaysText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            if (lastActivityText.isNotBlank()) {
                Text(
                    text = "آخرین حرکت: $lastActivityText",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
