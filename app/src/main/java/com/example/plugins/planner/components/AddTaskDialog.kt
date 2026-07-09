package com.example.plugins.planner.components

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (
        title: String,
        priority: String?,
        hour: Int?,
        minute: Int?,
        goalName: String?,
        valueTag: String?
    ) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        var title by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf<String?>(null) }
        var selectedHour by remember { mutableStateOf<Int?>(null) }
        var selectedMinute by remember { mutableStateOf<Int?>(null) }
        var showOptionalFields by remember { mutableStateOf(false) }
        var goalName by remember { mutableStateOf("") }
        var valueTag by remember { mutableStateOf<String?>(null) }
        val focusRequester = remember { FocusRequester() }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }

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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان برنامه", color = Color(0xFF49454F)) },
                    placeholder = { Text("مثال: بررسی معماری ماژولار", color = Color(0xFF938F99)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedLabelColor = Color(0xFF6750A4),
                        unfocusedLabelColor = Color(0xFF49454F),
                        cursorColor = Color(0xFF6750A4),
                        focusedTextColor = Color(0xFF1C1B1F),
                        unfocusedTextColor = Color(0xFF1C1B1F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (title.isNotBlank()) {
                                onAddTask(
                                    title,
                                    priority,
                                    selectedHour,
                                    selectedMinute,
                                    if (goalName.isNotBlank()) goalName else null,
                                    valueTag
                                )
                                onDismiss()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("task_title_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Optional metadata section (collapsible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOptionalFields = !showOptionalFields }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showOptionalFields) "جزئیات اختیاری" else "جزئیات اختیاری (اولویت، یادآور)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                    Icon(
                        imageVector = if (showOptionalFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "باز/بسته کردن بخش اختیاری",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (showOptionalFields) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Priority Level Selection
                        Text(
                            text = "سطح اولویت",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F)
                        )

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
                                        .clickable { priority = level }
                                        .then(
                                            Modifier.background(
                                                if (isSelected) Color(0xFFEADDFF) else Color.Transparent,
                                                RoundedCornerShape(18.dp)
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clip(RoundedCornerShape(18.dp))
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Semantic Attachment: Goal (optional)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "پیوند به هدف (اختیاری)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            OutlinedTextField(
                                value = goalName,
                                onValueChange = { goalName = it },
                                label = { Text("نام هدف، مثل: ورزیدن، یادگیری", fontSize = 12.sp, color = Color(0xFF49454F)) },
                                placeholder = { Text("بدون هدف", fontSize = 12.sp, color = Color(0xFF938F99)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFF49454F),
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF49454F),
                                    cursorColor = Color(0xFF6750A4),
                                    focusedTextColor = Color(0xFF1C1B1F),
                                    unfocusedTextColor = Color(0xFF1C1B1F)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { /* no target field */ })
                            )
                        }

                        // Semantic Attachment: ValueTag (optional lightweight chips)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "برچسب ارزش (اختیاری)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("سلامتی" to "Health", "یادگیری" to "Learning", "خانواده" to "Family", "انضباط" to "Discipline", "کار" to "Work", "رشد" to "Growth").forEach { (label, tag) ->
                                    val isSelected = valueTag == tag
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { valueTag = if (isSelected) null else tag },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.padding(bottom = 4.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFEADDFF),
                                            containerColor = Color(0xFFF3EDF7),
                                            selectedLabelColor = Color(0xFF21005D),
                                            labelColor = Color(0xFF49454F)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6750A4))
                    ) {
                        Text("انصراف", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAddTask(
                                    title,
                                    priority,
                                    selectedHour,
                                    selectedMinute,
                                    if (goalName.isNotBlank()) goalName else null,
                                    valueTag
                                )
                                onDismiss()
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
