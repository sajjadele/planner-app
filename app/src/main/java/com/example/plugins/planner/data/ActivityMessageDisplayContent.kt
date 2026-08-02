package com.example.plugins.planner.data

import com.example.plugins.planner.data.ActivityAttachment.File as FileAttachment
import com.example.plugins.planner.data.ActivityAttachment.Image as ImageAttachment

/**
 * ActivityMessageDisplayContent — Classifies message content for display.
 *
 * Multi-attachment support: activities can contain multiple images + multiple files.
 *
 * Types:
 * - [TextContent]: Plain text (with optional duration)
 * - [MultiMediaContent]: Multiple images/files + optional text
 * - [EmptyMessage]: No displayable content (should not reach UI)
 * - [DeletedMessage]: Soft-deleted message
 */
sealed class ActivityMessageDisplayContent {

    /** Text-only message (note, with optional duration). */
    data class TextContent(
        val text: String,
        val durationMinutes: Int? = null
    ) : ActivityMessageDisplayContent()

    /** Multi-attachment content: images + files + optional text. */
    data class MultiMediaContent(
        val images: List<ImageAttachment>,
        val files: List<FileAttachment>,
        val text: String? = null,
        val durationMinutes: Int? = null
    ) : ActivityMessageDisplayContent()

    /** No displayable content — should never reach UI. */
    data object EmptyMessage : ActivityMessageDisplayContent()

    /** Soft-deleted message. */
    data object DeletedMessage : ActivityMessageDisplayContent()

    companion object {
        /** Maximum attachments per activity (for UI compactness). */
        const val MAX_IMAGES = 5
        const val MAX_FILES = 5

        /**
         * Resolve display content from an ActivityMessageModel.
         *
         * Guarantees: no raw JSON, no URI strings, no event types in UI.
         * Supports multiple images + multiple files per activity.
         */
        fun from(message: ActivityMessageModel): ActivityMessageDisplayContent {
            if (message.isDeleted) return DeletedMessage

            val text = message.text?.ifBlank { null }
            val images = message.attachments.filterIsInstance<ImageAttachment>()
            val files = message.attachments.filterIsInstance<FileAttachment>()

            val hasText = text != null
            val hasMedia = images.isNotEmpty() || files.isNotEmpty()
            val hasDuration = message.durationMinutes != null

            return when {
                // Media (images/files) with or without text
                hasMedia -> MultiMediaContent(
                    images = images.take(MAX_IMAGES),
                    files = files.take(MAX_FILES),
                    text = text,
                    durationMinutes = if (hasDuration) message.durationMinutes else null
                )

                // Text only (with optional duration)
                hasText -> TextContent(
                    text = text!!,
                    durationMinutes = message.durationMinutes
                )

                // Duration-only activity (edge case)
                hasDuration -> TextContent(
                    text = "⏱ ${message.durationMinutes} دقیقه",
                    durationMinutes = message.durationMinutes
                )

                // No recognizable content
                else -> EmptyMessage
            }
        }
    }
}
