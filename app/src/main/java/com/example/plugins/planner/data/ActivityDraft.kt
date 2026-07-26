package com.example.plugins.planner.data

/**
 * ActivityDraft — Rich content container for creating a new activity.
 *
 * Architecture (Phase 4.7.1):
 * - Generic model: supports text + attachments + duration + intent
 * - Decouples content from event types
 * - Future-ready for Telegram-style unified composer
 *
 * Example:
 * ```
 * ActivityDraft(
 *     text = "جلسه طراحی UI",
 *     attachments = [ImageAttachment(uri)],
 *     durationMinutes = 60,
 *     intent = ACTIVITY
 * )
 * ```
 *
 * Mapping to ActivityEventType:
 * - intent == STEP → STEP_CREATED
 * - durationMinutes != null → MANUAL_ACTIVITY
 * - else → NOTE_ADDED
 *
 * Attachments are NOT separate events.
 * They belong to the activity payload.
 */
data class ActivityDraft(
    val text: String? = null,
    val attachments: List<ActivityAttachment> = emptyList(),
    val durationMinutes: Int? = null,
    val intent: ActivityIntent = ActivityIntent.ACTIVITY,
    val stepId: Int? = null
)
