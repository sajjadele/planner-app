package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityDraftResolverTest {

    // ════════════════════════════════════════════════════════════════
    // resolveEventType tests
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `STEP intent resolves to STEP_CREATED`() {
        val draft = ActivityDraft(
            text = "My step",
            intent = ActivityIntent.STEP
        )
        assertEquals(ActivityEventType.STEP_CREATED, ActivityDraftResolver.resolveEventType(draft))
    }

    @Test
    fun `duration exists resolves to MANUAL_ACTIVITY`() {
        val draft = ActivityDraft(
            text = "Code review",
            durationMinutes = 30,
            intent = ActivityIntent.ACTIVITY
        )
        assertEquals(ActivityEventType.MANUAL_ACTIVITY, ActivityDraftResolver.resolveEventType(draft))
    }

    @Test
    fun `normal text resolves to NOTE_ADDED`() {
        val draft = ActivityDraft(
            text = "Important observation",
            intent = ActivityIntent.ACTIVITY
        )
        assertEquals(ActivityEventType.NOTE_ADDED, ActivityDraftResolver.resolveEventType(draft))
    }

    @Test
    fun `image attachment does not create IMAGE_ADDED event`() {
        val draft = ActivityDraft(
            text = "Screenshot of login page",
            attachments = listOf(ActivityAttachment.Image("content://media/1")),
            intent = ActivityIntent.ACTIVITY
        )
        val eventType = ActivityDraftResolver.resolveEventType(draft)
        // Should be NOTE_ADDED, not IMAGE_ADDED
        assertEquals(ActivityEventType.NOTE_ADDED, eventType)
    }

    @Test
    fun `STEP intent with duration still resolves to STEP_CREATED`() {
        val draft = ActivityDraft(
            text = "My step",
            durationMinutes = 10,
            intent = ActivityIntent.STEP
        )
        assertEquals(ActivityEventType.STEP_CREATED, ActivityDraftResolver.resolveEventType(draft))
    }

    // ════════════════════════════════════════════════════════════════
    // encodeDescription tests
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `NOTE_ADDED encodes plain text`() {
        val draft = ActivityDraft(text = "Important note")
        assertEquals("Important note", ActivityDraftResolver.encodeDescription(draft))
    }

    @Test
    fun `MANUAL_ACTIVITY encodes title|duration format`() {
        val draft = ActivityDraft(text = "Code review", durationMinutes = 45)
        assertEquals("Code review|45", ActivityDraftResolver.encodeDescription(draft))
    }

    @Test
    fun `STEP_CREATED encodes plain text`() {
        val draft = ActivityDraft(text = "My step", intent = ActivityIntent.STEP)
        assertEquals("My step", ActivityDraftResolver.encodeDescription(draft))
    }

    @Test
    fun `image attachment encodes via ImageEventParser`() {
        val draft = ActivityDraft(
            text = "Screenshot",
            attachments = listOf(ActivityAttachment.Image("content://media/1"))
        )
        val description = ActivityDraftResolver.encodeDescription(draft)
        // Should use ImageEventParser format
        assertTrue(description!!.contains("content://media/1"))
        assertTrue(description.contains("Screenshot"))
    }

    // ════════════════════════════════════════════════════════════════
    // helper method tests
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `getStepTitle returns text for STEP intent`() {
        val draft = ActivityDraft(text = "My step", intent = ActivityIntent.STEP)
        assertEquals("My step", ActivityDraftResolver.getStepTitle(draft))
    }

    @Test
    fun `getStepTitle returns null for ACTIVITY intent`() {
        val draft = ActivityDraft(text = "Note", intent = ActivityIntent.ACTIVITY)
        assertNull(ActivityDraftResolver.getStepTitle(draft))
    }

    @Test
    fun `hasImageAttachment returns true when image present`() {
        val draft = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image("content://media/1"))
        )
        assertTrue(ActivityDraftResolver.hasImageAttachment(draft))
    }

    @Test
    fun `getImageUri returns first image URI`() {
        val draft = ActivityDraft(
            attachments = listOf(
                ActivityAttachment.Image("content://media/1"),
                ActivityAttachment.Image("content://media/2")
            )
        )
        assertEquals("content://media/1", ActivityDraftResolver.getImageUri(draft))
    }

    @Test
    fun `getImageUri returns null when no images`() {
        val draft = ActivityDraft(text = "No images")
        assertNull(ActivityDraftResolver.getImageUri(draft))
    }
}
