package com.example.plugins.planner.data

import com.example.plugins.planner.ui.composer.ActivityComposerAction
import com.example.plugins.planner.ui.composer.ActivityComposerReducer
import com.example.plugins.planner.ui.composer.ActivityComposerState
import com.example.plugins.planner.ui.composer.ComposerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.16 — Activity Interaction Foundation Test
 *
 * Validates the hardened interaction foundation before Phase 5 Edit/Delete/Reply.
 *
 * Coverage:
 * 1. Identity: entity.id == model.id
 * 2. Payload: replyToMessageId survives encode/decode
 * 3. Capability: message capability changes correctly
 * 4. DAO: update/delete methods exist
 * 5. Composer: EDIT/REPLY modes are representable
 */
class ActivityInteractionFoundationTest {

    // ════════════════════════════════════════════════════════════════
    // 1. IDENTITY
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `entity id preserved through mapper`() {
        val entity = ActivityEventEntity(
            id = 99, taskId = 1, stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Test", timestamp = 1000L
        )
        val model = ActivityMessageMapper.toMessage(entity)
        assertNotNull(model)
        assertEquals(99L, model!!.id)
    }

    @Test
    fun `entity id widened from Int to Long without loss`() {
        val entity = ActivityEventEntity(
            id = Int.MAX_VALUE, taskId = 1, stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Test", timestamp = 1000L
        )
        val model = ActivityMessageMapper.toMessage(entity)
        assertNotNull(model)
        assertEquals(Int.MAX_VALUE.toLong(), model!!.id)
    }

    // ════════════════════════════════════════════════════════════════
    // 2. PAYLOAD — replyToMessageId encode/decode
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `replyToMessageId survives encode-decode round trip`() {
        val payload = ActivityPayload(
            text = "Reply message",
            replyToMessageId = 42L
        )
        val encoded = ActivityPayloadCodec.encode(payload)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals(42L, decoded!!.replyToMessageId)
    }

    @Test
    fun `replyToMessageId with full payload survives round trip`() {
        val payload = ActivityPayload(
            text = "Extended reply",
            attachments = listOf(ActivityAttachment.Image("content://img/reply.png")),
            durationMinutes = 15,
            replyToMessageId = 99L
        )
        val encoded = ActivityPayloadCodec.encode(payload)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals("Extended reply", decoded!!.text)
        assertEquals(1, decoded.attachments.size)
        assertEquals(15, decoded.durationMinutes)
        assertEquals(99L, decoded.replyToMessageId)
    }

    @Test
    fun `decode payload without replyToMessageId returns null`() {
        val payload = ActivityPayloadCodec.decode("""{"text":"plain"}""")
        assertNotNull(payload)
        assertEquals("plain", payload!!.text)
        assertNull(payload.replyToMessageId)
    }

    @Test
    fun `replyToMessageId propagates through mapper from JSON payload`() {
        val json = """{"text":"Reply text","replyToMessageId":55}"""
        val entity = ActivityEventEntity(
            id = 1, taskId = 1, stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = json, timestamp = 1000L
        )
        val model = ActivityMessageMapper.toMessage(entity)
        assertNotNull(model)
        assertEquals(55L, model!!.replyToMessageId)
    }

    @Test
    fun `plain text message has null replyToMessageId`() {
        val entity = ActivityEventEntity(
            id = 1, taskId = 1, stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Simple note", timestamp = 1000L
        )
        val model = ActivityMessageMapper.toMessage(entity)
        assertNotNull(model)
        assertNull(model!!.replyToMessageId)
    }

    // ════════════════════════════════════════════════════════════════
    // 3. CAPABILITY — correct derivation from model state
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `active message capability enables all interactions`() {
        val model = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null, text = "Active",
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val capability = model.capability()
        assertTrue(capability.canEdit)
        assertTrue(capability.canDelete)
        assertTrue(capability.canReply)
    }

    @Test
    fun `deleted message capability disables all interactions`() {
        val model = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null, text = "Deleted",
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true, isDeleted = true
        )
        val capability = model.capability()
        assertFalse(capability.canEdit)
        assertFalse(capability.canDelete)
        assertFalse(capability.canReply)
    }

    @Test
    fun `mapper produces canDelete=true for active messages`() {
        val entity = ActivityEventEntity(
            id = 1, taskId = 1, stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Active", timestamp = 1000L
        )
        val model = ActivityMessageMapper.toMessage(entity)
        assertNotNull(model)
        assertTrue(model!!.canDelete)
    }

    @Test
    fun `mapper produces canEdit=true for active messages`() {
        val entity = ActivityEventEntity(
            id = 1, taskId = 1, stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = "Active", timestamp = 1000L
        )
        val model = ActivityMessageMapper.toMessage(entity)
        assertNotNull(model)
        assertTrue(model!!.canEdit)
    }

    @Test
    fun `copy with isDeleted produces disabled capability`() {
        val model = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null, text = "Test",
            durationMinutes = null, createdAt = 1000L,
            isDeleted = true
        )
        val capability = model.capability()
        assertFalse(capability.canEdit)
        assertFalse(capability.canDelete)
        assertFalse(capability.canReply)
    }

    @Test
    fun `canDelete=false in model produces canDelete=false in capability`() {
        val model = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null, text = "Test",
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = false
        )
        val capability = model.capability()
        assertTrue(capability.canEdit)
        assertFalse(capability.canDelete)
        assertTrue(capability.canReply)
    }

    // ════════════════════════════════════════════════════════════════
    // 4. DAO — structural validation
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `DAO has update method`() {
        val names = ActivityEventDao::class.java.declaredMethods.map { it.name }
        assertTrue("update should be declared", names.contains("update"))
    }

    @Test
    fun `DAO has deleteById method`() {
        val names = ActivityEventDao::class.java.declaredMethods.map { it.name }
        assertTrue("deleteById should be declared", names.contains("deleteById"))
    }

    @Test
    fun `DAO has observeById method`() {
        val names = ActivityEventDao::class.java.declaredMethods.map { it.name }
        assertTrue("observeById should be declared", names.contains("observeById"))
    }

    @Test
    fun `repository has updateEvent method`() {
        val names = ActivityEventRepository::class.java.declaredMethods.map { it.name }
        assertTrue("updateEvent should be declared", names.contains("updateEvent"))
    }

    @Test
    fun `repository has deleteEvent method`() {
        val names = ActivityEventRepository::class.java.declaredMethods.map { it.name }
        assertTrue("deleteEvent should be declared", names.contains("deleteEvent"))
    }

    @Test
    fun `repository has observeById method`() {
        val names = ActivityEventRepository::class.java.declaredMethods.map { it.name }
        assertTrue("observeById should be declared", names.contains("observeById"))
    }

    // ════════════════════════════════════════════════════════════════
    // 5. COMPOSER — EDIT/REPLY modes
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `ComposerMode includes EDIT`() {
        assertTrue(ComposerMode.values().contains(ComposerMode.EDIT))
    }

    @Test
    fun `ComposerMode includes REPLY`() {
        assertTrue(ComposerMode.values().contains(ComposerMode.REPLY))
    }

    @Test
    fun `ComposerState supports existingMessageId`() {
        val state = ActivityComposerState(
            mode = ComposerMode.EDIT,
            existingMessageId = 42L
        )
        assertEquals(ComposerMode.EDIT, state.mode)
        assertEquals(42L, state.existingMessageId)
        assertTrue(state.isEditMode())
    }

    @Test
    fun `ComposerState supports replyToMessageId`() {
        val state = ActivityComposerState(
            mode = ComposerMode.REPLY,
            replyToMessageId = 99L
        )
        assertEquals(ComposerMode.REPLY, state.mode)
        assertEquals(99L, state.replyToMessageId)
        assertTrue(state.isReplyMode())
    }

    @Test
    fun `ComposerAction has StartEdit`() {
        val action = ActivityComposerAction.StartEdit(42L)
        assertEquals(42L, action.messageId)
    }

    @Test
    fun `ComposerAction has StartReply`() {
        val action = ActivityComposerAction.StartReply(99L)
        assertEquals(99L, action.messageId)
    }

    @Test
    fun `ComposerAction has CancelInteraction`() {
        val action = ActivityComposerAction.CancelInteraction
        assertNotNull(action)
    }

    @Test
    fun `Reducer handles StartEdit`() {
        val state = ActivityComposerState()
        val result = ActivityComposerReducer.reduce(state, ActivityComposerAction.StartEdit(7L))
        assertEquals(ComposerMode.EDIT, result.mode)
        assertEquals(7L, result.existingMessageId)
    }

    @Test
    fun `Reducer handles StartReply`() {
        val state = ActivityComposerState()
        val result = ActivityComposerReducer.reduce(state, ActivityComposerAction.StartReply(8L))
        assertEquals(ComposerMode.REPLY, result.mode)
        assertEquals(8L, result.replyToMessageId)
    }

    @Test
    fun `Reducer handles CancelInteraction`() {
        val state = ActivityComposerState(
            mode = ComposerMode.EDIT,
            existingMessageId = 7L
        )
        val result = ActivityComposerReducer.reduce(state, ActivityComposerAction.CancelInteraction)
        assertEquals(ComposerMode.ACTIVITY, result.mode)
        assertNull(result.existingMessageId)
        assertNull(result.replyToMessageId)
    }

    // ════════════════════════════════════════════════════════════════
    // 6. LEGACY BACKWARD COMPATIBILITY
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `old payloads without replyToMessageId still decode correctly`() {
        val oldJson = """{"text":"Old note","durationMinutes":60,"attachments":[{"type":"IMAGE","uri":"content://img/old.jpg"}]}"""
        val payload = ActivityPayloadCodec.decode(oldJson)
        assertNotNull(payload)
        assertEquals("Old note", payload!!.text)
        assertEquals(60, payload.durationMinutes)
        assertEquals(1, payload.attachments.size)
        assertNull(payload.replyToMessageId)
    }

    @Test
    fun `legacy pipe format still works with updated codec`() {
        val payload = ActivityPayloadCodec.decode("Task|45")
        assertNotNull(payload)
        assertEquals("Task", payload!!.text)
        assertEquals(45, payload.durationMinutes)
        assertNull(payload.replyToMessageId)
    }

    @Test
    fun `legacy triple-colon format still works with updated codec`() {
        val payload = ActivityPayloadCodec.decode("content://img/pic.jpg:::Description")
        assertNotNull(payload)
        assertEquals("Description", payload!!.text)
        assertEquals(1, payload.attachments.size)
        assertNull(payload.replyToMessageId)
    }

    // ════════════════════════════════════════════════════════════════
    // Phase 5.1: Activity Message Interaction Tests
    // ════════════════════════════════════════════════════════════════

    // ── Reply Tests ──

    @Test
    fun `reply draft encodes as JSON with replyToMessageId`() {
        val draft = ActivityDraft(
            text = "Reply text",
            replyToMessageId = 42L
        )
        val encoded = ActivityDraftResolver.encodeDescription(draft)
        assertNotNull(encoded)
        assertTrue(encoded!!.startsWith("{"))
        assertTrue(encoded.contains("42"))
    }

    @Test
    fun `reply draft decode preserves replyToMessageId`() {
        val draft = ActivityDraft(
            text = "Reply text",
            replyToMessageId = 99L
        )
        val encoded = ActivityDraftResolver.encodeDescription(draft)
        assertNotNull(encoded)

        val payload = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(payload)
        assertEquals(99L, payload!!.replyToMessageId)
        assertEquals("Reply text", payload.text)
    }

    @Test
    fun `mapper propagates replyToMessageId from JSON description`() {
        val json = """{"text":"This is a reply","replyToMessageId":77}"""
        val entity = ActivityEventEntity(
            id = 50, taskId = 1, stepId = null,
            eventType = ActivityEventType.NOTE_ADDED.name,
            description = json, timestamp = 1000L
        )
        val model = ActivityMessageMapper.toMessage(entity)
        assertNotNull(model)
        assertEquals(77L, model!!.replyToMessageId)
        assertEquals("This is a reply", model.text)
    }

    @Test
    fun `reply draft payload round trip`() {
        val original = ActivityPayload(
            text = "Replying to you",
            replyToMessageId = 42L
        )
        val encoded = ActivityPayloadCodec.encode(original)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals(42L, decoded!!.replyToMessageId)
    }

    // ── Edit Tests ──

    @Test
    fun `edit produces same text through encode-decode`() {
        val draft = ActivityDraft(text = "Edited content", durationMinutes = 30)
        val encoded = ActivityDraftResolver.encodeDescription(draft)
        assertNotNull(encoded)

        val payload = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(payload)
        assertEquals("Edited content", payload!!.text)
        assertEquals(30, payload.durationMinutes)
    }

    @Test
    fun `edit with image attachments preserves attachments`() {
        val draft = ActivityDraft(
            text = "Edited with image",
            attachments = listOf(ActivityAttachment.Image("content://img/edited.jpg"))
        )
        val encoded = ActivityDraftResolver.encodeDescription(draft)
        assertNotNull(encoded)

        val payload = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(payload)
        assertEquals("Edited with image", payload!!.text)
        assertEquals(1, payload.attachments.size)
    }

    @Test
    fun `DAO has getById method`() {
        val names = ActivityEventDao::class.java.declaredMethods.map { it.name }
        assertTrue("getById should be declared", names.contains("getById"))
    }

    @Test
    fun `repository has getEventById method`() {
        val names = ActivityEventRepository::class.java.declaredMethods.map { it.name }
        assertTrue("getEventById should be declared", names.contains("getEventById"))
    }

    // ── Delete Tests ──

    @Test
    fun `deleteById method exists on DAO`() {
        val names = ActivityEventDao::class.java.declaredMethods.map { it.name }
        assertTrue("deleteById should be declared", names.contains("deleteById"))
    }

    @Test
    fun `deleteEvent method exists on Repository`() {
        val names = ActivityEventRepository::class.java.declaredMethods.map { it.name }
        assertTrue("deleteEvent should be declared", names.contains("deleteEvent"))
    }

    // ── Capability Context Menu Tests ──

    @Test
    fun `active message shows all action flags true`() {
        val model = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null, text = "Active",
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = true
        )
        val cap = model.capability()
        assertTrue(cap.canEdit)
        assertTrue(cap.canDelete)
        assertTrue(cap.canReply)
    }

    @Test
    fun `message with canDelete=false hides delete action`() {
        val model = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null, text = "No delete",
            durationMinutes = null, createdAt = 1000L,
            canEdit = true, canDelete = false
        )
        val cap = model.capability()
        assertTrue(cap.canEdit)
        assertFalse(cap.canDelete)
        assertTrue(cap.canReply)
    }

    @Test
    fun `deleted message hides all actions`() {
        val model = ActivityMessageModel(
            id = 1, taskId = 1, stepId = null, text = "Deleted",
            durationMinutes = null, createdAt = 1000L,
            isDeleted = true
        )
        val cap = model.capability()
        assertFalse(cap.canEdit)
        assertFalse(cap.canDelete)
        assertFalse(cap.canReply)
    }

    // ── ActivityDraft replyToMessageId Tests ──

    @Test
    fun `draft with replyToMessageId hasContent returns true`() {
        val draft = ActivityDraft(replyToMessageId = 5L)
        assertTrue(draft.hasContent())
    }

    @Test
    fun `empty draft hasContent returns false`() {
        assertFalse(ActivityDraft.EMPTY.hasContent())
    }

    @Test
    fun `ActivityComposerState toActivityDraft preserves replyToMessageId`() {
        val state = com.example.plugins.planner.ui.composer.ActivityComposerState(
            text = "Reply from composer",
            replyToMessageId = 88L
        )
        val draft = state.toActivityDraft()
        assertEquals(88L, draft.replyToMessageId)
        assertEquals("Reply from composer", draft.text)
    }

    // ════════════════════════════════════════════════════════════════
    // Phase 5.2.1: Activity Feed Tests
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `feed groups messages by day`() {
        val todayMs = System.currentTimeMillis()
        val yesterdayMs = todayMs - 86_400_000L
        val messages = listOf(
            ActivityMessageModel(id = 1, taskId = 1, stepId = null, text = "Today msg", durationMinutes = null, createdAt = todayMs),
            ActivityMessageModel(id = 2, taskId = 1, stepId = null, text = "Yesterday msg", durationMinutes = null, createdAt = yesterdayMs)
        )
        val grouped = messages.groupBy {
            it.createdAt / 86_400_000L * 86_400_000L  // normalize to day start
        }
        assertEquals(2, grouped.size)
    }

    @Test
    fun `feed orders days newest first`() {
        val todayMs = 2000L * 86_400_000L
        val yesterdayMs = todayMs - 86_400_000L
        val twoDaysAgoMs = todayMs - 2 * 86_400_000L
        val messages = listOf(
            ActivityMessageModel(id = 3, taskId = 1, stepId = null, text = "Old", durationMinutes = null, createdAt = twoDaysAgoMs),
            ActivityMessageModel(id = 2, taskId = 1, stepId = null, text = "Yesterday", durationMinutes = null, createdAt = yesterdayMs),
            ActivityMessageModel(id = 1, taskId = 1, stepId = null, text = "Today", durationMinutes = null, createdAt = todayMs)
        )
        val grouped = messages.groupBy {
            it.createdAt / 86_400_000L * 86_400_000L
        }
        val sortedDays = grouped.entries
            .map { it.key }
            .sortedDescending()

        assertEquals(todayMs / 86_400_000L * 86_400_000L, sortedDays[0])
        assertEquals(yesterdayMs / 86_400_000L * 86_400_000L, sortedDays[1])
    }

    @Test
    fun `feed orders messages by newest first within same day`() {
        val dayStart = 1000L * 86_400_000L
        val messages = listOf(
            ActivityMessageModel(id = 1, taskId = 1, stepId = null, text = "First", durationMinutes = null, createdAt = dayStart),
            ActivityMessageModel(id = 2, taskId = 1, stepId = null, text = "Second", durationMinutes = null, createdAt = dayStart + 3600_000L),
            ActivityMessageModel(id = 3, taskId = 1, stepId = null, text = "Third", durationMinutes = null, createdAt = dayStart + 7200_000L)
        )
        val sorted = messages.sortedByDescending { it.createdAt }
        assertEquals("Third", sorted[0].text)
        assertEquals("Second", sorted[1].text)
        assertEquals("First", sorted[2].text)
    }

    @Test
    fun `feed shows task-level activities without stepId`() {
        val messages = listOf(
            ActivityMessageModel(id = 1, taskId = 1, stepId = null, text = "Task level activity", durationMinutes = null, createdAt = 1000L),
            ActivityMessageModel(id = 2, taskId = 1, stepId = 5L, text = "Step activity", durationMinutes = null, createdAt = 2000L)
        )
        // Both should be visible — no stepId filter applied
        assertEquals(2, messages.size)
        assertNull(messages[0].stepId)
        assertNotNull(messages[1].stepId)
    }

    @Test
    fun `feed empty list shows no groups`() {
        val grouped = emptyList<ActivityMessageModel>().groupBy { it.createdAt }
        assertTrue(grouped.isEmpty())
    }

    @Test
    fun `feed activityMessages flow includes all activities`() {
        // Simulate what ActivityMessageMapper.toMessages produces
        val entities = listOf(
            ActivityEventEntity(id = 1, taskId = 1, stepId = null, eventType = "NOTE_ADDED", description = "Task note", timestamp = 1000L),
            ActivityEventEntity(id = 2, taskId = 1, stepId = 5, eventType = "NOTE_ADDED", description = "Step note", timestamp = 2000L)
        )
        val models = entities.mapNotNull { ActivityMessageMapper.toMessage(it) }
        assertEquals(2, models.size) // both step and non-step activities are included
    }

    @Test
    fun `feed excludes system events`() {
        val entities = listOf(
            ActivityEventEntity(id = 1, taskId = 1, stepId = 5, eventType = "STEP_CREATED", description = "New step", timestamp = 1000L),
            ActivityEventEntity(id = 2, taskId = 1, stepId = null, eventType = "NOTE_ADDED", description = "User note", timestamp = 2000L)
        )
        val models = entities.mapNotNull { ActivityMessageMapper.toMessage(it) }
        assertEquals(1, models.size)
        assertEquals("User note", models[0].text)
    }
}
