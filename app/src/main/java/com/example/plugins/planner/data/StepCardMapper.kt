package com.example.plugins.planner.data

/**
 * StepCardMapper — Converts TaskStepEntity + ActivityMessageModels to StepCardModel.
 *
 * Architecture (Phase 4.9.2):
 * - Single responsibility: Step + Activities → StepCardModel
 * - Aggregates activity data (attachments, duration)
 * - Handles empty/missing data gracefully
 * - Never throws exceptions
 *
 * Usage:
 * ```
 * val cardModel = StepCardMapper.toCardModel(step, activities)
 * // or
 * val cardModels = StepCardMapper.toCardModels(steps, activitiesMap)
 * ```
 */
object StepCardMapper {

    /**
     * Convert a single TaskStepEntity with its activities to StepCardModel.
     *
     * @param step The task step entity
     * @param activities List of activity messages associated with this step
     * @return StepCardModel with aggregated data
     */
    fun toCardModel(
        step: TaskStepEntity,
        activities: List<ActivityMessageModel> = emptyList()
    ): StepCardModel {
        // Sort activities by timestamp ascending
        val sortedActivities = activities.sortedBy { it.timestamp }

        // Calculate total attachment count
        val attachmentCount = sortedActivities.sumOf { it.attachments.size }

        // Calculate total duration from manual activities
        val totalDurationMinutes = sortedActivities
            .mapNotNull { it.durationMinutes }
            .takeIf { it.isNotEmpty() }
            ?.sum()

        return StepCardModel(
            id = step.id.toLong(),
            title = step.title,
            isCompleted = step.isCompleted,
            messages = sortedActivities,
            attachmentCount = attachmentCount,
            totalDurationMinutes = totalDurationMinutes,
            createdAt = step.createdAt
        )
    }

    /**
     * Convert a list of TaskStepEntity with their activities to StepCardModel list.
     *
     * @param steps List of task step entities
     * @param activitiesMap Map of stepId to list of activity messages
     * @return List of StepCardModel
     */
    fun toCardModels(
        steps: List<TaskStepEntity>,
        activitiesMap: Map<Int, List<ActivityMessageModel>> = emptyMap()
    ): List<StepCardModel> {
        return steps.map { step ->
            val activities = activitiesMap[step.id] ?: emptyList()
            toCardModel(step, activities)
        }
    }

    /**
     * Convert a list of ActivityEventEntity to ActivityMessageModel and group by stepId.
     *
     * @param events List of activity event entities
     * @return Map of stepId to list of ActivityMessageModel
     */
    fun groupActivitiesByStep(
        events: List<ActivityEventEntity>
    ): Map<Int, List<ActivityMessageModel>> {
        return events
            .filter { it.stepId != null }
            .groupBy { it.stepId!! }
            .mapValues { (_, stepEvents) ->
                stepEvents.map { ActivityMessageMapper.toMessage(it) }
            }
    }

    /**
     * Create a summary text for the step card.
     */
    fun createSummary(model: StepCardModel): String {
        return model.getActivitySummary()
    }

    /**
     * Check if a step has any previewable content.
     */
    fun hasPreviewContent(model: StepCardModel): Boolean {
        return model.hasRichContent()
    }
}
