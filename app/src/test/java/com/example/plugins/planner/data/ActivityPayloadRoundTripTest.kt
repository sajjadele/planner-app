package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.8.1 — ActivityPayload Round Trip Validation
 *
 * Tests encode/decode symmetry for all payload types.
 */
class ActivityPayloadRoundTripTest {

    // ════════════════════════════════════════════════════════════════
    // Test 1: Text only
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `round trip - text only`() {
        val original = ActivityPayload(text = "Design homepage")
        val encoded = ActivityPayloadCodec.encode(original)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals("Design homepage", decoded!!.text)
        assertTrue(decoded.attachments.isEmpty())
        assertNull(decoded.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 2: Image attachment
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `round trip - image attachment`() {
        val original = ActivityPayload(
            text = "Homepage mockup",
            attachments = listOf(ActivityAttachment.Image("content://image/test.jpg"))
        )
        val encoded = ActivityPayloadCodec.encode(original)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals("Homepage mockup", decoded!!.text)
        assertEquals(1, decoded.attachments.size)
        assertTrue(decoded.attachments[0] is ActivityAttachment.Image)
        assertEquals("content://image/test.jpg", (decoded.attachments[0] as ActivityAttachment.Image).uri)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 3: Multiple attachments
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `round trip - multiple attachments`() {
        val original = ActivityPayload(
            text = "Assets",
            attachments = listOf(
                ActivityAttachment.Image("content://image/1.jpg"),
                ActivityAttachment.File("content://file/doc.pdf", "document.pdf")
            )
        )
        val encoded = ActivityPayloadCodec.encode(original)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals("Assets", decoded!!.text)
        assertEquals(2, decoded.attachments.size)

        // Verify order preserved
        assertTrue(decoded.attachments[0] is ActivityAttachment.Image)
        assertTrue(decoded.attachments[1] is ActivityAttachment.File)

        // Verify metadata preserved
        assertEquals("content://image/1.jpg", (decoded.attachments[0] as ActivityAttachment.Image).uri)
        assertEquals("content://file/doc.pdf", (decoded.attachments[1] as ActivityAttachment.File).uri)
        assertEquals("document.pdf", (decoded.attachments[1] as ActivityAttachment.File).name)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 4: Manual activity
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `round trip - manual activity with duration`() {
        val original = ActivityPayload(
            text = "Coding",
            durationMinutes = 90
        )
        val encoded = ActivityPayloadCodec.encode(original)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals("Coding", decoded!!.text)
        assertEquals(90, decoded.durationMinutes)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 5: Full payload with all fields
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `round trip - full payload`() {
        val original = ActivityPayload(
            text = "طراحی صفحه اصلی",
            attachments = listOf(
                ActivityAttachment.Image("content://image/mockup.jpg")
            ),
            durationMinutes = 120
        )
        val encoded = ActivityPayloadCodec.encode(original)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals("طراحی صفحه اصلی", decoded!!.text)
        assertEquals(1, decoded.attachments.size)
        assertEquals(120, decoded.durationMinutes)
        assertNull(decoded.replyToMessageId)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 6: Reply reference
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `round trip - reply reference`() {
        val original = ActivityPayload(
            text = "Reply to your message",
            replyToMessageId = 42L
        )
        val encoded = ActivityPayloadCodec.encode(original)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals("Reply to your message", decoded!!.text)
        assertEquals(42L, decoded.replyToMessageId)
    }

    @Test
    fun `round trip - full payload with reply reference`() {
        val original = ActivityPayload(
            text = "Full reply with attachments",
            attachments = listOf(
                ActivityAttachment.Image("content://image/reply.jpg")
            ),
            durationMinutes = 30,
            replyToMessageId = 99L
        )
        val encoded = ActivityPayloadCodec.encode(original)
        assertNotNull(encoded)

        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)
        assertEquals("Full reply with attachments", decoded!!.text)
        assertEquals(1, decoded.attachments.size)
        assertEquals(30, decoded.durationMinutes)
        assertEquals(99L, decoded.replyToMessageId)
    }

    @Test
    fun `decode JSON without replyToMessageId field returns null`() {
        val payload = ActivityPayloadCodec.decode("""{"text":"No reply ref"}""")
        assertNotNull(payload)
        assertEquals("No reply ref", payload!!.text)
        assertNull(payload.replyToMessageId)
    }

    // ════════════════════════════════════════════════════════════════
    // Test 10: Draft → Payload → Codec → Decode symmetry
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `full pipeline - draft to payload to codec to decode`() {
        val draft = ActivityDraft(
            text = "Review PR",
            attachments = listOf(ActivityAttachment.Image("content://img/screenshot.png")),
            durationMinutes = 45
        )

        // Draft → Payload
        val payload = ActivityPayload(
            text = draft.text,
            attachments = draft.attachments,
            durationMinutes = draft.durationMinutes
        )

        // Payload → Codec
        val encoded = ActivityPayloadCodec.encode(payload)
        assertNotNull(encoded)

        // Codec → Decode
        val decoded = ActivityPayloadCodec.decode(encoded!!)
        assertNotNull(decoded)

        // Verify symmetry
        assertEquals(draft.text, decoded!!.text)
        assertEquals(draft.attachments.size, decoded.attachments.size)
        assertEquals(draft.durationMinutes, decoded.durationMinutes)
        assertNull(decoded.replyToMessageId)
    }

    @Test
    fun `encode null replyToMessageId omits field`() {
        val payload = ActivityPayload(text = "Hello", replyToMessageId = null)
        val encoded = ActivityPayloadCodec.encode(payload)
        assertNotNull(encoded)
        assertFalse(encoded!!.contains("replyToMessageId"))
    }
}
