package com.example.plugins.planner.ui.composer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL

/**
 * ComposerToolbar — Bottom toolbar for the unified composer.
 *
 * Vision Planner styled: Material icons, dark surface, rounded shapes.
 *
 * Layout:
 * ┌────────────────────────────────────┐
 * │ 📎   🖼   ⏱️                ➤ │
 * └────────────────────────────────────┘
 */
@Composable
fun ComposerToolbar(
    canSubmit: Boolean,
    onAddFile: () -> Unit,
    onAddImage: () -> Unit,
    onToggleDuration: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                    icon = Icons.Default.Attachment,
                    label = "${RTL}فایل",
                    enabled = false,
                    onClick = onAddFile
                )

                // Image button
                ToolbarActionButton(
                    icon = Icons.Default.Image,
                    label = "${RTL}تصویر",
                    enabled = true,
                    onClick = onAddImage
                )

                // Duration button
                ToolbarActionButton(
                    icon = Icons.Default.Schedule,
                    label = "${RTL}مدت",
                    enabled = true,
                    onClick = onToggleDuration
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
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SubmitButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = 36.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Send,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${RTL}ثبت",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
