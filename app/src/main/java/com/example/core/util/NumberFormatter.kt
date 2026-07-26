package com.example.core.util

/**
 * Converts any Persian-Indic (۰-۹) or Arabic-Indic (٠-٩) digits in a string to Western
 * (0-9) digits, leaving all other characters untouched. Vision Planner renders Persian UI
 * with English numbers, so this is the canonical formatter for any visible count/percentage.
 *
 * Pure Kotlin — no Android dependencies, host-JVM testable.
 */
private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

fun String.toEnglishDigits(): String {
    if (this.isEmpty()) return this
    return buildString(this.length) {
        for (ch in this@toEnglishDigits) {
            val idx = PERSIAN_DIGITS.indexOf(ch)
            if (idx >= 0) {
                append('0' + idx)
            } else {
                val aidx = ARABIC_DIGITS.indexOf(ch)
                if (aidx >= 0) append('0' + aidx) else append(ch)
            }
        }
    }
}

fun Int.toEnglishDigits(): String = this.toString().toEnglishDigits()

fun Long.toEnglishDigits(): String = this.toString().toEnglishDigits()

fun Float.toEnglishDigits(): String = this.toString().toEnglishDigits()

/** Renders a percentage with Western digits, e.g. 71.6f -> "71%". */
fun Float.toEnglishPercent(): String = "${this.toInt().toEnglishDigits()}%"
