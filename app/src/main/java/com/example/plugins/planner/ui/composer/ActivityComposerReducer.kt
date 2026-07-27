package com.example.plugins.planner.ui.composer

/**
 * ActivityComposerReducer — Pure function for state transitions.
 *
 * Phase 4.11.1: Unified Composer State
 * Phase 4.16: Activity Interaction Foundation (EDIT/REPLY modes)
 *
 * Architecture:
 * - Pure Kotlin, no Android dependencies
 * - Takes current state + action → returns new state
 * - No side effects, no mutations
 * - Testable in isolation
 *
 * Design Principles:
 * - Reducer is pure and deterministic
 * - Same input always produces same output
 * - No logging (removed for production)
 * - No side effects (no file I/O, no network)
 * - EDIT/REPLY transitions are placeholders for Phase 5
 *
 * Usage:
 * ```kotlin
 * val newState = ActivityComposerReducer.reduce(
 *     currentState,
 *     ActivityComposerAction.TextChanged("Hello")
 * )
 * ```
 */
object ActivityComposerReducer {

    /**
     * Reduce current state with an action to produce new state.
     *
     * @param state Current composer state
     * @param action User action to process
     * @return New state after applying action
     */
    fun reduce(
        state: ActivityComposerState,
        action: ActivityComposerAction
    ): ActivityComposerState {
        return when (action) {
            is ActivityComposerAction.TextChanged -> {
                state.copy(text = action.value)
            }

            is ActivityComposerAction.AddAttachment -> {
                state.copy(
                    attachments = state.attachments + action.attachment
                )
            }

            is ActivityComposerAction.RemoveAttachment -> {
                state.copy(
                    attachments = state.attachments.filter { it != action.attachment }
                )
            }

            is ActivityComposerAction.DurationChanged -> {
                state.copy(durationMinutes = action.minutes)
            }

            is ActivityComposerAction.StartEdit -> {
                state.copy(
                    mode = ComposerMode.EDIT,
                    existingMessageId = action.messageId
                )
            }

            is ActivityComposerAction.StartReply -> {
                state.copy(
                    mode = ComposerMode.REPLY,
                    replyToMessageId = action.messageId
                )
            }

            is ActivityComposerAction.CancelInteraction -> {
                state.clearInteraction()
            }

            is ActivityComposerAction.Reset -> {
                ActivityComposerState.EMPTY
            }
        }
    }
}
