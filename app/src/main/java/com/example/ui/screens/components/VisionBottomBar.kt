package com.example.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.plugin.AppPlugin
import com.example.ui.theme.*

private val FAB_SIZE = 56.dp
private val FAB_RADIUS = 28.dp
private val BAR_FLAT_HEIGHT = 80.dp
private val BAR_TOTAL_HEIGHT = BAR_FLAT_HEIGHT + FAB_RADIUS   // 108.dp
private val TRANSITION_HALF_WIDTH = 52.dp

@Composable
fun VisionBottomBar(
    activePlugins: List<AppPlugin>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    onActionClick: () -> Unit
) {
    val density = LocalDensity.current
    val domeRadiusPx = with(density) { FAB_RADIUS.toPx() }
    val barHeightPx = with(density) { BAR_FLAT_HEIGHT.toPx() }
    val transitionHalfWidthPx = with(density) { TRANSITION_HALF_WIDTH.toPx() }

    val convexShape: Shape = remember(domeRadiusPx, barHeightPx, transitionHalfWidthPx) {
        ConvexBottomBarShape(domeRadiusPx, barHeightPx, transitionHalfWidthPx)
    }

    var boxWidth by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(BAR_TOTAL_HEIGHT)
            .testTag("bottom_nav_bar")
            .onGloballyPositioned { boxWidth = it.size.width }
    ) {
        // ── Layer 1: The continuous surface ──
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(convexShape)
                .background(MaterialTheme.colorScheme.surface)
        )

        // ── Layer 2: Left nav item ──
        activePlugins.getOrNull(0)?.let { plugin ->
            val offsetX = with(density) { (boxWidth / 4f - navItemWidth.toPx() / 2f).toDp() }
            val offsetY = with(density) {
                (domeRadiusPx + barHeightPx / 2f - navItemHeight.toPx() / 2f).toDp()
            }
            Box(
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            ) {
                NavItem(
                    plugin = plugin,
                    isSelected = selectedTabId == plugin.id,
                    onClick = { onTabSelected(plugin.id) }
                )
            }
        }

        // ── Layer 3: Right nav item ──
        activePlugins.getOrNull(1)?.let { plugin ->
            val offsetX = with(density) { (boxWidth * 3f / 4f - navItemWidth.toPx() / 2f).toDp() }
            val offsetY = with(density) {
                (domeRadiusPx + barHeightPx / 2f - navItemHeight.toPx() / 2f).toDp()
            }
            Box(
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            ) {
                NavItem(
                    plugin = plugin,
                    isSelected = selectedTabId == plugin.id,
                    onClick = { onTabSelected(plugin.id) }
                )
            }
        }

        // ── Layer 4: Center "+" button — embedded in the crest ──
        Box(
            modifier = Modifier
                .size(FAB_SIZE)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag("center_action_hub")
                .clickable(onClick = onActionClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "افزودن",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    plugin: AppPlugin,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(navItemHeight)
            .width(navItemWidth)
            .padding(bottom = 6.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .then(if (isSelected) Modifier else Modifier.alpha(0.6f))
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = plugin.icon,
            contentDescription = plugin.name,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = plugin.name.take(12),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}

private val navItemWidth = 72.dp
private val navItemHeight = 56.dp
