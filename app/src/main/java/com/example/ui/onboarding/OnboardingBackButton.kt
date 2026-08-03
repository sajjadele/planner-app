package com.example.ui.onboarding

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.RTL

/** Minimal back affordance for the mid-steps (Goal / Task / Future). */
@Composable
fun OnboardingBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            interactionSource = interactionSource,
            modifier = Modifier.pressScale(interactionSource)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "بازگشت",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${RTL}بازگشت",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
