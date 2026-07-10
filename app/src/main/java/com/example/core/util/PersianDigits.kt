package com.example.core.util

/**
 * Converts Latin digits (0-9) in a string to Persian-Indic digits (۰-۹).
 * This prevents bidirectional text reordering issues when displaying
 * numbers within Persian (RTL) text.
 */
fun String.toPersianDigits(): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return buildString(this.length) {
        for (ch in this@toPersianDigits) {
            if (ch in '0'..'9') {
                append(persianDigits[ch - '0'])
            } else {
                append(ch)
            }
        }
    }
}

/**
 * Converts a number to Persian-Indic digit string.
 */
fun Int.toPersianDigits(): String = this.toString().toPersianDigits()

/**
 * Converts a long to Persian-Indic digit string.
 */
fun Long.toPersianDigits(): String = this.toString().toPersianDigits()

/**
 * Converts a float to Persian-Indic digit string with no decimal places.
 */
fun Float.toPersianDigits(): String = this.toInt().toString().toPersianDigits()
