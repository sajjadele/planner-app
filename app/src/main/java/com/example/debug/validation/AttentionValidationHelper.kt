package com.example.debug.validation

import com.example.domain.attention.AttentionResult
import com.example.domain.attention.TaskAttentionInput
import kotlin.math.pow
import kotlin.math.sqrt

data class AttentionSummary(
    val averageScore: Float,
    val maxScore: Float,
    val minScore: Float,
    val variance: Float,
    val highAttentionCount: Int,
    val totalActiveTasks: Int,
    val scores: Map<Int, Float>
)

object AttentionValidationHelper {

    private const val HIGH_ATTENTION_THRESHOLD = 0.5f

    fun summarize(
        attentionResults: Map<Int, AttentionResult>,
        activeTaskIds: List<Int>
    ): AttentionSummary {
        val activeScores = activeTaskIds.mapNotNull { id ->
            attentionResults[id]?.score
        }

        if (activeScores.isEmpty()) {
            return AttentionSummary(
                averageScore = 0f,
                maxScore = 0f,
                minScore = 0f,
                variance = 0f,
                highAttentionCount = 0,
                totalActiveTasks = activeTaskIds.size,
                scores = emptyMap()
            )
        }

        val avg = activeScores.average().toFloat()
        val max = activeScores.max()
        val min = activeScores.min()
        val variance = computeVariance(activeScores, avg)
        val highCount = activeScores.count { it >= HIGH_ATTENTION_THRESHOLD }

        val scoreMap = activeTaskIds.associate { id ->
            id to (attentionResults[id]?.score ?: 0f)
        }

        return AttentionSummary(
            averageScore = avg,
            maxScore = max,
            minScore = min,
            variance = variance,
            highAttentionCount = highCount,
            totalActiveTasks = activeTaskIds.size,
            scores = scoreMap
        )
    }

    fun summarizeAll(
        attentionResults: Map<Int, AttentionResult>,
        allTaskIds: List<Int>
    ): AttentionSummary {
        val scores = allTaskIds.mapNotNull { id ->
            attentionResults[id]?.score
        }

        if (scores.isEmpty()) {
            return AttentionSummary(
                averageScore = 0f,
                maxScore = 0f,
                minScore = 0f,
                variance = 0f,
                highAttentionCount = 0,
                totalActiveTasks = allTaskIds.size,
                scores = emptyMap()
            )
        }

        val avg = scores.average().toFloat()
        val max = scores.max()
        val min = scores.min()
        val variance = computeVariance(scores, avg)
        val highCount = scores.count { it >= HIGH_ATTENTION_THRESHOLD }

        val scoreMap = allTaskIds.associate { id ->
            id to (attentionResults[id]?.score ?: 0f)
        }

        return AttentionSummary(
            averageScore = avg,
            maxScore = max,
            minScore = min,
            variance = variance,
            highAttentionCount = highCount,
            totalActiveTasks = allTaskIds.size,
            scores = scoreMap
        )
    }

    fun format(summary: AttentionSummary): String {
        return buildString {
            appendLine("Attention Summary:")
            appendLine("  Average: ${String.format("%.3f", summary.averageScore)}")
            appendLine("  High: ${summary.highAttentionCount}/${summary.totalActiveTasks}")
            appendLine("  Range: ${String.format("%.3f", summary.minScore)} - ${String.format("%.3f", summary.maxScore)}")
            appendLine("  Variance: ${String.format("%.4f", summary.variance)}")
        }
    }

    private fun computeVariance(scores: List<Float>, mean: Float): Float {
        if (scores.isEmpty()) return 0f
        val sumSq = scores.sumOf { (it - mean).toDouble().pow(2) }
        return (sumSq / scores.size).toFloat()
    }
}