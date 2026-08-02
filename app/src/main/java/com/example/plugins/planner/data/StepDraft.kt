package com.example.plugins.planner.data

/**
 * TagDraft — Domain model for creating a Tag.
 *
 * Phase 5.5d: Tag is metadata only. No initial activities.
 * Phase 5.9.3: Added optional colorHex for tag color.
 *
 * Responsibility:
 * - Represents the user's intent to create a Tag
 * - Contains tag title and optional color
 * - Tag is pure metadata — activities are created independently
 *
 * Flow:
 * ```
 * TagDraft
 *     ↓
 * CreateTagUseCase.execute()
 *     ↓
 * TaskStepEntity (no ActivityEventEntity)
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
