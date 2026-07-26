package com.example.plugins.planner.ui.composer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun DurationPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var durationText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${RTL}مدت زمان")
        },
        text = {
            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it },
                label = { Text("${RTL}دقیقه") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(onDone = {
                    durationText.toIntOrNull()?.let { onConfirm(it) }
                }),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    durationText.toIntOrNull()?.let { onConfirm(it) }
                }
            ) {
                Text("${RTL}تأیید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("${RTL}لغو")
            }
        }
    )
}
