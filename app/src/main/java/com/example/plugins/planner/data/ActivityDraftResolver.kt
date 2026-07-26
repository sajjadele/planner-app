package com.example.plugins.planner.data

/**
 * ActivityDraftResolver — Converts ActivityDraft to existing ActivityEvent representation.
 *
 * Architecture (Phase 4.7.1):
 * - Pure Kotlin, no Android/Room dependencies
 * - Maps draft intent + metadata to ActivityEventType
 * - Maintains backward compatibility with current storage format
 *
 * Rules:
 * - intent == STEP → STEP_CREATED
 * - durationMinutes != null → MANUAL_ACTIVITY
 * - else → NOTE_ADDED
 *
 * Attachments are NOT separate events.
 * They are encoded in the description field for backward compatibility.
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
     * Maintains backward compatibility with current storage format.
     *
     * Format:
     * - IMAGE_ADDED: ImageEventParser.encode(uri, description)
     * - MANUAL_ACTIVITY: "title|durationMinutes"
     * - NOTE_ADDED / STEP_CREATED: plain text
     */
    fun encodeDescription(draft: ActivityDraft): String? {
        return when (resolveEventType(draft)) {
            ActivityEventType.IMAGE_ADDED -> {
                // Should not happen with new model, but handle gracefully
                val image = draft.attachments.filterIsInstance<ActivityAttachment.Image>().firstOrNull()
                if (image != null) {
                    ImageEventParser.encode(image.uri, draft.text)
                } else {
                    draft.text
                }
            }
            ActivityEventType.MANUAL_ACTIVITY -> {
                if (draft.durationMinutes != null) {
                    "${draft.text}|${draft.durationMinutes}"
                } else {
                    draft.text
                }
            }
            else -> draft.text
        }
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
