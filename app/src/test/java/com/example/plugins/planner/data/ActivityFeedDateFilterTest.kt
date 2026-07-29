package com.example.plugins.planner.data

import com.example.core.util.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ActivityFeedDateFilterTest — Tests for Phase 6.0.6 date-based filtering.
 *
 * Tests cover:
 * 1. Null date shows all activities (no date filter)
 * 2. Selected date filters to same Jalali day only
 * 3. Date + step filter combination
 * 4. Date filter with no matching messages returns empty
 * 5. Date filter respects same-day normalization
 * 6. Multiple messages on same day all pass filter
 * 7. Clearing date filter restores all messages
 */
class ActivityFeedDateFilterTest {

    private val DAY_MILLIS = 86_400_000L

    private fun createMessage(
        id: Long = 1,
        stepId: Long? = null,
        text: String? = "Test",
        createdAt: Long = 1000L
    ) = ActivityMessageModel(
        id = id, taskId = 100, stepId = stepId, text = text,
        attachments = emptyList(), durationMinutes = null,
        createdAt = createdAt, canEdit = true, canDelete = true
    )

    private fun normalizeToDayStart(epochMs: Long): Long =
        JalaliDate.toEpochMs(JalaliDate.fromEpochMs(epochMs))

    private fun applyFilter(
        messages: List<ActivityMessageModel>,
        filter: ActivityFeedFilterState,
        selectedDate: Long? = null
    ): List<ActivityMessageModel> {
        var result = messages
        if (filter.selectedStepId != null) {
            result = result.filter { it.stepId == filter.selectedStepId }
        }
        if (selectedDate != null) {
            result = result.filter { normalizeToDayStart(it.createdAt) == selectedDate }
        }
        if (filter.showImagesOnly) {
            result = result.filter { it.attachments.any { a -> a is ActivityAttachment.Image } }
        }
        if (filter.showFilesOnly) {
            result = result.filter { it.attachments.any { a -> a is ActivityAttachment.File } }
        }
        return result
    }

    private fun todayEpochMs(): Long = normalizeToDayStart(System.currentTimeMillis())
    private fun daysAgo(n: Int): Long = todayEpochMs() - n * DAY_MILLIS

    @Test fun `null date shows all activities`() {
        val messages = listOf(
            createMessage(id = 1, createdAt = daysAgo(0)),
            createMessage(id = 2, createdAt = daysAgo(1)),
            createMessage(id = 3, createdAt = daysAgo(2))
        )
        assertEquals(3, applyFilter(messages, ActivityFeedFilterState.DEFAULT, null).size)
    }

    @Test fun `selected date only shows messages from that day`() {
        val today = todayEpochMs()
        val yesterday = daysAgo(1)
        val messages = listOf(
            createMessage(id = 1, createdAt = today + 1000),
            createMessage(id = 2, createdAt = yesterday + 2000),
            createMessage(id = 3, createdAt = today + 5000)
        )
        val result = applyFilter(messages, ActivityFeedFilterState.DEFAULT, today)
        assertEquals(2, result.size)
        assertTrue(result.all { normalizeToDayStart(it.createdAt) == today })
    }

    @Test fun `date and step filter work together`() {
        val today = todayEpochMs()
        val yesterday = daysAgo(1)
        val messages = listOf(
            createMessage(id = 1, stepId = 1, createdAt = today),
            createMessage(id = 2, stepId = 2, createdAt = today),
            createMessage(id = 3, stepId = 1, createdAt = yesterday),
            createMessage(id = 4, stepId = null, createdAt = today)
        )
        val result = applyFilter(messages, ActivityFeedFilterState(selectedStepId = 1), today)
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test fun `date filter with no matching messages returns empty`() {
        val messages = listOf(
            createMessage(id = 1, createdAt = daysAgo(5)),
            createMessage(id = 2, createdAt = daysAgo(10))
        )
        assertEquals(0, applyFilter(messages, ActivityFeedFilterState.DEFAULT, todayEpochMs()).size)
    }

    @Test fun `messages at different times on same day all pass`() {
        val today = todayEpochMs()
        val messages = listOf(
            createMessage(id = 1, createdAt = today),
            createMessage(id = 2, createdAt = today + 6 * 3600_000),
            createMessage(id = 3, createdAt = today + 12 * 3600_000),
            createMessage(id = 4, createdAt = today + 23 * 3600_000)
        )
        assertEquals(4, applyFilter(messages, ActivityFeedFilterState.DEFAULT, today).size)
    }

    @Test fun `multiple messages on same day all pass filter`() {
        val today = todayEpochMs()
        val messages = (1..5).map { createMessage(id = it.toLong(), createdAt = today + it * 1000) }
        assertEquals(5, applyFilter(messages, ActivityFeedFilterState.DEFAULT, today).size)
    }

    @Test fun `clearing date filter shows all messages`() {
        val today = todayEpochMs()
        val yesterday = daysAgo(1)
        val messages = listOf(
            createMessage(id = 1, createdAt = today),
            createMessage(id = 2, createdAt = yesterday)
        )
        assertEquals(1, applyFilter(messages, ActivityFeedFilterState.DEFAULT, today).size)
        assertEquals(2, applyFilter(messages, ActivityFeedFilterState.DEFAULT, null).size)
    }

    @Test fun `empty list with date filter returns empty`() {
        assertEquals(0, applyFilter(emptyList(), ActivityFeedFilterState.DEFAULT, todayEpochMs()).size)
    }
}
