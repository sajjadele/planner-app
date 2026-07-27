package com.example.plugins.planner.data

/**
 * ActivityMessageAction — Represents an interaction the user can perform on a message.
 *
 * Phase 4.14: Identity & Interaction Foundation
 * Phase 5.3: Telegram-style Message Experience Polish (added ReplyNavigation)
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
 *     is ActivityMessageAction.ReplyNavigation -> { scrollToMessage(action.messageId) }
 * }
 * ```
 */
sealed class ActivityMessageAction {

    /** Edit a message by its ID. */
    data class Edit(
        val messageId: Long
    ) : ActivityMessageAction()

    /** Delete a message by its ID (hard delete). */
    data class Delete(
        val messageId: Long
    ) : ActivityMessageAction()

    /** Reply to a message by its ID. */
    data class Reply(
        val messageId: Long
    ) : ActivityMessageAction()

    /**
     * Navigate to the original message that this message is replying to.
     * Triggers scroll-to-message behavior in the feed.
     */
    data class ReplyNavigation(
        val messageId: Long
    ) : ActivityMessageAction()
}
