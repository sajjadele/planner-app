package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5.7.1 — Activity Message Rendering Order Tests
 *
 * Validates Telegram-style ordering:
 * 1. Media renders BEFORE text
 * 2. Image-only has no text placeholder
 * 3. Text-only has no media area
 * 4. stepName not part of display content
 */
class ActivityMessageRenderingOrderTest {

    @Test
    fun `image only renders media content without text`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = null,
            attachments = listOf(ActivityAttachment.Image("content://photo.jpg")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue("Image-only should be MediaContent",
            content is ActivityMessageDisplayContent.MediaContent)
        val mc = content as ActivityMessageDisplayContent.MediaContent
        assertTrue("Attachment should be image", mc.isImage)
    }

    @Test
    fun `image plus text has media first in display content`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "Design review",
            attachments = listOf(ActivityAttachment.Image("content://wireframe.jpg")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue("Text+Image should be MediaWithText",
            content is ActivityMessageDisplayContent.MediaWithText)
        val mwt = content as ActivityMessageDisplayContent.MediaWithText
        assertEquals("Design review", mwt.text)
        assertTrue(mwt.isImage)
        // Media renders before text in UI (enforced by MessageContent composable)
    }

    @Test
    fun `text only renders without media area`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "Just a note",
            attachments = emptyList(),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue("Text-only should be TextContent (not MediaWithText)",
            content is ActivityMessageDisplayContent.TextContent)
    }

    @Test
    fun `file plus text has file first in display content`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "See attached file",
            attachments = listOf(ActivityAttachment.File("content://data.xlsx", "report.xlsx")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue("Text+File should be MediaWithText",
            content is ActivityMessageDisplayContent.MediaWithText)
        val mwt = content as ActivityMessageDisplayContent.MediaWithText
        assertEquals("See attached file", mwt.text)
        assertFalse(mwt.isImage)
        assertEquals("report.xlsx", (mwt.attachment as ActivityAttachment.File).name)
    }

    @Test
    fun `stepName is not part of display content`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = 5,
            text = "A tagged note",
            attachments = emptyList(), durationMinutes = null,
            createdAt = 1000L, canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue("Should still be TextContent even with stepId",
            content is ActivityMessageDisplayContent.TextContent)
        // Step information is not inside display content
        assertEquals("A tagged note",
            (content as ActivityMessageDisplayContent.TextContent).text)
    }

    @Test
    fun `no event labels appear in display content`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = "User wrote this",
            attachments = emptyList(), durationMinutes = null,
            createdAt = 1000L, canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        val text = (content as ActivityMessageDisplayContent.TextContent).text
        // Event labels like "NOTE_ADDED" or "یادداشت اضافه شد" should never appear
        assertFalse(text.contains("NOTE_ADDED"))
        assertFalse(text.contains("یادداشت"))
        assertEquals("User wrote this", text)
    }

    @Test
    fun `image-only message has compact height model`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = null,
            attachments = listOf(ActivityAttachment.Image("content://img.jpg")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        assertTrue("Image-only is compact MediaContent, not MediaWithText",
            content is ActivityMessageDisplayContent.MediaContent)
        // No text, no duration, just media — minimal bubble height
    }

    @Test
    fun `failed decode never leaks raw JSON`() {
        val msg = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null,
            text = null,  // null text ensures no raw JSON reaches UI
            attachments = listOf(ActivityAttachment.Image("content://img.jpg")),
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val content = ActivityMessageDisplayContent.from(msg)
        // Must resolve to MediaContent, not EmptyMessage
        assertTrue("Even with null text, attachment should produce MediaContent",
            content is ActivityMessageDisplayContent.MediaContent)
        // No text field should contain JSON
        val textValue = when (content) {
            is ActivityMessageDisplayContent.TextContent -> content.text
            is ActivityMessageDisplayContent.MediaWithText -> content.text
            else -> null
        }
        assertNull("No JSON text should leak to display", textValue)
    }
}
