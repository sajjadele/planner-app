package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment

/**
 * ActivityComposerAction — All possible actions in the unified composer.
 *
 * Phase 4.11.1: Unified Composer State
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
 * - Mode changes are actions, not state mutations
 *
 * Usage:
 * ```kotlin
 * dispatch(ActivityComposerAction.TextChanged("Hello"))
 * dispatch(ActivityComposerAction.AddAttachment(Image(uri)))
 * dispatch(ActivityComposerAction.ConvertToStep)
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
     * Switch to STEP mode.
     * User wants to create a step with optional initial activities.
     * The text becomes the step title.
     */
    data object ConvertToStep : ActivityComposerAction()

    /**
     * Switch to ACTIVITY mode.
     * User wants to create a normal activity message.
     */
    data object ConvertToActivity : ActivityComposerAction()

    /**
     * Reset composer to default state.
     * Clears all fields and resets to ACTIVITY mode.
     */
    data object Reset : ActivityComposerAction()
}
