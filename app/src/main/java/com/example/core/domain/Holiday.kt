package com.example.core.domain

/**
 * A single holiday definition.
 *
 * @param name Display name in Persian, e.g. "عید نوروز".
 * @param jalaliMonth Jalali month (1–12) this holiday falls on.
 * @param jalaliDay Jalali day (1–31) this holiday falls on.
 * @param isOfficial Whether this is an official public holiday.
 */
data class Holiday(
    val name: String,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val isOfficial: Boolean = true
)
