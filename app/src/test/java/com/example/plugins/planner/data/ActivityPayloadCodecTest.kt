package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPayloadCodecTest {

    // ════════════════════════════════════════════════════════════════
    // Test 1: Text only
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `text only - encode and decode`() {
        val payload = ActivityPayload(text = "hello")
        val encoded = ActivityPayloadCodec.encode(payload)

        assertNotNull(encoded)
        assertTrue(encoded!!.contains("hello"))

        val decoded = ActivityPayloadCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals("hello", decoded!!.text)
        assertTrue(decoded.attachments.isEmpty())
        assertNull(decoded.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: Image attachment
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `image attachment - encode and decode`() {
        val payload = ActivityPayload(
            attachments = listOf(ActivityAttachment.Image("content://test/image.jpg"))
        )
        val encoded = ActivityPayloadCodec.encode(payload)

        assertNotNull(encoded)
        assertTrue(encoded!!.contains("IMAGE"))
        assertTrue(encoded.contains("content://test/image.jpg"))

        val decoded = ActivityPayloadCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.attachments.size)
        assertTrue(decoded.attachments[0] is ActivityAttachment.Image)
        assertEquals("content://test/image.jpg", (decoded.attachments[0] as ActivityAttachment.Image).uri)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: Multiple attachments
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `multiple attachments - encode and decode`() {
        val payload = ActivityPayload(
            attachments = listOf(
                ActivityAttachment.Image("content://img/1.jpg"),
                ActivityAttachment.File("content://file/doc.pdf", "document.pdf")
            )
        )
        val encoded = ActivityPayloadCodec.encode(payload)

        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(2, decoded!!.attachments.size)

        val images = decoded.attachments.filterIsInstance<ActivityAttachment.Image>()
        val files = decoded.attachments.filterIsInstance<ActivityAttachment.File>()
        assertEquals(1, images.size)
        assertEquals(1, files.size)
        assertEquals("content://img/1.jpg", images[0].uri)
        assertEquals("content://file/doc.pdf", files[0].uri)
        assertEquals("document.pdf", files[0].name)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: Duration
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `duration - encode and decode`() {
        val payload = ActivityPayload(
            text = "Code review",
            durationMinutes = 120
        )
        val encoded = ActivityPayloadCodec.encode(payload)

        assertNotNull(encoded)
        assertTrue(encoded!!.contains("120"))

        val decoded = ActivityPayloadCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals("Code review", decoded!!.text)
        assertEquals(120, decoded.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: Empty payload
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `empty payload - encode returns null`() {
        val payload = ActivityPayload()
        val encoded = ActivityPayloadCodec.encode(payload)
        assertNull(encoded)
    }

    @Test
    fun `empty payload - decode null returns null`() {
        val decoded = ActivityPayloadCodec.decode(null)
        assertNull(decoded)
    }

    @Test
    fun `empty payload - decode empty string returns null`() {
        val decoded = ActivityPayloadCodec.decode("")
        assertNull(decoded)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Invalid JSON
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `invalid JSON - graceful failure`() {
        val decoded = ActivityPayloadCodec.decode("not valid json {{{")
        // Should not crash, returns null or legacy format
        // Since it doesn't start with {, it tries legacy format
        assertNotNull(decoded)
        assertEquals("not valid json {{{", decoded!!.text)
    }

    @Test
    fun `malformed JSON object - graceful failure`() {
        val decoded = ActivityPayloadCodec.decode("{invalid json}")
        // Should handle gracefully — falls back to legacy text format
        assertNotNull(decoded)
        assertEquals("{invalid json}", decoded!!.text)
        assertTrue(decoded.attachments.isEmpty())
    }

    // ════════════════════════════════════════════════════════════════
    // Test 7: Legacy format compatibility
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `legacy image format - decode`() {
        val legacy = "content://image/test.jpg:::Screenshot"
        val decoded = ActivityPayloadCodec.decode(legacy)

        assertNotNull(decoded)
        assertEquals(1, decoded!!.attachments.size)
        assertTrue(decoded.attachments[0] is ActivityAttachment.Image)
        assertEquals("content://image/test.jpg", (decoded.attachments[0] as ActivityAttachment.Image).uri)
        assertEquals("Screenshot", decoded.text)
    }

    @Test
    fun `legacy manual activity format - decode`() {
        val legacy = "Code review|45"
        val decoded = ActivityPayloadCodec.decode(legacy)

        assertNotNull(decoded)
        assertEquals("Code review", decoded!!.text)
        assertEquals(45, decoded.durationMinutes)
    }

    @Test
    fun `legacy plain text - decode`() {
        val legacy = "Simple note"
        val decoded = ActivityPayloadCodec.decode(legacy)

        assertNotNull(decoded)
        assertEquals("Simple note", decoded!!.text)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 8: Full payload with all fields
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `full payload - encode and decode`() {
        val payload = ActivityPayload(
            text = "طراحی صفحه اصلی",
            attachments = listOf(
                ActivityAttachment.Image("content://image/test.jpg")
            ),
            durationMinutes = 90
        )
        val encoded = ActivityPayloadCodec.encode(payload)

        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals("طراحی صفحه اصلی", decoded!!.text)
        assertEquals(90, decoded.durationMinutes)
        assertEquals(1, decoded.attachments.size)
        assertEquals("content://image/test.jpg", (decoded.attachments[0] as ActivityAttachment.Image).uri)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 9: isJsonFormat
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `isJsonFormat - detects JSON`() {
        assertTrue(ActivityPayloadCodec.isJsonFormat("{\"text\":\"hello\"}"))
        assertTrue(ActivityPayloadCodec.isJsonFormat("  {\"text\":\"hello\"}"))
    }

    @Test
    fun `isJsonFormat - detects non-JSON`() {
        assertEquals(false, ActivityPayloadCodec.isJsonFormat("plain text"))
        assertEquals(false, ActivityPayloadCodec.isJsonFormat("uri:::desc"))
        assertEquals(false, ActivityPayloadCodec.isJsonFormat(null))
    }
}
