package com.example.plugins.planner.data

import androidx.room.withTransaction
import com.example.core.database.AppDatabase

/**
 * CreateTagUseCase — Atomic tag creation.
 *
 * Phase 5.5d: Tag is metadata only. No initial activities.
 *
 * Responsibility:
 * - Create a TaskStepEntity (tag) atomically
 * - No activity events are created — tag management is metadata, not user action
 *
 * Flow:
 * ```
 * val tagId = useCase.execute(taskId, tagDraft)
 * // Only: TaskStepEntity (no ActivityEventEntity)
 * ```
 *
 * Transaction Safety:
 * - Uses database.withTransaction {} for atomicity
 */
class CreateStepUseCase(
    private val database: AppDatabase
) {
    /**
     * Execute tag creation.
     *
     * @param taskId The task this tag belongs to
     * @param draft StepDraft with title only
     * @return Generated tagId
     * @throws IllegalStateException if tag creation fails
     */
    suspend fun execute(
        taskId: Int,
        draft: StepDraft
    ): Long {
        return database.withTransaction {
            // 1. Create tag entity
            val tagTitle = StepDraftResolver.getStepTitle(draft)
            val tagEntity = TaskStepEntity(
                taskId = taskId,
                title = tagTitle,
                colorHex = draft.colorHex
            )
            val tagId = database.taskStepDao().insert(tagEntity).toInt()

            // Verify tag was created
            check(tagId > 0) { "Failed to create tag for task $taskId" }

            // Tags are metadata — no activity event created.
            // Activities reference tags via stepId but tag creation is not a user action.

            tagId.toLong()
        }
    }
}
