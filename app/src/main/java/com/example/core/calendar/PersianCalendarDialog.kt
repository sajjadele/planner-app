package com.example.core.calendar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.data.HolidayRepository
import com.example.core.domain.DayContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersianCalendarDialog(
    selectedDateEpochMs: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    holidayRepository: HolidayRepository? = null,
    daysWithIndicators: Set<Long> = emptySet(),
    indicatorColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    showIndicator: (Long) -> Boolean = { false },
    confirmButtonText: String = "تأیید",
    showConfirmButton: Boolean = true,
    dayContextContent: (@Composable androidx.compose.foundation.layout.ColumnScope.(DayContext) -> Unit)? = null,
    minSelectableDate: Long? = null,
    maxSelectableDate: Long? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state = rememberPersianCalendarState(
        initialDateEpochMs = selectedDateEpochMs,
        minSelectableDate = minSelectableDate,
        maxSelectableDate = maxSelectableDate
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        PersianCalendarGrid(
            state = state,
            onDateSelected = onDateSelected,
            onDismiss = onDismiss,
            modifier = modifier,
            holidayRepository = holidayRepository,
            daysWithIndicators = daysWithIndicators,
            indicatorColor = indicatorColor,
            showIndicator = showIndicator,
            confirmButtonText = confirmButtonText,
            showConfirmButton = showConfirmButton,
            dayContextContent = dayContextContent,
            minSelectableDate = minSelectableDate,
            maxSelectableDate = maxSelectableDate
        )
    }
}
