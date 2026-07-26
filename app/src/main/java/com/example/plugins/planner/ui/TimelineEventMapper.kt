package com.example.plugins.planner.ui

import androidx.compose.ui.graphics.Color
import com.example.core.util.RTL
import com.example.core.util.formatPersianTime
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityEventType
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

    private fun formatStateTransition(from: String, to: String): String =
        "$from \u200E\u2192\u200E $to"

    private fun resolveSupportingText(
        currentEventType: ActivityEventType?,
        previousEventType: ActivityEventType?
    ): String? {
        if (currentEventType == null) return null
        val currentState = stateFromEvent[currentEventType] ?: return null
        val previousState = previousEventType?.let { stateFromEvent[it] }
        if (previousState == null) return null
        return formatStateTransition(previousState.label, currentState.label)
    }

    fun resolveColor(
        eventType: ActivityEventType?,
        primary: Color,
        error: Color,
        tertiary: Color,
        outline: Color
    ): Color = when (eventType) {
        ActivityEventType.STEP_CREATED -> primary
        ActivityEventType.STEP_COMPLETED -> Color(0xFF2E7D32)
        ActivityEventType.STEP_REOPENED -> Color(0xFFE65100)
        ActivityEventType.STEP_DELETED -> error
        ActivityEventType.NOTE_ADDED -> tertiary
        ActivityEventType.FILE_ADDED -> outline
        ActivityEventType.MANUAL_ACTIVITY -> Color(0xFF6A1B9A)
        ActivityEventType.IMAGE_ADDED -> Color(0xFF1565C0)
        null -> Color.Gray
    }

    fun mapEvents(
        events: List<ActivityEventEntity>,
        useTimeOnly: Boolean = false,
        primary: Color = Color.Transparent,
        error: Color = Color.Transparent,
        tertiary: Color = Color.Transparent,
        outline: Color = Color.Transparent
    ): List<TimelineEventUiModel> {
        val stepEvents = events
            .filter { it.stepId != null }
            .groupBy { it.stepId!! }
            .mapValues { (_, events) -> events.sortedBy { it.timestamp } }

        val supportMap = mutableMapOf<Int, String?>()
        for ((_, sorted) in stepEvents) {
            var previousType: ActivityEventType? = null
            for (event in sorted) {
                val currentType = parseEventType(event.eventType)
                val supportingText = resolveSupportingText(currentType, previousType)
                supportMap[event.id] = supportingText
                previousType = currentType
            }
        }

        return events.map { event ->
            val eventType = parseEventType(event.eventType)
            val icon: String
            val actionText: String
            val objectText: String?
            val supportingText: String?
            val color: Color
            var imageUri: String? = null

            when (eventType) {
                ActivityEventType.STEP_CREATED,
                ActivityEventType.STEP_COMPLETED,
                ActivityEventType.STEP_REOPENED,
                ActivityEventType.STEP_DELETED -> {
                    icon = stepIconMap[eventType] ?: "•"
                    actionText = stepActionMap[eventType] ?: event.eventType
                    objectText = event.description
                    supportingText = supportMap[event.id]
                    color = resolveColor(eventType, primary, error, tertiary, outline)
                }

                ActivityEventType.NOTE_ADDED -> {
                    icon = "📝"
                    actionText = "${RTL}یادداشت اضافه شد"
                    objectText = event.description
                    supportingText = null
                    color = resolveColor(eventType, primary, error, tertiary, outline)
                }

                ActivityEventType.FILE_ADDED -> {
                    icon = "📎"
                    actionText = "${RTL}فایل اضافه شد"
                    objectText = event.description
                    supportingText = null
                    color = resolveColor(eventType, primary, error, tertiary, outline)
                }

                ActivityEventType.MANUAL_ACTIVITY -> {
                    icon = "📌"
                    actionText = "${RTL}فعالیت ثبت شد"
                    val (title, durationMinutes) = parseManualActivityDescription(event.description)
                    objectText = title
                    supportingText = durationMinutes?.let { "${RTL}مدت زمان: ${it.formatPersian()} دقیقه" }
                    color = resolveColor(eventType, primary, error, tertiary, outline)
                }

                ActivityEventType.IMAGE_ADDED -> {
                    icon = "📷"
                    actionText = "${RTL}تصویر اضافه شد"
                    val imageData = ImageEventParser.decode(event.description)
                    objectText = imageData.description
                    supportingText = null
                    imageUri = imageData.uri.takeIf { it.isNotBlank() }
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

            TimelineEventUiModel(
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
