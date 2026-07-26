package com.example.plugins.planner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageEventParserTest {

    @Test
    fun `encode and decode URI only`() {
        val uri = "content://media/image/1"
        val encoded = ImageEventParser.encode(uri, null)
        assertEquals(uri, encoded)

        val decoded = ImageEventParser.decode(encoded)
        assertEquals(uri, decoded.uri)
        assertNull(decoded.description)
    }

    @Test
    fun `encode and decode URI with description`() {
        val uri = "content://media/image/123"
        val desc = "Screenshot of API design"
        val encoded = ImageEventParser.encode(uri, desc)
        assertEquals("$uri:::Screenshot of API design", encoded)

        val decoded = ImageEventParser.decode(encoded)
        assertEquals(uri, decoded.uri)
        assertEquals(desc, decoded.description)
    }

    @Test
    fun `description containing separator-like characters is preserved`() {
        val uri = "content://media/image/5"
        val desc = "This has ::: in it and | also"
        val encoded = ImageEventParser.encode(uri, desc)
        val decoded = ImageEventParser.decode(encoded)
        assertEquals(uri, decoded.uri)
        assertEquals(desc, decoded.description)
    }

    @Test
    fun `malformed payload without separator returns URI with null description`() {
        val payload = "content://image/1"
        val decoded = ImageEventParser.decode(payload)
        assertEquals(payload, decoded.uri)
        assertNull(decoded.description)
    }

    @Test
    fun `null payload returns empty URI and null description`() {
        val decoded = ImageEventParser.decode(null)
        assertEquals("", decoded.uri)
        assertNull(decoded.description)
    }

    @Test
    fun `blank payload returns empty URI and null description`() {
        val decoded = ImageEventParser.decode("")
        assertEquals("", decoded.uri)
        assertNull(decoded.description)
    }

    @Test
    fun `empty description after separator is treated as null`() {
        val uri = "content://media/image/9"
        val encoded = ImageEventParser.encode(uri, "")
        val decoded = ImageEventParser.decode(encoded)
        assertEquals(uri, decoded.uri)
        assertNull(decoded.description)
    }
}