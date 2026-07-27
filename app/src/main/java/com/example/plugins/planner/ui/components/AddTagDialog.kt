package com.example.plugins.planner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.util.RTL

/**
 * AddTagDialog — Simple dialog for creating a new Tag/دسته.
 *
 * Phase 5.9.2: Dedicated Tag Creation UX
 *
 * Tag is metadata/filter only — no Activity creation, no container behavior.
 * Color selector placeholder for future enhancement (requires DB migration).
 *
 * Layout:
 * ┌──────────────────────┐
 * │  ایجاد دسته جدید      │
 * │                      │
 * │  نام:                │
 * │  [ Android          ]│
 * │                      │
 * │  [     انصراف    ثبت ]│
 * └──────────────────────┘
 */
@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onCreateTag: (name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tagName by remember { mutableStateOf("") }
    val isValid = tagName.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Title ──
                Text(
                    text = "${RTL}ایجاد دسته جدید",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // ── Name input ──
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("${RTL}نام") },
                    placeholder = { Text("${RTL}مثلاً: طراحی UI") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                // ── Actions ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "${RTL}انصراف",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isValid) {
                                onCreateTag(tagName.trim())
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${RTL}ثبت",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
