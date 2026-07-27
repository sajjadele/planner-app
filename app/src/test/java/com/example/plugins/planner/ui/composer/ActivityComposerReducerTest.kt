package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ActivityComposerReducerTest — Tests for composer state transitions.
 *
 * Phase 5.9.1: Remove STEP mode — Composer is Activity-only.
 * Removed: ConvertToStep/ConvertToActivity, toStepDraft, isStepMode tests.
 */
class ActivityComposerReducerTest {

    // ════════════════════════════════════════════════════════════════
    // Test 1: Initial state
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `initial state has empty text`() {
        val state = ActivityComposerState()
        assertEquals("", state.text)
    }

    @Test
    fun `initial state has empty attachments`() {
        val state = ActivityComposerState()
        assertTrue(state.attachments.isEmpty())
    }

    @Test
    fun `initial state has no duration`() {
        val state = ActivityComposerState()
        assertEquals(null, state.durationMinutes)
    }

    @Test
    fun `initial state has ACTIVITY mode`() {
        val state = ActivityComposerState()
        assertEquals(ComposerMode.ACTIVITY, state.mode)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: Text change
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `text change updates text`() {
        val state = ActivityComposerState()
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.TextChanged("hello")
        )
        assertEquals("hello", newState.text)
    }

    @Test
    fun `text change preserves other fields`() {
        val state = ActivityComposerState(durationMinutes = 30)
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.TextChanged("hello")
        )
        assertEquals("hello", newState.text)
        assertEquals(30, newState.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: Add image
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `add image attachment`() {
        val state = ActivityComposerState()
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.AddAttachment(
                ActivityAttachment.Image("content://img/1")
            )
        )
        assertEquals(1, newState.attachments.size)
        assertTrue(newState.attachments[0] is ActivityAttachment.Image)
        assertEquals("content://img/1", (newState.attachments[0] as ActivityAttachment.Image).uri)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: Multiple attachments
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `multiple attachments`() {
        var state = ActivityComposerState()

        state = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.AddAttachment(
                ActivityAttachment.Image("content://img/1")
            )
        )

        state = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.AddAttachment(
                ActivityAttachment.File("content://file/1", "doc.pdf")
            )
        )

        assertEquals(2, state.attachments.size)
        assertEquals(1, state.attachments.filterIsInstance<ActivityAttachment.Image>().size)
        assertEquals(1, state.attachments.filterIsInstance<ActivityAttachment.File>().size)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: Reset
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `reset returns default state`() {
        val state = ActivityComposerState(
            text = "Hello",
            attachments = listOf(ActivityAttachment.Image("content://img/1")),
            durationMinutes = 60
        )
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.Reset
        )
        assertEquals(ActivityComposerState.EMPTY, newState)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Remove attachment
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `remove attachment`() {
        val image = ActivityAttachment.Image("content://img/1")
        val state = ActivityComposerState(attachments = listOf(image))
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.RemoveAttachment(image)
        )
        assertTrue(newState.attachments.isEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // Test 7: Duration change
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `duration change updates duration`() {
        val state = ActivityComposerState()
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.DurationChanged(45)
        )
        assertEquals(45, newState.durationMinutes)
    }

    @Test
    fun `duration change to null clears duration`() {
        val state = ActivityComposerState(durationMinutes = 45)
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.DurationChanged(null)
        )
        assertEquals(null, newState.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 8: State toActivityDraft conversion
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `state toActivityDraft conversion`() {
        val state = ActivityComposerState(
            text = "بررسی API",
            attachments = listOf(ActivityAttachment.Image("content://img/1")),
            durationMinutes = 60,
            mode = ComposerMode.ACTIVITY
        )
        val draft = state.toActivityDraft()

        assertEquals("بررسی API", draft.text)
        assertEquals(1, draft.attachments.size)
        assertEquals(60, draft.durationMinutes)
    }

    @Test
    fun `empty state toActivityDraft returns empty draft`() {
        val state = ActivityComposerState()
        val draft = state.toActivityDraft()

        assertEquals(null, draft.text)
        assertTrue(draft.attachments.isEmpty())
        assertEquals(null, draft.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 9: hasContent and canSubmit
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `empty state has no content`() {
        val state = ActivityComposerState()
        assertFalse(state.hasContent())
        assertFalse(state.canSubmit())
    }

    @Test
    fun `state with text has content`() {
        val state = ActivityComposerState(text = "Hello")
        assertTrue(state.hasContent())
        assertTrue(state.canSubmit())
    }

    @Test
    fun `state with attachment has content`() {
        val state = ActivityComposerState(
            attachments = listOf(ActivityAttachment.Image("content://img/1"))
        )
        assertTrue(state.hasContent())
        assertTrue(state.canSubmit())
    }

    @Test
    fun `state with duration has content`() {
        val state = ActivityComposerState(durationMinutes = 30)
        assertTrue(state.hasContent())
        assertTrue(state.canSubmit())
    }

    // ════════════════════════════════════════════════════════════════
    // Test 10: Mode helpers
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `isActivityMode returns true for ACTIVITY`() {
        val state = ActivityComposerState(mode = ComposerMode.ACTIVITY)
        assertTrue(state.isActivityMode())
    }

    // ════════════════════════════════════════════════════════════════
    // EDIT/REPLY mode support
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `StartEdit sets EDIT mode and stores messageId`() {
        val state = ActivityComposerState()
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.StartEdit(42L)
        )
        assertEquals(ComposerMode.EDIT, newState.mode)
        assertEquals(42L, newState.existingMessageId)
        assertNull(newState.replyToMessageId)
    }

    @Test
    fun `StartReply sets REPLY mode and stores messageId`() {
        val state = ActivityComposerState()
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.StartReply(99L)
        )
        assertEquals(ComposerMode.REPLY, newState.mode)
        assertEquals(99L, newState.replyToMessageId)
        assertNull(newState.existingMessageId)
    }

    @Test
    fun `CancelInteraction resets mode to ACTIVITY`() {
        val state = ActivityComposerState(
            mode = ComposerMode.EDIT,
            existingMessageId = 42L,
            replyToMessageId = 99L
        )
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.CancelInteraction
        )
        assertEquals(ComposerMode.ACTIVITY, newState.mode)
        assertNull(newState.existingMessageId)
        assertNull(newState.replyToMessageId)
    }

    @Test
    fun `isEditMode returns true for EDIT`() {
        val state = ActivityComposerState(mode = ComposerMode.EDIT)
        assertTrue(state.isEditMode())
        assertFalse(state.isActivityMode())
        assertFalse(state.isReplyMode())
    }

    @Test
    fun `isReplyMode returns true for REPLY`() {
        val state = ActivityComposerState(mode = ComposerMode.REPLY)
        assertTrue(state.isReplyMode())
        assertFalse(state.isActivityMode())
        assertFalse(state.isEditMode())
    }

    @Test
    fun `StartEdit preserves existing text`() {
        val state = ActivityComposerState(text = "Existing text")
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.StartEdit(42L)
        )
        assertEquals("Existing text", newState.text)
        assertEquals(ComposerMode.EDIT, newState.mode)
    }

    @Test
    fun `StartReply preserves existing text`() {
        val state = ActivityComposerState(text = "Existing text")
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.StartReply(99L)
        )
        assertEquals("Existing text", newState.text)
        assertEquals(ComposerMode.REPLY, newState.mode)
    }
}
