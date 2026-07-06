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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plugins.planner.TaskEntity

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    val itemBgColor = if (task.isCompleted) Color.White else Color(0xFFEADDFF).copy(alpha = 0.3f)
    val itemBorderColor = if (task.isCompleted) Color(0xFFCAC4D0) else Color(0xFFEADDFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBgColor, RoundedCornerShape(16.dp))
            .border(1.dp, itemBorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (task.isCompleted) Color(0xFF6750A4) else Color.Transparent,
                    RoundedCornerShape(6.dp)
                )
                .border(
                    2.dp, Color(0xFF6750A4), RoundedCornerShape(6.dp)
                )
                .clip(RoundedCornerShape(6.dp))
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

        Spacer(modifier = Modifier.width(16.dp))

        // Task Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = if (task.isCompleted) Color(0xFF938F99) else Color(0xFF1C1B1F),
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
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val formattedTime = String.format(
                        "%02d:%02d", task.reminderHour, task.reminderMinute
                    )
                    Text(
                        text = formattedTime,
                        color = Color(0xFF49454F),
                        fontSize = 10.sp
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
                tint = Color(0xFFB3261E).copy(alpha = 0.7f)
            )
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
        "MEDIUM" -> Color(0xFF6750A4)
        else -> Color(0xFF49454F)
    }

    Box(
        modifier = Modifier
            .background(priorityColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
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
