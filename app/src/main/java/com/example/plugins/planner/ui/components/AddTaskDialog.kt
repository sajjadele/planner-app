package com.example.plugins.planner.ui.components

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.platform.LocalContext
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
        deadlineEpochMs: Long?
    ) -> Unit,
    initialGoalId: Int? = null
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
        // Task-level deadline — fully independent of the Goal picker / Goal deadline. This is a
        // local optional due date for THIS task only; it never touches goalId or GoalEntity.
        var selectedDeadline by remember { mutableStateOf<Long?>(null) }
        var showDeadlineCalendar by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val context = LocalContext.current

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
                    selectedDeadline
                )
                onDismiss()
            }
        }

        // Open a standard Android TimePickerDialog
        val showTimePicker = {
            val calendar = Calendar.getInstance()
            val currentHour = selectedHour ?: calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = selectedMinute ?: calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                },
                currentHour,
                currentMinute,
                true // 24-hour format
            ).show()
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
                // REMINDER PILL — shown outside context for quick access
                // ============================================
                if (selectedHour != null && selectedMinute != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reminder pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "یادآوری: ${String.format("%02d:%02d", selectedHour, selectedMinute).isolated()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        selectedHour = null
                                        selectedMinute = null
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف یادآوری",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
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

                // Expandable context section
                AnimatedVisibility(
                    visible = showContext,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(
                            durationMillis = 250,
                            easing = FastOutSlowInEasing
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        // Reminder Time — Alarm/Clock button
                        // ============================================
                        Column {
                            Text(
                                text = "زمان یادآوری",
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
                                            if (selectedHour != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { showTimePicker() }
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = if (selectedHour != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedHour != null && selectedMinute != null)
                                            "امروز، ساعت ${String.format("%02d:%02d", selectedHour, selectedMinute).isolated()}"
                                        else
                                            "تنظیم زمان یادآوری",
                                        fontSize = 13.sp,
                                        color = if (selectedHour != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (selectedHour != null) {
                                    IconButton(
                                        onClick = {
                                            selectedHour = null
                                            selectedMinute = null
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ============================================
                        // Task Deadline (optional) — INDEPENDENT of the Goal deadline above.
                        // Purely a due date for this task; does not read/write goalId or Goal state.
                        // ============================================
                        Column {
                            Text(
                                text = "مهلت این کار",
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
                                            if (selectedDeadline != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { showDeadlineCalendar = true }
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (selectedDeadline != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedDeadline != null) {
                                            val j = JalaliDate.fromEpochMs(selectedDeadline!!)
                                            "مهلت: ${j.day.toEnglishDigits()} ${JalaliDate.MONTH_NAMES[j.month - 1]}"
                                        } else
                                            "تنظیم مهلت",
                                        fontSize = 13.sp,
                                        color = if (selectedDeadline != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (selectedDeadline != null) {
                                    IconButton(
                                        onClick = { selectedDeadline = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "حذف مهلت",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

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

        // Persian Calendar Dialog for the task deadline (independent of Goal deadline).
        if (showDeadlineCalendar) {
            PersianCalendarDialog(
                selectedDateEpochMs = selectedDeadline ?: System.currentTimeMillis(),
                onDateSelected = { epochMs -> selectedDeadline = epochMs },
                onDismiss = { showDeadlineCalendar = false },
                confirmButtonText = "انتخاب مهلت",
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
