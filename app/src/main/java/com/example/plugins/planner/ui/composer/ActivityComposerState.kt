package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.ActivityIntent

/**
 * ActivityComposerState — Single source of truth for the unified composer.
 *
 * Architecture (Phase 4.7.3):
 * - Replaces boolean expansion states (isStepExpanded, isNoteExpanded, etc.)
 * - One state object drives the entire composer UI
 * - Generates ActivityDraft when submitted
 *
 * Usage:
 * ```
 * var state by remember { mutableStateOf(ActivityComposerState()) }
 *
 * // Text change
 * state = state.copy(text = "Hello")
 *
 * // Add attachment
 * state = state.copy(attachments = state.attachments + Image(uri))
 *
 * // Submit
 * val draft = state.toDraft()
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
     */
    fun toDraft(): ActivityDraft = ActivityDraft(
        text = text.ifBlank { null },
        attachments = attachments,
        durationMinutes = durationMinutes,
        intent = intent
    )

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

    companion object {
        /**
         * Default empty state.
         */
        val EMPTY = ActivityComposerState()
    }
}
