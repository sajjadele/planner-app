package com.example.plugins.planner.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * ActivityPayloadCodec — Encode/decode ActivityPayload to/from JSON.
 *
 * Architecture (Phase 4.7.2):
 * - Uses org.json.JSONObject (available in Android)
 * - Encodes to JSON string for storage in ActivityEventEntity.description
 * - Decodes from JSON string (backward compatible with old format)
 *
 * Storage format:
 * ```json
 * {
 *   "text": "activity text",
 *   "durationMinutes": 60,
 *   "attachments": [
 *     {"type": "IMAGE", "uri": "content://..."},
 *     {"type": "FILE", "uri": "content://...", "name": "file.pdf"}
 *   ]
 * }
 * ```
 *
 * Backward compatibility:
 * - Old format: "uri:::description" (ImageEventParser)
 * - Old format: "title|duration" (ManualActivity)
 * - Old format: plain text (Note)
 * - New format: JSON (ActivityPayload)
 *
 * The codec handles all formats when decoding.
 */
object ActivityPayloadCodec {

    private const val KEY_TEXT = "text"
    private const val KEY_DURATION = "durationMinutes"
    private const val KEY_ATTACHMENTS = "attachments"
    private const val KEY_TYPE = "type"
    private const val KEY_URI = "uri"
    private const val KEY_NAME = "name"

    private const val TYPE_IMAGE = "IMAGE"
    private const val TYPE_FILE = "FILE"

    /**
     * Encode ActivityPayload to JSON string.
     * Returns null if payload is empty.
     */
    fun encode(payload: ActivityPayload): String? {
        // Don't encode empty payloads
        if (payload.text == null && payload.attachments.isEmpty() && payload.durationMinutes == null) {
            return null
        }

        val json = JSONObject()

        // Text
        if (payload.text != null) {
            json.put(KEY_TEXT, payload.text)
        }

        // Duration
        if (payload.durationMinutes != null) {
            json.put(KEY_DURATION, payload.durationMinutes)
        }

        // Attachments
        if (payload.attachments.isNotEmpty()) {
            val attachmentsArray = JSONArray()
            for (attachment in payload.attachments) {
                val attachmentJson = JSONObject()
                when (attachment) {
                    is ActivityAttachment.Image -> {
                        attachmentJson.put(KEY_TYPE, TYPE_IMAGE)
                        attachmentJson.put(KEY_URI, attachment.uri)
                    }
                    is ActivityAttachment.File -> {
                        attachmentJson.put(KEY_TYPE, TYPE_FILE)
                        attachmentJson.put(KEY_URI, attachment.uri)
                        if (attachment.name != null) {
                            attachmentJson.put(KEY_NAME, attachment.name)
                        }
                    }
                }
                attachmentsArray.put(attachmentJson)
            }
            json.put(KEY_ATTACHMENTS, attachmentsArray)
        }

        return json.toString()
    }

    /**
     * Decode JSON string to ActivityPayload.
     * Handles both new JSON format and legacy formats gracefully.
     *
     * @param description The stored description string
     * @return ActivityPayload or null if empty/invalid
     */
    fun decode(description: String?): ActivityPayload? {
        if (description.isNullOrBlank()) {
            return null
        }

        // Try JSON format first
        if (description.trimStart().startsWith("{")) {
            return try {
                val json = JSONObject(description)
                decodeJsonObject(json)
            } catch (e: Exception) {
                // Invalid JSON, try legacy formats
                decodeLegacyFormat(description)
            }
        }

        // Legacy format: try to decode
        return decodeLegacyFormat(description)
    }

    /**
     * Decode from JSONObject.
     */
    private fun decodeJsonObject(json: JSONObject): ActivityPayload {
        val text = if (json.has(KEY_TEXT)) json.getString(KEY_TEXT) else null
        val duration = if (json.has(KEY_DURATION)) json.getInt(KEY_DURATION) else null

        val attachments = mutableListOf<ActivityAttachment>()
        if (json.has(KEY_ATTACHMENTS)) {
            val attachmentsArray = json.getJSONArray(KEY_ATTACHMENTS)
            for (i in 0 until attachmentsArray.length()) {
                val attachmentJson = attachmentsArray.getJSONObject(i)
                val type = attachmentJson.optString(KEY_TYPE, "")
                val uri = attachmentJson.optString(KEY_URI, "")

                if (uri.isNotBlank()) {
                    when (type) {
                        TYPE_IMAGE -> {
                            attachments.add(ActivityAttachment.Image(uri))
                        }
                        TYPE_FILE -> {
                            val name = if (attachmentJson.has(KEY_NAME)) {
                                attachmentJson.getString(KEY_NAME)
                            } else null
                            attachments.add(ActivityAttachment.File(uri, name))
                        }
                    }
                }
            }
        }

        return ActivityPayload(
            text = text,
            attachments = attachments,
            durationMinutes = duration
        )
    }

    /**
     * Decode legacy format strings.
     *
     * Legacy formats:
     * - "uri:::description" (ImageEventParser)
     * - "title|duration" (ManualActivity)
     * - plain text (Note)
     */
    private fun decodeLegacyFormat(description: String): ActivityPayload {
        // Check for image format: "uri:::description"
        if (description.contains(":::")) {
            val parts = description.split(":::", limit = 2)
            val uri = parts[0]
            val imageDescription = parts.getOrNull(1)?.ifBlank { null }

            if (uri.isNotBlank()) {
                return ActivityPayload(
                    text = imageDescription,
                    attachments = listOf(ActivityAttachment.Image(uri))
                )
            }
        }

        // Check for manual activity format: "title|duration"
        if (description.contains("|")) {
            val parts = description.split("|", limit = 2)
            val title = parts[0]
            val duration = parts.getOrNull(1)?.toIntOrNull()

            if (title.isNotBlank()) {
                return ActivityPayload(
                    text = title,
                    durationMinutes = duration
                )
            }
        }

        // Plain text (note)
        return ActivityPayload(text = description)
    }

    /**
     * Check if a description string is in JSON format.
     */
    fun isJsonFormat(description: String?): Boolean {
        return description?.trimStart()?.startsWith("{") == true
    }
}
