package com.pning80.watchmycalories.data

import java.util.Calendar

enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack");

    companion object {
        /** Maps a timestamp to the appropriate meal type based on hour of day. Matches iOS logic. */
        fun fromTimestamp(timestampMillis: Long): MealType {
            val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
            return when (calendar.get(Calendar.HOUR_OF_DAY)) {
                in 7..9 -> BREAKFAST
                in 11..14 -> LUNCH
                in 17..20 -> DINNER
                else -> SNACK
            }
        }

        /** Looks up by raw string stored in Room (e.g. "Breakfast"). Falls back to SNACK. */
        fun fromRaw(raw: String?): MealType {
            return entries.find { it.displayName == raw } ?: SNACK
        }

        /** Canonical display order for UI sections. */
        val displayOrder = listOf(BREAKFAST, LUNCH, DINNER, SNACK)
    }
}
