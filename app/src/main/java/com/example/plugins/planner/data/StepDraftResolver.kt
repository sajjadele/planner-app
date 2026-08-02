package com.example.plugins.planner.data

/**
 * TagDraftResolver — Handles Tag creation logic.
 *
 * Phase 5.5d: Tag is metadata only. No initial activities.
 *
 * Responsibility:
 * - Extract tag title from TagDraft
 * - Tag is pure metadata
 *
 * Flow:
 * ```
 * TagDraft → getTagTitle → String
 * ```
 */
object StepDraftResolver {

    /** Extract step title from StepDraft. */
    fun getStepTitle(draft: StepDraft): String {
        return draft.title
    }
}
