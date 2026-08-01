package com.example.plugins.planner.ui.composer

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment

/**
 * AttachmentPreview — Shows attached images/files in the composer.
 *
 * Vision Planner styled: Material icons, dark surface, rounded shapes.
 */
@Composable
fun AttachmentPreview(
    attachments: List<ActivityAttachment>,
    onRemoveAttachment: (ActivityAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { attachment ->
            when (attachment) {
                is ActivityAttachment.Image -> {
                    ImageAttachmentPreview(
                        uri = attachment.uri,
                        onRemove = { onRemoveAttachment(attachment) }
                    )
                }
                is ActivityAttachment.File -> {
                    FileAttachmentPreview(
                        name = attachment.name ?: "فایل",
                        onRemove = { onRemoveAttachment(attachment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageAttachmentPreview(
    uri: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(uri))
                .crossfade(true)
                .build(),
            contentDescription = "${RTL}پیش‌نمایش تصویر",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "${RTL}حذف تصویر",
                tint = Color.White,
                modifier = Modifier
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun FileAttachmentPreview(
    name: String,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = name,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "${RTL}حذف فایل",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * DurationChip — Shows duration in the composer.
 *
 * Vision Planner styled: Material icons, primary accent.
 */
@Composable
fun DurationChip(
    durationMinutes: Int?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (durationMinutes == null) return

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "$durationMinutes ${RTL}دقیقه",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "${RTL}حذف مدت",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
