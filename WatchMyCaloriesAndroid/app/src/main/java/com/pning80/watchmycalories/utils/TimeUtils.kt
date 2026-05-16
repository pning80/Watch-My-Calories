package com.pning80.watchmycalories.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    /**
     * Determines whether a given epoch timestamp falls upon the current localized day.
     */
    fun isToday(timestamp: Long, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val dateLog = Date(timestamp)
        val today = Date(currentTimeMillis)
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(dateLog) == fmt.format(today)
    }

    /**
     * Automatically assigns meal strings matching iOS implementation.
     * 00:00 -> 10:30 : Breakfast
     * 10:30 -> 15:00 : Lunch
     * 15:00 -> 21:00 : Dinner
     * Else : Snack
     */
    fun autoAssignMeal(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val fractionalHour = hour + (minutes / 60.0)

        return when {
            fractionalHour in 0.0..10.5 -> "Breakfast"
            fractionalHour > 10.5 && fractionalHour <= 15.0 -> "Lunch"
            fractionalHour > 15.0 && fractionalHour <= 21.0 -> "Dinner"
            else -> "Snack"
        }
    }
}
