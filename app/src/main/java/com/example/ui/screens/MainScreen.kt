package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.plugin.ModuleSettingsViewModel
import com.example.core.plugin.PluginRegistry
import com.example.core.preferences.ThemeMode
import com.example.core.search.SearchDialog
import com.example.plugins.planner.ui.PlannerViewModel
import com.example.ui.screens.components.MainBottomBar
import com.example.ui.screens.components.MainTopBar
import com.example.ui.screens.components.ThemeSettingsDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeChanged: (ThemeMode) -> Unit = {},
    moduleViewModel: ModuleSettingsViewModel = viewModel()
) {
    val enabledModulesMap by moduleViewModel.enabledModulesState.collectAsState()

    // Primary navigation tabs: Planner + Goals (Notes stays in top bar)
    val primaryTabIds = listOf("planner", "goals")

    // All enabled plugins (for content rendering)
    val activePlugins = remember(enabledModulesMap) {
        PluginRegistry.allPlugins.filter { plugin ->
            enabledModulesMap[plugin.id] != false
        }
    }

    // Plugins shown in bottom bar (only primary tabs that are enabled)
    val bottomBarPlugins = remember(activePlugins) {
        activePlugins.filter { it.id in primaryTabIds }
    }

    var selectedTabId by remember { mutableStateOf("planner") }

    // Fallback: if selected tab is disabled, switch to planner
    LaunchedEffect(activePlugins) {
        if (activePlugins.none { it.id == selectedTabId }) {
            selectedTabId = "planner"
        }
    }

    var showThemeSettings by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    val plannerViewModel: PlannerViewModel = viewModel()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            MainTopBar(
                currentDate = getPersianTodayDate(),
                onSearchClick = { showSearchDialog = true },
                onNotesClick = { selectedTabId = "notes" },
                onSettingsClick = { showThemeSettings = true }
            )
        },
        bottomBar = {
            MainBottomBar(
                activePlugins = bottomBarPlugins,
                selectedTabId = selectedTabId,
                onTabSelected = { selectedTabId = it }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTabId,
            transitionSpec = {
                fadeIn().togetherWith(fadeOut())
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "ScreenTransitions"
        ) { tabId ->
            val currentPlugin = PluginRegistry.allPlugins.find { it.id == tabId }
            if (currentPlugin != null) {
                currentPlugin.Content(
                    modifier = Modifier.fillMaxSize(),
                    onNavigateToSettings = { showThemeSettings = true },
                    onBack = { selectedTabId = "planner" }
                )
            } else {
                // Fallback empty view
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "ماژول غیرفعال است",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Theme Settings Dialog
    if (showThemeSettings) {
        ThemeSettingsDialog(
            currentTheme = themeMode,
            onSelectTheme = { mode ->
                onThemeChanged(mode)
            },
            onDismiss = { showThemeSettings = false }
        )
    }

    if (showSearchDialog) {
        SearchDialog(
            onDismissRequest = { showSearchDialog = false },
            onNavigateToTask = { dayIndex ->
                plannerViewModel.selectDay(dayIndex)
                selectedTabId = "planner"
            },
            onNavigateToNotes = {
                selectedTabId = "notes"
            }
        )
    }
}

private fun getPersianTodayDate(): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("fa", "IR"))
    return sdf.format(Date())
}
