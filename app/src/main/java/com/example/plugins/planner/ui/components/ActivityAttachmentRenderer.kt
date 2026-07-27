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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment

/**
 * ActivityAttachmentRenderer — Telegram-style media preview with no JSON/URI leakage.
 *
 * Phase 5.6: Telegram-style Activity Message Renderer Polish
 *
 * Rules:
 * - Image: max height 240.dp, 16:9 aspect ratio, ContentScale.Crop
 * - File: compact card with icon + filename
 * - No URI text or JSON displayed anywhere
 */
@Composable
fun ActivityAttachmentRenderer(
    attachments: List<ActivityAttachment>,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .heightIn(max = 240.dp)
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

@Composable
private fun FileAttachmentPreview(
    file: ActivityAttachment.File,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
            Text(
                text = "📄",
                fontSize = 16.sp
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

            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "${RTL}دانلود",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
