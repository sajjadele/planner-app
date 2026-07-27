package com.example.plugins.planner.data

/**
 * ActivityDraft — Content container for creating a new Activity.
 *
 * Phase 4.10.1: Domain Separation
 *
 * Responsibility:
 * - Represents activity content ONLY
 * - Does NOT contain Step logic (intent, stepId)
 * - Step creation is handled by StepDraft
 *
 * Example:
 * ```kotlin
 * ActivityDraft(
 *     text = "جلسه طراحی UI",
 *     attachments = [ActivityAttachment.Image(uri)],
 *     durationMinutes = 60
 * )
 * ```
 *
 * Mapping to ActivityEventType:
 * - durationMinutes != null → MANUAL_ACTIVITY
 * - else → NOTE_ADDED
 *
 * Attachments are NOT separate events.
 * They belong to the activity payload.
 */
data class ActivityDraft(
    /**
     * Text content of the activity.
     */
    val text: String? = null,

    /**
     * Attached files/images.
     */
    val attachments: List<ActivityAttachment> = emptyList(),

    /**
     * Duration in minutes (for manual activity tracking).
     */
    val durationMinutes: Int? = null,

    /**
     * ID of the message this replies to. Null for top-level messages.
     */
    val replyToMessageId: Long? = null
) {
    /**
     * Check if this draft has any content.
     */
    fun hasContent(): Boolean {
        return text != null || attachments.isNotEmpty() || durationMinutes != null || replyToMessageId != null
    }

    /**
     * Check if this draft has image attachments.
     */
    fun hasImages(): Boolean {
        return attachments.any { it is ActivityAttachment.Image }
    }

    companion object {
        /**
         * Empty draft for default state.
         */
        val EMPTY = ActivityDraft()
    }
}
