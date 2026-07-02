package com.example.plugins.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.plugin.AppPlugin

class NotesPlugin : AppPlugin {
    override val id: String = "notes"
    override val name: String = "یادداشت سریع"
    override val description: String = "ثبت افکار، ایده‌ها و نوشته‌های کوتاه روزانه به صورت کاملاً آفلاین."
    override val icon = Icons.Default.EditNote

    @Composable
    override fun Content(
        modifier: Modifier,
        onNavigateToSettings: () -> Unit
    ) {
        NotesScreen(modifier = modifier)
    }
}
