package com.example.plugins.planner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment

/**
 * ActivityAttachmentRenderer — Renders image and file attachments
 * as card-style previews in a Telegram-style message layout.
 *
 * Phase 4.13: Telegram-style Activity UI
 *
 * Each attachment is rendered independently:
 * - IMAGE: AsyncImage with rounded corners, clickable for fullscreen preview
 * - FILE: Card with file icon and filename
 *
 * Usage:
 * ```kotlin
 * ActivityAttachmentRenderer(attachments = message.attachments)
 * ```
 */
@Composable
fun ActivityAttachmentRenderer(
    attachments: List<ActivityAttachment>,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { attachment ->
            when (attachment) {
                is ActivityAttachment.Image -> ImageAttachmentPreview(attachment)
                is ActivityAttachment.File -> FileAttachmentPreview(attachment)
            }
        }
    }
}

@Composable
private fun ImageAttachmentPreview(
    image: ActivityAttachment.Image,
    modifier: Modifier = Modifier
) {
    var showViewer by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showViewer = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "${RTL}تصویر پیوست",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }

    if (showViewer) {
        // TODO: Fullscreen image viewer (Phase 5+)
        // For now, rely on the clickable surface to trigger Android system viewer
    }
}

@Composable
private fun FileAttachmentPreview(
    file: ActivityAttachment.File,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // File type icon
            Text(
                text = "📄",
                fontSize = 18.sp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name ?: "فایل پیوست",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${RTL}پیوست",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Download icon
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Download,
                contentDescription = "${RTL}دانلود",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
