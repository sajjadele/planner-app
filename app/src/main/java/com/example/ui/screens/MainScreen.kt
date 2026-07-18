package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.onboarding.OnboardingDeepLink
import com.example.core.onboarding.OnboardingRepository
import com.example.core.plugin.ModuleSettingsViewModel
import com.example.core.plugin.PluginRegistry
import com.example.core.preferences.ThemeMode
import com.example.core.search.SearchDialog
import com.example.plugins.goals.ui.AddGoalDialog
import com.example.plugins.goals.ui.GoalViewModel
import com.example.plugins.planner.ui.PlannerViewModel
import com.example.plugins.planner.ui.components.AddTaskDialog
import com.example.ui.onboarding.OnboardingHost
import com.example.ui.onboarding.BrandedLoadingScreen
import com.example.ui.screens.components.VisionBottomBar
import com.example.ui.screens.components.MainTopBar
import com.example.ui.screens.components.ThemeSettingsDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeChanged: (ThemeMode) -> Unit = {},
    moduleViewModel: ModuleSettingsViewModel = viewModel()
) {
    val enabledModulesMap by moduleViewModel.enabledModulesState.collectAsState()

    // Onboarding: show the Value Discovery Journey once, before the main app.
    // Three-state gate: null = still reading DataStore (neutral), false = onboarding,
    // true = completed. Never render onboarding for a returning (completed) user.
    val context = LocalContext.current
    val onboardingRepo = remember { OnboardingRepository(context) }
    val onboardingDone by onboardingRepo.isCompleted.collectAsState(initial = null)
    val deepLinkGoalId = remember { mutableStateOf<Int?>(null) }

    when (onboardingDone) {
        null -> {
            BrandedLoadingScreen(modifier = Modifier.fillMaxSize())
            return
        }
        false -> {
            OnboardingHost(
                modifier = Modifier.fillMaxSize(),
                onFinish = { goalId ->
                    OnboardingDeepLink.set(goalId, showHomeHint = true)
                    deepLinkGoalId.value = goalId
                }
            )
            return
        }
        true -> { /* fall through to the main Planner dashboard below */ }
    }

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

    // Guards rapid swipe/click double-switches during the AnimatedContent crossfade (Phase 5.5).
    var isTabTransitioning by remember { mutableStateOf(false) }

    /**
     * Phase 5.5 gesture navigation: move to the adjacent primary bottom tab.
     * `direction` is the *physical* drag sign (+ = leftward, - = rightward in LTR). In RTL the
     * visual ordering is reversed, so the step is flipped by [LayoutDirection]. The bottom bar
     * remains the single source of truth — this only writes [selectedTabId].
     */
    fun swipeToAdjacentTab(direction: Float, layoutDirection: LayoutDirection) {
        if (isTabTransitioning) return
        val currentIndex = primaryTabIds.indexOf(selectedTabId).coerceAtLeast(0)
        val rawStep = when {
            direction > 0f -> 1
            direction < 0f -> -1
            else -> 0
        }
        val step = if (layoutDirection == LayoutDirection.Rtl) -rawStep else rawStep
        val nextIndex = (currentIndex + step).coerceIn(0, primaryTabIds.lastIndex)
        if (nextIndex != currentIndex) selectedTabId = primaryTabIds[nextIndex]
    }

    LaunchedEffect(deepLinkGoalId.value) {
        if (deepLinkGoalId.value != null) selectedTabId = "goals"
    }

    // Fallback: if selected tab is disabled, switch to planner
    LaunchedEffect(activePlugins) {
        if (activePlugins.none { it.id == selectedTabId }) {
            selectedTabId = "planner"
        }
    }

    // Clear the swipe/click guard after the AnimatedContent crossfade finishes (Phase 5.5).
    LaunchedEffect(selectedTabId) {
        if (isTabTransitioning) {
            delay(350)
            isTabTransitioning = false
        }
    }

    var showThemeSettings by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    val plannerViewModel: PlannerViewModel = viewModel()
    // Phase 6.5.7 (launch-jank): GoalViewModel is only needed by the Add-Goal dialog, so create it
    // lazily on first demand rather than eagerly at launch (when the Planner tab is the default).
    var goalViewModel: GoalViewModel? = null

    // FAB dialog states
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            MainTopBar(
                onSearchClick = { showSearchDialog = true },
                onNotesClick = { selectedTabId = "notes" },
                onSettingsClick = { showThemeSettings = true }
            )
        },
        bottomBar = {
            VisionBottomBar(
                activePlugins = bottomBarPlugins,
                selectedTabId = selectedTabId,
                onTabSelected = {
                    isTabTransitioning = true
                    selectedTabId = it
                },
                onActionClick = {
                    when (selectedTabId) {
                        "planner" -> showAddTaskDialog = true
                        "goals" -> showAddGoalDialog = true
                    }
                }
            )
        }
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        AnimatedContent(
            targetState = selectedTabId,
            transitionSpec = {
                fadeIn().togetherWith(fadeOut())
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Phase 5.5: horizontal swipe between the two bottom tabs (planner <-> goals).
                // Writes selectedTabId only; bottom-nav remains the source of truth.
                .pointerInput(primaryTabIds, layoutDirection) {
                    var accumulated = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulated = 0f },
                        onHorizontalDrag = { _, dragAmount -> accumulated += dragAmount },
                        onDragEnd = {
                            // Threshold so a tiny scroll doesn't switch tabs.
                            if (accumulated > 60f || accumulated < -60f) {
                                swipeToAdjacentTab(accumulated, layoutDirection)
                            }
                        },
                        onDragCancel = { accumulated = 0f }
                    )
                },
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

    // ── Dialogs ──

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            activeGoals = plannerViewModel.activeGoals,
            onAddTask = { title, priority, hour, minute, goalId, valueTag, lifeAreaId, deadlineEpochMs ->
                plannerViewModel.addTask(
                    title = title,
                    priority = priority,
                    hour = hour,
                    minute = minute,
                    goalId = goalId,
                    valueTag = valueTag,
                    lifeAreaId = lifeAreaId,
                    deadlineEpochMs = deadlineEpochMs
                )
                showAddTaskDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        val goalVm = goalViewModel ?: viewModel<GoalViewModel>().also { goalViewModel = it }
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onAddGoal = { title, description, why, deadlineEpochMs ->
                goalVm.addGoal(title, description, why, deadlineEpochMs)
                showAddGoalDialog = false
            }
        )
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
            onNavigateToTask = { dateEpochMs ->
                plannerViewModel.selectDate(dateEpochMs)
                selectedTabId = "planner"
            },
            onNavigateToNotes = {
                selectedTabId = "notes"
            }
        )
    }
}
