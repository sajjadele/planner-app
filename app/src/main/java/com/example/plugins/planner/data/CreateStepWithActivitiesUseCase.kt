package com.example.plugins.planner.data

import androidx.room.withTransaction
import com.example.core.database.AppDatabase

/**
 * CreateStepWithActivitiesUseCase — Atomic step + activities creation.
 *
 * Phase 4.10.2: Transaction Pipeline
 *
 * Responsibility:
 * - Create a Step and its initial activities atomically
 * - Ensure stepId is always assigned to child activities
 * - Rollback on failure (no partial data)
 *
 * Flow:
 * ```kotlin
 * val stepId = useCase.execute(taskId, stepDraft)
 * // stepId is guaranteed to be valid
 * // all initial activities have correct stepId
 * ```
 *
 * Transaction Safety:
 * - Uses database.withTransaction {} for atomicity
 * - If activity creation fails, step creation rolls back
 * - No partial data exists in database
 */
class CreateStepWithActivitiesUseCase(
    private val database: AppDatabase
) {
    /**
     * Execute step creation with initial activities.
     *
     * @param taskId The task this step belongs to
     * @param draft StepDraft with title and optional initial activities
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

            // 2. Create STEP_CREATED event
            database.activityEventDao().insert(
                ActivityEventEntity(
                    taskId = taskId,
                    stepId = stepId,
                    eventType = ActivityEventType.STEP_CREATED.name,
                    description = stepTitle
                )
            )

            // 3. Create initial activities with stepId
            val initialActivities = StepDraftResolver.getInitialActivities(draft)
            for (activity in initialActivities) {
                val eventType = StepDraftResolver.resolveActivityEventType(activity)
                val description = StepDraftResolver.encodeActivityDescription(activity)

                database.activityEventDao().insert(
                    ActivityEventEntity(
                        taskId = taskId,
                        stepId = stepId,
                        eventType = eventType.name,
                        description = description
                    )
                )
            }

            stepId.toLong()
        }
    }
}
