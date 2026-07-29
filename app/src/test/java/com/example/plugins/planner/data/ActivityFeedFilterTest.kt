package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ActivityFeedFilterTest — Tests for Activity Feed Filter logic.
 *
 * Phase 5.2.2: Activity Feed UX Layer
 *
 * Tests cover:
 * 1. Default filter shows all activities (including task-level)
 * 2. Step filter only shows matching activities
 * 3. Step filter hides task-level activities
 * 4. Empty filter result handled
 * 5. Image-only filter works
 * 6. File-only filter works
 * 7. Filter state transitions correctly
 */
class ActivityFeedFilterTest {

    // ════════════════════════════════════════════════════════════════
    // Test Helpers
    // ════════════════════════════════════════════════════════════════

    private fun createMessage(
        id: Long = 1,
        stepId: Long? = null,
        text: String? = "Test",
        attachments: List<ActivityAttachment> = emptyList(),
        durationMinutes: Int? = null
    ) = ActivityMessageModel(
        id = id,
        taskId = 100,
        stepId = stepId,
        text = text,
        attachments = attachments,
        durationMinutes = durationMinutes,
        createdAt = 1000L,
        canEdit = true,
        canDelete = true
    )

    private fun createImageMessage(
        id: Long = 1,
        stepId: Long? = null,
        text: String? = null
    ) = createMessage(
        id = id,
        stepId = stepId,
        text = text,
        attachments = listOf(ActivityAttachment.Image("content://test.jpg"))
    )

    private fun createFileMessage(
        id: Long = 1,
        stepId: Long? = null,
        text: String? = null
    ) = createMessage(
        id = id,
        stepId = stepId,
        text = text,
        attachments = listOf(ActivityAttachment.File("content://test.pdf", "test.pdf"))
    )

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
            result = result.filter { msg ->
                com.example.core.util.JalaliDate.toEpochMs(
                    com.example.core.util.JalaliDate.fromEpochMs(msg.createdAt)
                ) == selectedDate
            }
        }
        if (filter.showImagesOnly) {
            result = result.filter { it.attachments.any { a -> a is ActivityAttachment.Image } }
        }
        if (filter.showFilesOnly) {
            result = result.filter { it.attachments.any { a -> a is ActivityAttachment.File } }
        }
        return result
    }

    // ════════════════════════════════════════════════════════════════
    // Test 1: Default filter shows all activities
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `default filter shows all activities`() {
        val messages = listOf(
            createMessage(id = 1, stepId = 1, text = "Step 1 note"),
            createMessage(id = 2, stepId = 2, text = "Step 2 note"),
            createMessage(id = 3, stepId = null, text = "Task-level note")
        )

        val result = applyFilter(messages, ActivityFeedFilterState.DEFAULT)

        assertEquals(3, result.size)
    }

    @Test
    fun `default filter shows task-level activities`() {
        val messages = listOf(
            createMessage(id = 1, stepId = null, text = "Task-level note")
        )

        val result = applyFilter(messages, ActivityFeedFilterState.DEFAULT)

        assertEquals(1, result.size)
        assertNull(result[0].stepId)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: Step filter only shows matching activities
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step filter only shows matching step activities`() {
        val messages = listOf(
            createMessage(id = 1, stepId = 1, text = "Step 1 note"),
            createMessage(id = 2, stepId = 2, text = "Step 2 note"),
            createMessage(id = 3, stepId = 1, text = "Another Step 1 note"),
            createMessage(id = 4, stepId = null, text = "Task-level note")
        )

        val filter = ActivityFeedFilterState(selectedStepId = 1)
        val result = applyFilter(messages, filter)

        assertEquals(2, result.size)
        assertTrue(result.all { it.stepId == 1L })
    }

    @Test
    fun `step filter excludes other steps`() {
        val messages = listOf(
            createMessage(id = 1, stepId = 1, text = "Step 1"),
            createMessage(id = 2, stepId = 2, text = "Step 2")
        )

        val filter = ActivityFeedFilterState(selectedStepId = 1)
        val result = applyFilter(messages, filter)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].stepId)
    }

    @Test
    fun `step filter excludes task-level activities`() {
        val messages = listOf(
            createMessage(id = 1, stepId = 1, text = "Step note"),
            createMessage(id = 2, stepId = null, text = "Task-level")
        )

        val filter = ActivityFeedFilterState(selectedStepId = 1)
        val result = applyFilter(messages, filter)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].stepId)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: Empty filter result works
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step filter with no matching steps returns empty`() {
        val messages = listOf(
            createMessage(id = 1, stepId = 1, text = "Step 1"),
            createMessage(id = 2, stepId = null, text = "Task-level")
        )

        val filter = ActivityFeedFilterState(selectedStepId = 99)
        val result = applyFilter(messages, filter)

        assertTrue(result.isEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: Image-only filter
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `image-only filter shows only image messages`() {
        val messages = listOf(
            createImageMessage(id = 1, text = "Photo"),
            createMessage(id = 2, text = "Text note"),
            createFileMessage(id = 3, text = "File")
        )

        val filter = ActivityFeedFilterState(showImagesOnly = true)
        val result = applyFilter(messages, filter)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertTrue(result[0].attachments.any { it is ActivityAttachment.Image })
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: File-only filter
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `file-only filter shows only file messages`() {
        val messages = listOf(
            createFileMessage(id = 1, text = "Document"),
            createMessage(id = 2, text = "Text note"),
            createImageMessage(id = 3, text = "Photo")
        )

        val filter = ActivityFeedFilterState(showFilesOnly = true)
        val result = applyFilter(messages, filter)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertTrue(result[0].attachments.any { it is ActivityAttachment.File })
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Filter state defaults
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `filter state default has no filters`() {
        val state = ActivityFeedFilterState()

        assertNull(state.selectedStepId)
        assertFalse(state.showImagesOnly)
        assertFalse(state.showFilesOnly)
    }

    @Test
    fun `filter state stepId change works`() {
        val state = ActivityFeedFilterState.DEFAULT.copy(selectedStepId = 5)

        assertEquals(5L, state.selectedStepId)
    }

    @Test
    fun `filter state clear works`() {
        val state = ActivityFeedFilterState(selectedStepId = 3)
        val cleared = state.copy(selectedStepId = null)

        assertNull(cleared.selectedStepId)
    }
}
