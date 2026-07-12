package com.example.core.domain

/**
 * Contextual information about a selected day.
 *
 * Created when a user taps a calendar cell, wrapping the [CalendarDate]
 * with any additional data that enriches that day (holidays, events, etc.).
 *
 * Future extensions:
 * - taskCount: Int
 * - completedCount: Int
 * - notesCount: Int
 * - goalActivity: List<GoalActivity>
 * - aiSummary: String?
 */
data class DayContext(
    val date: CalendarDate,
    val holidays: List<Holiday> = emptyList()
) {
    /** True if there's any extra information worth showing. */
    val hasData: Boolean get() = holidays.isNotEmpty()
}
