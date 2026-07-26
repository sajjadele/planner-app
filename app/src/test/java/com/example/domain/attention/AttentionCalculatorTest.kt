package com.example.domain.attention

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Host-JVM unit tests for AttentionCalculator — pure logic, no Android/Room.
 */
class AttentionCalculatorTest {

    private fun task(
        id: Int = 1,
        title: String = "task-$id",
        dateEpochMs: Long? = null,
        deadlineEpochMs: Long? = null,
        lastMeaningfulInteractionMs: Long? = null,
        rescheduleCount: Int = 0
    ) = TaskAttentionInput(
        id = id,
        title = title,
        dateEpochMs = dateEpochMs,
        deadlineEpochMs = deadlineEpochMs,
        lastMeaningfulInteractionMs = lastMeaningfulInteractionMs,
        rescheduleCount = rescheduleCount
    )

    private fun nowMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun `datePressureFavorsNearDeadlineOverFarDeadline`() {
        val today = nowMillis()
        val tomorrow = today + 86400000L
        val twoMonthsLater = today + 30L * 86400000L * 30L

        val taskA = task(
            id = 1,
            dateEpochMs = tomorrow,
            deadlineEpochMs = tomorrow,
            lastMeaningfulInteractionMs = null,
            rescheduleCount = 0
        )

        val taskB = task(
            id = 2,
            dateEpochMs = twoMonthsLater,
            deadlineEpochMs = twoMonthsLater,
            lastMeaningfulInteractionMs = today,
            rescheduleCount = 0
        )

        val results = AttentionCalculator.compute(listOf(taskA, taskB), today)
        assertTrue("Task 2 should be closer (has interaction)", results[1]!!.score > results[2]!!.score)
    }

    @Test
    fun `staleOutranksRecentForSameDeadline`() {
        val today = nowMillis()
        val threeMonthsLater = today + 90L * 86400000L
        val twoWeeksAgo = today - 14L * 86400000L

        val taskA = task(
            id = 1,
            dateEpochMs = threeMonthsLater,
            deadlineEpochMs = threeMonthsLater,
            lastMeaningfulInteractionMs = twoWeeksAgo,
            rescheduleCount = 0
        )

        val taskB = task(
            id = 2,
            dateEpochMs = threeMonthsLater,
            deadlineEpochMs = threeMonthsLater,
            lastMeaningfulInteractionMs = today,
            rescheduleCount = 0
        )

        val results = AttentionCalculator.compute(listOf(taskA, taskB), today)
        assertTrue("Task 1 (stale) should be closer", results[1]!!.score > results[2]!!.score)
    }

    @Test
    fun `farDeadlineAndNoInteractionGivesZeroScore`() {
        val today = nowMillis()
        val farFuture = today + 365L * 86400000L

        val task = task(
            id = 1,
            dateEpochMs = farFuture,
            deadlineEpochMs = farFuture,
            lastMeaningfulInteractionMs = null,
            rescheduleCount = 0
        )

        val results = AttentionCalculator.compute(listOf(task), today)
        val score = results[1]!!.score
        assertFalse("Task with no interaction should not receive attention", score > 0f)
    }

    @Test
    fun `futureSubtaskDoesNotCauseArtificialPenalty`() {
        val today = nowMillis()
        val twoDaysAgo = today - 2L * 86400000L

        val task = task(
            id = 1,
            dateEpochMs = today,
            deadlineEpochMs = today + 30L * 86400000L,
            lastMeaningfulInteractionMs = twoDaysAgo,
            rescheduleCount = 0
        )

        val results = AttentionCalculator.compute(listOf(task), today)
        val score = results[1]!!.score
        assertTrue("Task with recent interaction should have some attention", score > 0f)
    }

    @Test
    fun `highAvoidanceDrivesAttention`() {
        val today = nowMillis()
        val tomorrow = today + 86400000L

        val taskLowAvoid = task(
            id = 1,
            dateEpochMs = tomorrow,
            deadlineEpochMs = tomorrow,
            lastMeaningfulInteractionMs = today,
            rescheduleCount = 0
        )

        val taskHighAvoid = task(
            id = 2,
            dateEpochMs = tomorrow,
            deadlineEpochMs = tomorrow,
            lastMeaningfulInteractionMs = today,
            rescheduleCount = 5
        )

        val results = AttentionCalculator.compute(listOf(taskLowAvoid, taskHighAvoid), today)
        assertTrue("Task with high avoidance should be closer", results[2]!!.score > results[1]!!.score)
    }

    @Test
    fun `noDeadlineMeansZeroDatePressure`() {
        val results = AttentionCalculator.compute(
            listOf(task(deadlineEpochMs = null, lastMeaningfulInteractionMs = null)),
            nowMillis()
        )
        assertEquals(0f, results[1]!!.components.datePressure)
    }

    @Test
    fun `overdueTaskPressureExceedsOne`() {
        val today = nowMillis()
        val yesterday = today - 86400000L

        val results = AttentionCalculator.compute(
            listOf(task(deadlineEpochMs = yesterday, rescheduleCount = 0)),
            today
        )
        val dp = results[1]!!.components.datePressure
        assertTrue("Overdue task should have pressure > 1.0, got: $dp", dp > 1.0f)
    }

    @Test
    fun `stalenessUsesLastMeaningfulInteraction`() {
        val today = nowMillis()
        val twoDaysAgo = today - 2L * 86400000L

        val results = AttentionCalculator.compute(
            listOf(task(lastMeaningfulInteractionMs = twoDaysAgo)),
            today
        )
        val staleness = results[1]!!.components.staleness
        assertTrue("Task with last interaction 2 days ago should have staleness > 0", staleness > 0f)
        assertTrue("2 days should be below max staleness", staleness < 0.5f)
    }

    @Test
    fun `avoidanceUsesRescheduleCountDiminishingReturns`() {
        val results = AttentionCalculator.compute(
            listOf(task(id = 1, rescheduleCount = 2), task(id = 2, rescheduleCount = 5)),
            nowMillis()
        )
        assertTrue(results[2]!!.components.avoidance > results[1]!!.components.avoidance)
    }

    @Test
    fun `combinationsOfSignalsProduceCorrectScores`() {
        val now = nowMillis()
        val today = now
        val yesterday = now - 86400000L
        val twoDaysLater = now + 2L * 86400000L

        val task = task(
            dateEpochMs = twoDaysLater,
            deadlineEpochMs = twoDaysLater,
            lastMeaningfulInteractionMs = yesterday,
            rescheduleCount = 1
        )

        val results = AttentionCalculator.compute(listOf(task), today)
        val result = results[1]!!
        assertTrue("Should have some attention", result.score > 0f)
        assertTrue("Score should be <= 1.0", result.score <= 1f)
        assertTrue("Should have at least one reason", result.reasons.isNotEmpty())
    }

    @Test
    fun `overdueGeneratesOverdueReason`() {
        val now = nowMillis()
        val yesterday = now - 86400000L

        val results = AttentionCalculator.compute(
            listOf(task(deadlineEpochMs = yesterday)),
            now
        )
        val result = results[1]!!
        val overdueReasons = result.reasons.filterIsInstance<AttentionReason.Overdue>()
        assertFalse("Should have Overdue reason", overdueReasons.isEmpty())
    }

    @Test
    fun `nearDeadlineGeneratesNearDeadlineReason`() {
        val now = nowMillis()
        val tomorrow = now + 86400000L

        val results = AttentionCalculator.compute(
            listOf(task(deadlineEpochMs = tomorrow)),
            now
        )
        val result = results[1]!!
        val nearReasons = result.reasons.filterIsInstance<AttentionReason.NearDeadline>()
        assertFalse("Should have NearDeadline reason", nearReasons.isEmpty())
    }

    @Test
    fun `staleInteractionGeneratesStaleInteractionReason`() {
        val now = nowMillis()
        val sevenDaysAgo = now - 7L * 86400000L

        val results = AttentionCalculator.compute(
            listOf(task(lastMeaningfulInteractionMs = sevenDaysAgo)),
            now
        )
        val result = results[1]!!
        val staleReasons = result.reasons.filterIsInstance<AttentionReason.StaleInteraction>()
        assertFalse("Should have StaleInteraction reason", staleReasons.isEmpty())
    }

    @Test
    fun `rescheduleGeneratesAvoidanceReason`() {
        val results = AttentionCalculator.compute(
            listOf(task(rescheduleCount = 3)),
            nowMillis()
        )
        val result = results[1]!!
        val avoidReasons = result.reasons.filterIsInstance<AttentionReason.Avoidance>()
        assertFalse("Should have Avoidance reason", avoidReasons.isEmpty())
    }
}
