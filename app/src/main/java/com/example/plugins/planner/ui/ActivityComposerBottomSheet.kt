@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.plugins.planner.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.util.RTL

enum class ComposerMode {
    NONE,
    STEP,
    NOTE,
    MANUAL_ACTIVITY,
    IMAGE
}

@Composable
fun ActivityComposerBottomSheet(
    onDismiss: () -> Unit,
    onAddStep: (String) -> Unit,
    onAddNote: (String) -> Unit,
    onAddManualActivity: (String, Int?) -> Unit,
    onAddImage: (String, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var composerMode by remember { mutableStateOf(ComposerMode.NONE) }
    var stepTitle by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var manualTitle by remember { mutableStateOf("") }
    var manualDuration by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageDescription by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            selectedImageUri = uri
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${RTL}ثبت مورد جدید",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when (composerMode) {
                ComposerMode.NONE -> {
                    ComposerOption(
                        icon = "✓",
                        label = "${RTL}مرحله جدید",
                        enabled = true,
                        onClick = { composerMode = ComposerMode.STEP }
                    )
                    ComposerOption(
                        icon = "📝",
                        label = "${RTL}یادداشت",
                        subtitle = "${RTL}افزودن توضیح یا ثبت یک فکر",
                        enabled = true,
                        onClick = { composerMode = ComposerMode.NOTE }
                    )
                    ComposerOption(
                        icon = "📎",
                        label = "${RTL}فایل",
                        subtitle = "(${RTL}به زودی)",
                        enabled = false,
                        onClick = {}
                    )
                    ComposerOption(
                        icon = "📷",
                        label = "${RTL}تصویر",
                        subtitle = "${RTL}افزودن یک تصویر به فعالیتها",
                        enabled = true,
                        onClick = { composerMode = ComposerMode.IMAGE }
                    )
                    ComposerOption(
                        icon = "📌",
                        label = "${RTL}فعالیت دستی",
                        subtitle = "${RTL}ثبت کاری که انجام دادی",
                        enabled = true,
                        onClick = { composerMode = ComposerMode.MANUAL_ACTIVITY }
                    )
                }
                ComposerMode.STEP -> {
                    StepExpansion(
                        stepTitle = stepTitle,
                        onStepTitleChange = { stepTitle = it },
                        onDismiss = {
                            composerMode = ComposerMode.NONE
                            stepTitle = ""
                        },
                        onSubmit = {
                            if (stepTitle.isNotBlank()) {
                                focusManager.clearFocus()
                                onAddStep(stepTitle)
                                composerMode = ComposerMode.NONE
                                stepTitle = ""
                            }
                        }
                    )
                }
                ComposerMode.NOTE -> {
                    NoteExpansion(
                        noteText = noteText,
                        onNoteTextChange = { noteText = it },
                        onDismiss = {
                            composerMode = ComposerMode.NONE
                            noteText = ""
                        },
                        onSubmit = {
                            if (noteText.isNotBlank()) {
                                focusManager.clearFocus()
                                onAddNote(noteText)
                                composerMode = ComposerMode.NONE
                                noteText = ""
                            }
                        }
                    )
                }
                ComposerMode.IMAGE -> {
                    ImageExpansion(
                        selectedImageUri = selectedImageUri,
                        imageDescription = imageDescription,
                        onImageDescriptionChange = { imageDescription = it },
                        onSelectImage = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemoveImage = { selectedImageUri = null },
                        onDismiss = {
                            composerMode = ComposerMode.NONE
                            selectedImageUri = null
                            imageDescription = ""
                        },
                        onSubmit = {
                            if (selectedImageUri != null) {
                                onAddImage(
                                    selectedImageUri.toString(),
                                    imageDescription.ifBlank { null }
                                )
                                composerMode = ComposerMode.NONE
                                selectedImageUri = null
                                imageDescription = ""
                                focusManager.clearFocus()
                            }
                        }
                    )
                }
                ComposerMode.MANUAL_ACTIVITY -> {
                    ManualActivityExpansion(
                        manualTitle = manualTitle,
                        manualDuration = manualDuration,
                        onManualTitleChange = { manualTitle = it },
                        onManualDurationChange = { manualDuration = it },
                        onDismiss = {
                            composerMode = ComposerMode.NONE
                            manualTitle = ""
                            manualDuration = ""
                        },
                        onSubmit = {
                            if (manualTitle.isNotBlank() && manualDuration.isValidDuration()) {
                                focusManager.clearFocus()
                                onAddManualActivity(manualTitle, manualDuration.toIntOrNull())
                                composerMode = ComposerMode.NONE
                                manualTitle = ""
                                manualDuration = ""
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepExpansion(
    stepTitle: String,
    onStepTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("✓", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${RTL}مرحله جدید",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = stepTitle,
                onValueChange = onStepTitleChange,
                placeholder = { Text("${RTL}عنوان مرحله", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() })
            )
            FooterButtons(onDismiss = onDismiss, onSubmit = onSubmit, enabled = stepTitle.isNotBlank())
        }
    }
}

@Composable
private fun NoteExpansion(
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("📝", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${RTL}یادداشت جدید",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteTextChange,
                placeholder = { Text("${RTL}افزودن توضیح یا ثبت یک فکر", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() })
            )
            FooterButtons(onDismiss = onDismiss, onSubmit = onSubmit, enabled = noteText.isNotBlank())
        }
    }
}

@Composable
private fun ManualActivityExpansion(
    manualTitle: String,
    manualDuration: String,
    onManualTitleChange: (String) -> Unit,
    onManualDurationChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("📌", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${RTL}فعالیت دستی",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = manualTitle,
                onValueChange = onManualTitleChange,
                placeholder = { Text("${RTL}عنوان فعالیت", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() })
            )
            OutlinedTextField(
                value = manualDuration,
                onValueChange = onManualDurationChange,
                placeholder = { Text("${RTL}مدت زمان (دقیقه، اختیاری)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            FooterButtons(onDismiss = onDismiss, onSubmit = onSubmit, enabled = manualTitle.isNotBlank() && manualDuration.isValidDuration())
        }
    }
}

@Composable
private fun FooterButtons(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) {
            Text("${RTL}لغو", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSubmit,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("${RTL}ثبت", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ComposerOption(
    icon: String,
    label: String,
    subtitle: String? = null,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (enabled)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 16.sp,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageExpansion(
    selectedImageUri: Uri?,
    imageDescription: String,
    onImageDescriptionChange: (String) -> Unit,
    onSelectImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("📷", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${RTL}تصویر",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onSelectImage,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (selectedImageUri == null) "${RTL}انتخاب تصویر" else "${RTL}تغییر تصویر",
                    fontSize = 13.sp
                )
            }

            if (selectedImageUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected image preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        shape = CircleShape,
                        color = Color.Red.copy(alpha = 0.8f)
                    ) {
                        IconButton(onClick = onRemoveImage) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "${RTL}حذف تصویر",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Text(
                    "${RTL}توضیح تصویر (اختیاری)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = imageDescription,
                    onValueChange = onImageDescriptionChange,
                    placeholder = { Text("${RTL}توضیحاتی درباره تصویر", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() })
                )
            }

            FooterButtons(
                onDismiss = onDismiss,
                onSubmit = onSubmit,
                enabled = selectedImageUri != null
            )
        }
    }
}

private fun String.isValidDuration(): Boolean {
    if (this.isBlank()) return true
    val value = this.toIntOrNull()
    return value != null && value > 0
}