package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.9.6 — StepCardMapper Tests
 */
class StepCardMapperTest {

    // ════════════════════════════════════════════════════════════════
    // Test 1: Empty step
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `empty step has no messages`() {
        val step = TaskStepEntity(
            id = 1,
            taskId = 100,
            title = "Empty step",
            isCompleted = false,
            order = 0
        )

        val model = StepCardMapper.toCardModel(step)

        assertEquals(1L, model.id)
        assertEquals("Empty step", model.title)
        assertFalse(model.isCompleted)
        assertTrue(model.messages.isEmpty())
        assertEquals(0, model.attachmentCount)
        assertNull(model.totalDurationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: Step with text activity
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with text activity has correct message count`() {
        val step = TaskStepEntity(
            id = 2,
            taskId = 100,
            title = "Design homepage",
            isCompleted = false,
            order = 1
        )

        val activities = listOf(
            ActivityMessageModel(
                id = 1,
                text = "Initial design",
                timestamp = 1000L,
                eventTypeRaw = "NOTE_ADDED"
            )
        )

        val model = StepCardMapper.toCardModel(step, activities)

        assertEquals(2L, model.id)
        assertEquals(1, model.messages.size)
        assertEquals("Initial design", model.messages[0].text)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: Step with image attachment
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with image attachment has attachmentCount=1`() {
        val step = TaskStepEntity(
            id = 3,
            taskId = 100,
            title = "Add mockup",
            isCompleted = false,
            order = 2
        )

        val activities = listOf(
            ActivityMessageModel(
                id = 1,
                text = "Mockup added",
                attachments = listOf(ActivityAttachment.Image("content://img/mockup.png")),
                timestamp = 1000L,
                eventTypeRaw = "IMAGE_ADDED"
            )
        )

        val model = StepCardMapper.toCardModel(step, activities)

        assertEquals(3L, model.id)
        assertEquals(1, model.attachmentCount)
        assertEquals(1, model.getAllImages().size)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: Step with multiple attachments
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with multiple attachments has correct count`() {
        val step = TaskStepEntity(
            id = 4,
            taskId = 100,
            title = "Multiple files",
            isCompleted = false,
            order = 3
        )

        val activities = listOf(
            ActivityMessageModel(
                id = 1,
                attachments = listOf(
                    ActivityAttachment.Image("content://img/1.jpg"),
                    ActivityAttachment.Image("content://img/2.jpg"),
                    ActivityAttachment.File("content://file/doc.pdf", "doc.pdf")
                ),
                timestamp = 1000L,
                eventTypeRaw = "IMAGE_ADDED"
            )
        )

        val model = StepCardMapper.toCardModel(step, activities)

        assertEquals(4L, model.id)
        assertEquals(3, model.attachmentCount)
        assertEquals(2, model.getAllImages().size)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: Step with multiple manual activities
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `step with multiple manual activities sums duration`() {
        val step = TaskStepEntity(
            id = 5,
            taskId = 100,
            title = "Coding session",
            isCompleted = false,
            order = 4
        )

        val activities = listOf(
            ActivityMessageModel(
                id = 1,
                text = "Morning coding",
                durationMinutes = 60,
                timestamp = 1000L,
                eventTypeRaw = "MANUAL_ACTIVITY"
            ),
            ActivityMessageModel(
                id = 2,
                text = "Afternoon coding",
                durationMinutes = 45,
                timestamp = 2000L,
                eventTypeRaw = "MANUAL_ACTIVITY"
            )
        )

        val model = StepCardMapper.toCardModel(step, activities)

        assertEquals(5L, model.id)
        assertEquals(105, model.totalDurationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Malformed payload
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `malformed payload falls back gracefully`() {
        val step = TaskStepEntity(
            id = 6,
            taskId = 100,
            title = "Test step",
            isCompleted = false,
            order = 5
        )

        val activities = listOf(
            ActivityMessageModel(
                id = 1,
                text = null,
                timestamp = 1000L,
                eventTypeRaw = "UNKNOWN"
            )
        )

        val model = StepCardMapper.toCardModel(step, activities)

        assertEquals(6L, model.id)
        assertEquals(1, model.messages.size)
        assertNull(model.messages[0].text)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 7: Messages sorted by timestamp
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `messages sorted by timestamp ascending`() {
        val step = TaskStepEntity(
            id = 7,
            taskId = 100,
            title = "Sorted step",
            isCompleted = false,
            order = 6
        )

        val activities = listOf(
            ActivityMessageModel(id = 1, text = "Second", timestamp = 2000L),
            ActivityMessageModel(id = 2, text = "First", timestamp = 1000L),
            ActivityMessageModel(id = 3, text = "Third", timestamp = 3000L)
        )

        val model = StepCardMapper.toCardModel(step, activities)

        assertEquals("First", model.messages[0].text)
        assertEquals("Second", model.messages[1].text)
        assertEquals("Third", model.messages[2].text)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 8: toCardModels batch conversion
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `toCardModels converts list correctly`() {
        val steps = listOf(
            TaskStepEntity(1, 100, "Step 1", false, 0),
            TaskStepEntity(2, 100, "Step 2", true, 1)
        )

        val activitiesMap = mapOf(
            1 to listOf(
                ActivityMessageModel(id = 1, text = "Note", timestamp = 1000L)
            )
        )

        val models = StepCardMapper.toCardModels(steps, activitiesMap)

        assertEquals(2, models.size)
        assertEquals("Step 1", models[0].title)
        assertEquals(1, models[0].messages.size)
        assertEquals("Step 2", models[1].title)
        assertTrue(models[1].messages.isEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // Test 9: StepCardModel helper methods
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `getActivitySummary returns correct format`() {
        val model = StepCardModel(
            id = 1,
            title = "Test",
            messages = listOf(
                ActivityMessageModel(id = 1, timestamp = 1000L),
                ActivityMessageModel(id = 2, timestamp = 2000L)
            ),
            attachmentCount = 3,
            totalDurationMinutes = 90,
            createdAt = System.currentTimeMillis()
        )

        val summary = model.getActivitySummary()
        assertTrue(summary.contains("2 فعالیت"))
        assertTrue(summary.contains("3 فایل پیوست"))
        assertTrue(summary.contains("90 دقیقه"))
    }

    @Test
    fun `hasRichContent returns true for messages`() {
        val model = StepCardModel(
            id = 1,
            title = "Test",
            messages = listOf(ActivityMessageModel(id = 1, timestamp = 1000L)),
            createdAt = System.currentTimeMillis()
        )
        assertTrue(model.hasRichContent())
    }

    @Test
    fun `hasRichContent returns true for attachments`() {
        val model = StepCardModel(
            id = 1,
            title = "Test",
            attachmentCount = 1,
            createdAt = System.currentTimeMillis()
        )
        assertTrue(model.hasRichContent())
    }

    @Test
    fun `hasRichContent returns true for duration`() {
        val model = StepCardModel(
            id = 1,
            title = "Test",
            totalDurationMinutes = 30,
            createdAt = System.currentTimeMillis()
        )
        assertTrue(model.hasRichContent())
    }

    @Test
    fun `hasRichContent returns false for empty step`() {
        val model = StepCardModel(
            id = 1,
            title = "Test",
            createdAt = System.currentTimeMillis()
        )
        assertFalse(model.hasRichContent())
    }
}
