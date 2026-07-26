package com.example.debug.validation

import com.example.core.goal.GoalRepository
import com.example.core.snapshot.SnapshotRepository
import com.example.debug.scenarios.GeneratedScenarioResult
import com.example.debug.scenarios.ScenarioGenerator
import com.example.debug.scenarios.ScenarioType
import com.example.domain.attention.AttentionCalculator
import com.example.domain.attention.AttentionResult
import com.example.domain.attention.TaskAttentionInput
import com.example.domain.graph.TaskMetadata
import com.example.domain.graph.VisibilityInput
import com.example.domain.graph.VisibilityLevel
import com.example.domain.graph.VisibilityResolver
import com.example.domain.graph.VisibleGraphModel
import com.example.domain.mirror.MirrorHeuristics
import com.example.domain.mirror.MirrorSignal
import com.example.domain.mirror.MirrorSignalType
import com.example.plugins.planner.data.InsightRepository
import com.example.plugins.planner.data.TaskEntity
import com.example.plugins.planner.data.TaskRepository
import kotlinx.coroutines.flow.first

class ScenarioValidationRunner(
    private val scenarioGenerator: ScenarioGenerator,
    private val taskRepository: TaskRepository,
    private val insightRepository: InsightRepository,
    private val goalRepository: GoalRepository,
    private val snapshotRepository: SnapshotRepository
) {

    suspend fun validate(scenario: ScenarioType): ValidationResult {
        val checks = mutableListOf<ValidationCheck>()

        scenarioGenerator.clearGeneratedScenarios()

        val result = scenarioGenerator.generate(scenario)
        val goalId = result.goalId

        val tasks = taskRepository.getTasksByGoalId(goalId).first()
        val reschedules = insightRepository.observeRescheduleCountsByGoal(goalId).first()
        val goal = goalRepository.getGoalById(goalId)
        val events = goalRepository.observeGoalEvents(goalId).first()

        val rescheduleMap = reschedules.associate { it.taskId to it.rescheduleCount }
        val rescheduleCreateMap = reschedules.associate { it.taskId to it.taskCreatedAt }

        val interactions = insightRepository.getLastMeaningfulInteractionPerTask(goalId)
        val interactionMap = interactions.associate { it.taskId to it.lastMeaningfulMs }

        val now = System.currentTimeMillis()

        val activeTasks = tasks.filter { !it.isCompleted }
        val attentionInputs = buildAttentionInputs(activeTasks, rescheduleMap, interactionMap)
        val attentionResults = AttentionCalculator.compute(attentionInputs, now)

        val attentionSummary = AttentionValidationHelper.summarize(attentionResults, activeTasks.map { it.id })

        val taskMetadata = buildTaskMetadata(tasks, rescheduleMap)

        val overviewInput = VisibilityInput(
            activeTaskIds = activeTasks.map { it.id },
            attentionResults = attentionResults,
            taskMetadata = taskMetadata,
            level = VisibilityLevel.OVERVIEW,
            viewportRadius = 320f,
            nowMillis = now
        )
        val overviewModel = VisibilityResolver.resolve(overviewInput)

        val insightInput = overviewInput.copy(level = VisibilityLevel.INSIGHT)
        val insightModel = VisibilityResolver.resolve(insightInput)

        val signals = evaluateMirrorSignals(
            goalId = goalId,
            tasks = tasks,
            reschedules = reschedules,
            events = events,
            goalTitle = goal?.title ?: "",
            now = now
        )
        val signalTypes = signals.map { it.type }

        val (summary, scenarioChecks) = validateScenario(
            scenario = scenario,
            result = result,
            tasks = tasks,
            activeTasks = activeTasks,
            rescheduleMap = rescheduleMap,
            attentionSummary = attentionSummary,
            attentionResults = attentionResults,
            signals = signalTypes,
            overviewModel = overviewModel,
            insightModel = insightModel,
            now = now
        )

        checks.addAll(scenarioChecks)

        val allPassed = checks.all { it.passed }

        return ValidationResult(
            scenario = scenario,
            passed = allPassed,
            checks = checks,
            summary = summary
        )
    }

    private suspend fun testAll(
        results: Map<ScenarioType, ValidationResult>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Scenario             | Result | Failed Checks")
        sb.appendLine("---------------------|--------|--------------")
        for ((scenario, result) in results) {
            sb.appendLine(ValidationReportFormatter.formatTableRow(result))
        }
        sb.appendLine()
        sb.appendLine("Summary: ${results.count { it.value.passed }}/${results.size} passed")
        val failed = results.filter { !it.value.passed }
        if (failed.isNotEmpty()) {
            sb.appendLine("Failed scenarios:")
            for ((scenario, result) in failed) {
                sb.appendLine("  - ${scenario.label}: ${result.checks.filter { !it.passed }.joinToString(", ") { it.name }}")
            }
        }
        return sb.toString()
    }

    private fun buildAttentionInputs(
        tasks: List<TaskEntity>,
        rescheduleMap: Map<Int, Int>,
        interactionMap: Map<Int, Long>
    ): List<TaskAttentionInput> {
        return tasks.map { task ->
            TaskAttentionInput(
                id = task.id,
                title = task.title,
                dateEpochMs = task.dateEpochMs,
                deadlineEpochMs = task.deadlineEpochMs,
                lastMeaningfulInteractionMs = interactionMap[task.id],
                rescheduleCount = rescheduleMap[task.id] ?: 0
            )
        }
    }

    private fun buildTaskMetadata(
        tasks: List<TaskEntity>,
        rescheduleMap: Map<Int, Int>
    ): Map<Int, TaskMetadata> {
        return tasks.associate { task ->
            task.id to TaskMetadata(
                id = task.id,
                title = task.title,
                priority = task.priority,
                dateEpochMs = task.dateEpochMs,
                deadlineEpochMs = task.deadlineEpochMs,
                rescheduleCount = rescheduleMap[task.id] ?: 0
            )
        }
    }

    private suspend fun evaluateMirrorSignals(
        goalId: Int,
        tasks: List<TaskEntity>,
        reschedules: List<com.example.plugins.planner.data.TaskRescheduleWithTitle>,
        events: List<com.example.core.goal.GoalEventEntity>,
        goalTitle: String,
        now: Long
    ): List<MirrorSignal> {
        val out = mutableListOf<MirrorSignal>()

        val rescheduleMap = reschedules.associateBy { it.taskId }
        val taskMap = tasks.associateBy { it.id }

        for ((taskId, reschedule) in rescheduleMap) {
            val task = taskMap[taskId]
            val signal = MirrorHeuristics.detectBoulder(
                taskId = taskId,
                taskTitle = reschedule.taskTitle ?: "task-$taskId",
                rescheduleCount = reschedule.rescheduleCount,
                createdAtMs = reschedule.taskCreatedAt,
                isCompleted = task?.isCompleted ?: false,
                nowMillis = now
            )
            if (signal != null) out.add(signal)
        }

        val activeCount = tasks.count { !it.isCompleted }
        if (activeCount > 0) {
            val lastEvent = events.maxByOrNull { it.timestamp }?.timestamp
            val attention = MirrorHeuristics.detectGoalAttention(
                goalId = goalId,
                goalTitle = goalTitle,
                activeTasks = activeCount,
                lastEventTimestampMs = lastEvent,
                nowMillis = now
            )
            if (attention != null) out.add(attention)
        }

        val week = 7L * 24 * 60 * 60 * 1000
        val start = events.minOfOrNull { it.timestamp } ?: (now - week)
        val end = now

        val behaviors = snapshotRepository.observeBehaviorRange(start, end).first()
        val currentCompleted = behaviors.sumOf { it.completed }

        val prevStart = start
        val prevEnd = now - week
        val previousCompleted = if (prevStart < prevEnd)
            snapshotRepository.observeBehaviorRange(prevStart, prevEnd).first().sumOf { it.completed }
        else 0

        val decay = MirrorHeuristics.detectConsistencyDecay(currentCompleted, previousCompleted)
        if (decay != null) out.add(decay)

        val createdCount = behaviors.sumOf { it.created }
        val initiator = MirrorHeuristics.detectInitiatorFinisher(createdCount, currentCompleted)
        if (initiator != null) out.add(initiator)

        return out.take(3)
    }

    data class ScenarioValidationSummary(
        val summary: String,
        val checks: List<ValidationCheck>
    )

    private fun validateScenario(
        scenario: ScenarioType,
        result: GeneratedScenarioResult,
        tasks: List<TaskEntity>,
        activeTasks: List<TaskEntity>,
        rescheduleMap: Map<Int, Int>,
        attentionSummary: AttentionSummary,
        attentionResults: Map<Int, AttentionResult>,
        signals: List<MirrorSignalType>,
        overviewModel: VisibleGraphModel,
        insightModel: VisibleGraphModel,
        now: Long
    ): Pair<String, List<ValidationCheck>> {
        return when (scenario) {
            ScenarioType.PRODUCTIVE_USER -> validateProductiveUser(
                tasks, activeTasks, attentionSummary, signals, overviewModel
            )
            ScenarioType.PROCRASTINATOR_USER -> validateProcrastinatorUser(
                tasks, activeTasks, rescheduleMap, attentionSummary, signals, overviewModel, attentionResults, now
            )
            ScenarioType.CHAOS_USER -> validateChaosUser(
                tasks, activeTasks, attentionSummary, signals, insightModel
            )
            ScenarioType.RECOVERY_USER -> validateRecoveryUser(
                tasks, attentionResults, signals, now
            )
        }
    }

    private fun validateProductiveUser(
        tasks: List<TaskEntity>,
        activeTasks: List<TaskEntity>,
        attentionSummary: AttentionSummary,
        signals: List<MirrorSignalType>,
        overviewModel: VisibleGraphModel
    ): Pair<String, List<ValidationCheck>> {
        val checks = mutableListOf<ValidationCheck>()
        val completed = tasks.count { it.isCompleted }

        checks.add(
            ValidationCheck(
                name = "Total tasks >= 18",
                expected = ">= 18",
                actual = tasks.size.toString(),
                passed = tasks.size >= 18
            )
        )
        checks.add(
            ValidationCheck(
                name = "Completed >= 18",
                expected = ">= 18",
                actual = completed.toString(),
                passed = completed >= 18
            )
        )
        checks.add(
            ValidationCheck(
                name = "Active <= 2",
                expected = "<= 2",
                actual = activeTasks.size.toString(),
                passed = activeTasks.size <= 2
            )
        )
        checks.add(
            ValidationCheck(
                name = "Average attention < 0.3",
                expected = "< 0.3",
                actual = String.format("%.3f", attentionSummary.averageScore),
                passed = attentionSummary.averageScore < 0.3f
            )
        )
        checks.add(
            ValidationCheck(
                name = "No Boulder signal",
                expected = "Absent",
                actual = if (MirrorSignalType.BOULDER in signals) "Present" else "Absent",
                passed = MirrorSignalType.BOULDER !in signals
            )
        )
        checks.add(
            ValidationCheck(
                name = "No GoalAttention signal",
                expected = "Absent",
                actual = if (MirrorSignalType.GOAL_ATTENTION in signals) "Present" else "Absent",
                passed = MirrorSignalType.GOAL_ATTENTION !in signals
            )
        )
        checks.add(
            ValidationCheck(
                name = "No InitiatorFinisher signal",
                expected = "Absent",
                actual = if (MirrorSignalType.INITIATOR_FINISHER in signals) "Present" else "Absent",
                passed = MirrorSignalType.INITIATOR_FINISHER !in signals
            )
        )
        checks.add(
            ValidationCheck(
                name = "No ConsistencyDecay signal",
                expected = "Absent",
                actual = if (MirrorSignalType.CONSISTENCY_DECAY in signals) "Present" else "Absent",
                passed = MirrorSignalType.CONSISTENCY_DECAY !in signals
            )
        )

        val desc = "Productive User: $completed completed, ${activeTasks.size} active. " +
                "Attention avg: ${String.format("%.3f", attentionSummary.averageScore)}. " +
                "Signals: $signals. Overview: ${overviewModel.level}."

        return desc to checks
    }

    private fun validateProcrastinatorUser(
        tasks: List<TaskEntity>,
        activeTasks: List<TaskEntity>,
        rescheduleMap: Map<Int, Int>,
        attentionSummary: AttentionSummary,
        signals: List<MirrorSignalType>,
        overviewModel: VisibleGraphModel,
        attentionResults: Map<Int, AttentionResult>,
        now: Long
    ): Pair<String, List<ValidationCheck>> {
        val checks = mutableListOf<ValidationCheck>()
        val totalReschedules = rescheduleMap.values.sum()
        val activeTaskIds = activeTasks.map { it.id }

        checks.add(
            ValidationCheck(
                name = "Total tasks >= 17",
                expected = ">= 17",
                actual = tasks.size.toString(),
                passed = tasks.size >= 17
            )
        )
        checks.add(
            ValidationCheck(
                name = "Active >= 15",
                expected = ">= 15",
                actual = activeTasks.size.toString(),
                passed = activeTasks.size >= 15
            )
        )
        checks.add(
            ValidationCheck(
                name = "Reschedules >= 10",
                expected = ">= 10",
                actual = totalReschedules.toString(),
                passed = totalReschedules >= 10
            )
        )
        checks.add(
            ValidationCheck(
                name = "Average attention > 0.4",
                expected = "> 0.4",
                actual = String.format("%.3f", attentionSummary.averageScore),
                passed = attentionSummary.averageScore > 0.4f
            )
        )
        checks.add(
            ValidationCheck(
                name = "Boulder signal present",
                expected = "Present",
                actual = if (MirrorSignalType.BOULDER in signals) "Present" else "Absent",
                passed = MirrorSignalType.BOULDER in signals
            )
        )
        checks.add(
            ValidationCheck(
                name = "GoalAttention signal present",
                expected = "Present",
                actual = if (MirrorSignalType.GOAL_ATTENTION in signals) "Present" else "Absent",
                passed = MirrorSignalType.GOAL_ATTENTION in signals
            )
        )
        checks.add(
            ValidationCheck(
                name = "InitiatorFinisher signal present",
                expected = "Present",
                actual = if (MirrorSignalType.INITIATOR_FINISHER in signals) "Present" else "Absent",
                passed = MirrorSignalType.INITIATOR_FINISHER in signals
            )
        )

        val overdueExists = activeTasks.any { task ->
            val deadlineMs = task.deadlineEpochMs
            deadlineMs != null && deadlineMs < now
        }
        checks.add(
            ValidationCheck(
                name = "Overdue tasks exist",
                expected = "True",
                actual = overdueExists.toString(),
                passed = overdueExists
            )
        )

        val highAttention = activeTaskIds.any { id ->
            (attentionResults[id]?.score ?: 0f) >= 0.5f
        }
        checks.add(
            ValidationCheck(
                name = "High attention tasks exist",
                expected = "True",
                actual = highAttention.toString(),
                passed = highAttention
            )
        )

        val desc = "Procrastinator User: ${activeTasks.size} active, $totalReschedules reschedules. " +
                "Attention avg: ${String.format("%.3f", attentionSummary.averageScore)}. " +
                "Signals: $signals. Overview: ${overviewModel.level}."

        return desc to checks
    }

    private fun validateChaosUser(
        tasks: List<TaskEntity>,
        activeTasks: List<TaskEntity>,
        attentionSummary: AttentionSummary,
        signals: List<MirrorSignalType>,
        insightModel: VisibleGraphModel
    ): Pair<String, List<ValidationCheck>> {
        val checks = mutableListOf<ValidationCheck>()

        checks.add(
            ValidationCheck(
                name = "Total tasks = 50",
                expected = "50",
                actual = tasks.size.toString(),
                passed = tasks.size == 50
            )
        )
        checks.add(
            ValidationCheck(
                name = "Active >= 30",
                expected = ">= 30",
                actual = activeTasks.size.toString(),
                passed = activeTasks.size >= 30
            )
        )
        checks.add(
            ValidationCheck(
                name = "Attention variance > 0",
                expected = "> 0",
                actual = String.format("%.4f", attentionSummary.variance),
                passed = attentionSummary.variance > 0f
            )
        )
        checks.add(
            ValidationCheck(
                name = "At least one Mirror signal",
                expected = "Signals present",
                actual = if (signals.isEmpty()) "None" else signals.joinToString(", "),
                passed = signals.isNotEmpty()
            )
        )

        val desc = "Chaos User: ${tasks.size} tasks, ${activeTasks.size} active. " +
                "Attention avg: ${String.format("%.3f", attentionSummary.averageScore)}, " +
                "variance: ${String.format("%.4f", attentionSummary.variance)}. " +
                "Signals: $signals. Clustered: ${insightModel.isClustered}."

        return desc to checks
    }

    private fun validateRecoveryUser(
        tasks: List<TaskEntity>,
        attentionResults: Map<Int, AttentionResult>,
        signals: List<MirrorSignalType>,
        now: Long
    ): Pair<String, List<ValidationCheck>> {
        val checks = mutableListOf<ValidationCheck>()

        val sortedByCreation = tasks.sortedBy { it.timestamp }
        val midPoint = sortedByCreation.size / 2
        val earlyTasks = sortedByCreation.take(midPoint)
        val recentTasks = sortedByCreation.drop(midPoint)

        val earlyScores = earlyTasks.mapNotNull { attentionResults[it.id]?.score }
        val recentScores = recentTasks.mapNotNull { attentionResults[it.id]?.score }

        val earlyAvg = if (earlyScores.isNotEmpty()) earlyScores.average().toFloat() else 0f
        val recentAvg = if (recentScores.isNotEmpty()) recentScores.average().toFloat() else 0f

        checks.add(
            ValidationCheck(
                name = "Early tasks have data",
                expected = "> 0 tasks",
                actual = "${earlyTasks.size} tasks",
                passed = earlyTasks.isNotEmpty()
            )
        )
        checks.add(
            ValidationCheck(
                name = "Recent tasks have data",
                expected = "> 0 tasks",
                actual = "${recentTasks.size} tasks",
                passed = recentTasks.isNotEmpty()
            )
        )
        checks.add(
            ValidationCheck(
                name = "Decreasing attention (recent < early)",
                expected = "recent < early",
                actual = String.format("early: %.3f, recent: %.3f", earlyAvg, recentAvg),
                passed = recentAvg < earlyAvg
            )
        )
        checks.add(
            ValidationCheck(
                name = "No ConsistencyDecay signal",
                expected = "Absent",
                actual = if (MirrorSignalType.CONSISTENCY_DECAY in signals) "Present" else "Absent",
                passed = MirrorSignalType.CONSISTENCY_DECAY !in signals
            )
        )

        val desc = "Recovery User: ${tasks.size} tasks. " +
                "Early avg: ${String.format("%.3f", earlyAvg)}, " +
                "Recent avg: ${String.format("%.3f", recentAvg)}. " +
                "Signals: $signals."

        return desc to checks
    }
}