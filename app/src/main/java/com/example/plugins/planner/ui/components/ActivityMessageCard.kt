package com.example.plugins.planner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityMessageAction
import com.example.plugins.planner.data.ActivityMessageCapability
import com.example.plugins.planner.data.ActivityMessageModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * ActivityMessageCard — Enhanced Telegram-style message card.
 *
 * Phase 5.2.2: Activity Feed UX Layer
 *
 * Visual improvements:
 * - Telegram-like bubble with proper padding
 * - Timestamp positioned at bottom-right
 * - Better spacing between content elements
 * - Clean handling of image-only messages
 * - No visible JSON/URI/event type in UI
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityMessageCard(
    message: ActivityMessageModel,
    repliedToMessage: ActivityMessageModel? = null,
    onAction: ((ActivityMessageAction) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val capability = remember(message) { message.capability() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    if (capability.canEdit || capability.canDelete || capability.canReply) {
                        showMenu = true
                    }
                }
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // ── Reply Reference ──
            repliedToMessage?.let { replied ->
                ReplyReference(repliedToMessage = replied)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Text Content ──
            val hasText = !message.text.isNullOrBlank()
            if (hasText) {
                Text(
                    text = message.text!!,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            // ── Image Attachments ──
            val images = message.attachments.filterIsInstance<ActivityAttachment.Image>()
            if (images.isNotEmpty()) {
                if (hasText) Spacer(modifier = Modifier.height(8.dp))
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
            }

            // ── File Attachments (only show if no images) ──
            val files = message.attachments.filterIsInstance<ActivityAttachment.File>()
            if (files.isNotEmpty() && images.isEmpty()) {
                if (hasText) Spacer(modifier = Modifier.height(8.dp))
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

            // ── Duration (inline before timestamp) ──
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

            // ── Timestamp (bottom-right, Telegram style) ──
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = formatTimestamp(message.createdAt),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    // ── Context Menu ──
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        offset = DpOffset(x = 48.dp, y = 0.dp)
    ) {
        if (capability.canEdit) {
            DropdownMenuItem(
                text = { Text("${RTL}ویرایش", fontSize = 13.sp) },
                onClick = {
                    showMenu = false
                    onAction?.invoke(ActivityMessageAction.Edit(message.id))
                },
                leadingIcon = { Text("✏️", fontSize = 14.sp) }
            )
        }
        if (capability.canReply) {
            DropdownMenuItem(
                text = { Text("${RTL}پاسخ", fontSize = 13.sp) },
                onClick = {
                    showMenu = false
                    onAction?.invoke(ActivityMessageAction.Reply(message.id))
                },
                leadingIcon = { Text("↩", fontSize = 14.sp) }
            )
        }
        if (capability.canDelete) {
            DropdownMenuItem(
                text = { Text("${RTL}حذف", fontSize = 13.sp) },
                onClick = {
                    showMenu = false
                    onAction?.invoke(ActivityMessageAction.Delete(message.id))
                },
                leadingIcon = { Text("🗑", fontSize = 14.sp) }
            )
        }
    }
}

@Composable
private fun ReplyReference(repliedToMessage: ActivityMessageModel) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "↩",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${RTL}پاسخ به",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = repliedToMessage.text ?: "${RTL}پیام",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
