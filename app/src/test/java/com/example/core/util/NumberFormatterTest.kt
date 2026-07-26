package com.example.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun `persian digits convert to english`() {
        assertEquals("71", "۷۱".toEnglishDigits())
    }

    @Test
    fun `single persian digit converts`() {
        assertEquals("3", "۳".toEnglishDigits())
    }

    @Test
    fun `int stays same when already western`() {
        assertEquals("100", 100.toEnglishDigits())
    }

    @Test
    fun `arabic indic digits convert too`() {
        assertEquals("45", "٤٥".toEnglishDigits())
    }

    @Test
    fun `mixed text keeps non digits`() {
        assertEquals("goal 12 روز", "goal ۱۲ روز".toEnglishDigits())
    }

    @Test
    fun `float percent formats with percent sign`() {
        assertEquals("71%", 71.6f.toEnglishPercent())
    }

    @Test
    fun `zero formats correctly`() {
        assertEquals("0%", 0f.toEnglishPercent())
    }
}
