package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.plugin.AppPlugin
import com.example.core.plugin.ModuleSettingsViewModel
import com.example.core.plugin.PluginRegistry
import com.example.core.search.SearchDialog
import com.example.plugins.planner.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    moduleViewModel: ModuleSettingsViewModel = viewModel()
) {
    val enabledModulesMap by moduleViewModel.enabledModulesState.collectAsState()
    
    // Determine which registered plugins are currently enabled
    val activePlugins = remember(enabledModulesMap) {
        PluginRegistry.allPlugins.filter { plugin ->
            enabledModulesMap[plugin.id] != false
        }
    }

    // Keep track of current selected tab.
    // 0 -> First active plugin (if any)
    // 1 -> Second active plugin (if any)
    // ...
    // Last -> "Modules" Manager Tab (Always present)
    var selectedTabId by remember { mutableStateOf("modules") }

    // Fallback logic: if selected tab is disabled, switch to the modules tab
    LaunchedEffect(activePlugins) {
        if (selectedTabId != "modules" && activePlugins.none { it.id == selectedTabId }) {
            selectedTabId = "modules"
        } else if (selectedTabId == "modules" && activePlugins.isNotEmpty() && selectedTabId != "modules") {
            selectedTabId = activePlugins.first().id
        }
    }

    var showSettingsInfo by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    val plannerViewModel: PlannerViewModel = viewModel()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF)),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Vision Planner",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = getPersianTodayDate(),
                            fontSize = 12.sp,
                            color = Color(0xFF49454F),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Advanced Search Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEADDFF).copy(alpha = 0.5f), CircleShape)
                                .clip(CircleShape)
                                .clickable { showSearchDialog = true }
                                .testTag("search_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "جستجو",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Settings/About Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEADDFF), CircleShape)
                                .clip(CircleShape)
                                .clickable { showSettingsInfo = true }
                                .testTag("settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "About",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Elegant navigation bar conforming to Material 3 standard in the mockup
            NavigationBar(
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(80.dp)
                    .testTag("bottom_nav_bar")
            ) {
                // Render tabs for all active plugins
                activePlugins.forEach { plugin ->
                    val isSelected = selectedTabId == plugin.id
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabId = plugin.id },
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D192B),
                            unselectedIconColor = Color(0xFF49454F),
                            selectedTextColor = Color(0xFF1D192B),
                            unselectedTextColor = Color(0xFF49454F),
                            indicatorColor = Color(0xFFE8DEF8)
                        ),
                        modifier = Modifier.testTag("nav_tab_${plugin.id}")
                    )
                }

                // Standard Modules Management Tab
                val isModulesSelected = selectedTabId == "modules"
                NavigationBarItem(
                    selected = isModulesSelected,
                    onClick = { selectedTabId = "modules" },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Modules"
                        )
                    },
                    label = {
                        Text(
                            text = "ماژول‌ها",
                            fontSize = 11.sp,
                            fontWeight = if (isModulesSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D192B),
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = Color(0xFF1D192B),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    ),
                    modifier = Modifier.testTag("nav_tab_modules")
                )
            }
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
            if (tabId == "modules") {
                ModulesManagerScreen(
                    viewModel = moduleViewModel,
                    enabledMap = enabledModulesMap
                )
            } else {
                val currentPlugin = activePlugins.find { it.id == tabId }
                if (currentPlugin != null) {
                    currentPlugin.Content(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToSettings = { selectedTabId = "modules" }
                    )
                } else {
                    // Fallback empty view
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ماژول غیرفعال است")
                    }
                }
            }
        }
    }

    // Developer credits / architecture summary dialog
    if (showSettingsInfo) {
        Dialog(onDismissRequest = { showSettingsInfo = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Architecture",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "درباره Vision Planner",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "نسخه ۱.۰ (معماری ماژولار پویا)",
                        fontSize = 12.sp,
                        color = Color(0xFF6750A4),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "این برنامه بر اساس معماری توسعه‌پذیر Plugin-Based طراحی شده است. افزودن هر ماژول جدید بدون تغییر در هسته اصلی صورت می‌پذیرد. تمامی داده‌ها ۱۰۰٪ محلی و به صورت آفلاین ذخیره شده و هیچ دسترسی اینترنتی استفاده نمی‌شود.",
                        fontSize = 13.sp,
                        color = Color(0xFF49454F),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showSettingsInfo = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("متوجه شدم", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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

@Composable
fun ModulesManagerScreen(
    viewModel: ModuleSettingsViewModel,
    enabledMap: Map<String, Boolean>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "مدیریت ماژول‌ها و ابزارها",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1B1F)
        )
        Text(
            text = "فعال یا غیرفعال کردن ویژگی‌ها بر اساس نیاز شما",
            fontSize = 12.sp,
            color = Color(0xFF938F99),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Render switches for all available modules
        PluginRegistry.allPlugins.forEach { plugin ->
            val isEnabled = enabledMap[plugin.id] != false
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFEADDFF).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = plugin.icon,
                            contentDescription = plugin.name,
                            tint = Color(0xFF21005D)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = plugin.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = plugin.description,
                            fontSize = 11.sp,
                            color = Color(0xFF49454F),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.toggleModule(plugin.id, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6750A4),
                        uncheckedThumbColor = Color(0xFF49454F),
                        uncheckedTrackColor = Color(0xFFCAC4D0).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("switch_module_${plugin.id}")
                )
            }
        }
    }
}

private fun getPersianTodayDate(): String {
    // Elegant standard fallback
    val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("fa", "IR"))
    return sdf.format(Date())
}
