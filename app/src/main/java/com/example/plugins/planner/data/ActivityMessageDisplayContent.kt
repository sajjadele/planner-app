package com.example.plugins.planner.data

import com.example.plugins.planner.data.ActivityAttachment.File as FileAttachment
import com.example.plugins.planner.data.ActivityAttachment.Image as ImageAttachment

/**
 * ActivityMessageDisplayContent — Classifies the visual composition of a message.
 *
 * Phase 5.6: Telegram-style Activity Message Renderer Polish
 *
 * Responsibility:
 * - Determines how a message should be visually rendered
 * - Separates display logic from ActivityMessageCard
 * - Prevents raw JSON/URI leakage by enforcing content structure
 *
 * Types:
 * - [TextOnly]: Single text message (no attachments)
 * - [ImageOnly]: Single image (no text)
 * - [TextWithImages]: Text + one or more images
 * - [FileOnly]: Single file (no text)
 * - [TextWithFiles]: Text + one or more files
 * - [DurationActivity]: Activity with duration (with or without text)
 * - [EmptyMessage]: No displayable content (should not reach UI)
 * - [DeletedMessage]: Soft-deleted message
 */
sealed class ActivityMessageDisplayContent {

    /** Text-only message (note). */
    data class TextOnly(val text: String) : ActivityMessageDisplayContent()

    /** Image-only message (no text, no files). */
    data class ImageOnly(val image: ImageAttachment) : ActivityMessageDisplayContent()

    /** Text + images. */
    data class TextWithImages(
        val text: String,
        val images: List<ImageAttachment>,
        val durationMinutes: Int? = null
    ) : ActivityMessageDisplayContent()

    /** File-only message (no text). */
    data class FileOnly(val file: FileAttachment) : ActivityMessageDisplayContent()

    /** Text + files. */
    data class TextWithFiles(
        val text: String,
        val files: List<FileAttachment>,
        val durationMinutes: Int? = null
    ) : ActivityMessageDisplayContent()

    /** Duration activity (with optional text). */
    data class DurationActivity(
        val durationMinutes: Int,
        val text: String? = null
    ) : ActivityMessageDisplayContent()

    /** No displayable content — should never reach UI. */
    data object EmptyMessage : ActivityMessageDisplayContent()

    /** Soft-deleted message. */
    data object DeletedMessage : ActivityMessageDisplayContent()

    companion object {
        /**
         * Resolve display content from an ActivityMessageModel.
         *
         * This is the single entry point for classifying a message.
         * Guarantees: no raw JSON, no URI strings, no event types in UI.
         */
        fun from(message: ActivityMessageModel): ActivityMessageDisplayContent {
            // Deleted messages
            if (message.isDeleted) return DeletedMessage

            val text = message.text?.ifBlank { null }
            val images = message.attachments.filterIsInstance<ImageAttachment>()
            val files = message.attachments.filterIsInstance<FileAttachment>()

            val hasText = text != null
            val hasImages = images.isNotEmpty()
            val hasFiles = files.isNotEmpty()
            val hasDuration = message.durationMinutes != null

            // Classify by content type
            return when {
                // Duration activity (with or without text)
                hasDuration && !hasImages && !hasFiles ->
                    DurationActivity(
                        durationMinutes = message.durationMinutes!!,
                        text = text
                    )

                // Image only (no text)
                hasImages && !hasText && !hasFiles ->
                    ImageOnly(image = images.first())

                // Text + images
                hasText && hasImages &&
                !hasFiles ->
                    TextWithImages(
                        text = text!!,
                        images = images,
                        durationMinutes = message.durationMinutes
                    )

                // File only (no text)
                hasFiles && !hasText && !hasImages ->
                    FileOnly(file = files.first())

                // Text + files
                hasText && hasFiles &&
                !hasImages ->
                    TextWithFiles(
                        text = text!!,
                        files = files,
                        durationMinutes = message.durationMinutes
                    )

                // Text only
                hasText ->
                    TextOnly(text = text!!)

                // No recognizable content
                else -> EmptyMessage
            }
        }
    }
}
