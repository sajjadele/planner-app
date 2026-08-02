package com.example.core.util

/**
 * Normalizes Persian/Arabic text for forgiving search matching.
 *
 * Handles the common mismatches that break a naive contains() search in a
 * Persian app:
 *  - Letter variants: Arabic `ي`/`ى` → Persian `ی`, Arabic `ك` → `ک`,
 *    `ة`/`ۀ` → `ه`, `أ`/`إ`/`آ` → `ا`, `ؤ` → `و`
 *  - Half-space (نیمفاصله, U+200C): removed so a title stored with a ZWNJ
 *    matches a query typed without it (and vice-versa)
 *  - Digits: Persian-Indic (۰-۹) and Arabic-Indic (٠-٩) → Western (0-9)
 *  - Latin letters lower-cased
 *
 * Pure Kotlin — no Android dependencies, host-JVM testable.
 */
fun String.normalizeForSearch(): String {
    if (this.isEmpty()) return this
    return buildString(this.length) {
        for (ch in this@normalizeForSearch) {
            when (ch) {
                'ي', 'ى' -> append('ی')      // Arabic yeh / alif maqsura → Persian yeh
                'ك' -> append('ک')            // Arabic kaf → Persian kaf
                'ة', 'ۀ' -> append('ه')       // teh marbuta → heh
                'أ', 'إ', 'آ' -> append('ا')  // hamza carriers → alef
                'ؤ' -> append('و')
                '\u200C' -> Unit              // ZWNJ (نیمفاصله) — drop
                else -> append(ch)
            }
        }
    }.toEnglishDigits().lowercase()
}
