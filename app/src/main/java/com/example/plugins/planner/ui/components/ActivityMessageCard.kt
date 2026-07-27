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
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // ── Reply Reference ──
            repliedToMessage?.let { replied ->
                ReplyReference(repliedToMessage = replied)
                Spacer(modifier = Modifier.height(8.dp))
            }

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

            // ── File Attachments ──
            val files = message.attachments.filterIsInstance<ActivityAttachment.File>()
            if (files.isNotEmpty() && images.isEmpty()) {
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

            // ── Duration ──
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
                text = formatTimestamp(message.createdAt),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
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

private fun hasMediaContent(message: ActivityMessageModel): Boolean {
    return message.attachments.isNotEmpty()
}

private fun hasOtherContentAfterMedia(message: ActivityMessageModel): Boolean {
    return message.text.isNullOrBlank().not() || message.durationMinutes != null
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
