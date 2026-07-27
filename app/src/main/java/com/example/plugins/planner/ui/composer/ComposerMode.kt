package com.example.plugins.planner.ui.composer

/**
 * ComposerMode — Represents the user's creation or interaction intent.
 *
 * Phase 4.11.1: Unified Composer State
 * Phase 4.16: Activity Interaction Foundation
 *
 * Responsibility:
 * - Defines what the user is currently creating or modifying
 * - Drives the composer UI and submission logic
 *
 * Modes:
 * - ACTIVITY: Normal activity message (note, image, duration)
 * - STEP: Creating a new step with optional initial activities
 * - EDIT: Editing an existing message (Phase 5+)
 * - REPLY: Replying to an existing message (Phase 5+)
 *
 * Design:
 * - Mode determines which draft type to create
 * - UI adapts based on mode
 * - EDIT and REPLY carry context via existingMessageId / replyToMessageId in state
 */
enum class ComposerMode {
    ACTIVITY,
    STEP,
    EDIT,
    REPLY
}
