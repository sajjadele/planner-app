package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPayloadParserTest {

    @Test
    fun `step draft has correct intent and text`() {
        val draft = ActivityPayloadParser.step("My step title")
        assertEquals(ActivityIntent.STEP, draft.intent)
        assertEquals("My step title", draft.text)
        assertTrue(draft.attachments.isEmpty())
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `note draft has correct intent and text`() {
        val draft = ActivityPayloadParser.note("Important observation")
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
        assertEquals("Important observation", draft.text)
        assertTrue(draft.attachments.isEmpty())
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `manual activity draft with duration`() {
        val draft = ActivityPayloadParser.manualActivity("Code review", 45)
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
        assertEquals("Code review", draft.text)
        assertEquals(45, draft.durationMinutes)
        assertTrue(draft.attachments.isEmpty())
    }

    @Test
    fun `manual activity draft without duration`() {
        val draft = ActivityPayloadParser.manualActivity("Quick fix")
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
        assertEquals("Quick fix", draft.text)
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `image draft with description`() {
        val draft = ActivityPayloadParser.image(
            uri = "content://media/image/123",
            description = "Screenshot of login page"
        )
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
        assertEquals("Screenshot of login page", draft.text)
        assertEquals(1, draft.attachments.size)
        assertTrue(draft.attachments[0] is ActivityAttachment.Image)
        assertEquals("content://media/image/123", (draft.attachments[0] as ActivityAttachment.Image).uri)
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `image draft without description`() {
        val draft = ActivityPayloadParser.image(uri = "content://media/image/456")
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
        assertNull(draft.text)
        assertEquals(1, draft.attachments.size)
        assertEquals("content://media/image/456", (draft.attachments[0] as ActivityAttachment.Image).uri)
    }

    @Test
    fun `all drafts are data classes with equals`() {
        val draft1 = ActivityPayloadParser.step("Title")
        val draft2 = ActivityPayloadParser.step("Title")
        assertEquals(draft1, draft2)
    }

    @Test
    fun `different intents produce different drafts`() {
        val step = ActivityPayloadParser.step("Title")
        val note = ActivityPayloadParser.note("Title")
        val manual = ActivityPayloadParser.manualActivity("Title")
        val image = ActivityPayloadParser.image("uri")

        assertEquals(ActivityIntent.STEP, step.intent)
        assertEquals(ActivityIntent.ACTIVITY, note.intent)
        assertEquals(ActivityIntent.ACTIVITY, manual.intent)
        assertEquals(ActivityIntent.ACTIVITY, image.intent)
    }
}
