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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.util.formatPersianTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.core.search.SearchResult
import com.example.core.util.RTL
import com.example.ui.components.VisionText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    onDismissRequest: () -> Unit,
    onNavigateToTask: (dateEpochMs: Long) -> Unit,
    onNavigateToTaskDetails: (taskId: Int) -> Unit = {},
    onNavigateToGoalDetails: (goalId: Int) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentTasks: List<SearchResult> by viewModel.recentTasks.collectAsState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
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
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .size(36.dp)
                                .testTag("close_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        if (recentTasks.isNotEmpty()) {
                            // Show recent tasks
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    VisionText(
                                        text = "${RTL}آخرین جستجوها",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                items(recentTasks.size) { index ->
                                    val result = recentTasks[index]
                                    when (result) {
                                        is SearchResult.TaskResult -> {
                                            SearchTaskItem(
                                                task = result,
                                                onClick = {
                                                    viewModel.recordTaskAccess(result.id)
                                                    onNavigateToTask(result.dateEpochMs)
                                                    onDismissRequest()
                                                },
                                                onEdit = {
                                                    viewModel.recordTaskAccess(result.id)
                                                    onNavigateToTaskDetails(result.id)
                                                    onDismissRequest()
                                                }
                                            )
                                        }
                                        is SearchResult.GoalResult -> {
                                            SearchGoalItem(
                                                goal = result,
                                                onClick = {
                                                    viewModel.recordTaskAccess(result.id)
                                                    onNavigateToGoalDetails(result.id)
                                                    onDismissRequest()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty search hint state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    VisionText(
                                        text = "${RTL}عبارت مورد نظر خود را تایپ کنید",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    VisionText(
                                        text = "${RTL}جستجوی آنی در بین تمامی برنامه‌ها",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "لطفاً املای کلمات را بررسی کنید یا عبارت دیگری بنویسید",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        is SearchResult.GoalResult -> "goal:${it.id}"
                                    }
                                }
                            ) { result ->
                                when (result) {
                                    is SearchResult.TaskResult -> {
                                        SearchTaskItem(
                                            task = result,
                                            onClick = {
                                                viewModel.recordTaskAccess(result.id)
                                                onNavigateToTask(result.dateEpochMs)
                                                onDismissRequest()
                                            },
                                            onEdit = {
                                                viewModel.recordTaskAccess(result.id)
                                                onNavigateToTaskDetails(result.id)
                                                onDismissRequest()
                                            }
                                        )
                                    }
                                    is SearchResult.GoalResult -> {
                                        SearchGoalItem(
                                            goal = result,
                                            onClick = {
                                                viewModel.recordTaskAccess(result.id)
                                                onNavigateToGoalDetails(result.id)
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

@Composable
fun SearchTaskItem(
    task: SearchResult.TaskResult,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    // Handle nullable priority safely - default to LOW for display
    val priorityValue = task.priority ?: "LOW"
    
    val priorityColor = when (priorityValue.uppercase()) {
        "HIGH" -> MaterialTheme.colorScheme.error
        "MEDIUM" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val priorityLabel = when (priorityValue.uppercase()) {
        "HIGH" -> "فوری"
        "MEDIUM" -> "متوسط"
        else -> "عادی"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircleOutline,
                contentDescription = "تسک",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Tags row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Type Badge
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "تسک",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Priority Badge
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

                if (task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "انجام شده",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            VisionText(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Edit button
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "جزئیات",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SearchGoalItem(
    goal: SearchResult.GoalResult,
    onClick: () -> Unit
) {
    val statusColor = when (goal.status) {
        "active" -> MaterialTheme.colorScheme.primary
        "paused" -> MaterialTheme.colorScheme.tertiary
        "completed" -> MaterialTheme.colorScheme.secondary
        "archived" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val statusLabel = when (goal.status) {
        "active" -> "فعال"
        "paused" -> "متوقف"
        "completed" -> "تکمیل"
        "archived" -> "آرشیو"
        else -> goal.status
    }
    
    val goalIcon = when (goal.status) {
        "completed" -> Icons.Default.CheckCircle
        "paused" -> Icons.Default.PauseCircle
        "active" -> Icons.Default.Flag
        else -> Icons.Default.Flag
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = goalIcon,
                contentDescription = "هدف",
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Tags row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Type Badge
                Box(
                    modifier = Modifier
                        .background(statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "هدف",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            VisionText(
                text = goal.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Description (if present)
            if (goal.description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                VisionText(
                    text = goal.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}