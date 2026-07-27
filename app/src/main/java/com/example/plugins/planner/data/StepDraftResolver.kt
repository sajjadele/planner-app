package com.example.plugins.planner.data

/**
 * StepDraftResolver — Handles Step (Tag) creation logic.
 *
 * Phase 5.5d: Step is metadata/tag only. No initial activities.
 *
 * Responsibility:
 * - Extract step title from StepDraft
 * - Step is now pure tag creation
 *
 * Flow:
 * ```
 * StepDraft → getStepTitle → String
 * ```
 */
object StepDraftResolver {

    /** Extract step title from StepDraft. */
    fun getStepTitle(draft: StepDraft): String {
        return draft.title
    }
}
