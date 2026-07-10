package com.example.plugins.goals

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiFlags
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.plugin.AppPlugin
import com.example.plugins.goals.ui.GoalDashboardScreen

class GoalsPlugin : AppPlugin {
    override val id: String = "goals"
    override val name: String = "اهداف"
    override val description: String = "مدیریت اهداف بلندمدت و پیشرفت به سوی آن‌ها."
    override val icon = Icons.Default.EmojiFlags

    @Composable
    override fun Content(
        modifier: Modifier,
        onNavigateToSettings: () -> Unit,
        onBack: () -> Unit
    ) {
        GoalDashboardScreen(modifier = modifier)
    }
}
