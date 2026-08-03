package com.example.plugins.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.CalendarDate
import com.example.core.domain.DayContext
import com.example.core.domain.Holiday

/**
 * Level 1 (always visible): selected Jalali date + expand/collapse trigger.
 */
@Composable
fun DayContextHeader(
    calendarDate: CalendarDate,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = calendarDate.toJalaliDisplay(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isExpanded) "اطلاعات کمتر" else "اطلاعات بیشتر",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Level 2 (expanded): detailed information about the selected day.
 */
@Composable
fun DayContextDetails(
    context: DayContext,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Gregorian date
        Text(
            text = context.date.toGregorianDisplay(),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Holidays
        if (context.holidays.isNotEmpty()) {
            Divider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            context.holidays.forEach { holiday ->
                HolidayRow(holiday)
            }
        }

        // Future placeholder slots
        // - Task count for this day
        // - Goal activity
        // - Notes
        // - AI summary
    }
}

@Composable
private fun HolidayRow(
    holiday: Holiday,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (holiday.isOfficial) "🌙" else "🕌",
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = holiday.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "تعطیل رسمی",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
