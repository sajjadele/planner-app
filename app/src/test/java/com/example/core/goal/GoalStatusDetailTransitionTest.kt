package com.example.core.goal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure tests for GoalStatus transition rules as consumed by the Goal Detail
 * status dropdown (only valid next states are offered).
 */
class GoalStatusDetailTransitionTest {

    @Test
    fun `active goal can transition to completed`() {
        assertTrue(GoalStatus.canTransition(GoalStatus.ACTIVE, GoalStatus.COMPLETED))
    }

    @Test
    fun `active goal cannot transition to archived directly`() {
        assertFalse(GoalStatus.canTransition(GoalStatus.ACTIVE, GoalStatus.ARCHIVED))
    }

    @Test
    fun `paused goal can resume to active`() {
        assertTrue(GoalStatus.canTransition(GoalStatus.PAUSED, GoalStatus.ACTIVE))
    }

    @Test
    fun `completed goal can only archive`() {
        assertTrue(GoalStatus.canTransition(GoalStatus.COMPLETED, GoalStatus.ARCHIVED))
        assertFalse(GoalStatus.canTransition(GoalStatus.COMPLETED, GoalStatus.ACTIVE))
    }

    @Test
    fun `archived goal is terminal`() {
        assertFalse(GoalStatus.canTransition(GoalStatus.ARCHIVED, GoalStatus.ACTIVE))
        assertFalse(GoalStatus.canTransition(GoalStatus.ARCHIVED, GoalStatus.COMPLETED))
    }
}
