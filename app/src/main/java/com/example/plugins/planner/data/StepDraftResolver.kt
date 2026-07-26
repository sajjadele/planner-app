package com.example.plugins.planner.data

/**
 * StepDraftResolver — Handles Step creation logic.
 *
 * Phase 4.10.1: Domain Separation
 *
 * Responsibility:
 * - Create TaskStepEntity from StepDraft
 * - Coordinate creation of initial activities with stepId
 * - Ensure stepId is always assigned to child activities
 *
 * Flow:
 * ```kotlin
 * StepDraft
 *     ↓
 * StepDraftResolver.resolve()
 *     ↓
 * TaskStepEntity (created first)
 *     ↓
 * stepId (generated)
 *     ↓
 * ActivityEventEntity[] (created with stepId)
 * ```
 *
 * This resolver owns the stepId propagation logic.
 * UI does NOT need to pass stepId manually.
 */
object StepDraftResolver {

    /**
     * Extract step title from StepDraft.
     */
    fun getStepTitle(draft: StepDraft): String {
        return draft.title
    }

    /**
     * Check if StepDraft has initial activities.
     */
    fun hasInitialActivities(draft: StepDraft): Boolean {
        return draft.initialActivities.isNotEmpty()
    }

    /**
     * Get initial activities from StepDraft.
     */
    fun getInitialActivities(draft: StepDraft): List<ActivityDraft> {
        return draft.initialActivities
    }

    /**
     * Encode initial activity to description string.
     * Uses ActivityPayloadCodec for rich payloads.
     */
    fun encodeActivityDescription(activity: ActivityDraft): String? {
        return ActivityDraftResolver.encodeDescription(activity)
    }

    /**
     * Resolve ActivityEventType for an initial activity.
     * Initial activities are always NOTE_ADDED (not STEP_CREATED).
     */
    fun resolveActivityEventType(activity: ActivityDraft): ActivityEventType {
        // Initial activities are always notes or manual activities
        return if (activity.durationMinutes != null) {
            ActivityEventType.MANUAL_ACTIVITY
        } else {
            ActivityEventType.NOTE_ADDED
        }
    }
}
