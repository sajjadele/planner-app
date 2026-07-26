package com.example.domain.mirror

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class MirrorHeuristicsTest {

    private fun at(year: Int, month: Int, day: Int): Long {
        val c = Calendar.getInstance()
        c.set(year, month, day, 12, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun daysAgo(days: Int, nowMillis: Long = System.currentTimeMillis()): Long =
        nowMillis - (days.toLong() * 24 * 60 * 60 * 1000)

    // ── Boulder ──────────────────────────────────────────────────────────────

    @Test
    fun detectBoulder_returnsNull_whenCompleted() {
        val signal = MirrorHeuristics.detectBoulder(
            taskId = 1, taskTitle = "Done Task",
            rescheduleCount = 5, createdAtMs = daysAgo(14),
            isCompleted = true
        )
        assertNull("completed task must not trigger Boulder", signal)
    }

    @Test
    fun detectBoulder_returnsNull_whenRescheduleBelowThreshold() {
        val signal = MirrorHeuristics.detectBoulder(
            taskId = 2, taskTitle = "Almost Fresh",
            rescheduleCount = 1, createdAtMs = daysAgo(14),
            isCompleted = false
        )
        assertNull("fewer than 2 reschedules must not trigger Boulder", signal)
    }

    @Test
    fun detectBoulder_returnsNull_whenTooYoung() {
        val signal = MirrorHeuristics.detectBoulder(
            taskId = 3, taskTitle = "New Task",
            rescheduleCount = 3, createdAtMs = daysAgo(2),
            isCompleted = false
        )
        assertNull("task younger than 7 days must not trigger Boulder", signal)
    }

    @Test
    fun detectBoulder_returnsSignal_whenCriteriaMet() {
        val signal = MirrorHeuristics.detectBoulder(
            taskId = 4, taskTitle = "Stuck Task",
            rescheduleCount = 4, createdAtMs = daysAgo(21),
            isCompleted = false
        )
        assertNotNull("stuck task must trigger Boulder", signal)
        signal?.let {
            assertEquals(MirrorSignalType.BOULDER, it.type)
            assertEquals(4, it.taskId)
            assertEquals("Stuck Task", it.metadata["taskTitle"])
            assertEquals("4", it.metadata["rescheduleCount"])
            assertEquals("21", it.metadata["ageDays"])
        }
    }

    @Test
    fun detectBoulder_usesActualTaskAge_oldTaskTriggers() {
        // Mirrors real wiring: task creation timestamp (TaskEntity.timestamp) is supplied.
        val signal = MirrorHeuristics.detectBoulder(
            taskId = 6, taskTitle = "Old Stuck",
            rescheduleCount = 3, createdAtMs = daysAgo(30),
            isCompleted = false
        )
        assertNotNull("old task with reschedules must trigger", signal)
    }

    @Test
    fun detectBoulder_usesActualTaskAge_youngTaskSuppressed() {
        // If the real (young) creation timestamp is passed, age gate must suppress.
        val signal = MirrorHeuristics.detectBoulder(
            taskId = 7, taskTitle = "Young Stuck",
            rescheduleCount = 3, createdAtMs = daysAgo(2),
            isCompleted = false
        )
        assertNull("young task must not trigger despite reschedules", signal)
    }

    @Test
    fun detectBoulder_confidence_cappedAtOne() {
        val signal = MirrorHeuristics.detectBoulder(
            taskId = 5, taskTitle = "Very Stuck",
            rescheduleCount = 10, createdAtMs = daysAgo(30),
            isCompleted = false
        )
        assertNotNull(signal)
        assertEquals("confidence must not exceed 1.0", 1.0f, signal!!.confidence)
    }

    // ── Goal Attention ───────────────────────────────────────────────────────

    @Test
    fun detectGoalAttention_returnsNull_whenNoActiveTasks() {
        val signal = MirrorHeuristics.detectGoalAttention(
            goalId = 1, goalTitle = "Empty Goal",
            activeTasks = 0,
            lastEventTimestampMs = daysAgo(30)
        )
        assertNull("goal with no active tasks must not trigger", signal)
    }

    @Test
    fun detectGoalAttention_returnsNull_whenRecentEvent() {
        val signal = MirrorHeuristics.detectGoalAttention(
            goalId = 2, goalTitle = "Recent Goal",
            activeTasks = 3,
            lastEventTimestampMs = daysAgo(7)
        )
        assertNull("event within 14 days must not trigger", signal)
    }

    @Test
    fun detectGoalAttention_returnsSignal_whenStale() {
        val signal = MirrorHeuristics.detectGoalAttention(
            goalId = 3, goalTitle = "Neglected Goal",
            activeTasks = 5,
            lastEventTimestampMs = daysAgo(21)
        )
        assertNotNull("stale goal with active tasks must trigger", signal)
        signal?.let {
            assertEquals(MirrorSignalType.GOAL_ATTENTION, it.type)
            assertEquals(3, it.goalId)
            assertEquals("21", it.metadata["daysSinceLastEvent"])
            assertEquals("5", it.metadata["activeTasks"])
        }
    }

    @Test
    fun detectGoalAttention_triggers_whenNoEventsAtAll() {
        // No events ever logged = no lastEventTimestampMs
        val signal = MirrorHeuristics.detectGoalAttention(
            goalId = 4, goalTitle = "Brand New Goal",
            activeTasks = 2,
            lastEventTimestampMs = null
        )
        assertNotNull("goal with no events must trigger if it has active tasks", signal)
        assertEquals(MirrorSignalType.GOAL_ATTENTION, signal!!.type)
    }

    // ── Initiator / Finisher ─────────────────────────────────────────────────

    @Test
    fun detectInitiatorFinisher_returnsNull_whenTooFewCreated() {
        val signal = MirrorHeuristics.detectInitiatorFinisher(
            createdCount = 2, completedCount = 0
        )
        assertNull("fewer than 3 created must not trigger", signal)
    }

    @Test
    fun detectInitiatorFinisher_returnsNull_whenEnoughCompleted() {
        val signal = MirrorHeuristics.detectInitiatorFinisher(
            createdCount = 10, completedCount = 5
        )
        assertNull("completed >= half created must not trigger", signal)
    }

    @Test
    fun detectInitiatorFinisher_returnsSignal_whenImbalance() {
        val signal = MirrorHeuristics.detectInitiatorFinisher(
            createdCount = 10, completedCount = 3
        )
        assertNotNull("imbalance between created and completed must trigger", signal)
        signal?.let {
            assertEquals(MirrorSignalType.INITIATOR_FINISHER, it.type)
            assertEquals(0.7f, it.confidence)
            assertEquals("10", it.metadata["createdCount"])
            assertEquals("3", it.metadata["completedCount"])
        }
    }

    @Test
    fun detectInitiatorFinisher_returnsNull_whenNoCompletion() {
        // completedCount = createdCount = 0 → createdCount < 3
        assertNull(MirrorHeuristics.detectInitiatorFinisher(0, 0))
    }

    // ── Consistency Decay ────────────────────────────────────────────────────

    @Test
    fun detectConsistencyDecay_returnsNull_whenNoPreviousData() {
        assertNull(MirrorHeuristics.detectConsistencyDecay(5, 0))
    }

    @Test
    fun detectConsistencyDecay_returnsNull_whenCurrentNotLower() {
        assertNull(MirrorHeuristics.detectConsistencyDecay(8, 8))
        assertNull(MirrorHeuristics.detectConsistencyDecay(10, 7))
    }

    @Test
    fun detectConsistencyDecay_returnsSignal_whenDropped() {
        val signal = MirrorHeuristics.detectConsistencyDecay(2, 8)
        assertNotNull("drop from 8 to 2 must trigger", signal)
        signal?.let {
            assertEquals(MirrorSignalType.CONSISTENCY_DECAY, it.type)
            assertEquals("2", it.metadata["currentWindowCompleted"])
            assertEquals("8", it.metadata["previousWindowCompleted"])
        }
    }

    @Test
    fun detectConsistencyDecay_confidence_cappedAtOne() {
        val signal = MirrorHeuristics.detectConsistencyDecay(0, 10)
        assertNotNull(signal)
        assertEquals("confidence must not exceed 1.0", 1.0f, signal!!.confidence)
    }

    // ── MirrorEngine ─────────────────────────────────────────────────────────

    @Test
    fun render_boulder_mapsToTitleAndMessage() {
        val signal = MirrorSignal(type = MirrorSignalType.BOULDER, taskId = 7, confidence = 1f)
        val insight = MirrorEngine.render(signal)
        assertEquals("تسک مسدودشده", insight.title)
        assertEquals(7, insight.relatedTaskId)
        assertNull(insight.relatedGoalId)
    }

    @Test
    fun render_goalAttention_mapsToTitleAndMessage() {
        val signal = MirrorSignal(type = MirrorSignalType.GOAL_ATTENTION, goalId = 3, confidence = 0.8f)
        val insight = MirrorEngine.render(signal)
        assertEquals("هدف مورد نیاز توجه", insight.title)
        assertEquals(3, insight.relatedGoalId)
    }

    @Test
    fun render_initiatorFinisher_mapsToTitleAndMessage() {
        val signal = MirrorSignal(type = MirrorSignalType.INITIATOR_FINISHER, goalId = 1, confidence = 0.7f)
        val insight = MirrorEngine.render(signal)
        assertEquals("تعادل شروع و پایان", insight.title)
        assertEquals(1, insight.relatedGoalId)
    }

    @Test
    fun render_consistencyDecay_mapsToTitleAndMessage() {
        val signal = MirrorSignal(type = MirrorSignalType.CONSISTENCY_DECAY, goalId = 2, confidence = 1f)
        val insight = MirrorEngine.render(signal)
        assertEquals("کاهش انتظام فعالیت", insight.title)
        assertEquals(2, insight.relatedGoalId)
    }
}
