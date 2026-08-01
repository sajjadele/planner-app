package com.example.plugins.planner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.isolated
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * VisionWheelPicker — iOS-style wheel picker for time selection.
 *
 * Features:
 * - Shows 3 items visible (previous, current, next)
 * - Snap behavior for smooth scrolling
 * - Auto-scroll to initial value
 * - Animated highlight for selected item
 * - Dark theme with primary accent
 */
@Composable
fun VisionWheelPicker(
    items: List<Int>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    formatItem: (Int) -> String = { String.format("%02d", it) },
    modifier: Modifier = Modifier,
    visibleItems: Int = 3
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex
    )
    val coroutineScope = rememberCoroutineScope()

    // Detect when scrolling stops
    val isScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }

    LaunchedEffect(isScrolling) {
        if (!isScrolling && listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val centerIndex = listState.layoutInfo.visibleItemsInfo
                .minByOrNull { kotlin.math.abs(it.offset) }
                ?.index ?: selectedIndex
            if (centerIndex in items.indices && centerIndex != selectedIndex) {
                onItemSelected(centerIndex)
            }
        }
    }

    // Scroll to selected item when it changes
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = modifier
            .height((32.dp * visibleItems) + 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        // Center highlight
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        )

        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val distance = kotlin.math.abs(
                    (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                        (it.index - index)
                    } ?: 0)
                )
                val alpha = if (distance <= 1) 1f else 0.3f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatItem(item),
                        fontSize = 20.sp,
                        fontWeight = if (index == selectedIndex)
                            FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selectedIndex)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * VisionTimeWheel — Combined hour + minute wheel picker.
 *
 * Shows hour and minute wheels side by side.
 * Auto-switches from hour to minute when hour stops scrolling.
 */
@Composable
fun VisionTimeWheel(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onModeAutoSwitch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hours = remember { (1..12).toList() }
    val minutes = remember { (0..55 step 5).toList() }

    val coroutineScope = rememberCoroutineScope()

    // Track current selected indices
    var selectedHourIndex by remember { mutableStateOf(hours.indexOf(hour).coerceAtLeast(0)) }
    var selectedMinuteIndex by remember { mutableStateOf(minutes.indexOf(minute).coerceAtLeast(0)) }

    // Track if hour was just changed (for auto-switch)
    var hourJustChanged by remember { mutableStateOf(false) }

    // Auto-switch to minute after hour stops scrolling
    LaunchedEffect(hourJustChanged) {
        if (hourJustChanged) {
            kotlinx.coroutines.delay(800) // Wait for scrolling to stop
            onModeAutoSwitch?.invoke()
            hourJustChanged = false
        }
    }

    // Update indices when external values change
    LaunchedEffect(hour) {
        val newIndex = hours.indexOf(hour)
        if (newIndex >= 0 && newIndex != selectedHourIndex) {
            selectedHourIndex = newIndex
        }
    }

    LaunchedEffect(minute) {
        val newIndex = minutes.indexOf(minute)
        if (newIndex >= 0 && newIndex != selectedMinuteIndex) {
            selectedMinuteIndex = newIndex
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour wheel
        VisionWheelPicker(
            items = hours,
            selectedIndex = selectedHourIndex,
            onItemSelected = { index ->
                selectedHourIndex = index
                onHourChange(hours[index])
                hourJustChanged = true
            },
            modifier = Modifier.width(80.dp)
        )

        // Separator
        Text(
            text = ":",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Minute wheel
        VisionWheelPicker(
            items = minutes,
            selectedIndex = selectedMinuteIndex,
            onItemSelected = { index ->
                selectedMinuteIndex = index
                onMinuteChange(minutes[index])
            },
            modifier = Modifier.width(80.dp)
        )
    }
}
