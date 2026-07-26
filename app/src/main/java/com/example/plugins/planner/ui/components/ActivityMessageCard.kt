package com.example.plugins.planner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * ActivityMessageCard — Reusable component for rendering activity messages.
 *
 * Phase 4.12: Telegram-style Activity UI
 *
 * Renders:
 * - Text content
 * - Image attachments (with thumbnails)
 * - File attachments
 * - Duration
 * - Timestamp
 * - Activity type indicator
 *
 * Usage:
 * ```kotlin
 * ActivityMessageCard(message = activityMessageModel)
 * ```
 */
@Composable
fun ActivityMessageCard(
    message: ActivityMessageModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ── Header: Icon + Type Label ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = getMessageIcon(message),
                    fontSize = 14.sp
                )
                Text(
                    text = getMessageLabel(message),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Text Content ──
            message.text?.let { text ->
                if (text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }

            // ── Image Attachments ──
            val images = message.attachments.filterIsInstance<ActivityAttachment.Image>()
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
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── File Attachments ──
            val files = message.attachments.filterIsInstance<ActivityAttachment.File>()
            if (files.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                files.forEach { file ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "📎", fontSize = 12.sp)
                            Text(
                                text = file.name ?: "فایل پیوست",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ── Duration ──
            message.durationMinutes?.let { duration ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⏱️", fontSize = 11.sp)
                        Text(
                            text = "$duration ${RTL}دقیقه",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Timestamp ──
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Get icon for message type.
 */
private fun getMessageIcon(message: ActivityMessageModel): String {
    return when {
        message.isStep -> "✓"
        message.attachments.any { it is ActivityAttachment.Image } -> "📷"
        message.attachments.any { it is ActivityAttachment.File } -> "📎"
        message.durationMinutes != null -> "⏱️"
        else -> "📝"
    }
}

/**
 * Get label for message type.
 */
private fun getMessageLabel(message: ActivityMessageModel): String {
    return when {
        message.isStep -> "${RTL}مرحله"
        message.attachments.any { it is ActivityAttachment.Image } -> "${RTL}تصویر"
        message.attachments.any { it is ActivityAttachment.File } -> "${RTL}فایل"
        message.durationMinutes != null -> "${RTL}فعالیت"
        else -> "${RTL}یادداشت"
    }
}

/**
 * Format timestamp to Persian date/time.
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
