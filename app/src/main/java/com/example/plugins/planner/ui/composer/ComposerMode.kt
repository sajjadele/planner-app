package com.example.plugins.planner.ui.composer

/**
 * ComposerMode — Represents the user's creation intent.
 *
 * Phase 4.11.1: Unified Composer State
 *
 * Responsibility:
 * - Defines what the user is currently creating
 * - Drives the composer UI and submission logic
 *
 * Modes:
 * - ACTIVITY: Normal activity message (note, image, duration)
 * - STEP: Creating a new step with optional initial activities
 *
 * Design:
 * - Only two modes (no feature-specific modes)
 * - Mode determines which draft type to create
 * - UI adapts based on mode (but mode is NOT UI state)
 */
enum class ComposerMode {
    /**
     * Normal activity message.
     * Creates: ActivityDraft
     * Examples: quick note, image capture, manual activity
     */
    ACTIVITY,

    /**
     * Creating a new step.
     * Creates: StepDraft with optional initial activities
     * The text becomes the step title.
     * Attachments become initial activities.
     */
    STEP
}
