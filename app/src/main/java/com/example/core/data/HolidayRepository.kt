package com.example.core.data

import android.content.Context
import com.example.core.domain.CalendarDate
import com.example.core.domain.Holiday
import org.json.JSONObject

/**
 * Supplies holiday data from an embedded holidays.json file.
 *
 * Offline-first: the JSON is bundled in assets and parsed at query time.
 * The file is structured as a flat list for the current Jalali year.
 * All fields use unabbreviated keys ("month", "day", "name") for clarity.
 */
class HolidayRepository(private val context: Context) {

    private var allHolidays: List<Holiday>? = null

    /**
     * Return all holidays falling on [date].
     */
    fun getHolidays(date: CalendarDate): List<Holiday> {
        val yearHolidays = allHolidays ?: loadHolidays()
        return yearHolidays.filter {
            it.jalaliMonth == date.jalaliMonth && it.jalaliDay == date.jalaliDay
        }
    }

    /** True if [date] has any holiday. */
    fun isHoliday(date: CalendarDate): Boolean = getHolidays(date).isNotEmpty()

    private fun loadHolidays(): List<Holiday> {
        val result = try {
            val text = context.assets.open("holidays.json")
                .bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val arr = json.getJSONArray("holidays")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Holiday(
                    name = obj.getString("name"),
                    jalaliMonth = obj.getInt("month"),
                    jalaliDay = obj.getInt("day"),
                    isOfficial = true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
        allHolidays = result
        return result
    }
}
