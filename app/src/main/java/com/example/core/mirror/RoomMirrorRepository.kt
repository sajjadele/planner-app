package com.example.core.mirror

import com.example.core.goal.GoalRepository
import com.example.core.snapshot.RoomSnapshotRepository
import com.example.domain.mirror.MirrorEngine
import com.example.domain.mirror.MirrorInsight
import com.example.domain.mirror.MirrorHeuristics
import com.example.domain.mirror.MirrorReadiness
import com.example.domain.mirror.MirrorSignal
import com.example.plugins.planner.data.InsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomMirrorRepository(
    private val insightRepository: InsightRepository,
    private val goalRepository: GoalRepository,
    private val snapshotRepository: RoomSnapshotRepository
) : MirrorRepository {

    override fun observeRescheduleCounts(): Flow<List<RescheduleSummary>> =
        insightRepository.observeRescheduleCounts(
            System.currentTimeMillis() - MIRROR_LOOKBACK_MS
        ).map { list ->
            list.map { RescheduleSummary(it.taskId, it.taskTitle, it.rescheduleCount, 0L, false) }
        }

    override fun observeGoalCompletionRates(): Flow<List<GoalMirrorRate>> =
        insightRepository.observeGoalCompletionRates().map { list ->
            list.map { GoalMirrorRate(it.goalId, it.goalTitle, it.totalTasks, it.completedTasks, it.completionRate) }
        }

    override fun observeGoalEvents(): Flow<List<GoalEventMirrorItem>> =
        goalRepository.observeGoalEvents().map { list ->
            list.map { GoalEventMirrorItem(it.goalId, it.eventType, it.timestamp) }
        }

    override fun observeBehaviorRange(start: Long, end: Long): Flow<List<BehaviorMirrorItem>> =
        snapshotRepository.observeBehaviorRange(start, end).map { list ->
            list.map { BehaviorMirrorItem(it.dateEpochMs, it.completed, it.created) }
        }

    override suspend fun getEarliestTaskDateEpochMs(): Long? =
        insightRepository.getEarliestTaskDateEpochMs()

    override suspend fun evaluate(goalId: Int): List<MirrorSignal> {
        val now = System.currentTimeMillis()
        val week = 7L * 24 * 60 * 60 * 1000
        val earliest = getEarliestTaskDateEpochMs() ?: (now - week)
        val start = earliest
        val end = now

        val rates = insightRepository.observeGoalCompletionRates().first()
        val goalRate = rates.firstOrNull { it.goalId == goalId }
        val goalTasks = insightRepository.observeGoalCompletionRate(goalId).first()
        val reschedules = insightRepository.observeRescheduleCountsByGoal(goalId).first()
        val events = goalRepository.observeGoalEvents(goalId).first()
        val behaviors = snapshotRepository.observeBehaviorRange(start, end).first()

        // ── Eligibility gate: only ACTIVE goals with enough history ──
        val goal = goalRepository.getGoalById(goalId)
        val goalCreatedAtMs = events.firstOrNull { it.eventType == "created" }?.timestamp
        val linkedTaskCount = goalTasks?.totalTasks ?: 0
        if (!MirrorReadiness.isEligible(
                goalCreatedAtMs = goalCreatedAtMs,
                linkedTaskCount = linkedTaskCount,
                goalStatus = goal?.status ?: "",
                nowMillis = now
            )
        ) {
            return emptyList()
        }

        val taskMap = reschedules.associateBy { it.taskId }

        val out = ArrayList<MirrorSignal>()

        taskMap.values.forEach { r ->
            val signal = MirrorHeuristics.detectBoulder(
                taskId = r.taskId,
                taskTitle = r.taskTitle ?: "تسک ${r.taskId}",
                rescheduleCount = r.rescheduleCount,
                createdAtMs = r.taskCreatedAt,
                isCompleted = false,
                nowMillis = now
            )
            if (signal != null) out += signal
        }

        if (goalRate != null && goalTasks != null) {
            val lastEvent = events.maxByOrNull { it.timestamp }?.timestamp
            val attention = MirrorHeuristics.detectGoalAttention(
                goalId = goalId,
                goalTitle = goalRate.goalTitle,
                activeTasks = goalTasks.totalTasks,
                lastEventTimestampMs = lastEvent,
                nowMillis = now
            )
            if (attention != null) out += attention
        }

        val prevStart = earliest
        val prevEnd = now - week
        val currentCompleted = behaviors.sumOf { it.completed }
        val previousCompleted = if (prevStart < prevEnd)
            snapshotRepository.observeBehaviorRange(prevStart, prevEnd).first().sumOf { it.completed }
        else 0
        val decay = MirrorHeuristics.detectConsistencyDecay(currentCompleted, previousCompleted)
        if (decay != null) out += decay

        val createdCount = behaviors.sumOf { it.created }
        val initiator = MirrorHeuristics.detectInitiatorFinisher(createdCount, currentCompleted)
        if (initiator != null) out += initiator

        return out.take(3)
    }

    override suspend fun render(signals: List<MirrorSignal>): List<MirrorInsight> =
        signals.map { MirrorEngine.render(it) }

    companion object {
        /** Mirror needs more history than insight screen for pattern detection — 365 days. */
        private const val MIRROR_LOOKBACK_MS = 365L * 86_400_000L
    }
}
