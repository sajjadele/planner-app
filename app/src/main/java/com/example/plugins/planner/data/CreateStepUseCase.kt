package com.example.plugins.planner.data

import com.example.core.database.AppDatabase

/**
 * CreateStepUseCase — Atomic step (tag) creation.
 *
 * Phase 5.5d: Step is metadata/tag only. No initial activities.
 *
 * Responsibility:
 * - Create a TaskStepEntity (tag) and its STEP_CREATED event atomically
 * - No activities are created — activities are independent entities
 *
 * Flow:
 * ```
 * val stepId = useCase.execute(taskId, stepDraft)
 * // Only: TaskStepEntity + STEP_CREATED event
 * // No ActivityEventEntity for initial activities
 * ```
 *
 * Transaction Safety:
 * - Uses database.withTransaction {} for atomicity
 */
class CreateStepUseCase(
    private val database: AppDatabase
) {
    /**
     * Execute step (tag) creation.
     *
     * @param taskId The task this step belongs to
     * @param draft StepDraft with title only
     * @return Generated stepId
     * @throws IllegalStateException if step creation fails
     */
    suspend fun execute(
        taskId: Int,
        draft: StepDraft
    ): Long {
        return database.withTransaction {
            // 1. Create step entity
            val stepTitle = StepDraftResolver.getStepTitle(draft)
            val stepEntity = TaskStepEntity(
                taskId = taskId,
                title = stepTitle
            )
            val stepId = database.taskStepDao().insert(stepEntity).toInt()

            // Verify step was created
            check(stepId > 0) { "Failed to create step for task $taskId" }

            // 2. Create STEP_CREATED event (filtered by ActivityMessageMapper)
            database.activityEventDao().insert(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = stepId,
                    eventType = ActivityEventType.STEP_CREATED.name,
                    description = stepTitle
                )
            )

            stepId.toLong()
        }
    }
}
