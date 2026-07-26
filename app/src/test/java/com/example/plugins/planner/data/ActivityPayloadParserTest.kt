package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityPayloadParserTest {

    @Test
    fun `step draft has correct type and text`() {
        val draft = ActivityPayloadParser.step("My step title")
        assertEquals(ActivityDraftType.STEP, draft.type)
        assertEquals("My step title", draft.text)
        assertNull(draft.imageUri)
        assertNull(draft.imageDescription)
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `note draft has correct type and text`() {
        val draft = ActivityPayloadParser.note("Important observation")
        assertEquals(ActivityDraftType.NOTE, draft.type)
        assertEquals("Important observation", draft.text)
        assertNull(draft.imageUri)
        assertNull(draft.imageDescription)
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `manual activity draft with duration`() {
        val draft = ActivityPayloadParser.manualActivity("Code review", 45)
        assertEquals(ActivityDraftType.MANUAL_ACTIVITY, draft.type)
        assertEquals("Code review", draft.text)
        assertEquals(45, draft.durationMinutes)
        assertNull(draft.imageUri)
        assertNull(draft.imageDescription)
    }

    @Test
    fun `manual activity draft without duration`() {
        val draft = ActivityPayloadParser.manualActivity("Quick fix")
        assertEquals(ActivityDraftType.MANUAL_ACTIVITY, draft.type)
        assertEquals("Quick fix", draft.text)
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `image draft with description`() {
        val draft = ActivityPayloadParser.image(
            uri = "content://media/image/123",
            description = "Screenshot of login page"
        )
        assertEquals(ActivityDraftType.IMAGE, draft.type)
        assertEquals("content://media/image/123", draft.imageUri)
        assertEquals("Screenshot of login page", draft.imageDescription)
        assertNull(draft.text)
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `image draft without description`() {
        val draft = ActivityPayloadParser.image(uri = "content://media/image/456")
        assertEquals(ActivityDraftType.IMAGE, draft.type)
        assertEquals("content://media/image/456", draft.imageUri)
        assertNull(draft.imageDescription)
        assertNull(draft.text)
    }

    @Test
    fun `all drafts are data classes with equals`() {
        val draft1 = ActivityPayloadParser.step("Title")
        val draft2 = ActivityPayloadParser.step("Title")
        assertEquals(draft1, draft2)
    }

    @Test
    fun `different types produce different drafts`() {
        val step = ActivityPayloadParser.step("Title")
        val note = ActivityPayloadParser.note("Title")
        val manual = ActivityPayloadParser.manualActivity("Title")
        val image = ActivityPayloadParser.image("uri")

        assertEquals(ActivityDraftType.STEP, step.type)
        assertEquals(ActivityDraftType.NOTE, note.type)
        assertEquals(ActivityDraftType.MANUAL_ACTIVITY, manual.type)
        assertEquals(ActivityDraftType.IMAGE, image.type)
    }
}
