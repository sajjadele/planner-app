package com.example.ui.screens.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Vision Planner unified popup menu. Wraps Material3 [DropdownMenu] with the app's dark/elevated
 * surface, rounded corners, generous padding and RTL support so every overflow menu looks
 * consistent. Presentation only — behavior is driven by callers.
 */
@Composable
fun VisionPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(12.dp),
            content = {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    content()
                }
            }
        )
    }
}

/**
 * A single Vision popup menu item with optional leading icon.
 */
@Composable
fun ColumnScope.VisionMenuItem(
    text: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontSize = 13.sp,
                color = if (enabled) textColor else textColor.copy(alpha = 0.4f),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f),
                    modifier = androidx.compose.ui.Modifier
                        .padding(end = 4.dp)
                )
            }
        } else null,
        onClick = onClick,
        enabled = enabled
    )
}

/** Convenience divider matching the menu's vertical rhythm. */
@Composable
fun ColumnScope.VisionMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}
