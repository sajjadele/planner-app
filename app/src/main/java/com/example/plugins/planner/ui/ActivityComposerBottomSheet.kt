@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.plugins.planner.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.StepDraft
import com.example.plugins.planner.ui.composer.ActivityComposerAction
import com.example.plugins.planner.ui.composer.ActivityComposerReducer
import com.example.plugins.planner.ui.composer.ActivityComposerState
import com.example.plugins.planner.ui.composer.DurationPickerDialog
import com.example.plugins.planner.ui.composer.UnifiedComposerContent

/**
 * ActivityComposerBottomSheet — Telegram-style unified activity composer.
 *
 * Architecture (Phase 4.7.4):
 * - Single text input for all activity types
 * - Inline attachment previews
 * - Duration picker
 * - Convert to step
 * - State-driven with reducer pattern
 *
 * Layout:
 * ┌─────────────────────────────────────┐
 * │  ثبت مورد جدید                      │
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
 * │  📎   🖼   ⏱️   ✓ مرحله     ➤ │
 * └─────────────────────────────────────┘
 */
@Composable
fun ActivityComposerBottomSheet(
    onDismiss: () -> Unit,
    onCreateActivity: (ActivityDraft) -> Unit,
    onCreateStep: (StepDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    // ── State-driven composer ──
    var composerState by remember { mutableStateOf(ActivityComposerState()) }
    var showDurationPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // ── Dispatch helper (use rememberUpdatedState to avoid stale closure) ──
    val currentState by rememberUpdatedState(composerState)
    val dispatch = remember {
        { action: ActivityComposerAction ->
            composerState = ActivityComposerReducer.reduce(currentState, action)
        }
    }

    // ── Image picker launcher ──
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            Log.d("COMPOSER_DEBUG", "📸 Image picker returned: uri=$uri")
            if (uri != null) {
                // Take persistable permission so Coil can load the image
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Log.d("COMPOSER_DEBUG", "📸 Took persistable URI permission")
                } catch (e: Exception) {
                    Log.w("COMPOSER_DEBUG", "📸 Could not take persistable permission: ${e.message}")
                }
                
                Log.d("COMPOSER_DEBUG", "📸 Dispatching AddAttachment action")
                dispatch(ActivityComposerAction.AddAttachment(ActivityAttachment.Image(uri.toString())))
                Log.d("COMPOSER_DEBUG", "📸 After dispatch, composerState.attachments.size=${composerState.attachments.size}")
            } else {
                Log.d("COMPOSER_DEBUG", "📸 Image picker returned null uri")
            }
        }
    )

    // ── Submit handler ──
    val handleSubmit = remember(composerState) {
        {
            if (composerState.canSubmit()) {
                focusManager.clearFocus()
                if (composerState.isStepMode()) {
                    onCreateStep(composerState.toStepDraft())
                } else {
                    onCreateActivity(composerState.toActivityDraft())
                }
                dispatch(ActivityComposerAction.Reset)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            val scrollState = rememberScrollState()
            val attachmentCount = composerState.attachments.size

            LaunchedEffect(attachmentCount) {
                if (attachmentCount > 0) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                // ── Header ──
                Text(
                    text = "${RTL}ثبت مورد جدید",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                // ── Unified Composer Content ──
                UnifiedComposerContent(
                    state = composerState,
                    dispatch = dispatch,
                    onSubmit = handleSubmit,
                    onAddFile = { /* Future: file picker */ },
                    onAddImage = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onToggleDuration = { showDurationPicker = true },
                    onToggleStep = {
                        dispatch(
                            if (composerState.intent == com.example.plugins.planner.data.ActivityIntent.STEP) {
                                ActivityComposerAction.ConvertToActivity
                            } else {
                                ActivityComposerAction.ConvertToStep
                            }
                        )
                    },
                    onShowDurationPicker = { showDurationPicker = true }
                )
            }
        }

        // ── Duration Picker Dialog ──
        if (showDurationPicker) {
            DurationPickerDialog(
                onDismiss = { showDurationPicker = false },
                onConfirm = { minutes ->
                    dispatch(ActivityComposerAction.DurationChanged(minutes))
                    showDurationPicker = false
                }
            )
        }
    }
}
