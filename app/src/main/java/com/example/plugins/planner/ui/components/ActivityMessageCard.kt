package com.example.plugins.planner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
import com.example.plugins.planner.ui.composer.formatDuration
import com.example.ui.screens.components.VisionMenuDivider
import com.example.ui.screens.components.VisionMenuItem
import com.example.ui.screens.components.VisionPopupMenu
import java.text.SimpleDateFormat
import java.util.*

/**
 * ActivityMessageCard — Telegram-style message bubble for Activity Feed.
 *
 * Phase 5.7.2: Full Telegram UX alignment:
 * - Asymmetric corners (Telegram self-message style)
 * - RTL text alignment for Persian
 * - Context menu anchored to the bubble
 * - Timestamp overlay on image-only messages
 * - Click image to open fullscreen viewer
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityMessageCard(
    message: ActivityMessageModel,
    repliedToMessage: ActivityMessageModel? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onAction: ((ActivityMessageAction) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
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

    // ── Normal message bubble ──
    Box(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                // Duration box on the left side (outside bubble)
                if (message.durationMinutes != null && message.durationMinutes > 0) {
                    DurationBox(durationMinutes = message.durationMinutes)
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(maxBubbleWidthFraction)
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    // In selection mode: toggle select/deselect
                                    onLongPress?.invoke()
                                } else {
                                    // Normal mode: toggle context menu
                                    if (showMenu) showMenu = false
                                    else if (capability.canEdit || capability.canDelete || capability.canReply) showMenu = true
                                }
                            },
                            onLongClick = {
                                // Always enter selection mode
                                onLongPress?.invoke()
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

                        // ── Message Content ──
                        MessageContent(
                            displayContent = displayContent,
                            onAttachmentClick = onAttachmentClick
                        )

                        // ── Metadata: edited + timestamp ──
                        MessageMetadataRow(
                            isEdited = message.isEdited,
                            createdAt = message.createdAt
                        )
                    }
                }
            }
        }

        // ── Separator line between activities ──
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                thickness = 1.dp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        // ── Context Menu (anchored to bubble via Box) ──
        ContextMenu(
            showMenu = showMenu,
            onDismiss = { showMenu = false },
            capability = capability,
            messageId = message.id,
            onAction = onAction
        )
    }
}

// ════════════════════════════════════════════════════════════════
// MessageContent
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

        is ActivityMessageDisplayContent.MultiMediaContent -> {
            // Images gallery (compact thumbnails)
            if (displayContent.images.isNotEmpty()) {
                ImageGallery(
                    images = displayContent.images,
                    onImageClick = { image -> onAttachmentClick?.invoke(image) }
                )
            }

            // Files list (compact)
            if (displayContent.files.isNotEmpty()) {
                if (displayContent.images.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                FileList(
                    files = displayContent.files,
                    onFileClick = { file -> onAttachmentClick?.invoke(file) }
                )
            }

            // Text (after attachments)
            if (!displayContent.text.isNullOrBlank()) {
                if (displayContent.images.isNotEmpty() || displayContent.files.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                MessageText(text = displayContent.text)
            }
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
            textAlign = TextAlign.Start
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MULTI-ATTACHMENT COMPOSABLES
// ══════════════════════════════════════════════════════════════

@Composable
private fun ImageGallery(
    images: List<ActivityAttachment.Image>,
    onImageClick: (ActivityAttachment.Image) -> Unit
) {
    when (images.size) {
        1 -> {
            // Single image: 80×80 thumbnail
            SingleImageThumbnail(
                image = images[0],
                onClick = { onImageClick(images[0]) }
            )
        }
        2 -> {
            // Two images: 2 thumbnails side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                images.forEach { image ->
                    SingleImageThumbnail(
                        image = image,
                        modifier = Modifier.weight(1f),
                        onClick = { onImageClick(image) }
                    )
                }
            }
        }
        else -> {
            // 3+ images: 2-column grid, all clickable
            val rows = images.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { rowImages ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowImages.forEach { image ->
                            SingleImageThumbnail(
                                image = image,
                                modifier = Modifier.weight(1f),
                                onClick = { onImageClick(image) }
                            )
                        }
                        // Fill empty slot if odd number
                        if (rowImages.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleImageThumbnail(
    image: ActivityAttachment.Image,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.uri)
                .size(200, 200) // Thumbnail size for performance
                .crossfade(true)
                .build(),
            contentDescription = "${RTL}تصویر",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun FileList(
    files: List<ActivityAttachment.File>,
    onFileClick: (ActivityAttachment.File) -> Unit
) {
    val visibleFiles = files.take(3)
    val remainingCount = files.size - 3

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        visibleFiles.forEach { file ->
            FileItem(
                file = file,
                onClick = { onFileClick(file) }
            )
        }
        if (remainingCount > 0) {
            Text(
                text = "${RTL}و ${remainingCount} فایل دیگر",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FileItem(
    file: ActivityAttachment.File,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = file.name ?: "${RTL}فایل",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
    backgroundColor: Color,
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
    VisionPopupMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss
    ) {
        if (capability.canReply) {
            VisionMenuItem(
                text = "${RTL}پاسخ",
                onClick = { onDismiss(); onAction?.invoke(ActivityMessageAction.Reply(messageId)) },
                leadingIcon = Icons.Default.Reply
            )
        }
        if (capability.canEdit) {
            VisionMenuItem(
                text = "${RTL}ویرایش",
                onClick = { onDismiss(); onAction?.invoke(ActivityMessageAction.Edit(messageId)) },
                leadingIcon = Icons.Default.Edit
            )
        }
        if (capability.canDelete) {
            VisionMenuDivider()
            VisionMenuItem(
                text = "${RTL}حذف",
                onClick = { onDismiss(); onAction?.invoke(ActivityMessageAction.Delete(messageId)) },
                leadingIcon = Icons.Default.DeleteOutline,
                iconTint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error
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
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Right
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

// ════════════════════════════════════════════════════════════════
// Duration Box (outside bubble, left side)
// ════════════════════════════════════════════════════════════════

@Composable
private fun DurationBox(durationMinutes: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = formatDuration(durationMinutes),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
