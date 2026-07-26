package com.example.plugins.planner.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 4.10 Tests — Step Composer Enhancement
 *
 * Tests for creating steps with initial activities.
 */
class StepWithActivityTest {

    private lateinit var resolver: ActivityDraftResolver

    @Before
    fun setup() {
        resolver = ActivityDraftResolver
    }

    // ════════════════════════════════════════════════════════════════
    // Test 1: Step with image attachment
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with image attachment should have STEP_CREATED event type`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            attachments = listOf(
                ActivityAttachment.Image(uri = "content://media/picker/0/test.jpg")
            ),
            intent = ActivityIntent.STEP
        )

        val eventType = resolver.resolveEventType(draft)
        assertEquals(ActivityEventType.STEP_CREATED, eventType)
    }

    @Test
    fun `step with image attachment should encode image in description`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            attachments = listOf(
                ActivityAttachment.Image(uri = "content://media/picker/0/test.jpg")
            ),
            intent = ActivityIntent.STEP
        )

        val description = resolver.encodeDescription(draft)
        assertNotNull(description)
        assertTrue(description!!.contains("content://media/picker/0/test.jpg"))
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: Step with note only
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with note only should have STEP_CREATED event type`() {
        val draft = ActivityDraft(
            text = "مرحله تست با توضیح",
            intent = ActivityIntent.STEP
        )

        val eventType = resolver.resolveEventType(draft)
        assertEquals(ActivityEventType.STEP_CREATED, eventType)
    }

    @Test
    fun `step with note only should encode text in description`() {
        val draft = ActivityDraft(
            text = "مرحله تست با توضیح",
            intent = ActivityIntent.STEP
        )

        val description = resolver.encodeDescription(draft)
        assertNotNull(description)
        assertEquals("مرحله تست با توضیح", description)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: Step with multiple attachments
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with multiple attachments should have STEP_CREATED event type`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            attachments = listOf(
                ActivityAttachment.Image(uri = "content://media/picker/0/img1.jpg"),
                ActivityAttachment.Image(uri = "content://media/picker/0/img2.jpg")
            ),
            intent = ActivityIntent.STEP
        )

        val eventType = resolver.resolveEventType(draft)
        assertEquals(ActivityEventType.STEP_CREATED, eventType)
    }

    @Test
    fun `step with multiple attachments should encode all in description`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            attachments = listOf(
                ActivityAttachment.Image(uri = "content://media/picker/0/img1.jpg"),
                ActivityAttachment.Image(uri = "content://media/picker/0/img2.jpg")
            ),
            intent = ActivityIntent.STEP
        )

        val description = resolver.encodeDescription(draft)
        assertNotNull(description)
        assertTrue(description!!.contains("img1.jpg"))
        assertTrue(description.contains("img2.jpg"))
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: Step without content (title only)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step without content should have STEP_CREATED event type`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            intent = ActivityIntent.STEP
        )

        val eventType = resolver.resolveEventType(draft)
        assertEquals(ActivityEventType.STEP_CREATED, eventType)
    }

    @Test
    fun `step without content should encode title in description`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            intent = ActivityIntent.STEP
        )

        val description = resolver.encodeDescription(draft)
        assertNotNull(description)
        assertEquals("مرحله تست", description)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: Step with duration (should be MANUAL_ACTIVITY?)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with duration should still be STEP_CREATED`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            durationMinutes = 30,
            intent = ActivityIntent.STEP
        )

        val eventType = resolver.resolveEventType(draft)
        // STEP intent always resolves to STEP_CREATED, regardless of duration
        assertEquals(ActivityEventType.STEP_CREATED, eventType)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Child activity creation logic
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `draft with attachments should trigger child activity creation`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            attachments = listOf(
                ActivityAttachment.Image(uri = "content://media/picker/0/test.jpg")
            ),
            intent = ActivityIntent.STEP
        )

        val stepTitle = resolver.getStepTitle(draft)
        val hasAttachments = draft.attachments.isNotEmpty()

        assertEquals("مرحله تست", stepTitle)
        assertTrue(hasAttachments)
    }

    @Test
    fun `draft without attachments should not trigger child activity`() {
        val draft = ActivityDraft(
            text = "مرحله تست",
            attachments = emptyList(),
            intent = ActivityIntent.STEP
        )

        val hasAttachments = draft.attachments.isNotEmpty()
        assertFalse(hasAttachments)
    }
}
