package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.StepDraft

/**
 * ActivityComposerState — Single source of truth for the unified composer.
 *
 * Architecture:
 * - One state object drives the entire composer UI
 * - Mode determines what the user is creating
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
 * // Submit as step (tag only, no initial activities)
 * val stepDraft = state.toStepDraft()
 * ```
 */
data class ActivityComposerState(
    val text: String = "",
    val attachments: List<ActivityAttachment> = emptyList(),
    val durationMinutes: Int? = null,
    val mode: ComposerMode = ComposerMode.ACTIVITY,
    val existingMessageId: Long? = null,
    val replyToMessageId: Long? = null
) {
    // ════════════════════════════════════════════════════════════════
    // Draft Conversion
    // ════════════════════════════════════════════════════════════════

    /**
     * Convert state to ActivityDraft for submission.
     * Used when mode == ACTIVITY / EDIT / REPLY.
     */
    fun toActivityDraft(): ActivityDraft = ActivityDraft(
        text = text.ifBlank { null },
        attachments = attachments,
        durationMinutes = durationMinutes,
        replyToMessageId = replyToMessageId
    )

    /**
     * Convert to StepDraft — pure tag creation, no initial activities.
     *
     * Phase 5.5d: Step is metadata/tag only.
     * Only the title text is used as the step/tag name.
     * Attachments and duration are NOT used for tag creation.
     */
    fun toStepDraft(): StepDraft {
        return StepDraft(
            title = text.ifBlank { "" }
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
        return when (mode) {
            ComposerMode.STEP -> text.isNotBlank()  // tag only needs a name
            else -> hasContent()                     // activity needs text/attachments/duration
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Mode Helpers
    // ════════════════════════════════════════════════════════════════

    fun isStepMode(): Boolean = mode == ComposerMode.STEP
    fun isActivityMode(): Boolean = mode == ComposerMode.ACTIVITY
    fun isEditMode(): Boolean = mode == ComposerMode.EDIT
    fun isReplyMode(): Boolean = mode == ComposerMode.REPLY

    /**
     * Clear interaction context when switching away from EDIT/REPLY.
     */
    fun clearInteraction(): ActivityComposerState = copy(
        mode = ComposerMode.ACTIVITY,
        existingMessageId = null,
        replyToMessageId = null
    )

    companion object {
        val EMPTY = ActivityComposerState()
    }
}
