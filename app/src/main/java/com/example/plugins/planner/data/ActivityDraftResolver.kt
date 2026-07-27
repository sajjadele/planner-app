package com.example.plugins.planner.data

/**
 * ActivityDraftResolver — Converts ActivityDraft to ActivityEvent representation.
 *
 * Phase 4.10.1: Domain Separation
 *
 * Responsibility:
 * - Maps ActivityDraft to ActivityEventType
 * - Uses ActivityPayloadCodec for encoding description
 * - Maintains backward compatibility with storage format
 *
 * Rules:
 * - durationMinutes != null → MANUAL_ACTIVITY
 * - else → NOTE_ADDED
 *
 * Step creation is handled by StepDraftResolver.
 * This resolver only handles Activity creation.
 *
 * Attachments are part of the activity payload.
 * Future migration will simplify to single ACTIVITY_CREATED event.
 */
object ActivityDraftResolver {

    /**
     * Resolve the ActivityEventType for a given draft.
     */
    fun resolveEventType(draft: ActivityDraft): ActivityEventType {
        return if (draft.durationMinutes != null) {
            ActivityEventType.MANUAL_ACTIVITY
        } else {
            ActivityEventType.NOTE_ADDED
        }
    }

    /**
     * Encode draft content into the description field.
     * Uses ActivityPayloadCodec for rich payloads.
     * Falls back to legacy format for simple cases.
     *
     * Encoding strategy:
     * - If draft has attachments → use JSON format
     * - If draft has replyToMessageId → use JSON format
     * - If draft has duration → use legacy "title|duration" format
     * - Otherwise → use plain text
     */
    fun encodeDescription(draft: ActivityDraft): String? {
        // For drafts with complex data, use JSON format
        if (draft.attachments.isNotEmpty() || draft.replyToMessageId != null) {
            val payload = draftToPayload(draft)
            return ActivityPayloadCodec.encode(payload)
        }

        // For manual activity with duration, use legacy format
        if (draft.durationMinutes != null) {
            return if (draft.text != null) {
                "${draft.text}|${draft.durationMinutes}"
            } else {
                null
            }
        }

        // For plain text (note), use legacy format
        return draft.text
    }

    /**
     * Convert ActivityDraft to ActivityPayload for codec encoding.
     */
    private fun draftToPayload(draft: ActivityDraft): ActivityPayload {
        return ActivityPayload(
            text = draft.text,
            attachments = draft.attachments,
            durationMinutes = draft.durationMinutes,
            replyToMessageId = draft.replyToMessageId
        )
    }

    /**
     * Decode a stored description back to ActivityPayload.
     * Handles both JSON and legacy formats.
     */
    fun decodeDescription(description: String?): ActivityPayload? {
        return ActivityPayloadCodec.decode(description)
    }

    /**
     * Check if this draft has image attachments.
     */
    fun hasImageAttachment(draft: ActivityDraft): Boolean {
        return draft.attachments.any { it is ActivityAttachment.Image }
    }

    /**
     * Get the first image URI if present.
     */
    fun getImageUri(draft: ActivityDraft): String? {
        return (draft.attachments.filterIsInstance<ActivityAttachment.Image>().firstOrNull())?.uri
    }
}
