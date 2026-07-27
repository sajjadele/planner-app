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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityMessageAction
import com.example.plugins.planner.data.ActivityMessageCapability
import com.example.plugins.planner.data.ActivityMessageDisplayContent
import com.example.plugins.planner.data.ActivityMessageModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * ActivityMessageCard — Telegram-style message bubble.
 *
 * Phase 5.7.1: Clean message display — no event labels, no step chips, no JSON/URI.
 *
 * Design:
 * - Bubble layout: max 85% width, RTL-aligned
 * - Padding: 10dp horizontal, 8dp vertical
 * - Corner radius: 14dp (uniform)
 * - Content order: media first, then text, then metadata
 * - Step/tag is NOT rendered inside the bubble — it's filter metadata only
 * - Content classified via [ActivityMessageDisplayContent]
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
    modifier: Modifier = Modifier,
    maxBubbleWidthFraction: Float = 0.85f
) {
    var showMenu by remember { mutableStateOf(false) }
    val capability = remember(message) { message.capability() }

    val displayContent = remember(message) {
        ActivityMessageDisplayContent.from(message)
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "cardBg"
    )

    // ── Empty / Deleted ──
    if (displayContent is ActivityMessageDisplayContent.EmptyMessage) return
    if (displayContent is ActivityMessageDisplayContent.DeletedMessage) {
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

    // ── Normal bubble ──
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
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
            shape = RoundedCornerShape(14.dp),
            shadowElevation = if (isSelected) 3.dp else 0.5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
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

                // ── Message Content (classified by ActivityMessageDisplayContent) ──
                MessageContent(
                    displayContent = displayContent,
                    onAttachmentClick = onAttachmentClick
                )

                // ── Metadata row: edited + timestamp ──
                MessageMetadataRow(
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
// MessageContent — renders based on simplified display types
// ════════════════════════════════════════════════════════════════

@Composable
private fun MessageContent(
    displayContent: ActivityMessageDisplayContent,
    onAttachmentClick: ((ActivityAttachment) -> Unit)? = null
) {
    when (displayContent) {
        is ActivityMessageDisplayContent.TextContent -> {
            MessageText(text = displayContent.text)
        }

        is ActivityMessageDisplayContent.MediaContent -> {
            if (displayContent.isImage) {
                ImagePreview(
                    image = displayContent.attachment as ActivityAttachment.Image,
                    onClick = { onAttachmentClick?.invoke(displayContent.attachment) }
                )
            } else {
                FilePreview(
                    file = displayContent.attachment as ActivityAttachment.File,
                    onClick = { onAttachmentClick?.invoke(displayContent.attachment) }
                )
            }
        }

        is ActivityMessageDisplayContent.MediaWithText -> {
            if (displayContent.isImage) {
                ImagePreview(
                    image = displayContent.attachment as ActivityAttachment.Image,
                    onClick = { onAttachmentClick?.invoke(displayContent.attachment) }
                )
            } else {
                FilePreview(
                    file = displayContent.attachment as ActivityAttachment.File,
                    onClick = { onAttachmentClick?.invoke(displayContent.attachment) }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            MessageText(text = displayContent.text)
        }

        is ActivityMessageDisplayContent.EmptyMessage,
        is ActivityMessageDisplayContent.DeletedMessage -> {}
    }
}

// ════════════════════════════════════════════════════════════════
// Sub-Composables
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
private fun ImagePreview(
    image: ActivityAttachment.Image,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .heightIn(max = 260.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${RTL}تصویر",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun FilePreview(
    file: ActivityAttachment.File,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "📄", fontSize = 16.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name ?: "فایل پیوست",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MessageMetadataRow(
    isEdited: Boolean,
    createdAt: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (isEdited) {
            Text(
                text = "✓✓",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = formatTimestamp(createdAt),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.End
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Deleted / Context Menu / Reply Reference
// ════════════════════════════════════════════════════════════════

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
                onLongClick = { if (capability.canDelete) onShowMenu() }
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
        DropdownMenu(expanded = true, onDismissRequest = onDismissMenu) {
            if (capability.canDelete) {
                DropdownMenuItem(
                    text = { Text("${RTL}حذف", fontSize = 13.sp) },
                    onClick = { onDismissMenu(); onAction?.invoke(ActivityMessageAction.Delete(messageId)) },
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
                onClick = { onDismiss(); onAction?.invoke(ActivityMessageAction.Reply(messageId)) },
                leadingIcon = { Text("↩️", fontSize = 14.sp) }
            )
        }
        if (capability.canEdit) {
            DropdownMenuItem(
                text = { Text("${RTL}ویرایش", fontSize = 13.sp) },
                onClick = { onDismiss(); onAction?.invoke(ActivityMessageAction.Edit(messageId)) },
                leadingIcon = { Text("✏️", fontSize = 14.sp) }
            )
        }
        if (capability.canDelete) {
            DropdownMenuItem(
                text = { Text("${RTL}حذف", fontSize = 13.sp) },
                onClick = { onDismiss(); onAction?.invoke(ActivityMessageAction.Delete(messageId)) },
                leadingIcon = { Text("🗑", fontSize = 14.sp) }
            )
        }
    }
}

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
                    text = if (isDeleted) "${RTL}پیام حذف شده" else "${RTL}پاسخ به",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDeleted)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.primary
                )
                if (!isDeleted && repliedToMessage != null) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
