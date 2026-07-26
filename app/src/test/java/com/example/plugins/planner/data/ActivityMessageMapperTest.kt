package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.8.5 — ActivityMessageMapper Tests
 */
class ActivityMessageMapperTest {

    // ════════════════════════════════════════════════════════════════
    // Test 1: NOTE_ADDED event
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `NOTE_ADDED event maps to correct model`() {
        val entity = ActivityEventEntity(
            id = 1,
            taskId = 100,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Important observation",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(1L, model.id)
        assertEquals("Important observation", model.text)
        assertTrue(model.attachments.isEmpty())
        assertNull(model.durationMinutes)
        assertFalse(model.isStep)
        assertFalse(model.isCompleted)
        assertEquals(ActivityEventType.NOTE_ADDED.name, model.eventTypeRaw)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: IMAGE_ADDED event
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `IMAGE_ADDED event with JSON payload extracts image`() {
        val json = """{"text":"Screenshot","attachments":[{"type":"IMAGE","uri":"content://img/1.jpg"}]}"""
        val entity = ActivityEventEntity(
            id = 2,
            taskId = 100,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = json,
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(2L, model.id)
        assertEquals("Screenshot", model.text)
        assertEquals(1, model.attachments.size)
        assertTrue(model.attachments[0] is ActivityAttachment.Image)
        assertEquals("content://img/1.jpg", (model.attachments[0] as ActivityAttachment.Image).uri)
    }

    @Test
    fun `IMAGE_ADDED event with legacy format extracts image`() {
        val entity = ActivityEventEntity(
            id = 3,
            taskId = 100,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = "content://img/legacy.jpg:::Legacy image",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(3L, model.id)
        assertEquals("Legacy image", model.text)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: MANUAL_ACTIVITY event
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `MANUAL_ACTIVITY event with JSON payload extracts duration`() {
        val json = """{"text":"Coding","durationMinutes":90}"""
        val entity = ActivityEventEntity(
            id = 4,
            taskId = 100,
            eventType = ActivityEventType.MANUAL_ACTIVITY.name,
            description = json,
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(4L, model.id)
        assertEquals("Coding", model.text)
        assertEquals(90, model.durationMinutes)
    }

    @Test
    fun `MANUAL_ACTIVITY event with legacy format extracts duration`() {
        val entity = ActivityEventEntity(
            id = 5,
            taskId = 100,
            eventType = ActivityEventType.MANUAL_ACTIVITY.name,
            description = "Code review|45",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(5L, model.id)
        assertEquals("Code review", model.text)
        assertEquals(45, model.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: STEP_CREATED event
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `STEP_CREATED event has isStep=true`() {
        val entity = ActivityEventEntity(
            id = 6,
            taskId = 100,
            eventType = ActivityEventType.STEP_CREATED.name,
            description = "Create database",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(6L, model.id)
        assertEquals("Create database", model.text)
        assertTrue(model.isStep)
        assertFalse(model.isCompleted)
    }

    @Test
    fun `STEP_COMPLETED event has isStep=true and isCompleted=true`() {
        val entity = ActivityEventEntity(
            id = 7,
            taskId = 100,
            eventType = ActivityEventType.STEP_COMPLETED.name,
            description = "Database created",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(7L, model.id)
        assertTrue(model.isStep)
        assertTrue(model.isCompleted)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: Malformed JSON
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `malformed JSON falls back gracefully`() {
        val entity = ActivityEventEntity(
            id = 8,
            taskId = 100,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "not valid json {{{",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(8L, model.id)
        assertNotNull(model.text)
        assertTrue(model.attachments.isEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Old format compatibility
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `old image format still works`() {
        val entity = ActivityEventEntity(
            id = 9,
            taskId = 100,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = "content://old/image.jpg:::Old format",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(9L, model.id)
        assertEquals("Old format", model.text)
    }

    @Test
    fun `old manual activity format still works`() {
        val entity = ActivityEventEntity(
            id = 10,
            taskId = 100,
            eventType = ActivityEventType.MANUAL_ACTIVITY.name,
            description = "Old task|30",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(10L, model.id)
        assertEquals("Old task", model.text)
        assertEquals(30, model.durationMinutes)
    }

    @Test
    fun `plain text note still works`() {
        val entity = ActivityEventEntity(
            id = 11,
            taskId = 100,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Simple note",
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(11L, model.id)
        assertEquals("Simple note", model.text)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 7: toMessages batch conversion
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `toMessages converts list correctly`() {
        val entities = listOf(
            ActivityEventEntity(1, 100, "NOTE_ADDED", "Note 1", 1000L),
            ActivityEventEntity(2, 100, "STEP_CREATED", "Step 1", 2000L),
            ActivityEventEntity(3, 100, "MANUAL_ACTIVITY", "Task|30", 3000L)
        )

        val models = ActivityMessageMapper.toMessages(entities)

        assertEquals(3, models.size)
        assertEquals("Note 1", models[0].text)
        assertFalse(models[0].isStep)
        assertEquals("Step 1", models[1].text)
        assertTrue(models[1].isStep)
        assertEquals("Task", models[2].text)
        assertEquals(30, models[2].durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 8: Null description
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `null description handled gracefully`() {
        val entity = ActivityEventEntity(
            id = 12,
            taskId = 100,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = null,
            timestamp = System.currentTimeMillis()
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertEquals(12L, model.id)
        assertNull(model.text)
        assertTrue(model.attachments.isEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // Test 9: ActivityMessageModel helper methods
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `hasContent returns true for text`() {
        val model = ActivityMessageModel(id = 1, text = "Hello", timestamp = 1000L)
        assertTrue(model.hasContent())
    }

    @Test
    fun `hasContent returns true for attachments`() {
        val model = ActivityMessageModel(
            id = 1,
            attachments = listOf(ActivityAttachment.Image("uri")),
            timestamp = 1000L
        )
        assertTrue(model.hasContent())
    }

    @Test
    fun `hasContent returns true for duration`() {
        val model = ActivityMessageModel(id = 1, durationMinutes = 30, timestamp = 1000L)
        assertTrue(model.hasContent())
    }

    @Test
    fun `hasContent returns false for empty model`() {
        val model = ActivityMessageModel(id = 1, timestamp = 1000L)
        assertFalse(model.hasContent())
    }

    @Test
    fun `getSummary returns correct format`() {
        val model = ActivityMessageModel(
            id = 1,
            text = "Coding",
            attachments = listOf(ActivityAttachment.Image("uri")),
            durationMinutes = 90,
            timestamp = 1000L
        )
        val summary = model.getSummary()
        assertTrue(summary.contains("Coding"))
        assertTrue(summary.contains("1 فایل پیوست"))
        assertTrue(summary.contains("90 دقیقه"))
    }
}
