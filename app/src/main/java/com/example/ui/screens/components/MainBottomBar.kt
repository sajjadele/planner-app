package com.example.ui.screens.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.plugin.AppPlugin

@Composable
fun MainBottomBar(
    activePlugins: List<AppPlugin>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFF3EDF7),
        tonalElevation = 0.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .height(80.dp)
            .testTag("bottom_nav_bar")
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
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = navigationBarColors(),
                modifier = Modifier.testTag("nav_tab_${plugin.id}")
            )
        }

        // Module management intentionally hidden from primary MVP navigation.
        // Plugin infrastructure still exists but should not dominate the UX.
    }
}

@Composable
private fun navigationBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color(0xFF1D192B),
    unselectedIconColor = Color(0xFF49454F),
    selectedTextColor = Color(0xFF1D192B),
    unselectedTextColor = Color(0xFF49454F),
    indicatorColor = Color(0xFFE8DEF8)
)
