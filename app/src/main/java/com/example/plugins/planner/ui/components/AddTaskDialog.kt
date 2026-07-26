package com.example.plugins.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import com.example.core.data.HolidayRepository
import com.example.core.domain.DayContext
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentFire
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.calendar.PersianCalendarDialog
import com.example.core.constants.LifeAreas
import com.example.core.goal.GoalEntity
import com.example.core.util.JalaliDate
import com.example.core.util.isolated
import com.example.core.util.toEnglishDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

private val LifeAreaSelectedBg = Color(0xFF6750A4)
private val LifeAreaSelectedText = Color.White
private val LifeAreaUnselectedBg = Color(0xFFF3EDF7)
private val LifeAreaUnselectedText = Color(0xFF49454F)

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    activeGoals: StateFlow<List<GoalEntity>>,
    onAddTask: (
        title: String,
        priority: String?,
        hour: Int?,
        minute: Int?,
        goalId: Int?,
        valueTag: String?,
        lifeAreaId: Int?,
        dateEpochMs: Long?
    ) -> Unit,
    initialGoalId: Int? = null,
    daysWithTasks: Set<Long> = emptySet(),
    showDateField: Boolean = false,
    initialDateEpochMs: Long? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        var title by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf<String?>(null) }
        var selectedHour by remember { mutableStateOf<Int?>(null) }
        var selectedMinute by remember { mutableStateOf<Int?>(null) }
        var showContext by remember { mutableStateOf(false) }
        var selectedGoalId by remember { mutableStateOf<Int?>(null) }
        var selectedGoalTitle by remember { mutableStateOf<String?>(null) }
        var showGoalDropdown by remember { mutableStateOf(false) }
        var selectedLifeAreaId by remember { mutableStateOf<Int?>(null) }
        // Task scheduled date. When showDateField is false (Planner tab) the dialog does not
        // expose a date UI; the Planner ViewModel supplies the day via its own selected day.
        // When true (GoalDetail), the user picks a day here and it is passed straight through
        // as the task's dateEpochMs.
        var selectedTaskDate by remember { mutableStateOf(initialDateEpochMs) }
        var showDateCalendar by remember { mutableStateOf(false) }
        val todayMidnight = remember {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        val focusRequester = remember { FocusRequester() }

        // When opened from a goal (e.g. Goal Detail empty state), preselect that goal so the
        // task is linked on creation. Reuses the existing goal picker — no new creation flow.
        val goals by activeGoals.collectAsState()
        LaunchedEffect(initialGoalId) {
            if (initialGoalId != null && selectedGoalId == null) {
                val goal = goals.firstOrNull { it.id == initialGoalId }
                if (goal != null) {
                    selectedGoalId = goal.id
                    selectedGoalTitle = goal.title
                }
            }
        }

        // Collect the StateFlow to get live goal updates
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }

        // Helper: emit the task with current goal state
        val emitTask = {
            if (title.isNotBlank()) {
                onAddTask(
                    title, priority, selectedHour, selectedMinute,
                    selectedGoalId,
                    null,
                    selectedLifeAreaId,
                    if (showDateField) selectedTaskDate else null
                )
                onDismiss()
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // ============================================
                // LAYER 1 — Thought Capture (always visible)
                // ============================================

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("چیزی بنویسید...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = false,
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { emitTask() })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ============================================
                // Task scheduled date — always visible when opened from GoalDetail
                // (showDateField = true). Planner tab supplies the day itself, so it stays hidden.
                // ============================================
                if (showDateField) {
                    Column {
                        Text(
                            text = "تاریخ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { showDateCalendar = true }
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedTaskDate != null && selectedTaskDate == todayMidnight) {
                                    "امروز"
                                } else if (selectedTaskDate != null) {
                                    val j = JalaliDate.fromEpochMs(selectedTaskDate!!)
                                    "${j.day.toEnglishDigits()} ${JalaliDate.MONTH_NAMES[j.month - 1]}"
                                } else {
                                    "امروز"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ============================================
                // LAYER 2 — Context (expandable)
                // ============================================

                // Expand/collapse toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showContext = !showContext }
                        .background(
                            if (showContext) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (showContext) Icons.Default.ExpandLess else Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showContext) "بستن زمینه" else "افزودن زمینه",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Expandable context section — fade-only reveal. We deliberately avoid
                // `expandVertically`/`shrinkVertically` here: on mid-range devices the height
                // animation re-measures the whole block (Goal DropdownMenu + Reminder row +
                // Deadline row + 2×3 LifeArea grid) every frame, producing dropped frames / jank.
                // A pure alpha fade is cheap (no per-frame layout) and reads as a calm reveal.
                AnimatedVisibility(
                    visible = showContext,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ============================================
                        // Goal Picker (DropdownMenu)
                        // ============================================
                        Column {
                            Text(
                                text = "هدف این کار چیست؟",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box {
                                // Selector card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .clickable { showGoalDropdown = true }
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedGoalTitle ?: "بدون هدف",
                                        fontSize = 13.sp,
                                        color = if (selectedGoalTitle != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Dropdown menu
                                DropdownMenu(
                                    expanded = showGoalDropdown,
                                    onDismissRequest = { showGoalDropdown = false },
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    // "No Goal" option
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "بدون هدف",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            selectedGoalId = null
                                            selectedGoalTitle = null
                                            showGoalDropdown = false
                                        }
                                    )

                                    // Active goals
                                    goals.forEach { goal ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = goal.title,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                selectedGoalId = goal.id
                                                selectedGoalTitle = goal.title
                                                showGoalDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ============================================
                        // PRIORITY ROW — moved into context to keep the
                        // main area a single-tap capture surface.
                        // ============================================
                        Column {
                            Text(
                                text = ":اولویت",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PriorityOptionChip(
                                    label = "بالا",
                                    color = AccentRed,
                                    isSelected = priority == "HIGH",
                                    onClick = { priority = if (priority == "HIGH") null else "HIGH" },
                                    modifier = Modifier.weight(1f)
                                )
                                PriorityOptionChip(
                                    label = "متوسط",
                                    color = AccentFire,
                                    isSelected = priority == "MEDIUM",
                                    onClick = { priority = if (priority == "MEDIUM") null else "MEDIUM" },
                                    modifier = Modifier.weight(1f)
                                )
                                PriorityOptionChip(
                                    label = "پایین",
                                    color = AccentGreen,
                                    isSelected = priority == "LOW",
                                    onClick = { priority = if (priority == "LOW") null else "LOW" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // ============================================
                        // Reminder
                        // ============================================
                        ReminderSection(
                            reminderHour = selectedHour,
                            reminderMinute = selectedMinute,
                            onSetReminder = { h, m ->
                                selectedHour = h
                                selectedMinute = m
                            },
                            onClearReminder = {
                                selectedHour = null
                                selectedMinute = null
                            }
                        )

                        // ============================================
                        // Life Area — 2×3 grid for clean layout
                        // ============================================
                        Column {
                            Text(
                                text = "حوزه زندگی",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 1: 3 items
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LifeAreas.all.take(3).forEach { area ->
                                    LifeAreaChip(
                                        area = area,
                                        isSelected = selectedLifeAreaId == area.id,
                                        onClick = { selectedLifeAreaId = if (selectedLifeAreaId == area.id) null else area.id },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 2: 3 items
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LifeAreas.all.drop(3).forEach { area ->
                                    LifeAreaChip(
                                        area = area,
                                        isSelected = selectedLifeAreaId == area.id,
                                        onClick = { selectedLifeAreaId = if (selectedLifeAreaId == area.id) null else area.id },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        }

                    }

                Spacer(modifier = Modifier.height(16.dp))

                // ============================================
                // Action Buttons
                // ============================================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("انصراف", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { emitTask() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = title.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text("ذخیره", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Persian Calendar Dialog for the task scheduled date (independent of Goal deadline).
        if (showDateCalendar) {
            val ctx = LocalContext.current
            val holidayRepo = remember(ctx) { HolidayRepository(ctx) }
            PersianCalendarDialog(
                selectedDateEpochMs = selectedTaskDate ?: System.currentTimeMillis(),
                onDateSelected = { epochMs -> selectedTaskDate = epochMs },
                onDismiss = { showDateCalendar = false },
                holidayRepository = holidayRepo,
                daysWithIndicators = daysWithTasks,
                indicatorColor = AccentGreen,
                showIndicator = { it in daysWithTasks },
                confirmButtonText = "انتخاب تاریخ",
                showConfirmButton = true
            )
        }
    }
}

@Composable
private fun LifeAreaChip(
    area: LifeAreas.LifeArea,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) LifeAreaSelectedBg else LifeAreaUnselectedBg,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFD1D5DB)),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = area.icon,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = area.name,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) LifeAreaSelectedText else LifeAreaUnselectedText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PriorityOptionChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) color else MaterialTheme.colorScheme.outline),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
