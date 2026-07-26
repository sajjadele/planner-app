package com.example.plugins.planner.data

import org.junit.Assert.*
import org.junit.Test

/**
 * ActivityPayloadParserTest — Tests for ActivityPayloadParser.
 *
 * Phase 4.10.1: Domain Separation
 * - ActivityPayloadParser no longer creates STEP drafts
 * - Use StepDraft for step creation
 */
class ActivityPayloadParserTest {

    @Test
    fun `note draft has correct text`() {
        val draft = ActivityPayloadParser.note("My note")
        assertEquals("My note", draft.text)
        assertTrue(draft.attachments.isEmpty())
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `manualActivity draft has correct text and duration`() {
        val draft = ActivityPayloadParser.manualActivity("Work", 60)
        assertEquals("Work", draft.text)
        assertEquals(60, draft.durationMinutes)
    }

    @Test
    fun `manualActivity draft without duration`() {
        val draft = ActivityPayloadParser.manualActivity("Work")
        assertEquals("Work", draft.text)
        assertNull(draft.durationMinutes)
    }

    @Test
    fun `image draft has correct attachment`() {
        val draft = ActivityPayloadParser.image("content://test.jpg", "Description")
        assertEquals("Description", draft.text)
        assertEquals(1, draft.attachments.size)
        assertTrue(draft.attachments[0] is ActivityAttachment.Image)
    }

    @Test
    fun `image draft without description`() {
        val draft = ActivityPayloadParser.image("content://test.jpg")
        assertNull(draft.text)
        assertEquals(1, draft.attachments.size)
    }

    @Test
    fun `different activity types produce different drafts`() {
        val note = ActivityPayloadParser.note("Note")
        val manual = ActivityPayloadParser.manualActivity("Work", 60)
        val image = ActivityPayloadParser.image("content://test.jpg")

        assertNotEquals(note, manual)
        assertNotEquals(note, image)
        assertNotEquals(manual, image)
    }
}
