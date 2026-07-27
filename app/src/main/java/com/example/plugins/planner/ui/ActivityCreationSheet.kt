package com.example.plugins.planner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityCreationAction

/**
 * ActivityCreationSheet — Bottom sheet with 4 creation options.
 *
 * Options:
 * - 📝 Note (opens composer)
 * - 📷 Image (opens image picker directly)
 * - 📎 File (opens composer in file mode)
 * - ⏱️ Manual Activity (opens composer with duration mode)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCreationSheet(
    onDismiss: () -> Unit,
    onSelectAction: (ActivityCreationAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "${RTL}ثبت فعالیت",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            CreationOption(
                icon = "📝",
                title = "${RTL}یادداشت",
                description = "${RTL}ثبت متن، خاطره یا یادداشت روزانه",
                onClick = {
                    onSelectAction(ActivityCreationAction.Note)
                    onDismiss()
                }
            )
            CreationOption(
                icon = "📷",
                title = "${RTL}تصویر",
                description = "${RTL}انتخاب تصویر از گالری",
                onClick = {
                    onSelectAction(ActivityCreationAction.Image)
                    onDismiss()
                }
            )
            CreationOption(
                icon = "📎",
                title = "${RTL}فایل",
                description = "${RTL}پیوست فایل به فعالیت",
                onClick = {
                    onSelectAction(ActivityCreationAction.File)
                    onDismiss()
                }
            )
            CreationOption(
                icon = "⏱️",
                title = "${RTL}فعالیت دستی",
                description = "${RTL}ثبت فعالیت با مدت‌زمان مشخص",
                onClick = {
                    onSelectAction(ActivityCreationAction.ManualActivity)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun CreationOption(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, fontSize = 24.sp)
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
