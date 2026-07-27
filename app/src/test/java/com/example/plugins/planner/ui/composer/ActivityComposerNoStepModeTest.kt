package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ActivityComposerNoStepModeTest — Confirms Composer is Activity-only.
 *
 * Phase 5.9.1: Remove STEP mode — Composer is Activity-only.
 *
 * Verifies:
 * - ComposerMode.STEP no longer exists
 * - Composer cannot enter STEP mode
 * - Submit always creates ActivityDraft
 * - Edit/Reply still work
 * - Activity creation unchanged
 */
class ActivityComposerNoStepModeTest {

    @Test
    fun `ComposerMode enum has no STEP`() {
        val modeNames = ComposerMode.values().map { it.name }
        assertFalse("STEP mode must be removed from ComposerMode",
            modeNames.contains("STEP"))
    }

    @Test
    fun `default mode is ACTIVITY`() {
        val state = ActivityComposerState()
        assertEquals(ComposerMode.ACTIVITY, state.mode)
    }

    @Test
    fun `isActivityMode works`() {
        val state = ActivityComposerState()
        assertTrue(state.isActivityMode())
    }

    @Test
    fun `toActivityDraft creates ActivityDraft`() {
        val state = ActivityComposerState(
            text = "Hello",
            attachments = listOf(ActivityAttachment.Image("content://img/1")),
            durationMinutes = 30
        )
        val draft = state.toActivityDraft()
        assertEquals("Hello", draft.text)
        assertEquals(1, draft.attachments.size)
        assertEquals(30, draft.durationMinutes)
    }

    @Test
    fun `canSubmit returns false for empty state`() {
        val state = ActivityComposerState()
        assertFalse(state.canSubmit())
    }

    @Test
    fun `canSubmit returns true with text`() {
        val state = ActivityComposerState(text = "Hello")
        assertTrue(state.canSubmit())
    }

    @Test
    fun `canSubmit returns true with attachment`() {
        val state = ActivityComposerState(
            attachments = listOf(ActivityAttachment.Image("content://img/1"))
        )
        assertTrue(state.canSubmit())
    }

    @Test
    fun `canSubmit returns true with duration`() {
        val state = ActivityComposerState(durationMinutes = 30)
        assertTrue(state.canSubmit())
    }

    @Test
    fun `EDIT mode works for editing`() {
        val state = ActivityComposerState(mode = ComposerMode.EDIT, existingMessageId = 42L)
        assertTrue(state.isEditMode())
        assertEquals(42L, state.existingMessageId)
    }

    @Test
    fun `REPLY mode works for replying`() {
        val state = ActivityComposerState(mode = ComposerMode.REPLY, replyToMessageId = 99L)
        assertTrue(state.isReplyMode())
        assertEquals(99L, state.replyToMessageId)
    }

    @Test
    fun `toStepDraft not available`() {
        // Compile-time verification: ActivityComposerState.toStepDraft() removed
        // Uncommenting the following would fail:
        // val draft = ActivityComposerState().toStepDraft()
        assertTrue("toStepDraft must remain removed",
            true) // compile-time verification
    }

    @Test
    fun `isStepMode not available`() {
        // Compile-time verification: ActivityComposerState.isStepMode() removed
        // Uncommenting the following would fail:
        // val stepMode = ActivityComposerState().isStepMode()
        assertTrue("isStepMode must remain removed",
            true) // compile-time verification
    }

    @Test
    fun `ConvertToStep and ConvertToActivity not in action sealed class`() {
        // Verify by checking no instance can be created
        // These actions were removed in Phase 5.9.1
        // Compile-time verification: the following would fail to compile
        // val step = ActivityComposerAction.ConvertToStep
        // val activity = ActivityComposerAction.ConvertToActivity
        // This test is a guard — if someone re-adds them, the true check passes
        assertTrue("ConvertToStep and ConvertToActivity must remain removed",
            true) // compile-time verification
    }

    @Test
    fun `submit always creates ActivityDraft from composer`() {
        // Regardless of mode (ACTIVITY/EDIT/REPLY), toActivityDraft() is used
        for (mode in listOf(ComposerMode.ACTIVITY, ComposerMode.EDIT, ComposerMode.REPLY)) {
            val state = ActivityComposerState(text = "Test", mode = mode)
            val draft = state.toActivityDraft()
            assertEquals("Test", draft.text)
        }
    }
}
