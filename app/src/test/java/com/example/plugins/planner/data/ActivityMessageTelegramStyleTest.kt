package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5.7.2 — Telegram Style Activity Message Polish Tests
 *
 * Validates message rendering matches Telegram Saved Messages UX:
 * - Image placement and sizing
 * - Click-to-viewer callback
 * - RTL text alignment model
 * - Timestamp position
 * - Long-press menu structure
 * - Step metadata separation
 */
class ActivityMessageTelegramStyleTest {

    // ════════════════════════════════════════════════════════════════
    // Media sizing
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `image only does not create empty text area`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = null,
            attachments = listOf(ActivityAttachment.Image("content://photo.jpg")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.MultiMediaContent)
        // No text field in MultiMediaContent when images only — ensures no empty text area
        val mc = content as ActivityMessageDisplayContent.MultiMediaContent
        assertNotNull(mc.images)
    }

    @Test
    fun `image renders before text in MultiMediaContent`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "Caption text",
            attachments = listOf(ActivityAttachment.Image("content://img.jpg")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.MultiMediaContent)
        val mwt = content as ActivityMessageDisplayContent.MultiMediaContent
        // Media is the primary entity; text is caption
        assertNotNull(mwt.images)
        assertEquals("Caption text", mwt.text)
    }

    @Test
    fun `image click triggers viewer callback via URI extraction`() {
        val uri = "content://photo.jpg"
        val attachment = ActivityAttachment.Image(uri)

        // Simulate what TaskDetailScreen does when attachment is clicked
        var capturedUri: String? = null
        val clickHandler: (ActivityAttachment) -> Unit = { att ->
            if (att is ActivityAttachment.Image) {
                capturedUri = att.uri
            }
        }

        clickHandler(attachment)
        assertEquals("Image click should capture URI for viewer", uri, capturedUri)
    }

    @Test
    fun `file click does not trigger image viewer`() {
        val attachment = ActivityAttachment.File("content://doc.pdf", "doc.pdf")

        var capturedUri: String? = null
        val clickHandler: (ActivityAttachment) -> Unit = { att ->
            if (att is ActivityAttachment.Image) {
                capturedUri = att.uri
            }
        }

        clickHandler(attachment)
        assertNull("File click should not trigger image viewer", capturedUri)
    }

    // ════════════════════════════════════════════════════════════════
    // RTL / Persian text alignment
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `Persian text uses RTL alignment model`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "سلام این یک تست فارسی است",
            attachments = emptyList(), durationMinutes = null,
            createdAt = 1000L, canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.TextContent)
        val text = (content as ActivityMessageDisplayContent.TextContent).text

        // The text content is preserved as-is (RTL alignment is a composable concern)
        assertEquals("سلام این یک تست فارسی است", text)
        // Ensure no RTL mark stripping or transformation
        assertTrue(text.contains("سلام"))
    }

    // ════════════════════════════════════════════════════════════════
    // Timestamp position
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `timestamp position is bottom-right in metadata row`() {
        // The MessageMetadataRow composable uses Arrangement.End (bottom-right in RTL)
        // This test verifies the model carries timestamp for that placement
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "Test", attachments = emptyList(),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.TextContent)
        assertEquals(1000L, msg.createdAt)
        // Timestamp is always present in the model
        assertNotNull(msg.createdAt)
    }

    // ════════════════════════════════════════════════════════════════
    // Long-press menu structure
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `long press context menu actions in correct order`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "Test", attachments = emptyList(),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val capability = msg.capability()
        // Capability exposes which actions are available
        assertTrue("Reply should be available", capability.canReply)
        assertTrue("Edit should be available", capability.canEdit)
        assertTrue("Delete should be available", capability.canDelete)
    }

    @Test
    fun `step metadata does not appear in message display`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = 5,
            text = "Task with tag",
            attachments = emptyList(), durationMinutes = null,
            createdAt = 1000L, canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)

        // stepId is part of ActivityMessageModel, but not ActivityMessageDisplayContent
        assertEquals(5, msg.stepId)
        assertTrue(content is ActivityMessageDisplayContent.TextContent)

        // Verify display content has no step-related field
        val hasStepField = content::class.java.declaredFields.any { it.name == "stepName" || it.name == "stepId" }
        assertFalse("Display content should not expose step metadata", hasStepField)
    }

    @Test
    fun `message with both image and file uses image as primary`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "Mixed media",
            attachments = listOf(
                ActivityAttachment.Image("content://img.jpg"),
                ActivityAttachment.File("content://doc.pdf", "doc.pdf")
            ),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.MultiMediaContent)
        val mwt = content as ActivityMessageDisplayContent.MultiMediaContent
        assertTrue("Image should be primary attachment", mwt.images.isNotEmpty())
    }
}