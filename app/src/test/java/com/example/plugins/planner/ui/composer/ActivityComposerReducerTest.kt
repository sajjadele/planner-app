package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.ActivityIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `initial state has ACTIVITY intent`() {
        val state = ActivityComposerState()
        assertEquals(ActivityIntent.ACTIVITY, state.intent)
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
        val state = ActivityComposerState(
            durationMinutes = 30,
            intent = ActivityIntent.STEP
        )
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.TextChanged("hello")
        )
        assertEquals("hello", newState.text)
        assertEquals(30, newState.durationMinutes)
        assertEquals(ActivityIntent.STEP, newState.intent)
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

        // Add image
        state = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.AddAttachment(
                ActivityAttachment.Image("content://img/1")
            )
        )

        // Add file
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
    // Test 5: Convert to step
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `convert to step changes intent`() {
        val state = ActivityComposerState()
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.ConvertToStep
        )
        assertEquals(ActivityIntent.STEP, newState.intent)
    }

    @Test
    fun `convert to step preserves other fields`() {
        val state = ActivityComposerState(
            text = "My step",
            durationMinutes = 30
        )
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.ConvertToStep
        )
        assertEquals(ActivityIntent.STEP, newState.intent)
        assertEquals("My step", newState.text)
        assertEquals(30, newState.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Reset
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `reset returns default state`() {
        val state = ActivityComposerState(
            text = "Hello",
            attachments = listOf(ActivityAttachment.Image("content://img/1")),
            durationMinutes = 60,
            intent = ActivityIntent.STEP
        )
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.Reset
        )
        assertEquals(ActivityComposerState.EMPTY, newState)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 7: Remove attachment
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `remove attachment`() {
        val image = ActivityAttachment.Image("content://img/1")
        val state = ActivityComposerState(
            attachments = listOf(image)
        )
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.RemoveAttachment(image)
        )
        assertTrue(newState.attachments.isEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // Test 8: Duration change
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
    // Test 9: Convert to activity
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `convert to activity changes intent`() {
        val state = ActivityComposerState(intent = ActivityIntent.STEP)
        val newState = ActivityComposerReducer.reduce(
            state,
            ActivityComposerAction.ConvertToActivity
        )
        assertEquals(ActivityIntent.ACTIVITY, newState.intent)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 10: State toDraft conversion
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `state toDraft conversion`() {
        val state = ActivityComposerState(
            text = "بررسی API",
            attachments = listOf(ActivityAttachment.Image("content://img/1")),
            durationMinutes = 60,
            intent = ActivityIntent.ACTIVITY
        )
        val draft = state.toDraft()

        assertEquals("بررسی API", draft.text)
        assertEquals(1, draft.attachments.size)
        assertEquals(60, draft.durationMinutes)
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
    }

    @Test
    fun `empty state toDraft returns empty draft`() {
        val state = ActivityComposerState()
        val draft = state.toDraft()

        assertEquals(null, draft.text)
        assertTrue(draft.attachments.isEmpty())
        assertEquals(null, draft.durationMinutes)
        assertEquals(ActivityIntent.ACTIVITY, draft.intent)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 11: hasContent and canSubmit
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
}
