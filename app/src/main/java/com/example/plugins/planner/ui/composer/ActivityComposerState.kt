package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.StepDraft

/**
 * ActivityComposerState — Single source of truth for the unified composer.
 *
 * Phase 4.11.1: Unified Composer State
 *
 * Architecture:
 * - One state object drives the entire composer UI
 * - Mode determines what the user is creating
 * - No UI visibility state (isExpanded flags removed)
 * - Clean conversion to domain models (ActivityDraft or StepDraft)
 *
 * Design Principles:
 * - Store user input only (text, attachments, duration)
 * - Mode is a domain concept, not UI state
 * - No feature-specific flags (no isNoteExpanded, etc.)
 * - UI adapts based on mode, not vice versa
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
 * // Switch to step mode
 * state = state.copy(mode = ComposerMode.STEP)
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
    val mode: ComposerMode = ComposerMode.ACTIVITY
) {
    // ════════════════════════════════════════════════════════════════
    // Draft Conversion
    // ════════════════════════════════════════════════════════════════

    /**
     * Convert state to ActivityDraft for submission.
     * Used when mode == ACTIVITY.
     *
     * Creates a simple activity with text, attachments, and duration.
     */
    fun toActivityDraft(): ActivityDraft = ActivityDraft(
        text = text.ifBlank { null },
        attachments = attachments,
        durationMinutes = durationMinutes
    )

    /**
     * Convert state to StepDraft for submission.
     * Used when mode == STEP.
     *
     * The step title comes from the text field.
     * Initial activities are created from attachments.
     *
     * Example:
     * State:
     *   text="Design UI"
     *   attachments=[Image(uri)]
     *   mode=STEP
     *
     * Produces:
     * StepDraft(
     *   title="Design UI",
     *   initialActivities=[
     *     ActivityDraft(
     *       attachments=[Image(uri)]
     *     )
     *   ]
     * )
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

    // ════════════════════════════════════════════════════════════════
    // State Validation
    // ════════════════════════════════════════════════════════════════

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

    // ════════════════════════════════════════════════════════════════
    // Mode Helpers
    // ════════════════════════════════════════════════════════════════

    /**
     * Check if current mode is STEP.
     */
    fun isStepMode(): Boolean = mode == ComposerMode.STEP

    /**
     * Check if current mode is ACTIVITY.
     */
    fun isActivityMode(): Boolean = mode == ComposerMode.ACTIVITY

    companion object {
        /**
         * Default empty state (ACTIVITY mode).
         */
        val EMPTY = ActivityComposerState()
    }
}
