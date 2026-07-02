package com.example.core.constants

object DateConstants {
    val persianDaysOfWeek = listOf(
        "دوشنبه" to "د",
        "سه‌شنبه" to "س",
        "چهارشنبه" to "چ",
        "پنجشنبه" to "پ",
        "جمعه" to "ج",
        "شنبه" to "ش",
        "یکشنبه" to "ی"
    )

    val persianDayNames = persianDaysOfWeek.map { it.first }
}
