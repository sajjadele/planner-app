package com.example.plugins.planner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plugins.planner.TaskEntity
import com.example.ui.theme.*

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (task.isCompleted) SurfaceWhite else Color(0xFFF5F0FF).copy(alpha = 0.6f)

    NeumorphicSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = if (task.isCompleted) 4 else 6,
        backgroundColor = cardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .shadow(3.dp, RoundedCornerShape(7.dp))
                    .background(
                        if (task.isCompleted) AccentPurple else SurfaceWhite,
                        RoundedCornerShape(7.dp)
                    )
                    .border(
                        2.dp, AccentPurple, RoundedCornerShape(7.dp)
                    )
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(onClick = onToggleCompletion)
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Task Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (task.isCompleted) TextTertiary else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (task.priority != null) {
                        PriorityChip(priority = task.priority)
                    }

                    if (task.reminderHour != null && task.reminderMinute != null) {
                        Spacer(modifier = Modifier.width(12.dp))

                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Alarm scheduled",
                            tint = AccentPurple,
                            modifier = Modifier.size(12.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        val formattedTime = String.format(
                            "%02d:%02d", task.reminderHour, task.reminderMinute
                        )
                        Text(
                            text = formattedTime,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Delete action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = Color(0xFFB3261E).copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(priority: String?) {
    val priorityLabel = when (priority) {
        "HIGH" -> "اولویت بالا"
        "MEDIUM" -> "اولویت متوسط"
        else -> "اولویت پایین"
    }

    val priorityColor = when (priority) {
        "HIGH" -> Color(0xFFB3261E)
        "MEDIUM" -> AccentPurple
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .background(priorityColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priorityLabel,
            color = priorityColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
