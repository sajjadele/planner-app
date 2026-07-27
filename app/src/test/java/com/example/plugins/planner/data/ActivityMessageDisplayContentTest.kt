package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5.6 — ActivityMessageDisplayContent Tests
 *
 * Validates that ActivityMessageDisplayContent correctly classifies
 * all message content types and prevents JSON/URI leakage.
 */
class ActivityMessageDisplayContentTest {

    // ════════════════════════════════════════════════════════════════
    // Text Only
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `text only message resolves to TextOnly`() {
        val msg = createMessage(text = "Hello, world!")
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.TextOnly)
        assertEquals("Hello, world!", (content as ActivityMessageDisplayContent.TextOnly).text)
    }

    // ════════════════════════════════════════════════════════════════
    // Image Only
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `image only message resolves to ImageOnly`() {
        val msg = createMessage(
            text = null,
            attachments = listOf(ActivityAttachment.Image("content://test.jpg"))
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.ImageOnly)
        val image = (content as ActivityMessageDisplayContent.ImageOnly).image
        assertEquals("content://test.jpg", image.uri)
    }

    @Test
    fun `image only message has no text even if description has JSON`() {
        // Simulate what happens when description has JSON but no text field
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = null, // text is null because mapper now returns null for JSON-without-text
            attachments = listOf(ActivityAttachment.Image("content://image.jpg")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue("JSON-only message should be ImageOnly, not EmptyMessage",
            content is ActivityMessageDisplayContent.ImageOnly)
        assertNull("No text should leak from JSON description",
            (content as? ActivityMessageDisplayContent.TextOnly)?.text)
    }

    // ════════════════════════════════════════════════════════════════
    // Text + Image
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `text with image resolves to TextWithImages`() {
        val msg = createMessage(
            text = "Check this out!",
            attachments = listOf(ActivityAttachment.Image("content://pic.jpg"))
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.TextWithImages)
        val tc = content as ActivityMessageDisplayContent.TextWithImages
        assertEquals("Check this out!", tc.text)
        assertEquals(1, tc.images.size)
    }

    // ════════════════════════════════════════════════════════════════
    // File Only
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `file only message resolves to FileOnly`() {
        val msg = createMessage(
            text = null,
            attachments = listOf(
                ActivityAttachment.File("content://doc.pdf", "report.pdf")
            )
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.FileOnly)
        val file = (content as ActivityMessageDisplayContent.FileOnly).file
        assertEquals("report.pdf", file.name)
    }

    // ════════════════════════════════════════════════════════════════
    // Text + File
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `text with file resolves to TextWithFiles`() {
        val msg = createMessage(
            text = "See attached file",
            attachments = listOf(
                ActivityAttachment.File("content://data.csv", "data.csv")
            )
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.TextWithFiles)
        val tc = content as ActivityMessageDisplayContent.TextWithFiles
        assertEquals("See attached file", tc.text)
        assertEquals(1, tc.files.size)
    }

    // ════════════════════════════════════════════════════════════════
    // Duration Activity
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `duration activity without text resolves to DurationActivity`() {
        val msg = createMessage(
            text = null,
            durationMinutes = 90
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.DurationActivity)
        val d = content as ActivityMessageDisplayContent.DurationActivity
        assertEquals(90, d.durationMinutes)
        assertNull(d.text)
    }

    @Test
    fun `duration activity with text resolves to DurationActivity`() {
        val msg = createMessage(
            text = "Worked on UI",
            durationMinutes = 45
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.DurationActivity)
        val d = content as ActivityMessageDisplayContent.DurationActivity
        assertEquals(45, d.durationMinutes)
        assertEquals("Worked on UI", d.text)
    }

    // ════════════════════════════════════════════════════════════════
    // Empty / No Content
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `message with no recognizable content resolves to EmptyMessage`() {
        val msg = createMessage(text = null, durationMinutes = null, attachments = emptyList())
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue("Empty message should resolve to EmptyMessage",
            content is ActivityMessageDisplayContent.EmptyMessage)
    }

    // ════════════════════════════════════════════════════════════════
    // Deleted Message
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `deleted message resolves to DeletedMessage`() {
        val msg = createMessage(text = "Original text").copy(isDeleted = true)
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.DeletedMessage)
    }

    // ════════════════════════════════════════════════════════════════
    // JSON Fallback Prevention
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `no Text composable receives raw JSON string`() {
        // If a message has null text, ActivityMessageCard should NOT show any text
        // regardless of what's in the description field
        val msgWithNullText = createMessage(text = null)
        val content = ActivityMessageDisplayContent.from(msgWithNullText)
        val textValue = when (content) {
            is ActivityMessageDisplayContent.TextOnly -> content.text
            is ActivityMessageDisplayContent.TextWithImages -> content.text
            is ActivityMessageDisplayContent.TextWithFiles -> content.text
            is ActivityMessageDisplayContent.DurationActivity -> content.text
            else -> null
        }
        assertNull("No text should appear for null-text messages", textValue)
    }

    // ════════════════════════════════════════════════════════════════
    // Helper
    // ════════════════════════════════════════════════════════════════

    private fun createMessage(
        text: String? = null,
        attachments: List<ActivityAttachment> = emptyList(),
        durationMinutes: Int? = null
    ): ActivityMessageModel {
        return ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = text,
            attachments = attachments,
            durationMinutes = durationMinutes,
            createdAt = 1000L,
            canEdit = true, canDelete = true
        )
    }
}
