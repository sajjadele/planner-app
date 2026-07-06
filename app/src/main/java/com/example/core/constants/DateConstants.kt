package com.example.core.constants

object DateConstants {
    // Persian week starts on Saturday (شنبه), not Monday
    val persianDaysOfWeek = listOf(
        "شنبه" to "ش",
        "یکشنبه" to "ی",
        "دوشنبه" to "د",
        "سه‌شنبه" to "س",
        "چهارشنبه" to "چ",
        "پنج‌شنبه" to "پ",
        "جمعه" to "ج"
    )

    val persianDayNames = persianDaysOfWeek.map { it.first }
}
