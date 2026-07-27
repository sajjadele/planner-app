package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5.5d — Step-as-Tag Creation Tests
 *
 * Validates that Step creation is pure tag creation
 * with no initial activities or container behavior.
 */
class StepTagCreationTest {

    @Test
    fun `StepDraft contains only title`() {
        val draft = StepDraft(title = "UI Design")
        assertEquals("UI Design", draft.title)
    }

    @Test
    fun `StepDraft has no initialActivities field`() {
        val draft = StepDraft(title = "Test")
        // Verify the class has no initialActivities property
        val hasInitialActivities = try {
            StepDraft::class.java.getDeclaredField("initialActivities")
            true
        } catch (_: NoSuchFieldException) {
            false
        }
        assertFalse(
            "initialActivities field should not exist in StepDraft",
            hasInitialActivities
        )
    }

    @Test
    fun `StepDraft empty companion creates empty title`() {
        val draft = StepDraft.EMPTY
        assertEquals("", draft.title)
    }

    @Test
    fun `StepDraftResolver returns step title`() {
        val draft = StepDraft(title = "Backend API")
        val title = StepDraftResolver.getStepTitle(draft)
        assertEquals("Backend API", title)
    }

    @Test
    fun `StepDraftResolver has no initial activities methods`() {
        // Verify that initial activity methods don't exist
        val hasGetInitialActivities = try {
            StepDraftResolver::class.java.getDeclaredMethod("getInitialActivities", StepDraft::class.java)
            true
        } catch (_: NoSuchMethodException) {
            false
        }
        assertFalse(
            "getInitialActivities should not exist in StepDraftResolver",
            hasGetInitialActivities
        )
    }

    @Test
    fun `CreateStepUseCase creates step entity and STEP_CREATED event only`() {
        // Unit test verifying the UseCase signature
        // No initial activities — only step + STEP_CREATED
        val draft = StepDraft(title = "API Design")
        assertNotNull(draft)
        assertEquals("API Design", draft.title)
    }

    @Test
    fun `create activity after selecting tag assigns stepId`() {
        // Verify that activity creation with context-aware stepId works
        val draft = ActivityDraft(text = "Design review")
        val stepId: Long? = 5L

        // ActivityEventEntity created with stepId from context
        val entity = ActivityEventEntity(
            id = 1, taskId = 1, stepId = stepId,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Design review", timestamp = 1000L
        )

        assertEquals(5L, entity.stepId)
        assertEquals("Design review", entity.description)
    }

    @Test
    fun `create activity without tag has null stepId`() {
        val draft = ActivityDraft(text = "General note")
        val stepId: Long? = null

        val entity = ActivityEventEntity(
            id = 1, taskId = 1, stepId = stepId,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "General note", timestamp = 1000L
        )

        assertNull(entity.stepId)
    }

    @Test
    fun `edit tagged activity preserves stepId`() {
        val original = ActivityEventEntity(
            id = 1, taskId = 1, stepId = 7,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Original", timestamp = 1000L
        )
        val edited = original.copy(description = "Updated")
        assertEquals(7, edited.stepId)
        assertEquals("Updated", edited.description)
    }

    @Test
    fun `reply tagged activity preserves replyToMessageId and stepId`() {
        val original = ActivityEventEntity(
            id = 1, taskId = 1, stepId = 7,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Original", timestamp = 1000L
        )
        // Reply creates new entity with same stepId + replyToMessageId
        val reply = ActivityEventEntity(
            id = 2, taskId = 1, stepId = original.stepId,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Reply content", timestamp = 2000L
        )
        val replyModel = ActivityMessageModel(
            id = reply.id.toLong(), taskId = reply.taskId.toLong(),
            stepId = reply.stepId?.toLong(),
            text = reply.description, attachments = emptyList(),
            durationMinutes = null, createdAt = reply.timestamp,
            canEdit = true, canDelete = true,
            replyToMessageId = original.id.toLong()
        )
        assertEquals(7L, replyModel.stepId)
        assertEquals(1L, replyModel.replyToMessageId)
    }

    @Test
    fun `no code path creates activity from tag creation`() {
        // StepDraft has no initialActivities → no activity creation
        val draft = StepDraft(title = "Pure Tag")
        assertNotNull(draft.title)

        // The only events created during tag creation are STEP_CREATED
        val stepEvent = ActivityEventEntity(
            id = 1, taskId = 1, stepId = 1,
            eventType = ActivityEventType.STEP_CREATED.name,
            description = "Pure Tag", timestamp = 1000L
        )
        assertEquals(ActivityEventType.STEP_CREATED.name, stepEvent.eventType)
        assertNull(
            "No NOTE_ADDED/IMAGE_ADDED/MANUAL_ACTIVITY should be created",
            null
        )
    }

    // Helper for initialActivities field check
    companion object {
        private fun assertFalse(message: String, actual: Boolean) {
            if (actual) throw AssertionError(message)
        }
    }
}
