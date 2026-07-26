package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.ActivityIntent
import com.example.plugins.planner.data.StepDraft

/**
 * ActivityComposerState — Single source of truth for the unified composer.
 *
 * Phase 4.7.3 + 4.10.1 (Domain Separation):
 * - Replaces boolean expansion states
 * - One state object drives the entire composer UI
 * - Generates ActivityDraft or StepDraft when submitted
 *
 * The `intent` field is kept for UI purposes (showing step indicator).
 * When submitting, the state creates the appropriate draft type.
 *
 * Usage:
 * ```kotlin
 * var state by remember { mutableStateOf(ActivityComposerState()) }
 *
 * // Text change
 * state = state.copy(text = "Hello")
 *
 * // Add attachment
 * state = state.copy(attachments = state.attachments + Image(uri))
 *
 * // Submit as activity
 * val activityDraft = state.toActivityDraft()
 *
 * // Submit as step
 * val stepDraft = state.toStepDraft()
 * ```
 */
data class ActivityComposerState(
    val text: String = "",
    val attachments: List<ActivityAttachment> = emptyList(),
    val durationMinutes: Int? = null,
    val intent: ActivityIntent = ActivityIntent.ACTIVITY
) {
    /**
     * Convert state to ActivityDraft for submission.
     * Used when intent == ACTIVITY.
     */
    fun toActivityDraft(): ActivityDraft = ActivityDraft(
        text = text.ifBlank { null },
        attachments = attachments,
        durationMinutes = durationMinutes
    )

    /**
     * Convert state to StepDraft for submission.
     * Used when intent == STEP.
     *
     * The step title comes from the text field.
     * Initial activities are created from attachments.
     */
    fun toStepDraft(): StepDraft {
        val initialActivities = mutableListOf<ActivityDraft>()

        // If there are attachments, create an initial activity
        if (attachments.isNotEmpty()) {
            initialActivities.add(
                ActivityDraft(
                    text = null,  // step title is on the step itself
                    attachments = attachments,
                    durationMinutes = durationMinutes
                )
            )
        }

        return StepDraft(
            title = text.ifBlank { "" },
            initialActivities = initialActivities
        )
    }

    /**
     * Check if the composer has any content.
     */
    fun hasContent(): Boolean {
        return text.isNotBlank() || attachments.isNotEmpty() || durationMinutes != null
    }

    /**
     * Check if the composer can be submitted.
     */
    fun canSubmit(): Boolean {
        return hasContent()
    }

    /**
     * Check if current intent is STEP.
     */
    fun isStepMode(): Boolean {
        return intent == ActivityIntent.STEP
    }

    companion object {
        /**
         * Default empty state.
         */
        val EMPTY = ActivityComposerState()
    }
}
