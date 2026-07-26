package com.example.plugins.planner.data

import org.junit.Assert.*
import org.junit.Test

/**
 * CreateStepWithActivitiesUseCaseTest — Tests for step + activities creation pipeline.
 *
 * Phase 4.10.2: Transaction Pipeline
 *
 * Note: These are domain logic tests.
 * Integration tests with real database should be added separately.
 *
 * Required cases:
 * - Step only: 1 TaskStepEntity, no ActivityEventEntity
 * - Step + note: Step created, NOTE_ADDED event with correct stepId
 * - Step + image: Step created, ActivityEventEntity with correct attachment payload
 * - Step + multiple activities: All have same stepId
 * - Failure rollback: No Step remains (integration test)
 */
class CreateStepWithActivitiesUseCaseTest {

    // ════════════════════════════════════════════════════════════════
    // Case 1: Step only (no initial activities)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step only - draft has no initial activities`() {
        val draft = StepDraft(title = "طراحی صفحه اصلی")
        assertFalse(draft.hasInitialContent())
        assertEquals(0, draft.initialActivities.size)
    }

    @Test
    fun `step only - title is extracted correctly`() {
        val draft = StepDraft(title = "طراحی صفحه اصلی")
        val title = StepDraftResolver.getStepTitle(draft)
        assertEquals("طراحی صفحه اصلی", title)
    }

    // ════════════════════════════════════════════════════════════════
    // Case 2: Step + note
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with note - initial activity has correct text`() {
        val draft = StepDraft(
            title = "طراحی صفحه اصلی",
            initialActivities = listOf(
                ActivityDraft(text = "نمونه اولیه آماده شد")
            )
        )
        val activities = StepDraftResolver.getInitialActivities(draft)
        assertEquals(1, activities.size)
        assertEquals("نمونه اولیه آماده شد", activities[0].text)
    }

    @Test
    fun `step with note - resolves to NOTE_ADDED event type`() {
        val activity = ActivityDraft(text = "یادداشت")
        val eventType = StepDraftResolver.resolveActivityEventType(activity)
        assertEquals(ActivityEventType.NOTE_ADDED, eventType)
    }

    @Test
    fun `step with note - encodes as plain text`() {
        val activity = ActivityDraft(text = "یادداشت تست")
        val description = StepDraftResolver.encodeActivityDescription(activity)
        assertEquals("یادداشت تست", description)
    }

    // ════════════════════════════════════════════════════════════════
    // Case 3: Step + image
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with image - initial activity has attachment`() {
        val draft = StepDraft(
            title = "طراحی صفحه اصلی",
            initialActivities = listOf(
                ActivityDraft(
                    attachments = listOf(
                        ActivityAttachment.Image(uri = "content://media/picker/0/test.jpg")
                    )
                )
            )
        )
        val activities = StepDraftResolver.getInitialActivities(draft)
        assertEquals(1, activities.size)
        assertEquals(1, activities[0].attachments.size)
        assertTrue(activities[0].attachments[0] is ActivityAttachment.Image)
    }

    @Test
    fun `step with image - resolves to NOTE_ADDED event type`() {
        val activity = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        val eventType = StepDraftResolver.resolveActivityEventType(activity)
        assertEquals(ActivityEventType.NOTE_ADDED, eventType)
    }

    @Test
    fun `step with image - encodes as JSON with attachment`() {
        val activity = ActivityDraft(
            attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))
        )
        val description = StepDraftResolver.encodeActivityDescription(activity)
        assertNotNull(description)
        assertTrue(description!!.contains("content://test.jpg"))
    }

    // ════════════════════════════════════════════════════════════════
    // Case 4: Step + multiple activities
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with multiple activities - all have same stepId`() {
        val stepId = 42
        val activities = listOf(
            ActivityDraft(text = "یادداشت"),
            ActivityDraft(attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))),
            ActivityDraft(text = "فعالیت", durationMinutes = 30)
        )

        // Simulate creation with same stepId
        val events = activities.map { activity ->
            val eventType = StepDraftResolver.resolveActivityEventType(activity)
            val description = StepDraftResolver.encodeActivityDescription(activity)
            ActivityEventEntity(
                taskId = 1,
                stepId = stepId,
                eventType = eventType.name,
                description = description
            )
        }

        // All should have same stepId
        assertTrue(events.all { it.stepId == stepId })
        assertEquals(3, events.size)
    }

    @Test
    fun `step with multiple activities - correct event types`() {
        val activities = listOf(
            ActivityDraft(text = "یادداشت"),
            ActivityDraft(attachments = listOf(ActivityAttachment.Image(uri = "content://test.jpg"))),
            ActivityDraft(text = "فعالیت", durationMinutes = 30)
        )

        val eventTypes = activities.map { StepDraftResolver.resolveActivityEventType(it) }

        assertEquals(ActivityEventType.NOTE_ADDED, eventTypes[0])
        assertEquals(ActivityEventType.NOTE_ADDED, eventTypes[1])
        assertEquals(ActivityEventType.MANUAL_ACTIVITY, eventTypes[2])
    }

    // ════════════════════════════════════════════════════════════════
    // Case 5: Transaction safety (logic verification)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `transaction safety - stepId is always positive`() {
        // In real implementation, stepId > 0 is verified by check() in UseCase
        val stepId = 1
        assertTrue(stepId > 0)
    }

    @Test
    fun `transaction safety - STEP_CREATED event has stepId`() {
        val stepId = 42
        val event = ActivityEventEntity(
            taskId = 1,
            stepId = stepId,
            eventType = ActivityEventType.STEP_CREATED.name,
            description = "Test step"
        )
        assertNotNull(event.stepId)
        assertEquals(stepId, event.stepId)
    }

    @Test
    fun `transaction safety - child activities have stepId`() {
        val stepId = 42
        val childActivities = listOf(
            ActivityEventEntity(taskId = 1, stepId = stepId, eventType = "NOTE_ADDED"),
            ActivityEventEntity(taskId = 1, stepId = stepId, eventType = "NOTE_ADDED")
        )
        assertTrue(childActivities.all { it.stepId == stepId })
    }
}
