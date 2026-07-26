package com.example.domain.mirror

import java.util.Calendar

object MirrorHeuristics {

    fun detectBoulder(
        taskId: Int,
        taskTitle: String,
        rescheduleCount: Int,
        createdAtMs: Long,
        isCompleted: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): MirrorSignal? {
        if (isCompleted) return null
        if (rescheduleCount < 2) return null
        val ageDays = ageDays(createdAtMs, nowMillis)
        if (ageDays < 7) return null
        return MirrorSignal(
            type = MirrorSignalType.BOULDER,
            taskId = taskId,
            confidence = minOf(1f, rescheduleCount / 5f),
            metadata = mapOf(
                "taskTitle" to taskTitle,
                "rescheduleCount" to rescheduleCount.toString(),
                "ageDays" to ageDays.toString()
            )
        )
    }

    fun detectGoalAttention(
        goalId: Int,
        goalTitle: String,
        activeTasks: Int,
        lastEventTimestampMs: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): MirrorSignal? {
        if (activeTasks <= 0) return null
        val daysSinceLastEvent = lastEventTimestampMs?.let { ageDays(it, nowMillis) } ?: Int.MAX_VALUE
        if (daysSinceLastEvent < 14) return null
        return MirrorSignal(
            type = MirrorSignalType.GOAL_ATTENTION,
            goalId = goalId,
            confidence = 0.8f,
            metadata = mapOf(
                "goalTitle" to goalTitle,
                "daysSinceLastEvent" to daysSinceLastEvent.toString(),
                "activeTasks" to activeTasks.toString()
            )
        )
    }

    fun detectInitiatorFinisher(
        createdCount: Int,
        completedCount: Int,
        windowDays: Int = 7
    ): MirrorSignal? {
        if (createdCount < 3) return null
        if (completedCount >= createdCount / 2) return null
        return MirrorSignal(
            type = MirrorSignalType.INITIATOR_FINISHER,
            confidence = 0.7f,
            metadata = mapOf(
                "createdCount" to createdCount.toString(),
                "completedCount" to completedCount.toString(),
                "windowDays" to windowDays.toString()
            )
        )
    }

    fun detectConsistencyDecay(
        currentWindowCompleted: Int,
        previousWindowCompleted: Int
    ): MirrorSignal? {
        if (previousWindowCompleted <= 0) return null
        if (currentWindowCompleted >= previousWindowCompleted) return null
        val drop = previousWindowCompleted - currentWindowCompleted
        return MirrorSignal(
            type = MirrorSignalType.CONSISTENCY_DECAY,
            confidence = minOf(1f, drop / 5f),
            metadata = mapOf(
                "currentWindowCompleted" to currentWindowCompleted.toString(),
                "previousWindowCompleted" to previousWindowCompleted.toString()
            )
        )
    }

    private fun ageDays(fromMillis: Long, nowMillis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = fromMillis
        val fromDay = cal.get(Calendar.DAY_OF_YEAR)
        val fromYear = cal.get(Calendar.YEAR)
        cal.timeInMillis = nowMillis
        val toDay = cal.get(Calendar.DAY_OF_YEAR)
        val toYear = cal.get(Calendar.YEAR)
        val raw = (toYear - fromYear) * 365 + (toDay - fromDay)
        return if (raw < 0) 0 else raw
    }

    private fun minOf(a: Float, b: Float): Float = if (a < b) a else b
}
