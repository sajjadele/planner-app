package com.example.plugins.planner.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * ReminderTimeStateTest — Phase 1 tests for the pure reminder time state layer.
 *
 * Verifies:
 * - 12h + AM/PM -> 24h conversion (12 AM = 0, 12 PM = 12, 8 PM = 20, …)
 * - 24h -> internal conversion + minute snapping
 * - Hour boundary wrapping (11 -> 12 -> 1)
 * - Minute protection (values stay within 0..55 step 5; 55 <-> 00 wrap)
 * - Selection semantics (hour advances to minute mode, minute stays)
 * - Period toggle
 * - confirmValue returns the correct 24h pair
 */
class ReminderTimeStateTest {

    // ═══════════════════════════════════════════
    // Conversion — internal 12h+period -> 24h
    // ═══════════════════════════════════════════

    @Test
    fun `midnight 12 AM converts to 0`() {
        assertEquals(0, ReminderTimeState(12, 0, DayPeriod.AM).to24Hour())
    }

    @Test
    fun `1 AM converts to 1`() {
        assertEquals(1, ReminderTimeState(1, 0, DayPeriod.AM).to24Hour())
    }

    @Test
    fun `noon 12 PM converts to 12`() {
        assertEquals(12, ReminderTimeState(12, 0, DayPeriod.PM).to24Hour())
    }

    @Test
    fun `8 PM converts to 20`() {
        assertEquals(20, ReminderTimeState(8, 0, DayPeriod.PM).to24Hour())
    }

    @Test
    fun `1 PM converts to 13`() {
        assertEquals(13, ReminderTimeState(1, 0, DayPeriod.PM).to24Hour())
    }

    @Test
    fun `11 PM converts to 23`() {
        assertEquals(23, ReminderTimeState(11, 0, DayPeriod.PM).to24Hour())
    }

    @Test
    fun `6 AM converts to 6`() {
        assertEquals(6, ReminderTimeState(6, 0, DayPeriod.AM).to24Hour())
    }

    // ═══════════════════════════════════════════
    // Reverse conversion — 24h -> internal
    // ═══════════════════════════════════════════

    @Test
    fun `from24Hour midnight 0 maps to 12 AM`() {
        val s = ReminderTimeState.from24Hour(0, 0)
        assertEquals(12, s.hour)
        assertEquals(DayPeriod.AM, s.period)
    }

    @Test
    fun `from24Hour noon 12 maps to 12 PM`() {
        val s = ReminderTimeState.from24Hour(12, 0)
        assertEquals(12, s.hour)
        assertEquals(DayPeriod.PM, s.period)
    }

    @Test
    fun `from24Hour 23 maps to 11 PM`() {
        val s = ReminderTimeState.from24Hour(23, 0)
        assertEquals(11, s.hour)
        assertEquals(DayPeriod.PM, s.period)
    }

    @Test
    fun `from24Hour 13 maps to 1 PM`() {
        val s = ReminderTimeState.from24Hour(13, 30)
        assertEquals(1, s.hour)
        assertEquals(DayPeriod.PM, s.period)
    }

    @Test
    fun `from24Hour snaps minute to nearest 5 step`() {
        assertEquals(0, ReminderTimeState.from24Hour(8, 2).minute)
        assertEquals(5, ReminderTimeState.from24Hour(8, 3).minute)
        assertEquals(55, ReminderTimeState.from24Hour(8, 56).minute)
    }

    // ═══════════════════════════════════════════
    // Hour boundary wrapping
    // ═══════════════════════════════════════════

    @Test
    fun `increasing hour from 11 goes to 12`() {
        val picker = ReminderTimePickerState(ReminderTimeState(11, 0, DayPeriod.AM))
        picker.increaseHour()
        assertEquals(12, picker.timeState.hour)
    }

    @Test
    fun `increasing hour from 12 wraps to 1`() {
        val picker = ReminderTimePickerState(ReminderTimeState(12, 0, DayPeriod.AM))
        picker.increaseHour()
        assertEquals(1, picker.timeState.hour)
    }

    @Test
    fun `decreasing hour from 1 wraps to 12`() {
        val picker = ReminderTimePickerState(ReminderTimeState(1, 0, DayPeriod.AM))
        picker.decreaseHour()
        assertEquals(12, picker.timeState.hour)
    }

    @Test
    fun `decreasing hour from 12 goes to 11`() {
        val picker = ReminderTimePickerState(ReminderTimeState(12, 0, DayPeriod.AM))
        picker.decreaseHour()
        assertEquals(11, picker.timeState.hour)
    }
}

    // ═══════════════════════════════════════════
    // Minute protection (0..55 step 5)
    // ═══════════════════════════════════════════

    @Test
    fun `increasing minute from 55 wraps to 00`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 55, DayPeriod.AM))
        picker.increaseMinute()
        assertEquals(0, picker.timeState.minute)
    }

    @Test
    fun `decreasing minute from 00 wraps to 55`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 0, DayPeriod.AM))
        picker.decreaseMinute()
        assertEquals(55, picker.timeState.minute)
    }

    @Test
    fun `minute never exceeds 55`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 50, DayPeriod.AM))
        picker.increaseMinute() // -> 55
        picker.increaseMinute() // -> 00 (wrap, not 60)
        picker.increaseMinute() // -> 05
        assertEquals(5, picker.timeState.minute)
    }

    @Test
    fun `selectMinute snaps non-multiple-of-5 to nearest`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 0, DayPeriod.AM))
        picker.selectMinute(22)
        assertEquals(20, picker.timeState.minute)
        picker.selectMinute(23)
        assertEquals(25, picker.timeState.minute)
    }

    // ═══════════════════════════════════════════
    // Selection semantics
    // ═══════════════════════════════════════════

    @Test
    fun `selectHour updates hour and advances to minute mode`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 0, DayPeriod.AM))
        picker.selectHour(9)
        assertEquals(9, picker.timeState.hour)
        assertEquals(TimeSelectionMode.MINUTE, picker.mode)
    }

    @Test
    fun `selectHour coerces out-of-range to 1..12`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 0, DayPeriod.AM))
        picker.selectHour(0)
        assertEquals(1, picker.timeState.hour)
        picker.selectHour(13)
        assertEquals(12, picker.timeState.hour)
    }

    @Test
    fun `selectMinute updates minute and stays in minute mode`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 0, DayPeriod.AM))
        picker.mode = TimeSelectionMode.MINUTE
        picker.selectMinute(30)
        assertEquals(30, picker.timeState.minute)
        assertEquals(TimeSelectionMode.MINUTE, picker.mode)
    }

    @Test
    fun `setMode switches presentation mode`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 0, DayPeriod.AM))
        assertEquals(TimeSelectionMode.HOUR, picker.mode)
        picker.mode = TimeSelectionMode.MINUTE
        assertEquals(TimeSelectionMode.MINUTE, picker.mode)
    }

    @Test
    fun `togglePeriod switches AM to PM and back`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 30, DayPeriod.AM))
        picker.togglePeriod()
        assertEquals(DayPeriod.PM, picker.timeState.period)
        picker.togglePeriod()
        assertEquals(DayPeriod.AM, picker.timeState.period)
    }

    @Test
    fun `period toggle leaves hour and minute unchanged`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 30, DayPeriod.AM))
        picker.togglePeriod()
        assertEquals(8, picker.timeState.hour)
        assertEquals(30, picker.timeState.minute)
    }

    // ═══════════════════════════════════════════
    // Swipe mutations write the same single state
    // ═══════════════════════════════════════════

    @Test
    fun `swipe up increments hour through the shared state`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 0, DayPeriod.AM))
        picker.increaseHour()
        assertEquals(9, picker.timeState.hour)
        // Clock and swipe share one value — selecting from clock reflects the same state
        picker.selectHour(10)
        assertEquals(10, picker.timeState.hour)
    }

    @Test
    fun `swipe up minute goes 30 to 35`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 30, DayPeriod.AM))
        picker.increaseMinute()
        assertEquals(35, picker.timeState.minute)
    }

    @Test
    fun `swipe down minute goes 30 to 25`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 30, DayPeriod.AM))
        picker.decreaseMinute()
        assertEquals(25, picker.timeState.minute)
    }

    // ═══════════════════════════════════════════
    // Confirm
    // ═══════════════════════════════════════════

    @Test
    fun `confirmValue returns correct 24h hour and minute`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 30, DayPeriod.PM))
        assertEquals(20 to 30, picker.confirmValue())
    }

    @Test
    fun `confirmValue midnight returns 0`() {
        val picker = ReminderTimePickerState(ReminderTimeState(12, 0, DayPeriod.AM))
        assertEquals(0 to 0, picker.confirmValue())
    }

    @Test
    fun `confirmValue noon returns 12`() {
        val picker = ReminderTimePickerState(ReminderTimeState(12, 0, DayPeriod.PM))
        assertEquals(12 to 0, picker.confirmValue())
    }

    @Test
    fun `toggling period changes the confirmed value`() {
        val picker = ReminderTimePickerState(ReminderTimeState(8, 0, DayPeriod.AM))
        assertEquals(8 to 0, picker.confirmValue())
        picker.togglePeriod()
        assertEquals(20 to 0, picker.confirmValue())
        assertNotEquals(8, picker.confirmValue().first)
    }

    @Test
    fun `default state is morning 8 00`() {
        val picker = ReminderTimePickerState()
        assertEquals(8, picker.timeState.hour)
        assertEquals(0, picker.timeState.minute)
        assertEquals(DayPeriod.AM, picker.timeState.period)
        assertEquals(TimeSelectionMode.HOUR, picker.mode)
    }
}
