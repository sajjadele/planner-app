package com.example.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a timestamp into a human-readable Persian-relative or absolute time string.
 * @param timestamp The epoch millis timestamp
 * @param fallbackFormat The SimpleDateFormat pattern for absolute date formatting.
 *                       Defaults to "yyyy/MM/dd HH:mm". Use "yyyy/MM/dd" for date-only.
 */
fun formatPersianTime(timestamp: Long, fallbackFormat: String = "yyyy/MM/dd HH:mm"): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "همین الان"
        diff < 3_600_000 -> "${(diff / 60_000).toPersianDigits()} دقیقه پیش"
        diff < 86_400_000 -> "${(diff / 3_600_000).toPersianDigits()} ساعت پیش"
        else -> {
            val sdf = SimpleDateFormat(fallbackFormat, Locale.US)
            sdf.format(Date(timestamp))
        }
    }
}
