@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.plugins.planner.ui

import android.net.Uri
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
import com.example.plugins.planner.data.ActivityMessageModel
import com.example.plugins.planner.data.StepDraft
import com.example.plugins.planner.ui.composer.ActivityComposerAction
import com.example.plugins.planner.ui.composer.ActivityComposerReducer
import com.example.plugins.planner.ui.composer.ActivityComposerState
import com.example.plugins.planner.ui.composer.ComposerMode
import com.example.plugins.planner.ui.composer.DurationPickerDialog
import com.example.plugins.planner.ui.composer.UnifiedComposerContent

@Composable
fun ActivityComposerBottomSheet(
    onDismiss: () -> Unit,
    onCreateActivity: (ActivityDraft) -> Unit,
    onCreateStep: (StepDraft) -> Unit,
    onUpdateActivity: ((Long, ActivityDraft) -> Unit)? = null,
    initialMessage: ActivityMessageModel? = null,
    replyToMessage: ActivityMessageModel? = null,
    initialDurationMinutes: Int? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    val initialMode = when {
        initialMessage != null -> ComposerMode.EDIT
        replyToMessage != null -> ComposerMode.REPLY
        else -> ComposerMode.ACTIVITY
    }

    val initialState = remember(initialMessage, replyToMessage, initialDurationMinutes) {
        when {
            initialMessage != null -> ActivityComposerState(
                text = initialMessage.text ?: "",
                attachments = initialMessage.attachments,
                durationMinutes = initialMessage.durationMinutes,
                mode = ComposerMode.EDIT,
                existingMessageId = initialMessage.id
            )
            replyToMessage != null -> ActivityComposerState(
                mode = ComposerMode.REPLY,
                replyToMessageId = replyToMessage.id
            )
            else -> ActivityComposerState(
                durationMinutes = initialDurationMinutes
            )
        }
    }

    var composerState by remember { mutableStateOf(initialState) }
    var showDurationPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentState by rememberUpdatedState(composerState)
    val dispatch = remember {
        { action: ActivityComposerAction ->
            composerState = ActivityComposerReducer.reduce(currentState, action)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
                dispatch(ActivityComposerAction.AddAttachment(ActivityAttachment.Image(uri.toString())))
            }
        }
    )

    val handleSubmit = remember(composerState) {
        {
            if (composerState.canSubmit()) {
                focusManager.clearFocus()
                when {
                    composerState.isEditMode() && composerState.existingMessageId != null -> {
                        onUpdateActivity?.invoke(
                            composerState.existingMessageId!!,
                            composerState.toActivityDraft()
                        )
                    }
                    composerState.isStepMode() -> {
                        onCreateStep(composerState.toStepDraft())
                    }
                    else -> {
                        onCreateActivity(composerState.toActivityDraft())
                    }
                }
                dispatch(ActivityComposerAction.Reset)
            }
        }
    }

    val headerText = when {
        composerState.isEditMode() -> "${RTL}ویرایش فعالیت"
        composerState.isReplyMode() -> "${RTL}پاسخ به فعالیت"
        composerState.isStepMode() -> "${RTL}ثبت مرحله جدید"
        else -> "${RTL}ثبت فعالیت"
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
                Text(
                    text = headerText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                UnifiedComposerContent(
                    state = composerState,
                    dispatch = dispatch,
                    onSubmit = handleSubmit,
                    onAddFile = { },
                    onAddImage = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onToggleDuration = { showDurationPicker = true },
                    onToggleStep = {
                        if (!composerState.isEditMode() && !composerState.isReplyMode()) {
                            dispatch(
                                if (composerState.isStepMode()) {
                                    ActivityComposerAction.ConvertToActivity
                                } else {
                                    ActivityComposerAction.ConvertToStep
                                }
                            )
                        }
                    },
                    onShowDurationPicker = { showDurationPicker = true }
                )
            }
        }

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
