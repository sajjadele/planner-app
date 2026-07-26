package com.example.plugins.planner.data

/**
 * ActivityMessageModel — UI-independent read model for activity events.
 *
 * Architecture (Phase 4.8.2):
 * - Decouples UI from database entities
 * - Decouples UI from raw event types (NOTE_ADDED, IMAGE_ADDED, etc.)
 * - Provides clean, typed access to activity data
 * - Supports future Telegram-style unified UI
 *
 * Flow:
 * ```
 * ActivityEventEntity
 *       ↓
 *   Mapper (ActivityMessageMapper)
 *       ↓
 *   ActivityMessageModel
 *       ↓
 *   Future UI
 * ```
 *
 * Usage:
 * ```
 * val model = ActivityMessageMapper.toMessage(event)
 * if (model.isStep) { ... }
 * if (model.attachments.isNotEmpty()) { ... }
 * ```
 */
data class ActivityMessageModel(
    /**
     * Unique identifier from ActivityEventEntity.
     */
    val id: Long,

    /**
     * Text content of the activity.
     * Null if no text was provided.
     */
    val text: String? = null,

    /**
     * List of attached files/images.
     * Empty if no attachments.
     */
    val attachments: List<ActivityAttachment> = emptyList(),

    /**
     * Duration in minutes.
     * Null if no duration was set.
     */
    val durationMinutes: Int? = null,

    /**
     * Timestamp of the activity event.
     */
    val timestamp: Long,

    /**
     * Whether this activity is a step (task).
     */
    val isStep: Boolean = false,

    /**
     * Whether this step is completed.
     */
    val isCompleted: Boolean = false,

    /**
     * The raw event type string for analytics/debugging.
     * UI should NOT use this for display logic.
     */
    val eventTypeRaw: String = ""
) {
    /**
     * Check if this activity has any content.
     */
    fun hasContent(): Boolean {
        return text != null || attachments.isNotEmpty() || durationMinutes != null
    }

    /**
     * Get a summary text for display.
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()
        text?.let { parts.add(it) }
        if (attachments.isNotEmpty()) {
            parts.add("${attachments.size} فایل پیوست")
        }
        durationMinutes?.let {
            parts.add("${it} دقیقه")
        }
        return parts.joinToString(" • ").ifEmpty { "بدون محتوا" }
    }

    companion object {
        /**
         * Empty model for default state.
         */
        val EMPTY = ActivityMessageModel(
            id = 0,
            timestamp = System.currentTimeMillis()
        )
    }
}
