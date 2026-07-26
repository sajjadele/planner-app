package com.example.plugins.planner.data

/**
 * StepDraft — Domain model for creating a Step.
 *
 * Phase 4.10.1: Domain Separation
 *
 * Responsibility:
 * - Represents the user's intent to create a Step
 * - Contains step title and optional initial activities
 * - Does NOT contain Activity-specific logic
 *
 * Examples:
 * ```kotlin
 * // Simple step
 * StepDraft(title = "طراحی صفحه اصلی")
 *
 * // Step with initial content
 * StepDraft(
 *     title = "طراحی صفحه اصلی",
 *     initialActivities = listOf(
 *         ActivityDraft(
 *             text = "نمونه اولیه آماده شد",
 *             attachments = listOf(ActivityAttachment.Image("content://..."))
 *         )
 *     )
 * )
 * ```
 *
 * Flow:
 * ```kotlin
 * StepDraft
 *     ↓
 * StepDraftResolver.resolve()
 *     ↓
 * TaskStepEntity + ActivityEventEntity[]
 * ```
 */
data class StepDraft(
    /**
     * Title of the step. Required.
     */
    val title: String,

    /**
     * Optional initial activities to create with the step.
     * Each activity will be associated with the step via stepId.
     */
    val initialActivities: List<ActivityDraft> = emptyList()
) {
    /**
     * Check if this step has initial content.
     */
    fun hasInitialContent(): Boolean {
        return initialActivities.isNotEmpty()
    }

    /**
     * Get total attachment count across all initial activities.
     */
    fun getTotalAttachmentCount(): Int {
        return initialActivities.sumOf { it.attachments.size }
    }

    companion object {
        /**
         * Empty draft for default state.
         */
        val EMPTY = StepDraft(title = "")
    }
}
