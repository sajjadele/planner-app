package com.example.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.plugin.AppPlugin
import com.example.ui.theme.*

@Composable
fun MainBottomBar(
    activePlugins: List<AppPlugin>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .height(80.dp)
            .testTag("bottom_nav_bar")
            .drawBehind {
                drawRect(
                    color = Color.Black.copy(alpha = if (isDark) 0.20f else 0.04f),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
                )
            }
    ) {
        activePlugins.forEach { plugin ->
            val isSelected = selectedTabId == plugin.id

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(plugin.id) },
                icon = {
                    Icon(
                        imageVector = plugin.icon,
                        contentDescription = plugin.name
                    )
                },
                label = {
                    Text(
                        text = plugin.name.take(12),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                modifier = if (isSelected) Modifier.testTag("nav_tab_${plugin.id}") else Modifier
                    .alpha(0.6f)
                    .testTag("nav_tab_${plugin.id}")
            )
        }
    }
}
