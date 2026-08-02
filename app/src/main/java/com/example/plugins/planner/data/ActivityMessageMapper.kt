package com.example.plugins.planner.data

/**
 * ActivityMessageMapper — Converts ActivityEventEntity to ActivityMessageModel.
 *
 * Phase 4.12: Activity Message Domain Refactor
 *
 * Responsibilities:
 * - Decode ActivityPayload JSON via ActivityPayloadCodec
 * - Extract text, attachments, duration
 * - Handle legacy formats (uri:::description, title|duration, plain text)
 * - **Filter out system events** — they must never reach the UI
 * - Never expose raw JSON or internal event types to UI
 * - Return null for system events and unrecognizable formats
 *
 * System events (invisible to UI):
 * - STEP_CREATED, STEP_COMPLETED, STEP_REOPENED, STEP_DELETED — debug-only
 *
 * User messages (visible in UI):
 * - NOTE_ADDED
 * - IMAGE_ADDED
 * - FILE_ADDED
 * - MANUAL_ACTIVITY
 *
 * Usage:
 * ```kotlin
 * // Single event
 * val message = ActivityMessageMapper.toMessage(entity)
 * // message is null for system events
 *
 * // Batch conversion — automatically filters system events
 * val messages = entities.mapNotNull { ActivityMessageMapper.toMessage(it) }
 * ```
 */
object ActivityMessageMapper {

    // ════════════════════════════════════════════════════════════════
    // System events — should NEVER appear as user-facing messages
    // ════════════════════════════════════════════════════════════════

    private val SYSTEM_EVENTS = setOf(
        ActivityEventType.STEP_CREATED,
        ActivityEventType.STEP_COMPLETED,
        ActivityEventType.STEP_REOPENED,
        ActivityEventType.STEP_DELETED
    )

    // ════════════════════════════════════════════════════════════════
    // Public API
    // ════════════════════════════════════════════════════════════════

    /**
     * Convert a single ActivityEventEntity to ActivityMessageModel.
     *
     * @return ActivityMessageModel for user messages, null for system events or unrecognized types
     */
    fun toMessage(entity: ActivityEventEntity): ActivityMessageModel? {
        val eventType = parseEventType(entity.eventType) ?: return null

        // System events are invisible to the user — return null
        if (eventType in SYSTEM_EVENTS) return null

        // Decode JSON payload (new format) or legacy format
        val payload = ActivityPayloadCodec.decode(entity.description)

        // Extract fields from payload or legacy description
        val text = extractText(payload, entity.description, eventType)
        val attachments = payload?.attachments ?: emptyList()
        val durationMinutes = extractDuration(payload, entity.description, eventType)
        val replyToMessageId = payload?.replyToMessageId

        return ActivityMessageModel(
            id = entity.id.toLong(),
            taskId = entity.taskId.toLong(),
            stepId = entity.stepId?.toLong(),
            text = text,
            attachments = attachments,
            durationMinutes = durationMinutes,
            createdAt = entity.timestamp,
            canEdit = true,
            canDelete = true,
            isDeleted = false,
            replyToMessageId = replyToMessageId
        )
    }

    /**
     * Convert a list of ActivityEventEntity to ActivityMessageModel list.
     * Automatically filters system events (returns only user messages).
     */
    fun toMessages(entities: List<ActivityEventEntity>): List<ActivityMessageModel> {
        return entities.mapNotNull { toMessage(it) }
    }

    // ════════════════════════════════════════════════════════════════
    // Private helpers
    // ════════════════════════════════════════════════════════════════

    private fun parseEventType(raw: String): ActivityEventType? {
        return try {
            ActivityEventType.valueOf(raw)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract text content from payload or legacy format.
     *
     * CRITICAL: When payload is non-null (JSON was successfully decoded),
     * ONLY use payload.text. Never fall back to raw description string
     * which may contain JSON, URI, or internal metadata.
     */
    private fun extractText(
        payload: ActivityPayload?,
        description: String?,
        eventType: ActivityEventType
    ): String? {
        // If payload was decoded from JSON, only use payload fields — never raw description
        if (payload != null) {
            return payload.text  // null if no text (image-only message), never raw JSON
        }

        // Legacy format handling (description is NOT JSON)
        return when (eventType) {
            ActivityEventType.MANUAL_ACTIVITY -> {
                // Legacy: "title|duration" — take the title part
                description?.split("|")?.firstOrNull()?.ifBlank { null }
            }
            ActivityEventType.IMAGE_ADDED -> {
                // Legacy: "uri:::description" — take the description part
                description?.split(":::")?.getOrNull(1)?.ifBlank { null }
            }
            ActivityEventType.NOTE_ADDED,
            ActivityEventType.FILE_ADDED -> {
                // Plain text note or file name
                description?.ifBlank { null }
            }
            else -> description?.ifBlank { null }
        }
    }

    /**
     * Extract duration in minutes from payload or legacy format.
     *
     * CRITICAL: When payload is non-null (JSON was successfully decoded),
     * ONLY use payload.durationMinutes. Never fall back to raw description.
     */
    private fun extractDuration(
        payload: ActivityPayload?,
        description: String?,
        eventType: ActivityEventType
    ): Int? {
        // If payload was decoded from JSON, only use payload fields
        if (payload != null) {
            return payload.durationMinutes
        }

        // Legacy format: "title|duration"
        if (eventType == ActivityEventType.MANUAL_ACTIVITY) {
            return description?.split("|")?.getOrNull(1)?.toIntOrNull()
        }

        return null
    }
}
