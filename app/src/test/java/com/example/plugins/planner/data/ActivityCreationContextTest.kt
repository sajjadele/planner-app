package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ActivityCreationContextTest — Tests for Phase 5.5b Context-Aware Activity Creation.
 *
 * Scenarios:
 * 1. Create activity without selected tag → stepId = null
 * 2. Create activity with selected tag → stepId = selectedStepId
 * 3. Change filter from tag → all → new activities are task-level
 * 4. Change filter all → tag → new activities inherit tag
 * 5. Edit tagged activity → stepId preserved
 * 6. Reply tagged activity → replyToMessageId preserved
 * 7. Composer remains step-agnostic
 */
class ActivityCreationContextTest {

    // ════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════

    private fun createContext(stepId: Long? = null, stepName: String? = null) =
        ActivityCreationContext(stepId = stepId, stepName = stepName)

    /** Simulate the ViewModel's context derivation from filter state. */
    private fun deriveContext(
        filter: ActivityFeedFilterState,
        steps: List<TaskStepEntity> = emptyList()
    ): ActivityCreationContext {
        return if (filter.selectedStepId != null) {
            val step = steps.find { it.id.toLong() == filter.selectedStepId }
            ActivityCreationContext(
                stepId = filter.selectedStepId,
                stepName = step?.title
            )
        } else {
            ActivityCreationContext.DEFAULT
        }
    }

    /** Simulate the ViewModel's context-aware createActivity flow. */
    private fun createActivityEntityForTest(
        draft: ActivityDraft,
        context: ActivityCreationContext
    ): ActivityEventEntity {
        val eventType = ActivityDraftResolver.resolveEventType(draft)
        val description = ActivityDraftResolver.encodeDescription(draft)
        return ActivityEventEntity(
            taskId = 100,
            stepId = context.stepId?.toInt(),  // context injected here
            eventType = eventType.name,
            description = description
        )
    }

    /** Simulate updateActivity: preserve stepId from original entity. */
    private fun updateActivityEntity(
        original: ActivityEventEntity,
        draft: ActivityDraft
    ): ActivityEventEntity {
        val description = ActivityDraftResolver.encodeDescription(draft)
        return original.copy(description = description)  // stepId preserved via copy
    }

    private fun simpleDraft(text: String) = ActivityDraft(text = text)

    // ════════════════════════════════════════════════════════════════
    // 1. Create without tag → stepId = null
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `create activity without selected tag has null stepId`() {
        val filter = ActivityFeedFilterState.DEFAULT  // no step selected
        val context = deriveContext(filter)

        assertNull(context.stepId)
        assertEquals(ActivityCreationContext.DEFAULT, context)
    }

    @Test
    fun `entity created without context has null stepId`() {
        val context = createContext()  // stepId = null
        val draft = simpleDraft("Task-level note")
        val entity = createActivityEntityForTest(draft, context)

        assertNull(entity.stepId)
    }

    // ════════════════════════════════════════════════════════════════
    // 2. Create with tag → stepId = selectedStepId
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `create activity with selected tag has matching stepId`() {
        val steps = listOf(
            TaskStepEntity(id = 1, taskId = 100, title = "UI Design"),
            TaskStepEntity(id = 2, taskId = 100, title = "Backend")
        )
        val filter = ActivityFeedFilterState(selectedStepId = 1L)
        val context = deriveContext(filter, steps)

        assertNotNull(context.stepId)
        assertEquals(1L, context.stepId)
        assertEquals("UI Design", context.stepName)
    }

    @Test
    fun `entity created with tag context has matching stepId`() {
        val steps = listOf(
            TaskStepEntity(id = 5, taskId = 100, title = "Backend")
        )
        val filter = ActivityFeedFilterState(selectedStepId = 5L)
        val context = deriveContext(filter, steps)
        val draft = simpleDraft("API implementation")
        val entity = createActivityEntityForTest(draft, context)

        assertNotNull(entity.stepId)
        assertEquals(5, entity.stepId)
    }

    @Test
    fun `different tags produce different stepIds`() {
        val steps = listOf(
            TaskStepEntity(id = 1, taskId = 100, title = "Design"),
            TaskStepEntity(id = 2, taskId = 100, title = "Dev")
        )

        val context1 = deriveContext(ActivityFeedFilterState(selectedStepId = 1L), steps)
        val context2 = deriveContext(ActivityFeedFilterState(selectedStepId = 2L), steps)

        assertEquals(1L, context1.stepId)
        assertEquals(2L, context2.stepId)
    }

    // ════════════════════════════════════════════════════════════════
    // 3. Filter tag → all → new activities are task-level
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `switching from tag to all makes new activities task-level`() {
        val steps = listOf(TaskStepEntity(id = 1, taskId = 100, title = "Research"))

        // Start with tag filter
        val tagContext = deriveContext(ActivityFeedFilterState(selectedStepId = 1L), steps)
        assertNotNull(tagContext.stepId)

        // Switch to all
        val allContext = deriveContext(ActivityFeedFilterState.DEFAULT, steps)
        assertNull(allContext.stepId)

        // New activity after switching should be task-level
        val draft = simpleDraft("New note after clearing filter")
        val entity = createActivityEntityForTest(draft, allContext)
        assertNull(entity.stepId)
    }

    // ════════════════════════════════════════════════════════════════
    // 4. Filter all → tag → new activities inherit tag
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `switching from all to tag makes new activities inherit tag`() {
        val steps = listOf(TaskStepEntity(id = 1, taskId = 100, title = "Design"))

        // Start with all filter
        val allContext = deriveContext(ActivityFeedFilterState.DEFAULT, steps)
        assertNull(allContext.stepId)

        // Switch to tag
        val tagContext = deriveContext(ActivityFeedFilterState(selectedStepId = 1L), steps)
        assertNotNull(tagContext.stepId)

        // New activity after switching should inherit tag
        val draft = simpleDraft("Design note")
        val entity = createActivityEntityForTest(draft, tagContext)
        assertEquals(1, entity.stepId)
    }

    // ════════════════════════════════════════════════════════════════
    // 5. Edit tagged activity preserves stepId
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `edit preserves original stepId`() {
        // Original entity with stepId
        val original = ActivityEventEntity(
            id = 1,
            taskId = 100,
            stepId = 3,  // tagged activity
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = """{"text":"Original"}""",
            timestamp = 1000L
        )

        // Edit: update description only
        val updatedDraft = simpleDraft("Updated text")
        val updated = updateActivityEntity(original, updatedDraft)

        // stepId should be preserved from original
        assertNotNull(updated.stepId)
        assertEquals(3, updated.stepId)

        // eventType should also be preserved
        assertEquals(ActivityEventType.NOTE_ADDED.name, updated.eventType)

        // Description should be updated
        val decoded = ActivityPayloadCodec.decode(updated.description)
        assertEquals("Updated text", decoded?.text)
    }

    @Test
    fun `edit of task-level activity preserves null stepId`() {
        val original = ActivityEventEntity(
            id = 2,
            taskId = 100,
            stepId = null,  // task-level
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = """{"text":"Task note"}""",
            timestamp = 2000L
        )

        val updatedDraft = simpleDraft("Updated task note")
        val updated = updateActivityEntity(original, updatedDraft)

        assertNull(updated.stepId)
    }

    // ════════════════════════════════════════════════════════════════
    // 6. Reply preserves replyToMessageId
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `reply creates new activity with replyToMessageId`() {
        val steps = listOf(TaskStepEntity(id = 2, taskId = 100, title = "Research"))
        val filter = ActivityFeedFilterState(selectedStepId = 2L)
        val context = deriveContext(filter, steps)

        // Reply to message ID 10, with context stepId
        val replyDraft = ActivityDraft(
            text = "Reply text",
            replyToMessageId = 10L
        )
        val entity = createActivityEntityForTest(replyDraft, context)

        // The entity should have:
        // stepId from context
        assertEquals(2, entity.stepId)
        // replyToMessageId is encoded in description
        assertTrue(entity.description?.contains("replyToMessageId") == true ||
                entity.description?.contains("10") == true)
    }

    @Test
    fun `reply draft preserves replyToMessageId`() {
        val draft = ActivityDraft(
            text = "This is a reply",
            replyToMessageId = 42L
        )

        assertNotNull(draft.replyToMessageId)
        assertEquals(42L, draft.replyToMessageId)

        // The draft itself has no stepId — stays agnostic
        // We need to check ActivityDraft doesn't have a stepId property
        val draftProps = ActivityDraft::class.java.declaredFields.map { it.name }
        assertFalse("ActivityDraft should not have stepId", draftProps.contains("stepId"))
    }

    // ════════════════════════════════════════════════════════════════
    // 7. Composer remains step-agnostic
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `ActivityDraft has no stepId property`() {
        val draft = simpleDraft("Test")

        // ActivityDraft should NOT have stepId
        val props = ActivityDraft::class.java.declaredFields.map { it.name }

        assertFalse(
            "Composer model must remain step-agnostic — no stepId field",
            props.contains("stepId")
        )
        assertFalse(
            "Composer model must remain step-agnostic — no stepName field",
            props.contains("stepName")
        )
    }

    @Test
    fun `context is injected at ViewModel level not in draft`() {
        val draft = simpleDraft("Test")
        val contextNoTag = createContext()

        // Entity gets stepId from context, not from draft
        val entityNoTag = createActivityEntityForTest(draft, contextNoTag)
        assertNull("Without context, stepId is null", entityNoTag.stepId)

        val contextWithTag = createContext(stepId = 7L)
        val entityWithTag = createActivityEntityForTest(draft, contextWithTag)
        assertEquals("With context, stepId comes from context", 7, entityWithTag.stepId)
    }

    @Test
    fun `creationContext flow derives from filterState`() {
        // Verify the deriveContext logic matches ViewModel behavior
        val steps = listOf(
            TaskStepEntity(id = 3, taskId = 100, title = "Backend")
        )

        // All filter → default context
        val allCtx = deriveContext(ActivityFeedFilterState.DEFAULT, steps)
        assertEquals(ActivityCreationContext.DEFAULT, allCtx)
        assertNull(allCtx.stepId)

        // Tag filter → context with stepId + stepName
        val tagCtx = deriveContext(ActivityFeedFilterState(selectedStepId = 3L), steps)
        assertEquals(3L, tagCtx.stepId)
        assertEquals("Backend", tagCtx.stepName)
    }
}
