package com.example.plugins.planner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeumorphicBackground

/**
 * Neumorphic surface with soft shadow for card-like elements.
 */
@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = NeumorphicBackground,
    elevation: Int = 8,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation.dp, shape, ambientColor = Color(0xFFD1D5DB), spotColor = Color(0xFFB0B8C4))
            .background(backgroundColor, shape)
            .clip(shape)
    ) {
        content()
    }
}

/**
 * Neumorphic circle for day selector and interactive elements.
 */
@Composable
fun NeumorphicCircle(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: Dp = 44.dp,
    selectedColor: Color = Color(0xFF6750A4),
    unselectedBackground: Color = NeumorphicBackground,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isSelected) 6.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isSelected) selectedColor.copy(alpha = 0.3f) else Color(0xFFD1D5DB),
                spotColor = if (isSelected) selectedColor.copy(alpha = 0.4f) else Color(0xFFB0B8C4)
            )
            .background(
                color = if (isSelected) selectedColor else unselectedBackground,
                shape = CircleShape
            )
            .then(
                if (!isSelected) {
                    Modifier.border(1.dp, Color(0xFFD1D5DB).copy(alpha = 0.5f), CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}