package com.example.plugins.planner.data

import org.junit.Assert.*
import org.junit.Test

/**
 * ActivityDraftResolverTest — Tests for ActivityDraftResolver.
 *
 * Phase 4.10.1: Domain Separation
 * - ActivityDraft no longer has intent
 * - STEP_CREATED is handled by StepDraftResolver
 */
class ActivityDraftResolverTest {

    @Test
    fun `duration activity resolves to MANUAL_ACTIVITY`() {
        val draft = ActivityDraft(text = "Work session", durationMinutes = 60)
        assertEquals(ActivityEventType.MANUAL_ACTIVITY, ActivityDraftResolver.resolveEventType(draft))
    }

    @Test
    fun `note activity resolves to NOTE_ADDED`() {
        val draft = ActivityDraft(text = "Quick note")
        assertEquals(ActivityEventType.NOTE_ADDED, ActivityDraftResolver.resolveEventType(draft))
    }

    @Test
    fun `image activity resolves to NOTE_ADDED`() {
        val draft = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        assertEquals(ActivityEventType.NOTE_ADDED, ActivityDraftResolver.resolveEventType(draft))
    }

    @Test
    fun `duration with text encodes as legacy format`() {
        val draft = ActivityDraft(text = "Design work", durationMinutes = 90)
        val description = ActivityDraftResolver.encodeDescription(draft)
        assertEquals("Design work|90", description)
    }

    @Test
    fun `note with text encodes as plain text`() {
        val draft = ActivityDraft(text = "Quick note")
        val description = ActivityDraftResolver.encodeDescription(draft)
        assertEquals("Quick note", description)
    }

    @Test
    fun `image with attachments encodes as JSON`() {
        val draft = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        val description = ActivityDraftResolver.encodeDescription(draft)
        assertNotNull(description)
        assertTrue(description!!.contains("content://test.jpg"))
    }

    @Test
    fun `empty draft encodes to null`() {
        val draft = ActivityDraft()
        val description = ActivityDraftResolver.encodeDescription(draft)
        assertNull(description)
    }

    @Test
    fun `hasImageAttachment returns true for image`() {
        val draft = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        assertTrue(ActivityDraftResolver.hasImageAttachment(draft))
    }

    @Test
    fun `hasImageAttachment returns false for no images`() {
        val draft = ActivityDraft(text = "Note")
        assertFalse(ActivityDraftResolver.hasImageAttachment(draft))
    }

    @Test
    fun `getImageUri returns uri for image`() {
        val draft = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        assertEquals("content://test.jpg", ActivityDraftResolver.getImageUri(draft))
    }

    @Test
    fun `getImageUri returns null for no images`() {
        val draft = ActivityDraft(text = "Note")
        assertNull(ActivityDraftResolver.getImageUri(draft))
    }
}
