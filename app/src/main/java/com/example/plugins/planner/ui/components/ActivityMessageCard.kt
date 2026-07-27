package com.example.plugins.planner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * ActivityMessageCard — Telegram-style message card with full interaction support.
 *
 * Phase 5.3: Telegram-style Message Experience Polish
 *
 * Features:
 * - Visual selection state (highlight + elevation)
 * - Long press context menu (↩️ پاسخ, ✏️ ویرایش, 🗑 حذف)
 * - Reply reference with attachment indicator and deleted message handling
 * - Reply reference click → scroll to original message
 * - Inline duration display
 * - Small subtle timestamp with "ویرایش شده" indicator
 * - Attachment click callback (for future viewer)
 * - No visible JSON/URI/event type in UI
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityMessageCard(
    message: ActivityMessageModel,
    repliedToMessage: ActivityMessageModel? = null,
    isSelected: Boolean = false,
    onAction: ((ActivityMessageAction) -> Unit)? = null,
    onAttachmentClick: ((ActivityAttachment) -> Unit)? = null,
    onReplyReferenceClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val capability = remember(message) { message.capability() }

    // ── Animated selection highlight ──
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200),
        label = "cardBg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    // Click dismisses selection
                    if (showMenu) showMenu = false
                },
                onLongClick = {
                    if (capability.canEdit || capability.canDelete || capability.canReply) {
                        showMenu = true
                    }
                }
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // ── Reply Reference ──
            if (message.replyToMessageId != null) {
                ReplyReferencePreview(
                    repliedToMessage = repliedToMessage,
                    isDeleted = repliedToMessage == null,
                    onClick = {
                        if (repliedToMessage != null) {
                            onReplyReferenceClick?.invoke(repliedToMessage.id)
                        }
                    }
                )
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAttachmentClick?.invoke(images.first()) }
                ) {
                    ActivityAttachmentRenderer(
                        attachments = listOf(ActivityAttachment.Image(images.first().uri))
                    )
                }
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAttachmentClick?.invoke(files.first()) }
                ) {
                    ActivityAttachmentRenderer(attachments = listOf(files.first()))
                }
                if (files.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${RTL}+ ${files.size - 1} فایل دیگر",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // ── Bottom row: Duration + Timestamp (inline) ──
            val showBottomRow = message.durationMinutes != null || hasText || message.attachments.isNotEmpty()
            if (showBottomRow) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: inline duration
                    message.durationMinutes?.let { duration ->
                        Text(
                            text = "⏱ $duration ${RTL}دقیقه",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right: timestamp + edited indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.isEdited) {
                            Text(
                                text = "${RTL}ویرایش شده",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        Text(
                            text = formatTimestamp(message.createdAt),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // ── Context Menu (Telegram-style: ↩️ ✏️ 🗑) ──
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        offset = DpOffset(x = 48.dp, y = 0.dp)
    ) {
        if (capability.canReply) {
            DropdownMenuItem(
                text = { Text("${RTL}پاسخ", fontSize = 13.sp) },
                onClick = {
                    showMenu = false
                    onAction?.invoke(ActivityMessageAction.Reply(message.id))
                },
                leadingIcon = { Text("↩️", fontSize = 14.sp) }
            )
        }
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

/**
 * ReplyReferencePreview — Improved reply reference with Telegram-style layout.
 *
 * Shows:
 * - Original message short preview
 * - Attachment type indicator
 * - Handles deleted original message gracefully
 * - Clickable to navigate to original message
 */
@Composable
private fun ReplyReferencePreview(
    repliedToMessage: ActivityMessageModel?,
    isDeleted: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reply indicator bar (Telegram style vertical line)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDeleted) "${RTL}پیام حذف شده"
                    else "${RTL}پاسخ به",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDeleted)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.primary
                )
                if (!isDeleted && repliedToMessage != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Attachment indicator
                        val hasImage = repliedToMessage.attachments.any { it is ActivityAttachment.Image }
                        val hasFile = repliedToMessage.attachments.any { it is ActivityAttachment.File }
                        if (hasImage) {
                            Text("📷", fontSize = 10.sp)
                        } else if (hasFile) {
                            Text("📎", fontSize = 10.sp)
                        }

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
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
