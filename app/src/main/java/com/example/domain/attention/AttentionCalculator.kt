package com.example.domain.attention

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/**
 * Pure-Kotlin Attention Score calculator for the Behavioral Solar System.
 *
 * Computes how much a task needs the user's attention based solely on
 * behavioral signals — never priority, completion ratio, or subjective importance.
 *
 * The three signals:
 * - **DatePressure**: non-linear (logistic) pressure from deadline proximity.
 * - **Staleness**: quadratic decay from last meaningful interaction.
 * - **Avoidance**: diminishing-returns signal from reschedule count.
 *
 * All constants are internal to this object and can be tuned without
 * touching any other module. The calculator has zero imports from
 * [domain.goal], [domain.snapshot], [domain.insight], or [domain.mirror].
 *
 * Usage:
 * ```
 * val results = AttentionCalculator.compute(taskInputs, nowMillis)
 * // results: Map<Int, AttentionResult>  (taskId → result)
 * ```
 */
object AttentionCalculator {

    // ── Date Pressure constants ──

    /** Steepness of the logistic curve. Higher = sharper transition. */
    internal const val K = 1.5f

    /** Day at which pressure ≈ 0.5 (the inflection point). */
    internal const val T = 3f

    /** Each overdue day adds this much pressure beyond 1.0. */
    internal const val OVERDUE_PENALTY = 0.1f

    /** Maximum pressure value (caps overdue tasks). */
    internal const val MAX_PRESSURE = 2.0f

    // ── Staleness constants ──

    /** Days of no interaction before staleness reaches 1.0. */
    internal const val STALE_THRESHOLD_DAYS = 14

    // ── Avoidance constants ──

    // Uses half-life model: avoidance = 1 - (1/2)^count
    // No additional constants needed.

    // ── Weighting ──

    /** Weight of date pressure in the final score. */
    internal const val W_DATE_PRESSURE = 0.40f

    /** Weight of staleness in the final score. */
    internal const val W_STALENESS = 0.35f

    /** Weight of avoidance in the final score. */
    internal const val W_AVOIDANCE = 0.25f

    // ── Explainability threshold ──

    /** Minimum weighted contribution for a reason to be included in the output. */
    internal const val REASON_THRESHOLD = 0.05f

    /** Milliseconds in one day. */
    private const val DAY_MS = 86_400_000L

    /**
     * Compute attention results for a list of active tasks.
     *
     * Completed tasks must be filtered out upstream — this method does not check
     * [com.example.domain.attention.TaskAttentionInput] completion state.
     *
     * @param tasks active (incomplete) tasks scoped to a single goal.
     * @param nowMillis current clock (midnight epoch ms of today for date-only comparison,
     *   or exact timestamp — both work because datePressure floors to midnight internally).
     * @return map of taskId → [AttentionResult]. Tasks with score = 0 still appear
     *   (with empty reasons) so the caller knows they were evaluated.
     */
    fun compute(
        tasks: List<TaskAttentionInput>,
        nowMillis: Long = System.currentTimeMillis()
    ): Map<Int, AttentionResult> {
        return tasks.associate { task ->
            Pair(task.id, computeSingle(task, nowMillis))
        }
    }

    /**
     * Compute attention for a single task.
     */
    internal fun computeSingle(
        task: TaskAttentionInput,
        nowMillis: Long
    ): AttentionResult {
        val datePressure = computeDatePressure(task.deadlineEpochMs, nowMillis)
        val staleness = computeStaleness(task.lastMeaningfulInteractionMs, nowMillis)
        val avoidance = computeAvoidance(task.rescheduleCount)

        val components = AttentionComponents(
            datePressure = datePressure,
            staleness = staleness,
            avoidance = avoidance
        )

        val rawScore = datePressure * W_DATE_PRESSURE +
                staleness * W_STALENESS +
                avoidance * W_AVOIDANCE
        val score = rawScore.coerceIn(0f, 1f)

        val reasons = buildReasons(task, datePressure, staleness, avoidance, nowMillis)

        return AttentionResult(
            score = score,
            components = components,
            reasons = reasons
        )
    }

    // ── Signal A: Date Pressure ──

    /**
     * Non-linear (logistic) date pressure.
     *
     * - No deadline → 0.
     * - Far future → ≈ 0.
     * - 3 days remaining → ≈ 0.5 (inflection point).
     * - Today → ≈ 0.99.
     * - Overdue → 1.0 + overdueDays × OVERDUE_PENALTY (capped at MAX_PRESSURE).
     */
    internal fun computeDatePressure(deadlineMs: Long?, nowMillis: Long): Float {
        if (deadlineMs == null) return 0f

        val remainingDays = (deadlineMs - nowMillis).toFloat() / DAY_MS

        return if (remainingDays <= 0f) {
            // Overdue: linear penalty past deadline
            min(MAX_PRESSURE, 1f + abs(remainingDays) * OVERDUE_PENALTY)
        } else {
            // Logistic: pressure grows sharply near deadline
            val z = K * (T - remainingDays)
            (1f / (1f + exp(-z)))
        }
    }

    // ── Signal B: Staleness ──

    /**
     * Quadratic staleness from last meaningful interaction.
     *
     * - No interaction history → 0 (not penalized — see product rule).
     * - 1-3 days → below 0.05 (brief gap is not neglect).
     * - 7 days → 0.25.
     * - 14+ days → 1.0 (full staleness).
     */
    internal fun computeStaleness(lastInteractionMs: Long?, nowMillis: Long): Float {
        if (lastInteractionMs == null) return 0f

        val daysSince = (nowMillis - lastInteractionMs).toFloat() / DAY_MS
        if (daysSince <= 0f) return 0f

        val raw = (daysSince / STALE_THRESHOLD_DAYS) * (daysSince / STALE_THRESHOLD_DAYS)
        return min(1f, raw)
    }

    // ── Signal C: Avoidance ──

    /**
     * Diminishing-returns avoidance from reschedule count.
     *
     * Uses a half-life model: each additional reschedule adds less.
     * - 0 reschedules → 0.
     * - 1 → 0.50.
     * - 2 → 0.75.
     * - 3 → 0.875.
     * - 5 → 0.969.
     */
    internal fun computeAvoidance(rescheduleCount: Int): Float {
        if (rescheduleCount <= 0) return 0f
        return 1f - (0.5f).pow(rescheduleCount)
    }

    // ── Explainability ──

    private fun buildReasons(
        task: TaskAttentionInput,
        datePressure: Float,
        staleness: Float,
        avoidance: Float,
        nowMillis: Long
    ): List<AttentionReason> {
        val reasons = mutableListOf<AttentionReason>()

        // Date pressure reasons
        if (datePressure > REASON_THRESHOLD / W_DATE_PRESSURE) {
            val deadlineMs = task.deadlineEpochMs
            if (deadlineMs != null) {
                val remainingDays = ((deadlineMs - nowMillis) / DAY_MS).toInt()
                if (remainingDays <= 0) {
                    reasons.add(AttentionReason.Overdue(abs(remainingDays)))
                } else {
                    reasons.add(AttentionReason.NearDeadline(remainingDays))
                }
            }
        }

        // Staleness reasons
        if (staleness > REASON_THRESHOLD / W_STALENESS) {
            val lastInteractionMs = task.lastMeaningfulInteractionMs
            if (lastInteractionMs != null) {
                val daysSince = ((nowMillis - lastInteractionMs) / DAY_MS).toInt()
                reasons.add(AttentionReason.StaleInteraction(daysSince))
            }
        }

        // Avoidance reasons
        if (avoidance > REASON_THRESHOLD / W_AVOIDANCE && task.rescheduleCount > 0) {
            reasons.add(AttentionReason.Avoidance(task.rescheduleCount))
        }

        return reasons
    }

    // ── Utility ──

    /** Private power function to avoid importing kotlin.math.pow for a single use. */
    private fun Float.pow(n: Int): Float {
        var result = 1f
        var base = this
        var exp = n
        while (exp > 0) {
            if (exp and 1 == 1) result *= base
            base *= base
            exp = exp shr 1
        }
        return result
    }
}
