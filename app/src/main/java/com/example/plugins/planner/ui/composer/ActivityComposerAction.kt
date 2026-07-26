package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityAttachment

/**
 * ActivityComposerAction — All possible actions in the unified composer.
 *
 * Architecture (Phase 4.7.3):
 * - Sealed class for type-safe actions
 * - Dispatched to reducer to produce new state
 * - Replaces individual boolean flags and direct state mutations
 *
 * Usage:
 * ```
 * dispatch(ActivityComposerAction.TextChanged("Hello"))
 * dispatch(ActivityComposerAction.AddAttachment(Image(uri)))
 * dispatch(ActivityComposerAction.ConvertToStep)
 * ```
 */
sealed class ActivityComposerAction {

    /**
     * Text content changed.
     */
    data class TextChanged(
        val value: String
    ) : ActivityComposerAction()

    /**
     * Add an attachment (image, file, etc).
     */
    data class AddAttachment(
        val attachment: ActivityAttachment
    ) : ActivityComposerAction()

    /**
     * Remove an attachment.
     */
    data class RemoveAttachment(
        val attachment: ActivityAttachment
    ) : ActivityComposerAction()

    /**
     * Duration changed.
     */
    data class DurationChanged(
        val minutes: Int?
    ) : ActivityComposerAction()

    /**
     * Convert to step (changes intent to STEP).
     */
    data object ConvertToStep : ActivityComposerAction()

    /**
     * Convert back to normal activity (changes intent to ACTIVITY).
     */
    data object ConvertToActivity : ActivityComposerAction()

    /**
     * Reset composer to default state.
     */
    data object Reset : ActivityComposerAction()
}
