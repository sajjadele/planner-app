package com.example.plugins.planner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.plugin.AppPlugin

class PlannerPlugin : AppPlugin {
    override val id: String = "planner"
    override val name: String = "برنامه‌ریزی روزانه"
    override val description: String = "مدیریت زمان، اولویت‌بندی کارها و تنظیم یادآور محلی آفلاین."
    override val icon = Icons.Default.EventNote

    @Composable
    override fun Content(
        modifier: Modifier,
        onNavigateToSettings: () -> Unit
    ) {
        PlannerScreen(modifier = modifier)
    }
}
