package com.example.plugins.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Neumorphic surface with soft, clean dual-shadow effect.
 * Reads [LocalIsDarkTheme] so shadows react to the user's theme preference.
 */
@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Int = 8,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    val ambientColor = if (isDark) DarkShadowAmbient else LightShadowAmbient
    val spotColor = if (isDark) DarkShadowSpot else LightShadowSpot
    val blurRadiusDp = (elevation * 1.5f).dp

    Box(
        modifier = modifier
            .shadow(
                elevation = blurRadiusDp,
                shape = shape,
                ambientColor = ambientColor,
                spotColor = spotColor
            )
            .background(backgroundColor, shape)
            .clip(shape)
    ) {
        content()
    }
}

/**
 * Neumorphic circle — defaults react to dark theme.
 */
@Composable
fun NeumorphicCircle(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: Dp = 44.dp,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    val ambient = if (isSelected)
        selectedColor.copy(alpha = 0.30f)
    else if (isDark)
        DarkShadowAmbient
    else
        LightShadowAmbient

    val spot = if (isSelected)
        selectedColor.copy(alpha = 0.35f)
    else if (isDark)
        DarkShadowSpot
    else
        LightShadowSpot

    val blur = if (isSelected) 6.dp else 4.dp

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = blur,
                shape = CircleShape,
                ambientColor = ambient,
                spotColor = spot
            )
            .background(
                color = if (isSelected) selectedColor else unselectedBackground,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
