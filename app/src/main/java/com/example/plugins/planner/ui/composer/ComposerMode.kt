package com.example.plugins.planner.ui.composer

/**
 * ComposerMode — Represents the user's creation or interaction intent.
 *
 * Phase 5.9.1: Remove STEP mode — Composer is Activity-only.
 * Tag creation is handled via dedicated dialog, not the composer.
 *
 * Modes:
 * - ACTIVITY: Normal activity message (note, image, duration)
 * - EDIT: Editing an existing message
 * - REPLY: Replying to an existing message
 *
 * Design:
 * - Mode determines which draft type to create
 * - UI adapts based on mode
 * - EDIT and REPLY carry context via existingMessageId / replyToMessageId in state
 */
enum class ComposerMode {
    ACTIVITY,
    EDIT,
    REPLY
}
