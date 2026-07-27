package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment

/**
 * ActivityComposerAction — All possible actions in the unified composer.
 *
 * Phase 5.9.1: Remove ConvertToStep/ConvertToActivity — Composer is Activity-only.
 *
 * Architecture:
 * - Sealed class for type-safe actions
 * - Dispatched to reducer to produce new state
 * - Describes user intent, not UI operations
 *
 * Design Principles:
 * - Actions describe WHAT the user wants to do
 * - Actions do NOT describe HOW the UI should change
 * - No UI-specific actions (no OpenImageSection, ExpandNote, etc.)
 * - EDIT/REPLY actions handle interaction modes
 *
 * Usage:
 * ```kotlin
 * dispatch(ActivityComposerAction.TextChanged("Hello"))
 * dispatch(ActivityComposerAction.AddAttachment(Image(uri)))
 * dispatch(ActivityComposerAction.StartEdit(messageId))
 * dispatch(ActivityComposerAction.StartReply(messageId))
 * ```
 */
sealed class ActivityComposerAction {

    /**
     * Text content changed.
     * Updates the text field in the composer.
     */
    data class TextChanged(
        val value: String
    ) : ActivityComposerAction()

    /**
     * Add an attachment (image, file, etc).
     * Appends to the attachments list.
     */
    data class AddAttachment(
        val attachment: ActivityAttachment
    ) : ActivityComposerAction()

    /**
     * Remove an attachment.
     * Removes from the attachments list.
     */
    data class RemoveAttachment(
        val attachment: ActivityAttachment
    ) : ActivityComposerAction()

    /**
     * Duration changed.
     * Updates the duration in minutes (null to clear).
     */
    data class DurationChanged(
        val minutes: Int?
    ) : ActivityComposerAction()

    /**
     * Start editing an existing message.
     * Pre-populates composer with message content.
     */
    data class StartEdit(
        val messageId: Long
    ) : ActivityComposerAction()

    /**
     * Start replying to an existing message.
     * Opens composer in REPLY mode.
     */
    data class StartReply(
        val messageId: Long
    ) : ActivityComposerAction()

    /**
     * Cancel current interaction (EDIT or REPLY).
     * Resets composer to ACTIVITY mode without submitting.
     */
    data object CancelInteraction : ActivityComposerAction()

    /**
     * Reset composer to default state.
     * Clears all fields and resets to ACTIVITY mode.
     */
    data object Reset : ActivityComposerAction()
}
