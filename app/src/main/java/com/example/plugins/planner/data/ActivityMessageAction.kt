package com.example.plugins.planner.data

/**
 * ActivityMessageAction — Represents an interaction the user can perform on a message.
 *
 * Phase 4.14: Identity & Interaction Foundation
 *
 * Sealed class so all possible actions are type-safe and exhaustive.
 * UI layer observes these actions and triggers the appropriate behavior.
 *
 * Example:
 * ```kotlin
 * when (action) {
 *     is ActivityMessageAction.Edit -> { openEditor(action.messageId) }
 *     is ActivityMessageAction.Delete -> { showDeleteConfirmation(action.messageId) }
 *     is ActivityMessageAction.Reply -> { openReplyComposer(action.messageId) }
 * }
 * ```
 *
 * This is the foundation for Phase 5+ interaction layer.
 * UI components will emit these actions; ViewModel/repository will handle execution.
 */
sealed class ActivityMessageAction {

    /** Edit a message by its ID. */
    data class Edit(
        val messageId: Long
    ) : ActivityMessageAction()

    /** Delete a message by its ID (soft delete). */
    data class Delete(
        val messageId: Long
    ) : ActivityMessageAction()

    /** Reply to a message by its ID. */
    data class Reply(
        val messageId: Long
    ) : ActivityMessageAction()
}
