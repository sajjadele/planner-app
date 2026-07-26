package com.example.plugins.planner.data

/**
 * ActivityDraftResolver — Converts ActivityDraft to existing ActivityEvent representation.
 *
 * Architecture (Phase 4.7.2):
 * - Pure Kotlin, no Android/Room dependencies
 * - Maps draft intent + metadata to ActivityEventType
 * - Uses ActivityPayloadCodec for encoding description
 * - Maintains backward compatibility with current storage format
 *
 * Rules:
 * - intent == STEP → STEP_CREATED
 * - else → NOTE_ADDED / MANUAL_ACTIVITY (based on duration)
 *
 * Attachments are part of the activity payload.
 * For now, we preserve existing event types for backward compatibility.
 * Future migration will simplify to single ACTIVITY_CREATED event.
 */
object ActivityDraftResolver {

    /**
     * Resolve the ActivityEventType for a given draft.
     */
    fun resolveEventType(draft: ActivityDraft): ActivityEventType {
        return when {
            draft.intent == ActivityIntent.STEP -> ActivityEventType.STEP_CREATED
            draft.durationMinutes != null -> ActivityEventType.MANUAL_ACTIVITY
            else -> ActivityEventType.NOTE_ADDED
        }
    }

    /**
     * Encode draft content into the description field.
     * Uses ActivityPayloadCodec for rich payloads.
     * Falls back to legacy format for simple cases.
     *
     * Encoding strategy:
     * - If draft has attachments → use JSON format
     * - If draft has duration → use legacy "title|duration" format
     * - Otherwise → use plain text
     */
    fun encodeDescription(draft: ActivityDraft): String? {
        // For drafts with attachments, use JSON format
        if (draft.attachments.isNotEmpty()) {
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
            durationMinutes = draft.durationMinutes
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
     * Get the step title if this draft is a STEP intent.
     * Returns null for non-STEP drafts.
     */
    fun getStepTitle(draft: ActivityDraft): String? {
        return if (draft.intent == ActivityIntent.STEP) draft.text else null
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
