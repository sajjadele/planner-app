package com.example.core.util

/** Unicode Right-to-Left Mark (U+200F) — forces paragraph direction to RTL. */
const val RTL = "\u200F"

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

/**
 * Wraps a string in Unicode First-Strong Isolate characters (\u2066 ... \u2069).
 * Prevents bidirectional text reordering when Latin-script content (numbers,
 * English text) is embedded within Persian (RTL) text.
 *
 * Example: "09:30".isolated() → "\u206609:30\u2069"
 * In Persian context: "ساعت \u206609:30\u2069" renders correctly as "ساعت 09:30"
 */
fun String.isolated(): String = "\u2066$this\u2069"

/** Isolates an Int for safe BiDi rendering in Persian text. */
fun Int.isolated(): String = this.toString().isolated()

/** Isolates a Long for safe BiDi rendering in Persian text. */
fun Long.isolated(): String = this.toString().isolated()
