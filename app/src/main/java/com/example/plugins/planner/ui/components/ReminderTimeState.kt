package com.example.plugins.planner.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Reminder time picker state layer (Phase 1 of the picker refactor).
 *
 * This is intentionally decoupled from the UI composables so it stays pure,
 * focused, and host-JVM testable. The only Compose pieces are the [Stable]
 * annotation and [mutableStateOf] backing fields so the picker is reactive
 * with a single source of truth — all hour/minute values live ONLY here.
 *
 * Internal clock model is 12-hour + [DayPeriod] (a clock face is naturally
 * 12-hour). Persistence stays 24-hour; see [ReminderTimeState.to24Hour] /
 * [ReminderTimeState.from24Hour] which perform the conversion ONLY at the
 * picker boundary. The database, [TaskEntity] reminder fields, and the
 * scheduler are never touched.
 */

enum class DayPeriod { AM, PM }

/** Which clock face is currently rendered. Presentation state only. */
enum class TimeSelectionMode { HOUR, MINUTE }

/**
 * The single, immutable snapshot of the picker's time value.
 *
 * - [hour]: 1..12 (a natural 12-hour clock face)
 * - [minute]: 0..55 in steps of 5 (clock granularity), e.g. 00, 05, … 55
 * - [period]: AM/PM disambiguation
 */
data class ReminderTimeState(
    val hour: Int,
    val minute: Int,
    val period: DayPeriod
) {
    /** Convert this 12h+period value to a 24-hour hour (0..23) for persistence. */
    fun to24Hour(): Int = when (period) {
        DayPeriod.AM -> if (hour == 12) 0 else hour
        DayPeriod.PM -> if (hour == 12) 12 else hour + 12
    }

    companion object {
        const val MINUTE_STEP = 5
        const val MIN_MINUTE = 0
        const val MAX_MINUTE = 55

        /**
         * Build internal state from a persisted 24-hour value (0..23 hour,
         * 0..59 minute), snapping the minute to the nearest 5-step in 0..55.
         */
        fun from24Hour(hour24: Int, minute: Int): ReminderTimeState {
            val h = ((hour24 % 24) + 24) % 24
            val hour12 = when {
                h == 0 -> 12            // midnight => 12 AM
                h in 1..12 -> h
                else -> h - 12          // 13..23 => PM hour in 1..11
            }
            val period = if (h < 12) DayPeriod.AM else DayPeriod.PM
            return ReminderTimeState(
                hour = hour12,
                minute = snapMinute(minute),
                period = period
            )
        }

        /** Snap any 0..59 minute to the nearest 5-step, clamped to 0..55. */
        fun snapMinute(minute: Int): Int {
            val snapped = ((minute + MINUTE_STEP / 2) / MINUTE_STEP) * MINUTE_STEP
            return snapped.coerceIn(MIN_MINUTE, MAX_MINUTE)
        }
    }
}

/**
 * The ONLY permitted owner of reminder time state.
 *
 * Both supported interactions — tapping the clock face and vertical swiping
 * on the digital display — funnel into these methods. No UI component may
 * hold its own hour/minute value.
 */
@Stable
class ReminderTimePickerState(
    initial: ReminderTimeState = ReminderTimeState(8, 0, DayPeriod.AM)
) {
    /** Single source of truth for the time value. */
    var timeState by mutableStateOf(initial)
        private set

    /**
     * Which face (hour/minute) is visible. Presentation-only, not a data truth.
     * Set by tapping the clock mode chips (ساعت / دقیقه).
     */
    var mode by mutableStateOf(TimeSelectionMode.HOUR)

    /** Tap an hour on the clock face. Advances to the minute face for a continuous flow. */
    fun selectHour(hour: Int) {
        timeState = timeState.copy(hour = hour.coerceIn(1, 12))
        mode = TimeSelectionMode.MINUTE
    }

    /** Tap a minute on the clock face. Stays on the minute face. */
    fun selectMinute(minute: Int) {
        timeState = timeState.copy(minute = ReminderTimeState.snapMinute(minute))
    }

    /** Swipe up on hour: 1..12, wrapping 12 -> 1. */
    fun increaseHour() {
        val next = (timeState.hour % 12) + 1
        timeState = timeState.copy(hour = next)
    }

    /** Swipe down on hour: 12..1, wrapping 1 -> 12. */
    fun decreaseHour() {
        val prev = if (timeState.hour == 1) 12 else timeState.hour - 1
        timeState = timeState.copy(hour = prev)
    }

    /** Swipe up on minute: +5, wrapping 55 -> 00. Never exceeds 55. */
    fun increaseMinute() {
        val next = (timeState.minute + ReminderTimeState.MINUTE_STEP) % 60
        timeState = timeState.copy(minute = ReminderTimeState.snapMinute(next))
    }

    /** Swipe down on minute: -5, wrapping 00 -> 55. Never below 00. */
    fun decreaseMinute() {
        val prev = (timeState.minute - ReminderTimeState.MINUTE_STEP + 60) % 60
        timeState = timeState.copy(minute = ReminderTimeState.snapMinute(prev))
    }

    fun togglePeriod() {
        timeState = timeState.copy(
            period = if (timeState.period == DayPeriod.AM) DayPeriod.PM else DayPeriod.AM
        )
    }

    /**
     * The 24-hour (hour, minute) pair handed to the unchanged
     * `onSetReminder(hour: Int, minute: Int)` callback on confirm.
     */
    fun confirmValue(): Pair<Int, Int> = timeState.to24Hour() to timeState.minute
}
