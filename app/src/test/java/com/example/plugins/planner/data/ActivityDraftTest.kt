package com.example.plugins.planner.data

import org.junit.Assert.*
import org.junit.Test

/**
 * ActivityDraftTest — Tests for ActivityDraft domain model.
 *
 * Phase 4.10.1: Domain Separation
 * - ActivityDraft no longer has intent or stepId
 */
class ActivityDraftTest {

    @Test
    fun `empty draft has no content`() {
        val draft = ActivityDraft.EMPTY
        assertFalse(draft.hasContent())
        assertFalse(draft.hasImages())
    }

    @Test
    fun `draft with text has content`() {
        val draft = ActivityDraft(text = "Test note")
        assertTrue(draft.hasContent())
        assertFalse(draft.hasImages())
    }

    @Test
    fun `draft with image has images`() {
        val draft = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        assertTrue(draft.hasContent())
        assertTrue(draft.hasImages())
    }

    @Test
    fun `draft with duration has content`() {
        val draft = ActivityDraft(text = "Work", durationMinutes = 60)
        assertTrue(draft.hasContent())
    }

    @Test
    fun `draft equality by content`() {
        val draft1 = ActivityDraft(text = "Test")
        val draft2 = ActivityDraft(text = "Test")
        assertEquals(draft1, draft2)
    }

    @Test
    fun `draft inequality by text`() {
        val draft1 = ActivityDraft(text = "Test 1")
        val draft2 = ActivityDraft(text = "Test 2")
        assertNotEquals(draft1, draft2)
    }
}
