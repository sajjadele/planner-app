package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.12 — ActivityMessageMapper Tests
 *
 * Tests cover:
 * 1. NOTE_ADDED → text message
 * 2. IMAGE_ADDED → image attachment extracted
 * 3. Multiple attachments
 * 4. MANUAL_ACTIVITY → duration extracted
 * 5. STEP_CREATED ignored (returns null)
 * 6. STEP_COMPLETED ignored (returns null)
 * 7. Old format compatibility
 * 8. Malformed JSON fallback
 */
class ActivityMessageMapperTest {

    // ════════════════════════════════════════════════════════════════
    // Test 1: NOTE_ADDED → text message
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `NOTE_ADDED with plain text maps to text message`() {
        val entity = ActivityEventEntity(
            id = 1,
            taskId = 100,
            stepId = 10,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Initial wireframe completed",
            timestamp = 1000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(1L, model!!.id)
        assertEquals(100L, model.taskId)
        assertEquals(10L, model.stepId)
        assertEquals("Initial wireframe completed", model.text)
        assertTrue(model.attachments.isEmpty())
        assertNull(model.durationMinutes)
        assertEquals(1000L, model.createdAt)
        assertTrue(model.canEdit)
        assertFalse(model.isDeleted)
        assertNull(model.replyToMessageId)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: IMAGE_ADDED → image attachment extracted
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `IMAGE_ADDED with JSON payload extracts image attachment`() {
        val json = """{"text":"Screenshot","attachments":[{"type":"IMAGE","uri":"content://img/1.jpg"}]}"""
        val entity = ActivityEventEntity(
            id = 2,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = json,
            timestamp = 2000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(2L, model!!.id)
        assertEquals("Screenshot", model.text)
        assertEquals(1, model.attachments.size)
        assertTrue(model.attachments[0] is ActivityAttachment.Image)
        assertEquals("content://img/1.jpg", (model.attachments[0] as ActivityAttachment.Image).uri)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: Multiple attachments
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `IMAGE_ADDED with multiple attachments extracts all`() {
        val json = """{"text":"Multi upload","attachments":[{"type":"IMAGE","uri":"content://img/1.jpg"},{"type":"FILE","uri":"content://file/doc.pdf","name":"doc.pdf"}]}"""
        val entity = ActivityEventEntity(
            id = 3,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = json,
            timestamp = 3000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(2, model!!.attachments.size)
        assertTrue(model.attachments[0] is ActivityAttachment.Image)
        assertTrue(model.attachments[1] is ActivityAttachment.File)
        assertEquals("doc.pdf", (model.attachments[1] as ActivityAttachment.File).name)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: MANUAL_ACTIVITY → duration extracted
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `MANUAL_ACTIVITY with JSON payload extracts duration`() {
        val json = """{"text":"Coding session","durationMinutes":90}"""
        val entity = ActivityEventEntity(
            id = 4,
            taskId = 100,
            stepId = null,
            eventType = ActivityEventType.MANUAL_ACTIVITY.name,
            description = json,
            timestamp = 4000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(4L, model!!.id)
        assertEquals("Coding session", model.text)
        assertEquals(90, model.durationMinutes)
    }

    @Test
    fun `MANUAL_ACTIVITY with legacy format extracts duration`() {
        val entity = ActivityEventEntity(
            id = 5,
            taskId = 100,
            stepId = null,
            eventType = ActivityEventType.MANUAL_ACTIVITY.name,
            description = "Code review|45",
            timestamp = 5000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(5L, model!!.id)
        assertEquals("Code review", model.text)
        assertEquals(45, model.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: STEP_CREATED → filtered out (returns null)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `STEP_CREATED is filtered out (returns null)`() {
        val entity = ActivityEventEntity(
            id = 6,
            taskId = 100,
            stepId = 10,
            eventType = ActivityEventType.STEP_CREATED.name,
            description = "Create database",
            timestamp = 6000L
        )

        val model = ActivityMessageMapper.toMessage(entity)
        assertNull(model)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: STEP_COMPLETED → filtered out (returns null)
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `STEP_COMPLETED is filtered out (returns null)`() {
        val entity = ActivityEventEntity(
            id = 7,
            taskId = 100,
            stepId = 10,
            eventType = ActivityEventType.STEP_COMPLETED.name,
            description = "Database created",
            timestamp = 7000L
        )

        val model = ActivityMessageMapper.toMessage(entity)
        assertNull(model)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 7: Old format compatibility
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `legacy image format (uri:::description) still works`() {
        val entity = ActivityEventEntity(
            id = 8,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = "content://img/legacy.jpg:::Legacy image",
            timestamp = 8000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(8L, model!!.id)
        assertEquals("Legacy image", model.text)
        assertEquals(1, model.attachments.size)
        assertTrue(model.attachments[0] is ActivityAttachment.Image)
    }

    @Test
    fun `legacy manual activity format (title|duration) still works`() {
        val entity = ActivityEventEntity(
            id = 9,
            taskId = 100,
            stepId = null,
            eventType = ActivityEventType.MANUAL_ACTIVITY.name,
            description = "Old task|30",
            timestamp = 9000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(9L, model!!.id)
        assertEquals("Old task", model.text)
        assertEquals(30, model.durationMinutes)
    }

    @Test
    fun `plain text note still works`() {
        val entity = ActivityEventEntity(
            id = 10,
            taskId = 100,
            stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Simple note",
            timestamp = 10000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(10L, model!!.id)
        assertEquals("Simple note", model.text)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 8: Malformed JSON fallback
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `malformed JSON falls back gracefully`() {
        val entity = ActivityEventEntity(
            id = 11,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "not valid json {{{",
            timestamp = 11000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(11L, model!!.id)
        assertNotNull(model.text)
        assertTrue(model.attachments.isEmpty())
        assertNull(model.durationMinutes)
    }

    @Test
    fun `STEP_REOPENED is filtered out (returns null)`() {
        val entity = ActivityEventEntity(
            id = 12,
            taskId = 100,
            stepId = 10,
            eventType = ActivityEventType.STEP_REOPENED.name,
            description = "Step reopened",
            timestamp = 12000L
        )

        val model = ActivityMessageMapper.toMessage(entity)
        assertNull(model)
    }

    @Test
    fun `STEP_DELETED is filtered out (returns null)`() {
        val entity = ActivityEventEntity(
            id = 13,
            taskId = 100,
            stepId = 10,
            eventType = ActivityEventType.STEP_DELETED.name,
            description = "Step deleted",
            timestamp = 13000L
        )

        val model = ActivityMessageMapper.toMessage(entity)
        assertNull(model)
    }

    // ════════════════════════════════════════════════════════════════
    // Batch conversion tests
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `toMessages filters system events automatically`() {
        val entities = listOf(
            ActivityEventEntity(1, 100, 10, ActivityEventType.NOTE_ADDED.name, "Note 1", 1000L),
            ActivityEventEntity(2, 100, 10, ActivityEventType.STEP_CREATED.name, "Step 1", 2000L),
            ActivityEventEntity(3, 100, 10, ActivityEventType.MANUAL_ACTIVITY.name, "Task|30", 3000L),
            ActivityEventEntity(4, 100, 10, ActivityEventType.STEP_COMPLETED.name, "Done", 4000L)
        )

        val models = ActivityMessageMapper.toMessages(entities)

        assertEquals(2, models.size)
        assertEquals("Note 1", models[0].text)
        assertEquals("Task", models[1].text)
        assertEquals(30, models[1].durationMinutes)
    }

    @Test
    fun `null description handled gracefully`() {
        val entity = ActivityEventEntity(
            id = 14,
            taskId = 100,
            stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = null,
            timestamp = 14000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(14L, model!!.id)
        assertNull(model.text)
        assertTrue(model.attachments.isEmpty())
    }

    @Test
    fun `empty description returns null text`() {
        val entity = ActivityEventEntity(
            id = 15,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "",
            timestamp = 15000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(15L, model!!.id)
        assertNull(model.text)
    }

    @Test
    fun `IMAGE_ADDED with JSON but no text has null text`() {
        val json = """{"attachments":[{"type":"IMAGE","uri":"content://img/pic.jpg"}]}"""
        val entity = ActivityEventEntity(
            id = 16,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = json,
            timestamp = 16000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(16L, model!!.id)
        assertNull(model.text)
        assertEquals(1, model.attachments.size)
    }

    // ════════════════════════════════════════════════════════════════
    // Phase 4.14: Identity & Interaction Foundation
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `message identity preserved through mapping`() {
        val entity = ActivityEventEntity(
            id = 25,
            taskId = 100,
            stepId = 20,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Test note",
            timestamp = 5000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(25L, model!!.id)
        assertEquals(25L, model.id)
    }

    @Test
    fun `step relation preserved through mapping`() {
        val entity = ActivityEventEntity(
            id = 30,
            taskId = 100,
            stepId = 20,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Step note",
            timestamp = 6000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(20L, model!!.stepId)
    }

    @Test
    fun `task-level message has null stepId`() {
        val entity = ActivityEventEntity(
            id = 31,
            taskId = 100,
            stepId = null,
            eventType = ActivityEventType.MANUAL_ACTIVITY.name,
            description = "Task activity",
            timestamp = 7000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertNull(model!!.stepId)
        assertEquals(100L, model.taskId)
    }

    @Test
    fun `canDelete false by default for new messages`() {
        val entity = ActivityEventEntity(
            id = 32,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "New note",
            timestamp = 8000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertFalse(model!!.canDelete)
    }

    @Test
    fun `capability() returns correct values for active message`() {
        val entity = ActivityEventEntity(
            id = 33,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Active note",
            timestamp = 9000L
        )

        val model = ActivityMessageMapper.toMessage(entity)
        val capability = model!!.capability()

        assertTrue(capability.canEdit)
        assertFalse(capability.canDelete)
        assertTrue(capability.canReply)
    }

    @Test
    fun `capability() disables interactions for deleted messages`() {
        val entity = ActivityEventEntity(
            id = 34,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Deleted note",
            timestamp = 10000L
        )

        val model = ActivityMessageMapper.toMessage(entity)!!.copy(isDeleted = true)
        val capability = model.capability()

        assertFalse(capability.canEdit)
        assertFalse(capability.canDelete)
        assertFalse(capability.canReply)
    }

    @Test
    fun `attachment list preserved through mapping`() {
        val json = """{"text":"Multi","attachments":[{"type":"IMAGE","uri":"content://img/1.jpg"},{"type":"FILE","uri":"content://file/doc.pdf","name":"doc.pdf"}]}"""
        val entity = ActivityEventEntity(
            id = 35,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.IMAGE_ADDED.name,
            description = json,
            timestamp = 11000L
        )

        val model = ActivityMessageMapper.toMessage(entity)

        assertNotNull(model)
        assertEquals(2, model!!.attachments.size)
        assertTrue(model.attachments[0] is ActivityAttachment.Image)
        assertTrue(model.attachments[1] is ActivityAttachment.File)
        assertEquals("doc.pdf", (model.attachments[1] as ActivityAttachment.File).name)
    }

    @Test
    fun `capacity defaults to true for canEdit`() {
        val entity = ActivityEventEntity(
            id = 36,
            taskId = 100,
            stepId = 5,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Default capability check",
            timestamp = 12000L
        )

        val model = ActivityMessageMapper.toMessage(entity)
        assertNotNull(model)
        assertTrue(model!!.canEdit)
    }
}
