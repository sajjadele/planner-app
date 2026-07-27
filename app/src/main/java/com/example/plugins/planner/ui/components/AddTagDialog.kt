package com.example.plugins.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.util.RTL

/**
 * Predefined tag colors (hex without #).
 * Default (null) uses the system primary color.
 */
val TAG_PALETTE = listOf(
    null,          // default
    "8B5CF6",      // violet
    "10B981",      // emerald
    "3B82F6",      // blue
    "F59E0B",      // amber
    "EF4444",      // red
    "FCD34D",      // yellow
)

/**
 * Parse hex string (# optional) to Compose Color, or return null.
 */
fun parseColorHex(hex: String?): Color? {
    if (hex == null) return null
    val sanitized = hex.trimStart('#')
    return try {
        Color(android.graphics.Color.parseColor("#$sanitized"))
    } catch (_: Exception) {
        null
    }
}

/**
 * defaultTagColor — UI default for tags without stored color.
 */
val defaultTagColor: Color = Color(0xFF6366F1) // indigo

/**
 * AddTagDialog — Dialog for creating a new Tag (دسته/برچسب) with optional color.
 *
 * Phase 5.9.2: Dedicated Tag Creation UX.
 * Phase 5.9.3: Color selection palette.
 *
 * Layout:
 * ┌──────────────────────────────┐
 * │  ایجاد دسته جدید             │
 * │                              │
 * │  نام:                        │
 * │  [ Android                  ]│
 * │                              │
 * │  ⬜ 🟣 🟢 🔵 🟠 🔴 🟡     │
 * │                              │
 * │        [انصراف]     [ثبت]    │
 * └──────────────────────────────┘
 */
@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onCreateTag: (name: String, colorHex: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<String?>(null) }
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

                // ── Color palette ──
                Text(
                    text = "${RTL}رنگ:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TAG_PALETTE.forEach { colorHex ->
                        TagColorSwatch(
                            colorHex = colorHex,
                            isSelected = selectedColor == colorHex,
                            onClick = { selectedColor = colorHex }
                        )
                    }
                }

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
                                onCreateTag(tagName.trim(), selectedColor)
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

@Composable
private fun TagColorSwatch(
    colorHex: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (colorHex != null) parseColorHex(colorHex) ?: defaultTagColor
        else defaultTagColor.copy(alpha = 0.2f)

    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(borderModifier)
            .background(bgColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (colorHex == null) {
            // Default swatch: a small circle outline indicating "no color"
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
            )
        } else if (isSelected) {
            Text(
                text = "✓",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = android.graphics.Color.parseColor("#$colorHex")
                    .let { Color(it) }
                    .let { c -> if (c.luminance() > 0.5f) Color.Black else Color.White }
            )
        }
    }
}
