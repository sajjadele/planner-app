package com.example.plugins.planner.ui.composer

import com.example.plugins.planner.data.ActivityIntent

/**
 * ActivityComposerReducer — Pure function for state transitions.
 *
 * Architecture (Phase 4.7.3):
 * - Pure Kotlin, no Android dependencies
 * - Takes current state + action → returns new state
 * - No side effects, no mutations
 * - Testable in isolation
 *
 * Usage:
 * ```
 * val newState = ActivityComposerReducer.reduce(
 *     currentState,
 *     ActivityComposerAction.TextChanged("Hello")
 * )
 * ```
 */
object ActivityComposerReducer {

    /**
     * Reduce current state with an action to produce new state.
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

            is ActivityComposerAction.ConvertToStep -> {
                state.copy(intent = ActivityIntent.STEP)
            }

            is ActivityComposerAction.ConvertToActivity -> {
                state.copy(intent = ActivityIntent.ACTIVITY)
            }

            is ActivityComposerAction.Reset -> {
                ActivityComposerState.EMPTY
            }
        }
    }
}
