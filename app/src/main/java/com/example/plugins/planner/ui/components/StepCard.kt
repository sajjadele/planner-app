package com.example.plugins.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.StepCardModel

/**
 * StepCard — Rich step card with activity preview.
 *
 * Phase 4.12: Telegram-style Activity UI
 *
 * Architecture:
 * - Collapsed: title + summary stats (activity count, image count, file count)
 * - Expanded: title + activity messages using ActivityMessageCard
 * - RTL compatible
 * - Material3 components
 *
 * Summary Stats:
 * Shows when collapsed:
 * - 📝 X فعالیت
 * - 📷 X تصویر
 * - 📎 X فایل
 */
@Composable
fun StepCard(
    model: StepCardModel,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onAddActivity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ── Header: Checkbox + Title + Actions ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = model.isCompleted,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(22.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${RTL}${model.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (model.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // ── Summary Stats (Collapsed) ──
                    if (model.messages.isNotEmpty() && !isExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Activity count
                            val activityCount = model.messages.size
                            Text(
                                text = "📝 $activityCount ${RTL}فعالیت",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Image count
                            val imageCount = model.messages.sumOf { msg ->
                                msg.attachments.filterIsInstance<ActivityAttachment.Image>().size
                            }
                            if (imageCount > 0) {
                                Text(
                                    text = "📷 $imageCount ${RTL}تصویر",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // File count
                            val fileCount = model.messages.sumOf { msg ->
                                msg.attachments.filterIsInstance<ActivityAttachment.File>().size
                            }
                            if (fileCount > 0) {
                                Text(
                                    text = "📎 $fileCount ${RTL}فایل",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Expand/Collapse button
                if (model.messages.isNotEmpty()) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "${RTL}بستن" else "${RTL}باز کردن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "${RTL}حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── Expanded Content: Activity Messages ──
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Activity count header
                    Text(
                        text = "${RTL}فعالیت‌ها (${model.messages.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Activity messages
                    model.messages.forEach { message ->
                        ActivityMessageCard(message = message)
                    }

                    // Add activity button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddActivity(model.id.toInt()) },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${RTL}افزودن فعالیت",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
