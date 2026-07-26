package com.example.plugins.planner.ui.composer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityIntent

/**
 * UnifiedComposerContent — Telegram-style unified composer.
 *
 * Layout:
 * ┌─────────────────────────────────────┐
 * │                                     │
 * │  چیزی که انجام دادی...             │
 * │                                     │
 * │  ┌─────────────────────────────┐   │
 * │  │      image preview          │   │
 * │  └─────────────────────────────┘   │
 * │                                     │
 * │  ┌─────────────────────────────┐   │
 * │  │  ⏱️ ۹۰ دقیقه               │   │
 * │  └─────────────────────────────┘   │
 * │                                     │
 * └─────────────────────────────────────┘
 * │ 📎   🖼   ⏱️   ✓ مرحله     ➤ │
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
    onToggleStep: () -> Unit,
    onShowDurationPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ── Debug: Log received state ──
        Log.d("COMPOSER_DEBUG", "🎨 UnifiedComposerContent received state: attachments.size=${state.attachments.size}, text='${state.text}', intent=${state.intent}")
        state.attachments.forEachIndexed { index, attachment ->
            Log.d("COMPOSER_DEBUG", "🎨   attachment[$index]: type=${attachment::class.simpleName}, uri=${(attachment as? com.example.plugins.planner.data.ActivityAttachment.Image)?.uri}")
        }
        // ── Step indicator ──
        if (state.intent == ActivityIntent.STEP) {
            StepIndicator()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Text input ──
        OutlinedTextField(
            value = state.text,
            onValueChange = { dispatch(ActivityComposerAction.TextChanged(it)) },
            placeholder = {
                Text(
                    "${RTL}چیزی که انجام دادی...",
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
            Log.d("COMPOSER_DEBUG", "🎨 Rendering AttachmentPreview section")
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Green.copy(alpha = 0.2f)) // DEBUG: visible background
                    .onGloballyPositioned { coordinates ->
                        val posInWindow = coordinates.positionInWindow()
                        val posInRoot = coordinates.positionInRoot()
                        Log.d("COMPOSER_DEBUG", "🎨 AttachmentPreview container posInWindow=(${posInWindow.x.toInt()},${posInWindow.y.toInt()}) posInRoot=(${posInRoot.x.toInt()},${posInRoot.y.toInt()}) size=${coordinates.size.width}x${coordinates.size.height}")
                    }
            ) {
                AttachmentPreview(
                    attachments = state.attachments,
                    onRemoveAttachment = { attachment ->
                        dispatch(ActivityComposerAction.RemoveAttachment(attachment))
                    }
                )
            }
            Log.d("COMPOSER_DEBUG", "🎨 AttachmentPreview section DONE")
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
            intent = state.intent,
            canSubmit = state.canSubmit(),
            onAddFile = onAddFile,
            onAddImage = onAddImage,
            onToggleDuration = onToggleDuration,
            onToggleStep = onToggleStep,
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun StepIndicator() {
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
            Text(text = "✓", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                text = "${RTL}مرحله جدید",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
