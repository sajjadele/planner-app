package com.example.plugins.planner.ui.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityIntent

/**
 * ComposerToolbar — Bottom toolbar for the unified composer.
 *
 * Layout:
 * ┌────────────────────────────────────────┐
 * │ 📎   🖼   ⏱️   ✓ مرحله          ➤ │
 * └────────────────────────────────────────┘
 *
 * Actions:
 * - 📎 Add File (future)
 * - 🖼 Add Image
 * - ⏱️ Duration
 * - ✓ Convert to Step
 * - ➤ Submit
 */
@Composable
fun ComposerToolbar(
    intent: ActivityIntent,
    canSubmit: Boolean,
    onAddFile: () -> Unit,
    onAddImage: () -> Unit,
    onToggleDuration: () -> Unit,
    onToggleStep: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: attachment actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File button (disabled for now)
                ToolbarActionButton(
                    icon = "📎",
                    label = "${RTL}فایل",
                    enabled = false,
                    onClick = onAddFile
                )

                // Image button
                ToolbarActionButton(
                    icon = "🖼",
                    label = "${RTL}تصویر",
                    enabled = true,
                    onClick = onAddImage
                )

                // Duration button
                ToolbarActionButton(
                    icon = "⏱️",
                    label = "${RTL}مدت",
                    enabled = true,
                    onClick = onToggleDuration
                )

                // Step toggle
                val isStep = intent == ActivityIntent.STEP
                ToolbarActionButton(
                    icon = "✓",
                    label = "${RTL}مرحله",
                    enabled = true,
                    isActive = isStep,
                    onClick = onToggleStep
                )
            }

            // Right side: submit button
            SubmitButton(
                enabled = canSubmit,
                onClick = onSubmit
            )
        }
    }
}

@Composable
private fun ToolbarActionButton(
    icon: String,
    label: String,
    enabled: Boolean,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !enabled -> Color.Transparent
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                fontSize = 14.sp
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SubmitButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Icon(
            imageVector = Icons.Default.Send,
            contentDescription = "${RTL}ارسال",
            tint = if (enabled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}
