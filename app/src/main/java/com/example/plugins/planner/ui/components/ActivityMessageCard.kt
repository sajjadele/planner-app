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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityMessageAction
import com.example.plugins.planner.data.ActivityMessageCapability
import com.example.plugins.planner.data.ActivityMessageDisplayContent
import com.example.plugins.planner.data.ActivityMessageModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * ActivityMessageCard — Telegram-style message bubble for the Activity Feed.
 *
 * Phase 5.6: Telegram-style Activity Message Renderer Polish
 *
 * Design:
 * - Bubble layout: max 85% width, RTL-aligned
 * - Small padding, compact spacing between messages
 * - Timestamp at bottom-right, inline with duration
 * - No full-width cards, no JSON/URI leakage
 * - Content classified via [ActivityMessageDisplayContent]
 *
 * Bubble shape (Telegram self-message):
 *   topStart=16.dp, topEnd=16.dp, bottomStart=4.dp, bottomEnd=16.dp
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityMessageCard(
    message: ActivityMessageModel,
    stepName: String? = null,
    repliedToMessage: ActivityMessageModel? = null,
    isSelected: Boolean = false,
    onAction: ((ActivityMessageAction) -> Unit)? = null,
    onAttachmentClick: ((ActivityAttachment) -> Unit)? = null,
    onReplyReferenceClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxBubbleWidthFraction: Float = 0.85f
) {
    var showMenu by remember { mutableStateOf(false) }
    val capability = remember(message) { message.capability() }

    // Resolve display content
    val displayContent = remember(message) {
        ActivityMessageDisplayContent.from(message)
    }

    // ── Animated selection highlight ──
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "cardBg"
    )

    // ── Empty / Deleted messages ──
    if (displayContent is ActivityMessageDisplayContent.EmptyMessage) {
        // Should never reach UI — render nothing
        return
    }
    if (displayContent is ActivityMessageDisplayContent.DeletedMessage) {
        // Render deleted message placeholder
        DeletedMessageBubble(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = backgroundColor,
            capability = capability,
            showMenu = showMenu,
            onShowMenu = { showMenu = true },
            onDismissMenu = { showMenu = false },
            onAction = onAction,
            messageId = message.id
        )
        return
    }

    // ── Normal message bubble ──
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End  // RTL: right-aligned
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(maxBubbleWidthFraction)
                .combinedClickable(
                    onClick = { if (showMenu) showMenu = false },
                    onLongClick = {
                        if (capability.canEdit || capability.canDelete || capability.canReply) {
                            showMenu = true
                        }
                    }
                ),
            color = backgroundColor,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = 4.dp, bottomEnd = 16.dp
            ),
            shadowElevation = if (isSelected) 3.dp else 0.5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
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
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // ── Content (classified by ActivityMessageDisplayContent) ──
                MessageContent(
                    displayContent = displayContent,
                    onAttachmentClick = onAttachmentClick
                )

                // ── Step Tag Chip ──
                if (stepName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ActivityTagChip(name = stepName)
                }

                // ── Bottom metadata row: duration + timestamp ──
                MessageMetadataRow(
                    displayContent = displayContent,
                    isEdited = message.isEdited,
                    createdAt = message.createdAt
                )
            }
        }
    }

    // ── Context Menu ──
    ContextMenu(
        showMenu = showMenu,
        onDismiss = { showMenu = false },
        capability = capability,
        messageId = message.id,
        onAction = onAction
    )
}

// ════════════════════════════════════════════════════════════════
// MessageContent — Renders based on display content type
// ════════════════════════════════════════════════════════════════

@Composable
private fun MessageContent(
    displayContent: ActivityMessageDisplayContent,
    onAttachmentClick: ((ActivityAttachment) -> Unit)? = null
) {
    when (displayContent) {
        is ActivityMessageDisplayContent.TextOnly -> {
            MessageText(text = displayContent.text)
        }

        is ActivityMessageDisplayContent.ImageOnly -> {
            ClickableImagePreview(
                image = displayContent.image,
                onClick = { onAttachmentClick?.invoke(displayContent.image) }
            )
        }

        is ActivityMessageDisplayContent.TextWithImages -> {
            MessageText(text = displayContent.text)
            Spacer(modifier = Modifier.height(6.dp))
            displayContent.images.forEach { image ->
                ClickableImagePreview(
                    image = image,
                    onClick = { onAttachmentClick?.invoke(image) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        is ActivityMessageDisplayContent.FileOnly -> {
            FilePreview(
                file = displayContent.file,
                onClick = { onAttachmentClick?.invoke(displayContent.file) }
            )
        }

        is ActivityMessageDisplayContent.TextWithFiles -> {
            MessageText(text = displayContent.text)
            Spacer(modifier = Modifier.height(6.dp))
            displayContent.files.forEach { file ->
                FilePreview(
                    file = file,
                    onClick = { onAttachmentClick?.invoke(file) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        is ActivityMessageDisplayContent.DurationActivity -> {
            if (displayContent.text != null) {
                MessageText(text = displayContent.text)
                Spacer(modifier = Modifier.height(4.dp))
            }
            DurationBadge(durationMinutes = displayContent.durationMinutes)
        }

        // Should never reach UI
        is ActivityMessageDisplayContent.EmptyMessage,
        is ActivityMessageDisplayContent.DeletedMessage -> {
            // Handled in parent
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Sub-composables
// ════════════════════════════════════════════════════════════════

@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 10,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 20.sp
    )
}

@Composable
private fun ClickableImagePreview(
    image: ActivityAttachment.Image,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        ActivityAttachmentRenderer(
            attachments = listOf(ActivityAttachment.Image(image.uri))
        )
    }
}

@Composable
private fun FilePreview(
    file: ActivityAttachment.File,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        ActivityAttachmentRenderer(
            attachments = listOf(file)
        )
    }
}

@Composable
private fun DurationBadge(durationMinutes: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "⏱ $durationMinutes ${RTL}دقیقه",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MessageMetadataRow(
    displayContent: ActivityMessageDisplayContent,
    isEdited: Boolean,
    createdAt: Long
) {
    // Duration activities show duration above; others show timestamp
    val showTimestamp = displayContent !is ActivityMessageDisplayContent.EmptyMessage

    if (showTimestamp) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Edited indicator
            if (isEdited) {
                Text(
                    text = "✓✓",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }

            // Timestamp
            Text(
                text = formatTimestamp(createdAt),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.End
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeletedMessageBubble(
    modifier: Modifier = Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color,
    capability: ActivityMessageCapability,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onAction: ((ActivityMessageAction) -> Unit)?,
    messageId: Long
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (capability.canDelete) onShowMenu()
                }
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${RTL}پیام حذف شده",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showMenu) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismissMenu
        ) {
            if (capability.canDelete) {
                DropdownMenuItem(
                    text = { Text("${RTL}حذف", fontSize = 13.sp) },
                    onClick = {
                        onDismissMenu()
                        onAction?.invoke(ActivityMessageAction.Delete(messageId))
                    },
                    leadingIcon = { Text("🗑", fontSize = 14.sp) }
                )
            }
        }
    }
}

@Composable
private fun ContextMenu(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    capability: ActivityMessageCapability,
    messageId: Long,
    onAction: ((ActivityMessageAction) -> Unit)?
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 48.dp, y = 0.dp)
    ) {
        if (capability.canReply) {
            DropdownMenuItem(
                text = { Text("${RTL}پاسخ", fontSize = 13.sp) },
                onClick = {
                    onDismiss()
                    onAction?.invoke(ActivityMessageAction.Reply(messageId))
                },
                leadingIcon = { Text("↩️", fontSize = 14.sp) }
            )
        }
        if (capability.canEdit) {
            DropdownMenuItem(
                text = { Text("${RTL}ویرایش", fontSize = 13.sp) },
                onClick = {
                    onDismiss()
                    onAction?.invoke(ActivityMessageAction.Edit(messageId))
                },
                leadingIcon = { Text("✏️", fontSize = 14.sp) }
            )
        }
        if (capability.canDelete) {
            DropdownMenuItem(
                text = { Text("${RTL}حذف", fontSize = 13.sp) },
                onClick = {
                    onDismiss()
                    onAction?.invoke(ActivityMessageAction.Delete(messageId))
                },
                leadingIcon = { Text("🗑", fontSize = 14.sp) }
            )
        }
    }
}

/**
 * ReplyReferencePreview — Reply reference bar (Telegram style).
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
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical reply bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.width(6.dp))

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
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val hasImage = repliedToMessage.attachments.any { it is ActivityAttachment.Image }
                        val hasFile = repliedToMessage.attachments.any { it is ActivityAttachment.File }
                        if (hasImage) Text("📷", fontSize = 10.sp)
                        else if (hasFile) Text("📎", fontSize = 10.sp)

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
