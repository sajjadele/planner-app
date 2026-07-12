fun main() {
    // Test: July 11, 2026 → should be 20 Tir 1405
    val j = JalaliDate.fromGregorian(2026, 7, 11)
    val g = JalaliDate.toGregorian(j)
    println("July 11, 2026 → $j")
    println("Round-trip → ${g.first}/${g.second}/${g.third}")
    check(j == JalaliDate(1405, 4, 20)) { "Expected 1405/4/20, got $j" }
    check(g.first == 2026 && g.second == 7 && g.third == 11) { "Round-trip fail: $g" }

    // Test: epoch ms of today
    val now = System.currentTimeMillis()
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = now
    val gYear = cal.get(java.util.Calendar.YEAR)
    val gMonth = cal.get(java.util.Calendar.MONTH) + 1
    val gDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val fromEpoch = JalaliDate.fromEpochMs(now)
    println("Today (${gYear}/${gMonth}/${gDay}) → $fromEpoch")

    // Test: epoch round-trip
    val epoch2 = JalaliDate.toEpochMs(fromEpoch)
    val fromEpoch2 = JalaliDate.fromEpochMs(epoch2)
    println("Epoch round-trip: $fromEpoch → $fromEpoch2")
    check(fromEpoch == fromEpoch2) { "Epoch round-trip fail" }

    // Test: day-of-week
    // July 11, 2026 is Saturday = index 0
    val dow = JalaliDate.dayOfWeekIndex(now)
    val dowName = JalaliDate.DAY_NAMES[dow]
    println("Day of week index: $dow ($dowName)")

    // Test: saturdayOfWeek
    val sat = JalaliDate.saturdayOfWeek(now)
    val satDate = JalaliDate.fromEpochMs(sat)
    println("Saturday of this week: $satDate")

    println("\nAll checks passed ✓")
}
