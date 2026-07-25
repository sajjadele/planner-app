package com.example.core.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.util.formatPersianTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    onDismissRequest: () -> Unit,
    onNavigateToTask: (dateEpochMs: Long) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Force RTL layout direction for Persian support
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFDFBFF))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Header with back/close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "جستجوی پیشرفته",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .background(Color(0xFFF3EDF7), CircleShape)
                                .size(36.dp)
                                .testTag("close_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = Color(0xFF49454F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Search input text field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = {
                            Text(
                                "جستجو در برنامه‌ها...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "آیکون جستجو",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "پاک کردن",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Results content
                    if (searchQuery.isBlank()) {
                        // Empty search hint state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔍", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "عبارت مورد نظر خود را تایپ کنید",
                                    fontSize = 14.sp,
                                    color = Color(0xFF938F99),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "جستجوی آنی در بین تمامی برنامه‌ها",
                                    fontSize = 11.sp,
                                    color = Color(0xFF938F99)
                                )
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        // No results found
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🍃", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "نتیجه‌ای یافت نشد",
                                    fontSize = 14.sp,
                                    color = Color(0xFF938F99),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "لطفاً املای کلمات را بررسی کنید یا عبارت دیگری بنویسید",
                                    fontSize = 11.sp,
                                    color = Color(0xFF938F99)
                                )
                            }
                        }
                    } else {
                        // Scrollable results list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_results_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                searchResults,
                                key = {
                                    when (it) {
                                        is SearchResult.TaskResult -> "task:${it.id}"
                                    }
                                }
                            ) { result ->
                                when (result) {
                                    is SearchResult.TaskResult -> {
                                        SearchTaskItem(
                                            task = result,
                                            onClick = {
                                                onNavigateToTask(result.dateEpochMs)
                                                onDismissRequest()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTaskItem(
    task: SearchResult.TaskResult,
    onClick: () -> Unit
) {
    // Handle nullable priority safely - default to LOW for display
    val priorityValue = task.priority ?: "LOW"
    
    val priorityColor = when (priorityValue.uppercase()) {
        "HIGH" -> Color(0xFFB3261E)
        "MEDIUM" -> Color(0xFF6750A4)
        else -> Color(0xFF49454F)
    }
    
    val priorityLabel = when (priorityValue.uppercase()) {
        "HIGH" -> "فوری"
        "MEDIUM" -> "متوسط"
        else -> "عادی"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFEADDFF).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = "برنامه",
                tint = Color(0xFF21005D),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEADDFF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "برنامه",
                        color = Color(0xFF21005D),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Day Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.dayName,
                        color = Color(0xFF49454F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF21005D).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "انجام شده",
                            color = Color(0xFF21005D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1B1F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Priority Marker
        Box(
            modifier = Modifier
                .background(priorityColor.copy(alpha = 0.1f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = priorityLabel,
                color = priorityColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
