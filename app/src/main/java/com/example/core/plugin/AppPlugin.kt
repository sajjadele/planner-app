package com.example.core.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Interface representing a modular plugin in the Vision Planner ecosystem.
 * Any new module (e.g., Fitness Tracker, Water tracker) just needs to implement this interface.
 */
interface AppPlugin {
    val id: String
    val name: String
    val description: String
    val icon: ImageVector

    @Composable
    fun Content(
        modifier: Modifier,
        onNavigateToSettings: () -> Unit,
        onBack: () -> Unit = {}
    )
}
