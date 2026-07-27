package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment
import com.example.plugins.planner.data.ActivityDraft
import com.example.plugins.planner.data.StepDraft

/**
 * ActivityComposerState — Single source of truth for the unified composer.
 *
 * Phase 5.9.1: Remove STEP mode — Composer is Activity-only.
 * Removed: toStepDraft(), isStepMode(), step-specific canSubmit().
 *
 * Architecture:
 * - One state object drives the entire composer UI
 * - Mode determines what the user is creating
 * - Only converts to ActivityDraft (no StepDraft)
 *
 * Design Principles:
 * - Store user input only (text, attachments, duration)
 * - Mode is a domain concept, not UI state
 * - Composer is Activity-only — tag creation uses dedicated dialog
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
     */
    fun toActivityDraft(): ActivityDraft = ActivityDraft(
        text = text.ifBlank { null },
        attachments = attachments,
        durationMinutes = durationMinutes,
        replyToMessageId = replyToMessageId
    )

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
    fun canSubmit(): Boolean = hasContent()

    // ════════════════════════════════════════════════════════════════
    // Mode Helpers
    // ════════════════════════════════════════════════════════════════

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
