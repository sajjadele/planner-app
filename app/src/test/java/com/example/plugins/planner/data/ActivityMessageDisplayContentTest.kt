package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5.7.1 — ActivityMessageDisplayContent Tests
 *
 * Validates the simplified 3-type classification:
 * TextContent, MultiMediaContent, EmptyMessage, DeletedMessage
 */
class ActivityMessageDisplayContentTest {

    // ════════════════════════════════════════════════════════════════
    // TextContent
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `text only message resolves to TextContent`() {
        val msg = createMessage(text = "Hello, world!")
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.TextContent)
        assertEquals("Hello, world!", (content as ActivityMessageDisplayContent.TextContent).text)
    }

    @Test
    fun `duration only resolves to TextContent showing duration text`() {
        val msg = createMessage(text = null, durationMinutes = 90)
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.TextContent)
        val tc = content as ActivityMessageDisplayContent.TextContent
        assertTrue(tc.text.contains("90"))
        assertEquals(90, tc.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // MultiMediaContent (image only)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `image only message resolves to MultiMediaContent as image`() {
        val msg = createMessage(
            text = null,
            attachments = listOf(ActivityAttachment.Image("content://test.jpg"))
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.MultiMediaContent)
        val mc = content as ActivityMessageDisplayContent.MultiMediaContent
        assertTrue(mc.images.isNotEmpty())
        assertTrue(mc.images.first() is ActivityAttachment.Image)
    }

    @Test
    fun `JSON-only message never leaks JSON to text`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = null,
            attachments = listOf(ActivityAttachment.Image("content://image.jpg")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.MultiMediaContent)
    }

    // ════════════════════════════════════════════════════════════════
    // MultiMediaContent (image + text)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `text with image resolves to MultiMediaContent`() {
        val msg = createMessage(
            text = "Check this out!",
            attachments = listOf(ActivityAttachment.Image("content://pic.jpg"))
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.MultiMediaContent)
        val mwt = content as ActivityMessageDisplayContent.MultiMediaContent
        assertEquals("Check this out!", mwt.text)
        assertTrue(mwt.images.isNotEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // MultiMediaContent (file only)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `file only message resolves to MultiMediaContent as file`() {
        val msg = createMessage(
            text = null,
            attachments = listOf(ActivityAttachment.File("content://doc.pdf", "report.pdf"))
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.MultiMediaContent)
        val mc = content as ActivityMessageDisplayContent.MultiMediaContent
        assertTrue(mc.files.isNotEmpty())
        assertTrue(mc.files.first() is ActivityAttachment.File)
    }

    // ════════════════════════════════════════════════════════════════
    // MultiMediaContent (file + text)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `text with file resolves to MultiMediaContent as file`() {
        val msg = createMessage(
            text = "See attached file",
            attachments = listOf(ActivityAttachment.File("content://data.csv", "data.csv"))
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.MultiMediaContent)
        val mwt = content as ActivityMessageDisplayContent.MultiMediaContent
        assertEquals("See attached file", mwt.text)
        assertTrue(mwt.files.isNotEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // Empty / Deleted
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `empty message resolves to EmptyMessage`() {
        val msg = createMessage(text = null, durationMinutes = null, attachments = emptyList())
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.EmptyMessage)
    }

    @Test
    fun `deleted message resolves to DeletedMessage`() {
        val msg = createMessage(text = "Original").copy(isDeleted = true)
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue(content is ActivityMessageDisplayContent.DeletedMessage)
    }

    // ════════════════════════════════════════════════════════════════
    // No event label / step name leakage
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `stepName is not part of display content`() {
        val msg = createMessage(text = "A note")
        val content = ActivityMessageDisplayContent.from(msg)
        // Ensure content has no stepName property
        val hasStepName = try {
            content::class.java.getDeclaredField("stepName")
            true
        } catch (_: NoSuchFieldException) {
            false
        }
        assertFalse("Display content should not have stepName", hasStepName)
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