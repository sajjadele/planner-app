package com.example.plugins.planner.data

/**
 * StepPreviewModel — Read model for step card UI.
 *
 * Architecture (Phase 4.8.4):
 * - Dedicated model for step/task preview cards
 * - Contains payload for rich content (attachments, duration)
 * - Supports future Telegram-style step cards
 *
 * Future UI Example:
 * ```
 * ┌─────────────────────┐
 * │ ☐ Design homepage   │
 * │                     │
 * │ 📷 mockup.png       │
 * │ 📝 description      │
 * │ ⏱️ 90 minutes        │
 * └─────────────────────┘
 * ```
 *
 * Usage:
 * ```
 * val preview = StepPreviewMapper.toPreview(task, activities)
 * if (preview.payload?.attachments?.isNotEmpty() == true) {
 *     // Show attachments
 * }
 * ```
 */
data class StepPreviewModel(
    /**
     * Unique identifier for the step.
     */
    val id: Long,

    /**
     * Title of the step.
     */
    val title: String,

    /**
     * Associated activity payload.
     * Contains attachments, duration, etc.
     */
    val payload: ActivityPayload? = null,

    /**
     * Whether this step is completed.
     */
    val completed: Boolean = false,

    /**
     * Timestamp of the step.
     */
    val timestamp: Long,

    /**
     * Goal ID if this step is linked to a goal.
     */
    val goalId: Int? = null,

    /**
     * Priority level.
     */
    val priority: String? = null
) {
    /**
     * Get a summary of the step content.
     */
    fun getContentSummary(): String {
        val parts = mutableListOf<String>()
        payload?.text?.let { parts.add(it) }
        payload?.attachments?.size?.let {
            if (it > 0) parts.add("$it فایل پیوست")
        }
        payload?.durationMinutes?.let {
            parts.add("$it دقیقه")
        }
        return parts.joinToString(" • ").ifEmpty { title }
    }

    /**
     * Check if the step has rich content (attachments, duration, etc).
     */
    fun hasRichContent(): Boolean {
        return payload?.attachments?.isNotEmpty() == true ||
                payload?.durationMinutes != null
    }

    companion object {
        /**
         * Empty model for default state.
         */
        val EMPTY = StepPreviewModel(
            id = 0,
            title = "",
            timestamp = System.currentTimeMillis()
        )
    }
}
