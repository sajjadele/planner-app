package com.example.plugins.planner

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = viewModel()
) {
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val daysOfWeek = listOf(
        "دوشنبه" to "د",
        "سه‌شنبه" to "س",
        "چهارشنبه" to "چ",
        "پنجشنبه" to "پ",
        "جمعه" to "ج",
        "شنبه" to "ش",
        "یکشنبه" to "ی"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Selection Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                daysOfWeek.forEachIndexed { index, (fullName, shortName) ->
                    val isSelected = index == selectedDayIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectDay(index) }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isSelected) Color(0xFF6750A4) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color(0xFFCAC4D0),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = shortName,
                                color = if (isSelected) Color.White else Color(0xFF49454F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fullName.take(3), // display compact name
                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFF938F99),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section header
            val (currentDayName, _) = daysOfWeek[selectedDayIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "برنامه‌های $currentDayName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF49454F),
                    letterSpacing = 0.5.sp
                )
                
                val completedCount = tasks.count { it.isCompleted }
                Text(
                    text = "$completedCount از ${tasks.size} انجام شده",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6750A4)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                // Beautiful minimal empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📝",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "برنامه‌ای برای امروز ثبت نشده است",
                            color = Color(0xFF938F99),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای شروع، دکمه + را بزنید",
                            color = Color(0xFF938F99),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("planner_task_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val itemBgColor = if (task.isCompleted) Color.White else Color(0xFFEADDFF).copy(alpha = 0.3f)
                        val itemBorderColor = if (task.isCompleted) Color(0xFFCAC4D0) else Color(0xFFEADDFF)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(itemBgColor, RoundedCornerShape(16.dp))
                                .border(1.dp, itemBorderColor, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (task.isCompleted) Color(0xFF6750A4) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        2.dp,
                                        Color(0xFF6750A4),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.toggleTaskCompletion(task) }
                            ) {
                                if (task.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Task Details
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = task.title,
                                    color = if (task.isCompleted) Color(0xFF938F99) else Color(0xFF1C1B1F),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    val priorityLabel = when (task.priority) {
                                        "HIGH" -> "اولویت بالا"
                                        "MEDIUM" -> "اولویت متوسط"
                                        else -> "اولویت پایین"
                                    }
                                    val priorityColor = when (task.priority) {
                                        "HIGH" -> Color(0xFFB3261E)
                                        "MEDIUM" -> Color(0xFF6750A4)
                                        else -> Color(0xFF49454F)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(priorityColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = priorityLabel,
                                            color = priorityColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (task.reminderHour != null && task.reminderMinute != null) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(
                                            imageVector = Icons.Default.Alarm,
                                            contentDescription = "Alarm scheduled",
                                            tint = Color(0xFF6750A4),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val formattedTime = String.format("%02d:%02d", task.reminderHour, task.reminderMinute)
                                        Text(
                                            text = formattedTime,
                                            color = Color(0xFF49454F),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Delete action
                            IconButton(
                                onClick = { viewModel.deleteTask(task) },
                                modifier = Modifier.testTag("delete_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete task",
                                    tint = Color(0xFFB3261E).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB to add task
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = Color(0xFFD0BCFF),
            contentColor = Color(0xFF381E72),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp)
                .testTag("add_task_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task",
                modifier = Modifier.size(24.dp)
            )
        }

        // Minimal Add Task Dialog
        if (showAddTaskDialog) {
            Dialog(onDismissRequest = { showAddTaskDialog = false }) {
                var title by remember { mutableStateOf("") }
                var priority by remember { mutableStateOf("MEDIUM") }
                var selectedHour by remember { mutableStateOf<Int?>(null) }
                var selectedMinute by remember { mutableStateOf<Int?>(null) }

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
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "افزودن برنامه‌ی جدید",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text input for Title
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان برنامه") },
                            placeholder = { Text("مثال: بررسی معماری ماژولار") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                focusedLabelColor = Color(0xFF6750A4)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_title_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Priority Level Selection
                        Text(
                            text = "سطح اولویت",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("HIGH" to "بالا", "MEDIUM" to "متوسط", "LOW" to "پایین").forEach { (level, label) ->
                                val isSelected = priority == level
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .background(
                                            if (isSelected) Color(0xFFEADDFF) else Color.Transparent,
                                            RoundedCornerShape(18.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { priority = level }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reminder Switch / TimePicker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "تنظیم یادآور محلی",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF49454F)
                                )
                                Text(
                                    text = if (selectedHour != null && selectedMinute != null) {
                                        String.format("ساعت %02d:%02d", selectedHour, selectedMinute)
                                    } else {
                                        "بدون یادآور"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF6750A4)
                                )
                            }

                            Button(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minuteOfHour ->
                                            selectedHour = hourOfDay
                                            selectedMinute = minuteOfHour
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF3EDF7),
                                    contentColor = Color(0xFF6750A4)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("انتخاب ساعت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showAddTaskDialog = false },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6750A4))
                            ) {
                                Text("انصراف", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        viewModel.addTask(
                                            title = title,
                                            priority = priority,
                                            hour = selectedHour,
                                            minute = selectedMinute
                                        )
                                        showAddTaskDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = title.isNotBlank()
                            ) {
                                Text("ذخیره", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
