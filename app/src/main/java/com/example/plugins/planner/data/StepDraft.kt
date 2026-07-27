package com.example.plugins.planner.data

/**
 * StepDraft — Domain model for creating a Step (Tag).
 *
 * Phase 5.5d: Step is metadata/tag only. No initial activities.
 * Phase 5.9.3: Added optional colorHex for tag color.
 *
 * Responsibility:
 * - Represents the user's intent to create a Step (Tag)
 * - Contains step title and optional color
 * - Step is pure metadata — activities are created independently
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
    val title: String,
    /** Optional hex color (#RRGGBB) for tag chip display. */
    val colorHex: String? = null
) {
    companion object {
        /** Empty draft for default state. */
        val EMPTY = StepDraft(title = "")
    }
}
