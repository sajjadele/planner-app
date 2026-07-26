package com.example

import android.util.Log.plugins.planner.data

/**
 * ActivityMessageMapper — Converts ActivityEventEntity to ActivityMessageModel.
 *
 * Architecture (Phase 4.8.3):
 * - Single responsibility: Entity → Model mapping
 * - Decodes JSON payloads using ActivityPayloadCodec
 * - Falls back to legacy format for backward compatibility
 * - Never exposes raw JSON to UI
 *
 * Usage:
 * ```
 * val model = ActivityMessageMapper.toMessage(entity)
 * // or
 * val models = ActivityMessageMapper.toMessages(entities)
 * ```
 */
object ActivityMessageMapper {

    /**
     * Convert a single ActivityEventEntity to ActivityMessageModel.
     */
    fun toMessage(entity: ActivityEventEntity): ActivityMessageModel {
        val eventType = parseEventType(entity.eventType)

        // Try to decode JSON payload first
        val payload = decodePayload(entity.description)

        // Debug: Log mapping details
        Log.d("STEP_IMAGE_DEBUG", "📝 ActivityMessageMapper: entityId=${entity.id}, eventType=${entity.eventType}, stepId=${entity.stepId}")
        Log.d("STEP_IMAGE_DEBUG", "📝 Payload attachments count: ${payload?.attachments?.size ?: 0}")
        payload?.attachments?.forEachIndexed { idx, att ->
            if (att is ActivityAttachment.Image) {
                Log.d("STEP_IMAGE_DEBUG", "📝 Image[$idx]: ${att.uri}")
            }
        }

        // Extract fields based on event type
        val text = extractText(payload, entity.description, eventType)
        val attachments = payload?.attachments ?: emptyList()
        val durationMinutes = extractDuration(payload, entity, eventType)
        val isStep = eventType == ActivityEventType.STEP_CREATED
        val isCompleted = eventType == ActivityEventType.STEP_COMPLETED

        return ActivityMessageModel(
            id = entity.id.toLong(),
            text = text,
            attachments = attachments,
            durationMinutes = durationMinutes,
            timestamp = entity.timestamp,
            isStep = isStep,
            isCompleted = isCompleted,
            eventTypeRaw = entity.eventType
        )
    }

    /**
     * Convert a list of ActivityEventEntity to ActivityMessageModel list.
     */
    fun toMessages(entities: List<ActivityEventEntity>): List<ActivityMessageModel> {
        return entities.map { toMessage(it) }
    }

    /**
     * Decode JSON payload using ActivityPayloadCodec.
     * Returns null if not JSON format.
     */
    private fun decodePayload(description: String?): ActivityPayload? {
        if (description == null) return null
        return ActivityPayloadCodec.decode(description)
    }

    /**
     * Extract text based on payload and event type.
     */
    private fun extractText(
        payload: ActivityPayload?,
        description: String?,
        eventType: ActivityEventType?
    ): String? {
        // If payload has text, use it
        if (payload?.text != null) {
            return payload.text
        }

        // Legacy format handling
        return when (eventType) {
            ActivityEventType.MANUAL_ACTIVITY -> {
                // Legacy: "title|duration"
                description?.split("|")?.firstOrNull()
            }
            ActivityEventType.IMAGE_ADDED -> {
                // Legacy: "uri:::description"
                description?.split(":::")?.getOrNull(1)
            }
            else -> description
        }
    }

    /**
     * Extract duration based on payload and event type.
     */
    private fun extractDuration(
        payload: ActivityPayload?,
        entity: ActivityEventEntity,
        eventType: ActivityEventType?
    ): Int? {
        // If payload has duration, use it
        if (payload?.durationMinutes != null) {
            return payload.durationMinutes
        }

        // Legacy format: "title|duration"
        if (eventType == ActivityEventType.MANUAL_ACTIVITY) {
            return entity.description?.split("|")?.getOrNull(1)?.toIntOrNull()
        }

        return null
    }

    /**
     * Parse event type string to ActivityEventType enum.
     */
    private fun parseEventType(raw: String): ActivityEventType? {
        return try {
            ActivityEventType.valueOf(raw)
        } catch (_: Exception) {
            null
        }
    }
}
