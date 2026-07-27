package com.example.plugins.planner.data

/**
 * ActivityCreationContext — Context in which an activity is created.
 *
 * Phase 5.5b: Context-Aware Activity Creation
 *
 * Injected at the ViewModel level when creating activities.
 * The Composer remains step-agnostic — it never sees this context.
 *
 * Derivation:
 * - When a tag filter is active (e.g., selectedStepId=1):
 *   context.stepId = 1 (new activities inherit the tag)
 * - When no filter is active (همه):
 *   context.stepId = null (new activities are task-level)
 */
data class ActivityCreationContext(
    /** The step/tag to associate new activities with, or null for task-level. */
    val stepId: Long? = null,

    /** Human-readable name of the step/tag, for UI hint display. */
    val stepName: String? = null
) {
    companion object {
        /** Default context — no tag, task-level activities. */
        val DEFAULT = ActivityCreationContext()
    }
}
