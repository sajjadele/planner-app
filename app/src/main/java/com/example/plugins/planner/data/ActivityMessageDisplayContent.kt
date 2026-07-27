package com.example.plugins.planner.data

import com.example.plugins.planner.data.ActivityAttachment.File as FileAttachment
import com.example.plugins.planner.data.ActivityAttachment.Image as ImageAttachment

/**
 * ActivityMessageDisplayContent — Classifies message content for display.
 *
 * Phase 5.7.1: Three display types only — TextContent, MediaContent, MediaWithText.
 * No event labels, no step chips, no raw JSON/URI in UI.
 *
 * Types:
 * - [TextContent]: Plain text (with optional duration)
 * - [MediaContent]: Image or file (no text)
 * - [MediaWithText]: Image/file + text caption
 * - [EmptyMessage]: No displayable content (should not reach UI)
 * - [DeletedMessage]: Soft-deleted message
 */
sealed class ActivityMessageDisplayContent {

    /** Text-only message (note, with optional duration). */
    data class TextContent(
        val text: String,
        val durationMinutes: Int? = null
    ) : ActivityMessageDisplayContent()

    /** Media-only (image or file, no text). */
    data class MediaContent(
        val attachment: ActivityAttachment,
        val isImage: Boolean
    ) : ActivityMessageDisplayContent()

    /** Media + text caption. */
    data class MediaWithText(
        val attachment: ActivityAttachment,
        val text: String,
        val isImage: Boolean,
        val durationMinutes: Int? = null
    ) : ActivityMessageDisplayContent()

    /** No displayable content — should never reach UI. */
    data object EmptyMessage : ActivityMessageDisplayContent()

    /** Soft-deleted message. */
    data object DeletedMessage : ActivityMessageDisplayContent()

    companion object {
        /**
         * Resolve display content from an ActivityMessageModel.
         *
         * Guarantees: no raw JSON, no URI strings, no event types in UI.
         * Step/tag metadata is NOT part of display content — it belongs in filter chips.
         */
        fun from(message: ActivityMessageModel): ActivityMessageDisplayContent {
            if (message.isDeleted) return DeletedMessage

            val text = message.text?.ifBlank { null }
            val images = message.attachments.filterIsInstance<ImageAttachment>()
            val files = message.attachments.filterIsInstance<FileAttachment>()

            val hasText = text != null
            val hasImages = images.isNotEmpty()
            val hasFiles = files.isNotEmpty()
            val hasDuration = message.durationMinutes != null

            // Determine primary attachment (image takes priority)
            val primaryAttachment: ActivityAttachment? = when {
                hasImages -> images.first()
                hasFiles -> files.first()
                else -> null
            }
            val isImage = primaryAttachment is ImageAttachment

            return when {
                // Media with text caption
                primaryAttachment != null && hasText ->
                    MediaWithText(
                        attachment = primaryAttachment,
                        text = text!!,
                        isImage = isImage,
                        durationMinutes = if (hasDuration) message.durationMinutes else null
                    )

                // Media only (no text)
                primaryAttachment != null ->
                    MediaContent(
                        attachment = primaryAttachment,
                        isImage = isImage
                    )

                // Text (with optional duration)
                hasText ->
                    TextContent(
                        text = text!!,
                        durationMinutes = message.durationMinutes
                    )

                // Duration-only activity (edge case: no text, no media, only duration)
                hasDuration ->
                    TextContent(
                        text = "⏱ ${message.durationMinutes} دقیقه",
                        durationMinutes = message.durationMinutes
                    )

                // No recognizable content
                else -> EmptyMessage
            }
        }
    }
}
