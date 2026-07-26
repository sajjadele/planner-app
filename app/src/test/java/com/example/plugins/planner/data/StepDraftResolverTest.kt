package com.example.plugins.planner.data

import org.junit.Assert.*
import org.junit.Test

/**
 * StepDraftResolverTest — Tests for StepDraftResolver.
 *
 * Phase 4.10.1: Domain Separation
 */
class StepDraftResolverTest {

    @Test
    fun `getStepTitle returns title from draft`() {
        val draft = StepDraft(title = "طراحی صفحه اصلی")
        val title = StepDraftResolver.getStepTitle(draft)
        assertEquals("طراحی صفحه اصلی", title)
    }

    @Test
    fun `hasInitialActivities returns true when activities exist`() {
        val draft = StepDraft(
            title = "مرحله تست",
            initialActivities = listOf(ActivityDraft(text = "یادداشت"))
        )
        assertTrue(StepDraftResolver.hasInitialActivities(draft))
    }

    @Test
    fun `hasInitialActivities returns false when no activities`() {
        val draft = StepDraft(title = "مرحله تست")
        assertFalse(StepDraftResolver.hasInitialActivities(draft))
    }

    @Test
    fun `getInitialActivities returns empty list when no activities`() {
        val draft = StepDraft(title = "مرحله تست")
        val activities = StepDraftResolver.getInitialActivities(draft)
        assertTrue(activities.isEmpty())
    }

    @Test
    fun `getInitialActivities returns activities when present`() {
        val activity = ActivityDraft(text = "یادداشت")
        val draft = StepDraft(
            title = "مرحله تست",
            initialActivities = listOf(activity)
        )
        val activities = StepDraftResolver.getInitialActivities(draft)
        assertEquals(1, activities.size)
        assertEquals("یادداشت", activities[0].text)
    }

    @Test
    fun `resolveActivityEventType returns NOTE_ADDED for text activity`() {
        val activity = ActivityDraft(text = "یادداشت")
        val eventType = StepDraftResolver.resolveActivityEventType(activity)
        assertEquals(ActivityEventType.NOTE_ADDED, eventType)
    }

    @Test
    fun `resolveActivityEventType returns MANUAL_ACTIVITY for duration activity`() {
        val activity = ActivityDraft(text = "فعالیت", durationMinutes = 30)
        val eventType = StepDraftResolver.resolveActivityEventType(activity)
        assertEquals(ActivityEventType.MANUAL_ACTIVITY, eventType)
    }

    @Test
    fun `resolveActivityEventType returns NOTE_ADDED for image activity`() {
        val activity = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        val eventType = StepDraftResolver.resolveActivityEventType(activity)
        assertEquals(ActivityEventType.NOTE_ADDED, eventType)
    }

    @Test
    fun `encodeActivityDescription returns null for empty activity`() {
        val activity = ActivityDraft()
        val description = StepDraftResolver.encodeActivityDescription(activity)
        assertNull(description)
    }

    @Test
    fun `encodeActivityDescription returns text for note activity`() {
        val activity = ActivityDraft(text = "یادداشت تست")
        val description = StepDraftResolver.encodeActivityDescription(activity)
        assertEquals("یادداشت تست", description)
    }

    @Test
    fun `encodeActivityDescription returns JSON for image activity`() {
        val activity = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        val description = StepDraftResolver.encodeActivityDescription(activity)
        assertNotNull(description)
        assertTrue(description!!.contains("content://test.jpg"))
    }
}
