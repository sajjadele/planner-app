package com.example.plugins.planner.data

/**
 * ActivityAttachment — Represents a file/image attached to an activity.
 *
 * Future: Will support multiple attachments in a single activity.
 * Currently: Only Image is implemented in UI.
 */
sealed class ActivityAttachment {

    /**
     * Image attachment.
     * @param uri Content URI of the image
     */
    data class Image(
        val uri: String
    ) : ActivityAttachment()

    /**
     * File attachment (future-ready, not implemented in UI yet).
     * @param uri Content URI of the file
     * @param name Display name of the file
     */
    data class File(
        val uri: String,
        val name: String? = null
    ) : ActivityAttachment()
}
