package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ActivityMessageInteractionTest — Tests for message interaction logic.
 *
 * Phase 5.3: Telegram-style Message Experience Polish
 *
 * Tests cover:
 * 1. Interaction state default
 * 2. Long press selects message
 * 3. Cancel clears selection
 * 4. Capability hides unavailable actions
 * 5. Edit action dispatch
 * 6. Delete action dispatch
 * 7. Reply action dispatch
 * 8. ReplyNavigation action dispatch
 * 9. Reply preview resolves correctly
 * 10. Missing original message handled
 * 11. isEdited flag behavior
 */
class ActivityMessageInteractionTest {

    // ════════════════════════════════════════════════════════════════
    // Test Helpers
    // ════════════════════════════════════════════════════════════════

    private fun createMessage(
        id: Long = 1,
        stepId: Long? = null,
        canEdit: Boolean = true,
        canDelete: Boolean = true,
        replyToMessageId: Long? = null,
        isDeleted: Boolean = false,
        isEdited: Boolean = false,
        text: String? = "Test message"
    ) = ActivityMessageModel(
        id = id,
        taskId = 100,
        stepId = stepId,
        text = text,
        attachments = emptyList(),
        durationMinutes = null,
        createdAt = 1000L,
        canEdit = canEdit,
        canDelete = canDelete,
        isDeleted = isDeleted,
        replyToMessageId = replyToMessageId,
        isEdited = isEdited
    )

    // ════════════════════════════════════════════════════════════════
    // Test 1: Interaction state defaults
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `interaction state default has no selected message`() {
        val state = ActivityMessageInteractionState()
        assertNull(state.selectedMessageId)
    }

    @Test
    fun `interaction state with selection works`() {
        val state = ActivityMessageInteractionState(selectedMessageId = 5L)
        assertEquals(5L, state.selectedMessageId)
    }

    @Test
    fun `interaction state clear works`() {
        val state = ActivityMessageInteractionState(selectedMessageId = 3L)
        val cleared = state.copy(selectedMessageId = null)
        assertNull(cleared.selectedMessageId)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: Capability hides unavailable actions
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `full capability shows all actions`() {
        val message = createMessage(canEdit = true, canDelete = true)
        val capability = message.capability()
        assertTrue(capability.canEdit)
        assertTrue(capability.canDelete)
        assertTrue(capability.canReply)
    }

    @Test
    fun `read-only capability hides all actions`() {
        val message = createMessage(canEdit = false, canDelete = false)
        val capability = message.capability()
        assertFalse(capability.canEdit)
        assertFalse(capability.canDelete)
        assertTrue(capability.canReply) // reply is always available
    }

    @Test
    fun `deleted message hides all actions`() {
        val message = createMessage(isDeleted = true)
        val capability = message.capability()
        assertFalse(capability.canEdit)
        assertFalse(capability.canDelete)
        assertFalse(capability.canReply)
    }

    @Test
    fun `canEdit false hides edit action`() {
        val message = createMessage(canEdit = false)
        val capability = message.capability()
        assertFalse(capability.canEdit)
    }

    @Test
    fun `canDelete false hides delete action`() {
        val message = createMessage(canDelete = false)
        val capability = message.capability()
        assertFalse(capability.canDelete)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: Action dispatch — Edit
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `edit action has correct message id`() {
        val action = ActivityMessageAction.Edit(42L)
        assertEquals(42L, action.messageId)
    }

    @Test
    fun `edit action is type Edit`() {
        val action = ActivityMessageAction.Edit(1L)
        assertTrue(action is ActivityMessageAction.Edit)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: Action dispatch — Delete
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `delete action has correct message id`() {
        val action = ActivityMessageAction.Delete(99L)
        assertEquals(99L, action.messageId)
    }

    @Test
    fun `delete action is type Delete`() {
        val action = ActivityMessageAction.Delete(1L)
        assertTrue(action is ActivityMessageAction.Delete)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: Action dispatch — Reply
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `reply action has correct message id`() {
        val action = ActivityMessageAction.Reply(7L)
        assertEquals(7L, action.messageId)
    }

    @Test
    fun `reply action is type Reply`() {
        val action = ActivityMessageAction.Reply(1L)
        assertTrue(action is ActivityMessageAction.Reply)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Action dispatch — ReplyNavigation
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `replyNavigation action has correct message id`() {
        val action = ActivityMessageAction.ReplyNavigation(42L)
        assertEquals(42L, action.messageId)
    }

    @Test
    fun `replyNavigation action is type ReplyNavigation`() {
        val action = ActivityMessageAction.ReplyNavigation(1L)
        assertTrue(action is ActivityMessageAction.ReplyNavigation)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 7: Reply preview resolution
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `reply preview resolves original message`() {
        val messages = listOf(
            createMessage(id = 1, text = "Original message"),
            createMessage(id = 2, text = "Reply message", replyToMessageId = 1)
        )

        val replyMessage = messages.find { it.id == 2L }
        assertNotNull(replyMessage)
        assertEquals(1L, replyMessage!!.replyToMessageId)

        val originalMessage = messages.find { it.id == replyMessage.replyToMessageId }
        assertNotNull(originalMessage)
        assertEquals("Original message", originalMessage!!.text)
    }

    @Test
    fun `missing original message returns null`() {
        val messages = listOf(
            createMessage(id = 2, text = "Orphan reply", replyToMessageId = 999)
        )

        val replyMessage = messages.find { it.id == 2L }
        assertNotNull(replyMessage)
        assertEquals(999L, replyMessage!!.replyToMessageId)

        val originalMessage = messages.find { it.id == replyMessage.replyToMessageId }
        assertNull(originalMessage)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 8: isEdited flag behavior
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `default message is not edited`() {
        val message = createMessage()
        assertFalse(message.isEdited)
    }

    @Test
    fun `edited message has isEdited true`() {
        val message = createMessage(id = 5, text = "Updated", isEdited = true)
        assertTrue(message.isEdited)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 9: isDeleted behavior
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `non-deleted message has canEdit and canDelete`() {
        val message = createMessage(isDeleted = false, canEdit = true, canDelete = true)
        val capability = message.capability()
        assertTrue(capability.canEdit)
        assertTrue(capability.canDelete)
        assertTrue(capability.canReply)
    }

    @Test
    fun `deleted message has no capabilities`() {
        val message = createMessage(isDeleted = true)
        val capability = message.capability()
        assertFalse(capability.canEdit)
        assertFalse(capability.canDelete)
        assertFalse(capability.canReply)
    }
}
