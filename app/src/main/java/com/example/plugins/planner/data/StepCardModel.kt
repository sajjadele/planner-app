package com.example.plugins.planner.data

/**
 * StepCardModel — UI-independent model for rendering a Step card.
 *
 * Architecture (Phase 4.9.1):
 * - Dedicated model for rich step card UI
 * - Contains aggregated activity data
 * - Supports expandable/collapsed states
 * - Decoupled from database entities
 *
 * Future UI Example:
 * ```
 * ╭──────────────────────────╮
 * │ ☑️ طراحی صفحه اصلی         │
 * │                          │
 * │ 3 فعالیت                  │
 * │                          │
 * │ ┌──────────────────────┐ │
 * │ │ 📝 توضیح اولیه        │ │
 * │ └──────────────────────┘ │
 * │                          │
 * │ ┌──────────────────────┐ │
 * │ │ 📷 image thumbnail   │ │
 * │ └──────────────────────┘ │
 * │                          │
 * │ ⏱️ 90 دقیقه               │
 * ╰──────────────────────────╯
 * ```
 */
data class StepCardModel(
    /**
     * Unique identifier for the step.
     */
    val id: Long,

    /**
     * Title of the step.
     */
    val title: String,

    /**
     * Whether this step is completed.
     */
    val isCompleted: Boolean = false,

    /**
     * List of activity messages associated with this step.
     * Sorted ascending by timestamp.
     */
    val messages: List<ActivityMessageModel> = emptyList(),

    /**
     * Total number of attachments across all messages.
     */
    val attachmentCount: Int = 0,

    /**
     * Total duration in minutes from all manual activities.
     * Null if no duration was recorded.
     */
    val totalDurationMinutes: Int? = null,

    /**
     * Timestamp when the step was created.
     */
    val createdAt: Long
) {
    /**
     * Get all image attachments from all messages.
     */
    fun getAllImages(): List<ActivityAttachment.Image> {
        return messages.flatMap { msg ->
            msg.attachments.filterIsInstance<ActivityAttachment.Image>()
        }
    }

    /**
     * Get a summary of the step's activity.
     */
    fun getActivitySummary(): String {
        val parts = mutableListOf<String>()

        if (messages.isNotEmpty()) {
            parts.add("${messages.size} فعالیت")
        }

        attachmentCount.let {
            if (it > 0) parts.add("$it فایل پیوست")
        }

        totalDurationMinutes?.let {
            parts.add("$it دقیقه")
        }

        return parts.joinToString(" • ").ifEmpty { "بدون فعالیت" }
    }

    /**
     * Check if the step has any rich content.
     */
    fun hasRichContent(): Boolean {
        return messages.isNotEmpty() || attachmentCount > 0 || totalDurationMinutes != null
    }

    companion object {
        /**
         * Empty model for default state.
         */
        val EMPTY = StepCardModel(
            id = 0,
            title = "",
            createdAt = System.currentTimeMillis()
        )
    }
}