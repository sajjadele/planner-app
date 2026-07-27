package com.example.plugins.planner.data

/**
 * StepDraft — Domain model for creating a Step (Tag).
 *
 * Phase 5.5d: Step is metadata/tag only. No initial activities.
 *
 * Responsibility:
 * - Represents the user's intent to create a Step (Tag)
 * - Contains step title only
 * - Step is now pure metadata — activities are created independently
 *
 * Flow:
 * ```
 * StepDraft
 *     ↓
 * CreateStepUseCase.execute()
 *     ↓
 * TaskStepEntity + STEP_CREATED event
 * ```
 */
data class StepDraft(
    /** Title of the step/tag. Required. */
    val title: String
) {
    companion object {
        /** Empty draft for default state. */
        val EMPTY = StepDraft(title = "")
    }
}
