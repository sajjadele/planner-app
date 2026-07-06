package com.example.plugins.planner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.plugin.AppPlugin

class PlannerPlugin : AppPlugin {
    override val id: String = "planner"
    override val name: String = "وظایف"
    override val description: String = "ثبت سریع و مدیریت وظایف به صورت کاملاً آفلاین با سازمان‌دهی اختیاری."
    override val icon = Icons.Default.EventNote

    @Composable
    override fun Content(
        modifier: Modifier,
        onNavigateToSettings: () -> Unit
    ) {
        PlannerScreen(modifier = modifier)
    }
}
