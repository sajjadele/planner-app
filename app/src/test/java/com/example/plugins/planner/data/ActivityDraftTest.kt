package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityDraftTest {

    @Test
    fun `empty activity has all null fields`() {
        val draft = ActivityDraft()
        assertNull(draft.text)
        assertTrue(draft.attachments.isEmpty())
        assertNull(draft.durationMinutes)
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
    }

    @Test
    fun `text activity`() {
        val draft = ActivityDraft(text = "Test note")
        assertEquals("Test note", draft.text)
        assertTrue(draft.attachments.isEmpty())
        assertNull(draft.durationMinutes)
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
    }

    @Test
    fun `image attachment`() {
        val draft = ActivityDraft(
            text = "Screenshot",
            attachments = listOf(ActivityAttachment.Image("content://media/1"))
        )
        assertEquals("Screenshot", draft.text)
        assertEquals(1, draft.attachments.size)
        assertTrue(draft.attachments[0] is ActivityAttachment.Image)
        assertEquals("content://media/1", (draft.attachments[0] as ActivityAttachment.Image).uri)
    }

    @Test
    fun `file attachment`() {
        val draft = ActivityDraft(
            attachments = listOf(ActivityAttachment.File("content://file/1", "document.pdf"))
        )
        assertEquals(1, draft.attachments.size)
        assertTrue(draft.attachments[0] is ActivityAttachment.File)
        val file = draft.attachments[0] as ActivityAttachment.File
        assertEquals("content://file/1", file.uri)
        assertEquals("document.pdf", file.name)
    }

    @Test
    fun `step intent`() {
        val draft = ActivityDraft(
            text = "My step",
            intent = ActivityIntent.STEP
        )
        assertEquals("My step", draft.text)
        assertEquals(ActivityIntent.STEP, draft.intent)
    }

    @Test
    fun `manual activity with duration`() {
        val draft = ActivityDraft(
            text = "Code review",
            durationMinutes = 45,
            intent = ActivityIntent.ACTIVITY
        )
        assertEquals("Code review", draft.text)
        assertEquals(45, draft.durationMinutes)
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
    }

    @Test
    fun `multiple attachments`() {
        val draft = ActivityDraft(
            attachments = listOf(
                ActivityAttachment.Image("content://img/1"),
                ActivityAttachment.File("content://file/1", "doc.pdf"),
                ActivityAttachment.Image("content://img/2")
            )
        )
        assertEquals(3, draft.attachments.size)
        assertEquals(2, draft.attachments.filterIsInstance<ActivityAttachment.Image>().size)
        assertEquals(1, draft.attachments.filterIsInstance<ActivityAttachment.File>().size)
    }

    @Test
    fun `data class equality`() {
        val draft1 = ActivityDraft(text = "Test", intent = ActivityIntent.ACTIVITY)
        val draft2 = ActivityDraft(text = "Test", intent = ActivityIntent.ACTIVITY)
        assertEquals(draft1, draft2)
    }
}
