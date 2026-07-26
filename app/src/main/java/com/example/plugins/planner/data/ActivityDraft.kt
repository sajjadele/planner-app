package com.example.plugins.planner.data

/**
 * ActivityDraft — Unified model for creating a new activity event.
 *
 * This model abstracts the activity creation process from the UI layer.
 * In the future, this will be the base for the Telegram-style Unified Composer.
 *
 * Current usage:
 * - STEP → STEP_CREATED event
 * - NOTE → NOTE_ADDED event
 * - MANUAL_ACTIVITY → MANUAL_ACTIVITY event
 * - IMAGE → IMAGE_ADDED event
 *
 * Future usage:
 * - Will support multiple attachments (images, files)
 * - Will support combining text + image + duration in one event
 * - Will be the input for ActivityDraft.toEntity()
 */
data class ActivityDraft(
    val type: ActivityDraftType,
    val text: String? = null,
    val imageUri: String? = null,
    val imageDescription: String? = null,
    val durationMinutes: Int? = null
)

/**
 * Types of activities that can be created.
 * Currently maps 1:1 to ActivityEventType, but will evolve.
 */
enum class ActivityDraftType {
    STEP,
    NOTE,
    MANUAL_ACTIVITY,
    IMAGE
}
