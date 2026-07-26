package com.example.plugins.planner.ui

import androidx.compose.ui.graphics.Color
import com.example.core.util.RTL
import com.example.core.util.formatPersianTime
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityEventType
import com.example.plugins.planner.data.ActivityPayload
import com.example.plugins.planner.data.ActivityPayloadCodec
import com.example.plugins.planner.data.ImageEventParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimelineEventUiModel(
    val id: Int,
    val icon: String,
    val actionText: String,
    val objectText: String?,
    val supportingText: String?,
    val timeText: String,
    val color: Color,
    val timestamp: Long,
    val imageUri: String? = null
)

object TimelineEventMapper {

    private val stepIconMap = mapOf(
        ActivityEventType.STEP_CREATED to "＋",
        ActivityEventType.STEP_COMPLETED to "✓",
        ActivityEventType.STEP_REOPENED to "↻",
        ActivityEventType.STEP_DELETED to "✕"
    )

    private val stepActionMap = mapOf(
        ActivityEventType.STEP_CREATED to "${RTL}مرحله جدید ایجاد شد",
        ActivityEventType.STEP_COMPLETED to "${RTL}مرحله تکمیل شد",
        ActivityEventType.STEP_REOPENED to "${RTL}مرحله بازگشایی شد",
        ActivityEventType.STEP_DELETED to "${RTL}مرحله حذف شد"
    )

    private data class StepState(
        val label: String,
        val eventType: ActivityEventType?
    )

    private val stateFromEvent = mapOf(
        ActivityEventType.STEP_CREATED to StepState("فعال", ActivityEventType.STEP_CREATED),
        ActivityEventType.STEP_COMPLETED to StepState("تکمیل شده", ActivityEventType.STEP_COMPLETED),
        ActivityEventType.STEP_REOPENED to StepState("فعال", ActivityEventType.STEP_REOPENED),
        ActivityEventType.STEP_DELETED to StepState("حذف شده", ActivityEventType.STEP_DELETED)
    )

    fun mapToUiModel(
        event: ActivityEventEntity,
        primary: Color,
        error: Color,
        tertiary: Color,
        outline: Color,
        useTimeOnly: Boolean = false
    ): TimelineEventUiModel {
        val eventType = parseEventType(event.eventType)
        var icon = "•"
        var actionText = event.eventType
        var objectText: String? = event.description
        var supportingText: String? = null
        var imageUri: String? = null

        when (eventType) {
            ActivityEventType.STEP_CREATED,
            ActivityEventType.STEP_COMPLETED,
            ActivityEventType.STEP_REOPENED,
            ActivityEventType.STEP_DELETED -> {
                icon = stepIconMap[eventType] ?: "•"
                actionText = stepActionMap[eventType] ?: event.eventType

                // Decode JSON payload if present (Phase 4.7.4)
                val decoded = decodeDescription(event.description)
                objectText = decoded.text ?: event.description

                supportingText = supportMap[event.id]
                color = resolveColor(eventType, primary, error, tertiary, outline)
            }

            ActivityEventType.NOTE_ADDED -> {
                icon = "📝"
                actionText = "${RTL}یادداشت اضافه شد"

                // Decode JSON payload if present (Phase 4.7.4)
                val decoded = decodeDescription(event.description)
                objectText = decoded.text ?: event.description

                supportingText = null
                color = resolveColor(eventType, primary, error, tertiary, outline)
            }

            ActivityEventType.FILE_ADDED -> {
                icon = "📎"
                actionText = "${RTL}فایل اضافه شد"

                // Decode JSON payload if present (Phase 4.7.4)
                val decoded = decodeDescription(event.description)
                objectText = decoded.text ?: event.description

                supportingText = null
                color = resolveColor(eventType, primary, error, tertiary, outline)
            }

            ActivityEventType.MANUAL_ACTIVITY -> {
                icon = "📌"
                actionText = "${RTL}فعالیت ثبت شد"

                // Decode JSON payload if present (Phase 4.7.4)
                val decoded = decodeDescription(event.description)
                if (decoded.text != null) {
                    objectText = decoded.text
                    supportingText = decoded.durationMinutes?.let { "${RTL}مدت زمان: ${it.formatPersian()} دقیقه" }
                } else {
                    // Fallback to legacy format
                    val (title, durationMinutes) = parseManualActivityDescription(event.description)
                    objectText = title
                    supportingText = durationMinutes?.let { "${RTL}مدت زمان: ${it.formatPersian()} دقیقه" }
                }

                // Extract image from attachments if present
                val imageAttachment = decoded.attachments.firstOrNull { it is com.example.plugins.planner.data.ActivityAttachment.Image }
                if (imageAttachment is com.example.plugins.planner.data.ActivityAttachment.Image) {
                    imageUri = imageAttachment.uri
                }

                color = resolveColor(eventType, primary, error, tertiary, outline)
            }

            ActivityEventType.IMAGE_ADDED -> {
                icon = "📷"
                actionText = "${RTL}تصویر اضافه شد"

                // Decode JSON payload if present (Phase 4.7.4)
                val decoded = decodeDescription(event.description)
                if (decoded.attachments.isNotEmpty()) {
                    // New format: use decoded payload
                    objectText = decoded.text ?: ""
                    val imageAttachment = decoded.attachments.firstOrNull { it is com.example.plugins.planner.data.ActivityAttachment.Image }
                    if (imageAttachment is com.example.plugins.planner.data.ActivityAttachment.Image) {
                        imageUri = imageAttachment.uri
                    }
                } else {
                    // Legacy format: use ImageEventParser
                    val imageData = ImageEventParser.decode(event.description)
                    objectText = imageData.description
                    imageUri = imageData.uri.takeIf { it.isNotBlank() }
                }

                supportingText = null
                color = resolveColor(eventType, primary, error, tertiary, outline)
            }

            null -> {
                icon = "•"
                actionText = event.eventType
                objectText = event.description
                supportingText = null
                color = resolveColor(null, primary, error, tertiary, outline)
            }
        }

        val timeText = if (useTimeOnly) {
            SimpleDateFormat("HH:mm", Locale.US).format(Date(event.timestamp))
        } else {
            formatPersianTime(event.timestamp)
        }

        return TimelineEventUiModel(
            id = event.id,
            icon = icon,
            actionText = actionText,
            objectText = objectText,
            supportingText = supportingText,
            timeText = timeText,
            color = color,
            timestamp = event.timestamp,
            imageUri = imageUri
        )
    }

    /**
     * Decode description using ActivityPayloadCodec (Phase 4.7.4).
     * Falls back to empty payload if not JSON format.
     */
    private fun decodeDescription(description: String?): ActivityPayload {
        if (description == null) return ActivityPayload()
        return ActivityPayloadCodec.decode(description) ?: ActivityPayload()
    }

    private fun parseEventType(raw: String): ActivityEventType? = try {
        ActivityEventType.valueOf(raw)
    } catch (_: Exception) {
        null
    }

    private fun parseManualActivityDescription(description: String?): Pair<String, Int?> {
        if (description == null) return "" to null
        val parts = description.split("|", limit = 2)
        val title = parts[0]
        val duration = parts.getOrNull(1)?.toIntOrNull()
        return title to duration
    }

    private fun Int.formatPersian(): String =
        String.format(Locale.US, "%,d", this).replace(",", "‌")
}
