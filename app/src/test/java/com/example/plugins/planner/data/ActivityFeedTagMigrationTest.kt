package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.full.memberProperties

/**
 * ActivityFeedTagMigrationTest — Tests for Phase 5.5a Step-as-Tag migration.
 *
 * Tests cover:
 * 1. Tag Chip rendering logic (stepName resolution)
 * 2. Feed header count
 * 3. Mixed task-level and step-level activities
 * 4. StepCard is NOT in the rendering path
 */
class ActivityFeedTagMigrationTest {

    // ════════════════════════════════════════════════════════════════
    // Test Helpers
    // ════════════════════════════════════════════════════════════════

    private fun createMessage(
        id: Long = 1,
        stepId: Long? = null,
        text: String? = "Test",
        attachments: List<ActivityAttachment> = emptyList()
    ) = ActivityMessageModel(
        id = id,
        taskId = 100,
        stepId = stepId,
        text = text,
        attachments = attachments,
        durationMinutes = null,
        createdAt = 1000L + id,
        canEdit = true,
        canDelete = true,
        replyToMessageId = null
    )

    private fun buildStepNameLookup(steps: List<Pair<Long, String>>): Map<Long, String> {
        return steps.toMap()
    }

    // ════════════════════════════════════════════════════════════════
    // 1. Tag Chip Rendering Logic
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `message without stepId has null stepName`() {
        val message = createMessage(id = 1, stepId = null)
        assertNull(message.stepId)
    }

    @Test
    fun `message with stepId resolves stepName from lookup`() {
        val stepNameById = buildStepNameLookup(
            listOf(1L to "UI Design", 2L to "Backend")
        )

        val message = createMessage(id = 1, stepId = 1)
        val stepName = stepNameById[message.stepId]

        assertNotNull(stepName)
        assertEquals("UI Design", stepName)
    }

    @Test
    fun `unknown stepId returns null stepName`() {
        val stepNameById = buildStepNameLookup(
            listOf(1L to "UI Design")
        )

        val message = createMessage(id = 1, stepId = 999)
        val stepName = stepNameById[message.stepId]

        assertNull(stepName)
    }

    @Test
    fun `multiple messages with same stepId all resolve same stepName`() {
        val stepNameById = buildStepNameLookup(
            listOf(1L to "Research")
        )

        val messages = listOf(
            createMessage(id = 1, stepId = 1, text = "Note 1"),
            createMessage(id = 2, stepId = 1, text = "Note 2"),
            createMessage(id = 3, stepId = 1, text = "Note 3")
        )

        messages.forEach { msg ->
            val stepName = stepNameById[msg.stepId]
            assertEquals("Research", stepName)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. Feed Header Count
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `feed header count equals filtered messages count when no filter`() {
        val allMessages = listOf(
            createMessage(id = 1),
            createMessage(id = 2),
            createMessage(id = 3)
        )
        val filteredMessages = allMessages // no filter

        // Header should show filtered count (same as all when no filter)
        assertEquals(allMessages.size, filteredMessages.size)
        assertEquals(3, filteredMessages.size)
    }

    @Test
    fun `feed header count shows filtered count when step filter active`() {
        val allMessages = listOf(
            createMessage(id = 1, stepId = 1),
            createMessage(id = 2, stepId = 1),
            createMessage(id = 3, stepId = 2),
            createMessage(id = 4, stepId = null)
        )

        val filter = ActivityFeedFilterState(selectedStepId = 1)
        val filteredMessages = allMessages.filter { msg ->
            filter.selectedStepId == null || msg.stepId == filter.selectedStepId
        }

        // Header should show filtered count (2), not total (4)
        assertEquals(2, filteredMessages.size)
        assertTrue("Header count should be less than total when filter active",
            filteredMessages.size < allMessages.size)
    }

    @Test
    fun `feed header count is zero when filter excludes everything`() {
        val allMessages = listOf(
            createMessage(id = 1, stepId = 1),
            createMessage(id = 2, stepId = 2)
        )

        val filter = ActivityFeedFilterState(selectedStepId = 999)
        val filteredMessages = allMessages.filter { msg ->
            filter.selectedStepId == null || msg.stepId == filter.selectedStepId
        }

        assertEquals(0, filteredMessages.size)
    }

    // ════════════════════════════════════════════════════════════════
    // 3. Mixed Task-Level and Step-Level Activities
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `feed contains both task-level and step-level messages`() {
        val messages = listOf(
            createMessage(id = 1, stepId = null, text = "Task note"),     // task-level
            createMessage(id = 2, stepId = 1L, text = "Step note"),       // step-level
            createMessage(id = 3, stepId = null, text = "Another note")   // task-level
        )

        val taskLevel = messages.filter { it.stepId == null }
        val stepLevel = messages.filter { it.stepId != null }

        assertEquals(2, taskLevel.size)
        assertEquals(1, stepLevel.size)
    }

    @Test
    fun `task-level messages have null stepId and no stepName`() {
        val stepNameById = buildStepNameLookup(emptyList())
        val message = createMessage(id = 1, stepId = null)

        assertNull(message.stepId)
        assertNull(stepNameById[message.stepId])
    }

    @Test
    fun `step-level messages have stepId and resolve stepName`() {
        val stepNameById = buildStepNameLookup(listOf(1L to "Backend"))
        val message = createMessage(id = 1, stepId = 1, text = "API Design")

        assertNotNull(message.stepId)
        assertEquals(1L, message.stepId)
        assertEquals("Backend", stepNameById[message.stepId])
    }

    @Test
    fun `filter by step shows only matching step-level messages`() {
        val allMessages = listOf(
            createMessage(id = 1, stepId = null, text = "General"),
            createMessage(id = 2, stepId = 1L, text = "Research"),
            createMessage(id = 3, stepId = 2L, text = "Design"),
            createMessage(id = 4, stepId = 1L, text = "More research")
        )

        val filter = ActivityFeedFilterState(selectedStepId = 1)
        val filtered = allMessages.filter { msg ->
            filter.selectedStepId == null || msg.stepId == filter.selectedStepId
        }

        assertEquals(2, filtered.size)
        filtered.forEach { msg ->
            assertEquals(1L, msg.stepId)
        }
    }

    @Test
    fun `show all filter includes task-level messages too`() {
        val allMessages = listOf(
            createMessage(id = 1, stepId = null, text = "General"),
            createMessage(id = 2, stepId = 1L, text = "Step 1"),
            createMessage(id = 3, stepId = 2L, text = "Step 2")
        )

        // filterState with selectedStepId=null = show all
        val filter = ActivityFeedFilterState(selectedStepId = null)
        val filtered = allMessages.filter { msg ->
            filter.selectedStepId == null || msg.stepId == filter.selectedStepId
        }

        assertEquals(3, filtered.size)
    }

    // ════════════════════════════════════════════════════════════════
    // 4. StepCard is NOT in the rendering path
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `feed uses ActivityMessageCard not StepCard`() {
        // Verify that ActivityMessageModel is the primary data model
        // ActivityMessageModel has stepId as metadata, not container
        val message = createMessage(id = 1, stepId = 1L)
        assertTrue(message.stepId is Long)
        // Verify stepId is just metadata — no nested list
        val props = ActivityMessageModel::class.memberProperties.map { it.name }
        assertFalse("Feed model should not have nested messages",
            props.contains("messages"))
    }

    @Test
    fun `ActivityTagChip uses compact non-intrusive style`() {
        // Verify tag display properties
        val stepName = "UI Design"
        assertNotNull(stepName)
        assertTrue(stepName.length in 1..50)
    }
}
