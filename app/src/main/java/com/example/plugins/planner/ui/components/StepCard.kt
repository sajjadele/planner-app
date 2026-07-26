package com.example.plugins.planner.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityMessageModel
import com.example.plugins.planner.data.StepCardModel

/**
 * StepCard — Rich step card with activity preview.
 *
 * Architecture (Phase 4.9.3 + 4.9.4):
 * - Collapsed: title + summary
 * - Expanded: title + activity messages
 * - RTL compatible
 * - Neumorphic design system
 * - Material3 components
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

    // Debug: Log StepCard rendering
    Log.d("STEP_IMAGE_DEBUG", "🖼 StepCard: title=${model.title}, messages=${model.messages.size}")
    model.messages.forEachIndexed { idx, msg ->
        val imageAttachments = msg.attachments.filterIsInstance<ActivityAttachment.Image>()
        if (imageAttachments.isNotEmpty()) {
            Log.d("STEP_IMAGE_DEBUG", "🖼 Message[$idx] has ${imageAttachments.size} images")
            imageAttachments.forEach { img ->
                Log.d("STEP_IMAGE_DEBUG", "🖼 Image URI: ${img.uri}")
            }
        }
    }

    NeumorphicSurface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = 3
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

                    // Activity summary
                    if (model.hasRichContent()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = model.getActivitySummary(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        text = "فعالیت‌ها (${model.messages.size})",
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
                                text = "افزودن فعالیت",
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

/**
 * ActivityMessageCard — Shows a single activity message.
 */
@Composable
private fun ActivityMessageCard(
    message: ActivityMessageModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Message header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = getMessageIcon(message),
                    fontSize = 12.sp
                )
                Text(
                    text = getMessageLabel(message),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Text content
            message.text?.let { text ->
                if (text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Image attachments
            val images = message.attachments.filterIsInstance<ActivityAttachment.Image>()
            Log.d("STEP_IMAGE_DEBUG", "🎨 ActivityMessageCard: ${images.size} images to render")
            images.forEachIndexed { idx, img ->
                Log.d("STEP_IMAGE_DEBUG", "🎨 Rendering image[$idx]: ${img.uri}")
            }
            if (images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                images.forEach { image ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(android.net.Uri.parse(image.uri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "${RTL}تصویر پیوست",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Duration
            message.durationMinutes?.let { duration ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⏱️ $duration ${RTL}دقیقه",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Get icon for message type.
 */
private fun getMessageIcon(message: ActivityMessageModel): String {
    return when {
        message.isStep -> "✓"
        message.attachments.isNotEmpty() -> "📷"
        message.durationMinutes != null -> "📌"
        else -> "📝"
    }
}

/**
 * Get label for message type.
 */
private fun getMessageLabel(message: ActivityMessageModel): String {
    return when {
        message.isStep -> "مرحله"
        message.attachments.isNotEmpty() -> "تصویر"
        message.durationMinutes != null -> "فعالیت دستی"
        else -> "یادداشت"
    }
}
