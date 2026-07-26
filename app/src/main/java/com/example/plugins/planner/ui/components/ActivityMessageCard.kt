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
 * ActivityMessageCard — Telegram-style message renderer.
 *
 * Phase 4.13: Telegram-style Activity UI
 *
 * Renders a user-created message as a chat-like bubble:
 * - Text content as the main body
 * - Image previews with aspect ratio
 * - File attachment count footer
 * - Duration shown inline with text
 * - Timestamp subtle at bottom
 *
 * Does NOT render:
 * - Event type labels (no "NOTE_ADDED", "IMAGE_ADDED" headers)
 * - Raw JSON or URIs
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
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // ── Text Content ──
            message.text?.let { text ->
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    if (hasMediaContent(message)) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // ── Image Attachments ──
            val images = message.attachments.filterIsInstance<ActivityAttachment.Image>()
            if (images.isNotEmpty()) {
                ActivityAttachmentRenderer(
                    attachments = listOf(ActivityAttachment.Image(images.first().uri))
                )
                if (images.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${RTL}+ ${images.size - 1} تصویر دیگر",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                if (hasOtherContentAfterMedia(message)) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── File Attachments (count only, not individual cards in message view) ──
            val files = message.attachments.filterIsInstance<ActivityAttachment.File>()
            if (files.isNotEmpty() && images.isEmpty()) {
                // Show single file preview when no images
                ActivityAttachmentRenderer(attachments = listOf(files.first()))
                if (files.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${RTL}+ ${files.size - 1} فایل دیگر",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // ── Duration (inline, subtle) ──
            message.durationMinutes?.let { duration ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "⏱", fontSize = 12.sp)
                    Text(
                        text = "$duration ${RTL}دقیقه",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Timestamp ──
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Check if the message has media content (images or files).
 */
private fun hasMediaContent(message: ActivityMessageModel): Boolean {
    return message.attachments.isNotEmpty()
}

/**
 * Check if there's content after media (text or duration).
 */
private fun hasOtherContentAfterMedia(message: ActivityMessageModel): Boolean {
    return message.text.isNullOrBlank().not() || message.durationMinutes != null
}

/**
 * Format timestamp to Persian time.
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
