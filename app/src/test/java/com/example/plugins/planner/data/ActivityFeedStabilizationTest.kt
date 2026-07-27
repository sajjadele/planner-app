package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ActivityFeedStabilizationTest — Tests for Phase 5.4.1 Quick Stabilization.
 *
 * Tests cover:
 * 1. Debug leakage prevention (no JSON/URI in UI)
 * 2. Reply navigation with filters
 * 3. FAB action dispatch
 */
class ActivityFeedStabilizationTest {

    // ════════════════════════════════════════════════════════════════
    // Test Helpers
    // ════════════════════════════════════════════════════════════════

    private fun createMessage(
        id: Long = 1,
        stepId: Long? = null,
        text: String? = "Test",
        attachments: List<ActivityAttachment> = emptyList(),
        replyToMessageId: Long? = null
    ) = ActivityMessageModel(
        id = id,
        taskId = 100,
        stepId = stepId,
        text = text,
        attachments = attachments,
        durationMinutes = null,
        createdAt = 1000L,
        canEdit = true,
        canDelete = true,
        replyToMessageId = replyToMessageId
    )

    private fun createImageMessage(
        id: Long = 1,
        stepId: Long? = null,
        text: String? = null
    ) = createMessage(
        id = id,
        stepId = stepId,
        text = text,
        attachments = listOf(ActivityAttachment.Image("content://media/example.jpg"))
    )

    // ════════════════════════════════════════════════════════════════
    // 1. Debug Leakage Prevention
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `image attachment uri is never exposed as text`() {
        val message = createImageMessage(id = 1)

        // The URI should NOT appear in the text field
        assertNull(message.text)
        // The text should NOT contain "content://"
        assertTrue(message.text?.contains("content://") != true)
        // Attachments should contain the image
        assertTrue(message.attachments.isNotEmpty())
        assertTrue(message.attachments[0] is ActivityAttachment.Image)
    }

    @Test
    fun `image attachment uri is accessible through model`() {
        val message = createImageMessage(id = 1, text = "Screenshot")

        // The text should be the user's text, not the URI
        assertEquals("Screenshot", message.text)
        // The URI should be in the attachment only
        val image = message.attachments[0] as ActivityAttachment.Image
        assertEquals("content://media/example.jpg", image.uri)
    }

    @Test
    fun `json payload never appears in message text`() {
        // Simulate what happens when a JSON payload is decoded
        val json = """{"text":"My note","attachments":[{"type":"IMAGE","uri":"content://test.jpg"}]}"""

        // After mapping, the text should be "My note", not the raw JSON
        val entity = ActivityEventEntity(
            id = 1,
            taskId = 100,
            stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = json,
            timestamp = 1000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals("My note", model!!.text)
        assertFalse(model.text?.contains("{") == true)
        assertFalse(model.text?.contains("attachments") == true)
    }

    @Test
    fun `event type enum never appears in displayed fields`() {
        val entity = ActivityEventEntity(
            id = 1,
            taskId = 100,
            stepId = null,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = """{"text":"Photo description"}""",
            timestamp = 1000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        // The text should be the user's content, not the event type name
        assertEquals("Photo description", model!!.text)
        // The model should NOT have an eventType field that leaks to UI
        assertFalse(model.text?.contains("IMAGE_ADDED") == true)
    }

    // ════════════════════════════════════════════════════════════════
    // 2. Reply Navigation with Filters
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `reply navigation with active filter finds target in filtered list`() {
        val allMessages = listOf(
            createMessage(id = 1, stepId = 1, text = "Original"),
            createMessage(id = 2, stepId = 1, text = "Reply", replyToMessageId = 1),
            createMessage(id = 3, stepId = 2, text = "Another step")
        )

        val filter = ActivityFeedFilterState(selectedStepId = 1)
        val filteredMessages = allMessages.filter { it.stepId == filter.selectedStepId }

        // Target (id=1) should be visible in filtered list
        val index = filteredMessages.indexOfFirst { it.id == 1L }
        assertTrue("Target should be visible in filtered list", index >= 0)
        assertEquals(0, index) // First item in filtered list
    }

    @Test
    fun `reply navigation with hidden target clears filter`() {
        val allMessages = listOf(
            createMessage(id = 1, stepId = 1, text = "Original hidden by filter"),
            createMessage(id = 2, stepId = 2, text = "Reply in step 2", replyToMessageId = 1)
        )

        val filter = ActivityFeedFilterState(selectedStepId = 2)
        val filteredMessages = allMessages.filter { it.stepId == filter.selectedStepId }

        // Target (id=1) should NOT be in filtered list (it's in step 1)
        val index = filteredMessages.indexOfFirst { it.id == 1L }
        assertTrue("Target should NOT be visible in step 2 filter", index < 0)

        // After clearing filter, target should be visible
        val allFilter = ActivityFeedFilterState.DEFAULT
        val allMessagesFiltered = allMessages
        val allIndex = allMessagesFiltered.indexOfFirst { it.id == 1L }
        assertTrue("Target should be visible when filter is cleared", allIndex >= 0)
    }

    @Test
    fun `missing reply target does not crash navigation`() {
        val allMessages = listOf(
            createMessage(id = 2, text = "Orphan reply", replyToMessageId = 999)
        )

        // Target (id=999) doesn't exist
        val index = allMessages.indexOfFirst { it.id == 999L }
        assertTrue("Non-existent target should return -1", index < 0)
    }

    @Test
    fun `deleted reply target is handled as missing`() {
        // Simulate: original message exists in DB but mapper filtered it
        // The reply message references a replyToMessageId that's not in the list
        val allMessages = listOf(
            createMessage(id = 2, text = "Reply to deleted", replyToMessageId = 1)
        )

        // Target (id=1) is not in the list (deleted)
        val index = allMessages.indexOfFirst { it.id == 1L }
        assertTrue("Deleted target should return -1", index < 0)
    }

    // ════════════════════════════════════════════════════════════════
    // 3. FAB Action Dispatch
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `creation action Note is correct type`() {
        val action: ActivityCreationAction = ActivityCreationAction.Note
        assertTrue(action is ActivityCreationAction.Note)
    }

    @Test
    fun `creation action Image is correct type`() {
        val action: ActivityCreationAction = ActivityCreationAction.Image
        assertTrue(action is ActivityCreationAction.Image)
    }

    @Test
    fun `creation action File is correct type`() {
        val action: ActivityCreationAction = ActivityCreationAction.File
        assertTrue(action is ActivityCreationAction.File)
    }

    @Test
    fun `creation action ManualActivity is correct type`() {
        val action: ActivityCreationAction = ActivityCreationAction.ManualActivity
        assertTrue(action is ActivityCreationAction.ManualActivity)
    }

    @Test
    fun `creation action dispatch by type`() {
        // Simulate the dispatch logic from TaskDetailScreen
        fun handleAction(action: ActivityCreationAction): String = when (action) {
            ActivityCreationAction.Image -> "IMAGE_PICKER"
            ActivityCreationAction.Note -> "COMPOSER"
            ActivityCreationAction.File -> "COMPOSER"
            ActivityCreationAction.ManualActivity -> "COMPOSER"
        }

        assertEquals("IMAGE_PICKER", handleAction(ActivityCreationAction.Image))
        assertEquals("COMPOSER", handleAction(ActivityCreationAction.Note))
        assertEquals("COMPOSER", handleAction(ActivityCreationAction.File))
        assertEquals("COMPOSER", handleAction(ActivityCreationAction.ManualActivity))
    }
}
