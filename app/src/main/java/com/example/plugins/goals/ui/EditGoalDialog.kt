package com.example.plugins.goals.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.calendar.PersianCalendarDialog
import com.example.core.data.HolidayRepository
import com.example.core.domain.DayContext
import com.example.core.goal.GoalEntity
import com.example.core.util.JalaliDate
import com.example.core.util.toEnglishDigits
import com.example.plugins.planner.ui.components.DayContextDetails
import com.example.plugins.planner.ui.components.DayContextHeader
import com.example.ui.theme.AccentGreen
import kotlinx.coroutines.delay

@Composable
fun EditGoalDialog(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onUpdateGoal: (title: String, description: String?, why: String?, deadlineEpochMs: Long?) -> Unit,
    daysWithTasks: Set<Long> = emptySet()
) {
    Dialog(onDismissRequest = onDismiss) {
        var title by remember { mutableStateOf(goal.title) }
        var description by remember { mutableStateOf(goal.description ?: "") }
        var showDescription by remember { mutableStateOf(goal.description?.isNotBlank() == true) }
        var selectedDeadline by remember { mutableStateOf(goal.deadlineEpochMs) }
        var showCalendar by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
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
                // Title
                Text(
                    text = "ویرایش هدف",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "جزئیات هدف را ویرایش کنید",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Goal Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("مثال: یادگیری AI Engineering", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { showDescription = true })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description toggle
                TextButton(
                    onClick = { showDescription = !showDescription },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (showDescription) "بدون توضیح" else "افزودن توضیح",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Description field (expandable)
                if (showDescription) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("توضیحات اختیاری...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (title.isNotBlank() && title != goal.title || description.trim() != (goal.description ?: "")) {
                                onUpdateGoal(title.trim(), description.trim().ifBlank { null }, goal.why, selectedDeadline)
                            }
                            onDismiss()
                        })
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ============================================
                // Deadline — optional target date (Goal-level)
                // ============================================
                Column {
                    Text(
                        text = "مهلت (اختیاری)",
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
                            .clickable { showCalendar = true }
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
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

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
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
                        onClick = {
                            if (title.isNotBlank()) {
                                onUpdateGoal(title.trim(), description.trim().ifBlank { null }, goal.why, selectedDeadline)
                                onDismiss()
                            }
                        },
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

        // Persian Calendar Dialog for deadline selection
        if (showCalendar) {
            val ctx = LocalContext.current
            val holidayRepo = remember(ctx) { HolidayRepository(ctx) }
            PersianCalendarDialog(
                selectedDateEpochMs = selectedDeadline ?: System.currentTimeMillis(),
                onDateSelected = { epochMs -> selectedDeadline = epochMs },
                onDismiss = { showCalendar = false },
                holidayRepository = holidayRepo,
                daysWithIndicators = daysWithTasks,
                indicatorColor = AccentGreen,
                showIndicator = { it in daysWithTasks },
                confirmButtonText = "انتخاب مهلت",
                showConfirmButton = true,
                dayContextContent = { dayContext ->
                    var showDetails by remember { mutableStateOf(false) }
                    DayContextHeader(
                        calendarDate = dayContext.date,
                        isExpanded = showDetails,
                        onToggle = { showDetails = !showDetails }
                    )
                    AnimatedVisibility(
                        visible = showDetails,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        DayContextDetails(context = dayContext)
                    }
                }
            )
        }
    }
}
