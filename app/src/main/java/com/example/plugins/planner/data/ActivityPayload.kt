package com.example.plugins.planner.data

/**
 * ActivityPayload — Rich content container for activity data.
 *
 * Architecture (Phase 4.7.2):
 * - Immutable data class
 * - Pure Kotlin, no Android dependencies
 * - Encodes/decodes to JSON for storage in ActivityEventEntity.description
 *
 * This is the foundation for evolving from:
 * "Event types with special cases"
 * to:
 * "Rich Activity objects with content + attachments + metadata"
 *
 * Storage format: JSON string in ActivityEventEntity.description
 * Example:
 * ```json
 * {
 *   "text": "طراحی صفحه اصلی",
 *   "durationMinutes": 90,
 *   "attachments": [
 *     {
 *       "type": "IMAGE",
 *       "uri": "content://image/test.jpg"
 *     }
 *   ]
 * }
 * ```
 */
data class ActivityPayload(
    val text: String? = null,
    val attachments: List<ActivityAttachment> = emptyList(),
    val durationMinutes: Int? = null,
    val replyToMessageId: Long? = null
)
