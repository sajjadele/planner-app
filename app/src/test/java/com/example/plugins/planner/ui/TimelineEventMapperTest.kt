package com.example.plugins.planner.ui

import androidx.compose.ui.graphics.Color
import com.example.core.util.RTL
import com.example.plugins.planner.data.ActivityEventEntity
import com.example.plugins.planner.data.ActivityEventType
import com.example.plugins.planner.data.ImageEventParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineEventMapperTest {

    private fun event(
        id: Int,
        taskId: Int = 1,
        stepId: Int? = null,
        eventType: ActivityEventType,
        description: String? = null,
        timestamp: Long = id.toLong() * 1_000L
    ): ActivityEventEntity =
        ActivityEventEntity(
            id = id,
            taskId = taskId,
            stepId = stepId,
            eventType = eventType.name,
            description = description,
            timestamp = timestamp
        )

    private fun map(events: List<ActivityEventEntity>): List<TimelineEventUiModel> =
        TimelineEventMapper.mapEvents(
            events = events,
            useTimeOnly = true,
            primary = Color.Transparent,
            error = Color.Transparent,
            tertiary = Color.Transparent,
            outline = Color.Transparent
        )

    // ── STEP_CREATED ──────────────────────────────────────────────────

    @Test
    fun `created event has null supportingText`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_CREATED)))
        assertEquals(1, result.size)
        assertNull(result[0].supportingText)
    }

    @Test
    fun `created event has correct action header`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_CREATED)))
        assertEquals("${RTL}مرحله جدید ایجاد شد", result[0].actionText)
    }

    @Test
    fun `created event has step title as objectText`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_CREATED, description = "یادگیری FastAPI")))
        assertEquals("یادگیری FastAPI", result[0].objectText)
    }

    // ── STEP_COMPLETED ─────────────────────────────────────────────────

    @Test
    fun `completed event has previous→current supportingText with LRM marks`() {
        val events = listOf(
            event(1, stepId = 10, eventType = ActivityEventType.STEP_CREATED, timestamp = 1_000L),
            event(2, stepId = 10, eventType = ActivityEventType.STEP_COMPLETED, timestamp = 2_000L)
        )
        val result = map(events)
        val completed = result.find { it.id == 2 }!!
        assertEquals("فعال \u200E\u2192\u200E تکمیل شده", completed.supportingText)
    }

    @Test
    fun `completed event has correct action header`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_COMPLETED)))
        assertEquals("${RTL}مرحله تکمیل شد", result[0].actionText)
    }

    // ── STEP_REOPENED ──────────────────────────────────────────────────

    @Test
    fun `reopened event has previous→current supportingText with LRM marks`() {
        val events = listOf(
            event(1, stepId = 10, eventType = ActivityEventType.STEP_CREATED, timestamp = 1_000L),
            event(2, stepId = 10, eventType = ActivityEventType.STEP_COMPLETED, timestamp = 2_000L),
            event(3, stepId = 10, eventType = ActivityEventType.STEP_REOPENED, timestamp = 3_000L)
        )
        val result = map(events)
        val reopened = result.find { it.id == 3 }!!
        assertEquals("تکمیل شده \u200E\u2192\u200E فعال", reopened.supportingText)
    }

    @Test
    fun `reopened event has correct action header`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_REOPENED)))
        assertEquals("${RTL}مرحله بازگشایی شد", result[0].actionText)
    }

    // ── STEP_DELETED ───────────────────────────────────────────────────

    @Test
    fun `deleted event has null supportingText`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_DELETED)))
        assertEquals(1, result.size)
        assertNull(result[0].supportingText)
    }

    @Test
    fun `deleted event has correct action header`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_DELETED)))
        assertEquals("${RTL}مرحله حذف شد", result[0].actionText)
    }

    // ── NOTE_ADDED ─────────────────────────────────────────────────────

    @Test
    fun `note event has null supportingText`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED, description = "یادداشت محتوا")))
        assertEquals(1, result.size)
        assertNull(result[0].supportingText)
        assertEquals("یادداشت محتوا", result[0].objectText)
    }

    @Test
    fun `note event has correct action header`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED)))
        assertEquals("${RTL}یادداشت اضافه شد", result[0].actionText)
    }

    // ── FILE_ADDED ─────────────────────────────────────────────────────

    @Test
    fun `file event has null supportingText`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.FILE_ADDED, description = "report.pdf")))
        assertEquals(1, result.size)
        assertNull(result[0].supportingText)
        assertEquals("report.pdf", result[0].objectText)
    }

    @Test
    fun `file event has correct action header`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.FILE_ADDED)))
        assertEquals("${RTL}فایل اضافه شد", result[0].actionText)
    }

    // ── IMAGE_ADDED ─────────────────────────────────────────────────────

    @Test
    fun `image event has null supportingText`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED, description = "content://image/1:::Description")))
        assertEquals(1, result.size)
        assertNull(result[0].supportingText)
    }

    @Test
    fun `image event has correct action header`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED)))
        assertEquals("${RTL}تصویر اضافه شد", result[0].actionText)
    }

    @Test
    fun `image event has description as objectText`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED, description = "content://image/1:::Screenshot of API design")))
        assertEquals("Screenshot of API design", result[0].objectText)
    }

    @Test
    fun `image event without description has null objectText`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED, description = "content://image/1")))
        assertNull(result[0].objectText)
    }

    @Test
    fun `image event icon is camera`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED)))
        assertEquals("📷", result[0].icon)
    }

    @Test
    fun `image event gets correct color`() {
        val result = TimelineEventMapper.mapEvents(
            events = listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED)),
            useTimeOnly = true,
            primary = Color.Red,
            error = Color.Red,
            tertiary = Color.Red,
            outline = Color.Red
        )
        assertEquals(Color(0xFF1565C0), result[0].color)
    }

    @Test
    fun `image event with description has imageUri extracted from URI`() {
        val uri = "content://test/image.jpg"
        val desc = "Architecture screenshot"
        val encoded = ImageEventParser.encode(uri, desc)
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED, description = encoded)))
        assertEquals(uri, result[0].imageUri)
    }

    @Test
    fun `image event imageUri null when decoded URI is empty`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED, description = ImageEventParser.encode("", null))))
        assertNull(result[0].imageUri)
    }

    @Test
    fun `image event objectText does not contain URI`() {
        val uri = "content://test/image.jpg"
        val desc = "Architecture screenshot"
        val encoded = ImageEventParser.encode(uri, desc)
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED, description = encoded)))
        assertEquals(desc, result[0].objectText)
        assertFalse(result[0].objectText.orEmpty().contains(uri))
    }

    // ── NOTE_ADDED with JSON payload (Phase 4.7.4) ──────────────────────

    @Test
    fun `note event with json payload extracts imageUri from attachments`() {
        val json = """{"text":"","attachments":[{"type":"IMAGE","uri":"content://test/photo.jpg"}]}"""
        val result = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED, description = json)))
        assertEquals("content://test/photo.jpg", result[0].imageUri)
    }

    @Test
    fun `note event with json payload text and image extracts both`() {
        val json = """{"text":"تست تصویر","attachments":[{"type":"IMAGE","uri":"content://test/photo.jpg"}]}"""
        val result = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED, description = json)))
        assertEquals("تست تصویر", result[0].objectText)
        assertEquals("content://test/photo.jpg", result[0].imageUri)
    }

    @Test
    fun `note event with json payload imageOnly has null objectText`() {
        val json = """{"attachments":[{"type":"IMAGE","uri":"content://test/photo.jpg"}]}"""
        val result = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED, description = json)))
        assertNull(result[0].objectText)
        assertEquals("content://test/photo.jpg", result[0].imageUri)
    }

    @Test
    fun `note event with json payload no image has null imageUri`() {
        val json = """{"text":"note without image"}"""
        val result = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED, description = json)))
        assertEquals("note without image", result[0].objectText)
        assertNull(result[0].imageUri)
    }

    @Test
    fun `note event with plain text has null imageUri`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED, description = "plain note")))
        assertEquals("plain note", result[0].objectText)
        assertNull(result[0].imageUri)
    }

    // ── FILE_ADDED with JSON payload (Phase 4.7.4) ──────────────────────

    @Test
    fun `file event with json payload extracts imageUri from image attachment`() {
        val json = """{"text":"report","attachments":[{"type":"IMAGE","uri":"content://test/image.png"}]}"""
        val result = map(listOf(event(1, eventType = ActivityEventType.FILE_ADDED, description = json)))
        assertEquals("content://test/image.png", result[0].imageUri)
    }

    // ── IMAGE_ADDED with JSON payload (Phase 4.7.4) ─────────────────────

    @Test
    fun `image event with json payload extracts imageUri`() {
        val json = """{"text":"description","attachments":[{"type":"IMAGE","uri":"content://test/img.jpg"}]}"""
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED, description = json)))
        assertEquals("description", result[0].objectText)
        assertEquals("content://test/img.jpg", result[0].imageUri)
    }

    @Test
    fun `image event with json payload imageOnly has null objectText`() {
        val json = """{"attachments":[{"type":"IMAGE","uri":"content://test/img.jpg"}]}"""
        val result = map(listOf(event(1, eventType = ActivityEventType.IMAGE_ADDED, description = json)))
        assertNull(result[0].objectText)
        assertEquals("content://test/img.jpg", result[0].imageUri)
    }

    @Test
    fun `multiple attachments all preserved in event types`() {
        val json = """{"text":"multi","attachments":[{"type":"IMAGE","uri":"content://test/1.jpg"},{"type":"FILE","uri":"content://test/doc.pdf","name":"doc"}]}"""
        val noteResult = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED, description = json)))
        assertEquals("multi", noteResult[0].objectText)
        assertEquals("content://test/1.jpg", noteResult[0].imageUri)

        val imageResult = map(listOf(event(2, eventType = ActivityEventType.IMAGE_ADDED, description = json)))
        assertEquals("multi", imageResult[0].objectText)
        assertEquals("content://test/1.jpg", imageResult[0].imageUri)
    }

    // ── Color assignment ────────────────────────────────────────────────

    @Test
    fun `created event gets primary color`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_CREATED)))
        assertEquals(Color.Transparent, result[0].color)
    }

    @Test
    fun `completed event gets green`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_COMPLETED)))
        assertEquals(Color(0xFF2E7D32), result[0].color)
    }

    @Test
    fun `reopened event gets orange`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_REOPENED)))
        assertEquals(Color(0xFFE65100), result[0].color)
    }

    @Test
    fun `deleted event gets error color`() {
        val result = map(listOf(event(1, stepId = 10, eventType = ActivityEventType.STEP_DELETED)))
        assertEquals(Color.Transparent, result[0].color)
    }

    @Test
    fun `note event gets tertiary color`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.NOTE_ADDED)))
        assertEquals(Color.Transparent, result[0].color)
    }

    @Test
    fun `file event gets outline color`() {
        val result = map(listOf(event(1, eventType = ActivityEventType.FILE_ADDED)))
        assertEquals(Color.Transparent, result[0].color)
    }

    // ── Ordering preserved ──────────────────────────────────────────────

    @Test
    fun `output ordering matches input ordering`() {
        val events = listOf(
            event(1, stepId = 10, eventType = ActivityEventType.STEP_CREATED, timestamp = 1_000L),
            event(2, stepId = 10, eventType = ActivityEventType.STEP_COMPLETED, timestamp = 2_000L)
        )
        val result = map(events)
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals(2, result[1].id)
    }
}