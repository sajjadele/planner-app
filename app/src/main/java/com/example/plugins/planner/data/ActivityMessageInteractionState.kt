package com.example.plugins.planner.data

/**
 * ActivityMessageInteractionState — UI state for message interactions.
 *
 * Phase 5.3: Telegram-style Message Experience Polish
 *
 * Tracks which message is currently selected/long-pressed.
 * Used by ActivityMessageCard to show visual selection state.
 *
 * Default state means no message is selected (normal feed view).
 */
data class ActivityMessageInteractionState(
    /** ID of the currently selected message, or null if none selected. */
    val selectedMessageId: Long? = null
) {
    companion object {
        val DEFAULT = ActivityMessageInteractionState()
    }
}
