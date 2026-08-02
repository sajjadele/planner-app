package com.example.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchNormalizationTest {

    @Test
    fun `arabic yeh normalizes to persian yeh`() {
        assertEquals("میشود", "مي‌شود".normalizeForSearch())
    }

    @Test
    fun `alif maqsura normalizes to persian yeh`() {
        assertEquals("پستی", "پستى".normalizeForSearch())
    }

    @Test
    fun `arabic kaf normalizes to persian kaf`() {
        assertEquals("کتاب", "كتاب".normalizeForSearch())
    }

    @Test
    fun `teh marbuta normalizes to heh`() {
        assertEquals("خانهه", "خانهة".normalizeForSearch())
    }

    @Test
    fun `hamza carriers normalize to alef`() {
        assertEquals("ااااب", "أإآأب".normalizeForSearch())
    }

    @Test
    fun `zwnj is removed so half-space variants match`() {
        assertEquals("میخواهم", "می‌خواهم".normalizeForSearch())
    }

    @Test
    fun `persian and arabic digits normalize to western`() {
        assertEquals("1403/12/25", "۱۴۰۳/۱۲/۲٥".normalizeForSearch())
    }

    @Test
    fun `mixed text lowercases latin`() {
        assertEquals("read Book 3", "Read Book ۳".normalizeForSearch())
    }

    @Test
    fun `empty string stays empty`() {
        assertEquals("", "".normalizeForSearch())
    }

    @Test
    fun `already normalized text unchanged`() {
        assertEquals("کتاب خواندن", "کتاب خواندن".normalizeForSearch())
    }
}
