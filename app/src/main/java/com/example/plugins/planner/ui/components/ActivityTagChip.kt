package com.example.plugins.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ActivityTagChip — Non-intrusive step tag badge for message cards.
 *
 * Phase 5.5a: Step as Tag, Not Container
 *
 * Shows step name as a small rounded badge inline on ActivityMessageCard.
 * Pure metadata display — NOT clickable, NOT expandable, NOT a container.
 *
 * Example:
 * ```
 * ┌─────────────────────┐
 * │ 📝 Research notes    │
 * │                      │
 * │ [ 🟣 UI Design ]     │  ← tag chip
 * │ 10:30               │
 * └─────────────────────┘
 * ```
 */
@Composable
fun ActivityTagChip(
    name: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1
        )
    }
}
