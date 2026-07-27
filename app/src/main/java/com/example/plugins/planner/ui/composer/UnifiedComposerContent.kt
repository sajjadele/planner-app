package com.example.plugins.planner.ui.composer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment

/**
 * UnifiedComposerContent — Telegram-style unified composer.
 *
 * Phase 5.9.1: Remove StepIndicator and step mode — Composer is Activity-only.
 *
 * Layout:
 * ┌─────────────────────────────────────┐
 * │  [EDIT/REPLY indicator]             │
 * │  متن فعالیت                         │
 * │  ┌─────────────────────────────┐   │
 * │  │      image preview          │   │
 * │  └─────────────────────────────┘   │
 * │  ┌─────────────────────────────┐   │
 * │  │  ⏱️ 90 دقیقه               │   │
 * │  └─────────────────────────────┘   │
 * │ 📎   🖼   ⏱️                ➤ │
 * └─────────────────────────────────────┘
 */
@Composable
fun UnifiedComposerContent(
    state: ActivityComposerState,
    dispatch: (ActivityComposerAction) -> Unit,
    onSubmit: () -> Unit,
    onAddFile: () -> Unit,
    onAddImage: () -> Unit,
    onToggleDuration: () -> Unit,
    onShowDurationPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ── Mode indicator ──
        when {
            state.isEditMode() -> {
                EditIndicator()
                Spacer(modifier = Modifier.height(8.dp))
            }
            state.isReplyMode() -> {
                ReplyIndicator()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ── Text input ──
        OutlinedTextField(
            value = state.text,
            onValueChange = { dispatch(ActivityComposerAction.TextChanged(it)) },
            placeholder = {
                Text(
                    "${RTL}متن فعالیت",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            maxLines = 6
        )

        // ── Attachments preview ──
        if (state.attachments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            AttachmentPreview(
                attachments = state.attachments,
                onRemoveAttachment = { attachment ->
                    dispatch(ActivityComposerAction.RemoveAttachment(attachment))
                }
            )
        }

        // ── Duration chip ──
        if (state.durationMinutes != null) {
            Spacer(modifier = Modifier.height(8.dp))
            DurationChip(
                durationMinutes = state.durationMinutes,
                onRemove = {
                    dispatch(ActivityComposerAction.DurationChanged(null))
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Toolbar ──
        ComposerToolbar(
            canSubmit = state.canSubmit(),
            onAddFile = onAddFile,
            onAddImage = onAddImage,
            onToggleDuration = onToggleDuration,
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun EditIndicator() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "✏️", fontSize = 14.sp)
            Text(
                text = "${RTL}ویرایش فعالیت",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun ReplyIndicator() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "↩", fontSize = 14.sp)
            Text(
                text = "${RTL}پاسخ به فعالیت",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
