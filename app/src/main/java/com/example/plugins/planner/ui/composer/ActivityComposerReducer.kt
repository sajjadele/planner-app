package com.example.plugins.planner.ui.composer

/**
 * ActivityComposerReducer — Pure function for state transitions.
 *
 * Phase 4.11.1: Unified Composer State
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

            is ActivityComposerAction.ConvertToStep -> {
                state.copy(mode = ComposerMode.STEP)
            }

            is ActivityComposerAction.ConvertToActivity -> {
                state.copy(mode = ComposerMode.ACTIVITY)
            }

            is ActivityComposerAction.Reset -> {
                ActivityComposerState.EMPTY
            }
        }
    }
}
