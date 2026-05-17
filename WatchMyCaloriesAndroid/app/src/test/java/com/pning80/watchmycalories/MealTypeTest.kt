package com.pning80.watchmycalories

import com.google.gson.JsonParser
import com.pning80.watchmycalories.data.MealType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Hour-by-hour bucketing for meal-type auto-assignment.
 * Per PORTING_CRITERIA.md T1.3 + Resolved Decisions #1: every hour 0..23 must
 * produce the same MealType on iOS and Android even though the platforms
 * express the ranges differently (iOS exclusive, Android inclusive).
 *
 * Expected mapping (matches iOS):
 *   00..06 → SNACK
 *   07..09 → BREAKFAST
 *   10     → SNACK
 *   11..14 → LUNCH
 *   15..16 → SNACK
 *   17..20 → DINNER
 *   21..23 → SNACK
 */
class MealTypeTest {

    private fun timestampForHour(hour: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Test fun `every hour maps to the expected meal type`() {
        val expected = mapOf(
            0 to MealType.SNACK,  1 to MealType.SNACK,  2 to MealType.SNACK,  3 to MealType.SNACK,
            4 to MealType.SNACK,  5 to MealType.SNACK,  6 to MealType.SNACK,
            7 to MealType.BREAKFAST, 8 to MealType.BREAKFAST, 9 to MealType.BREAKFAST,
            10 to MealType.SNACK,
            11 to MealType.LUNCH, 12 to MealType.LUNCH, 13 to MealType.LUNCH, 14 to MealType.LUNCH,
            15 to MealType.SNACK, 16 to MealType.SNACK,
            17 to MealType.DINNER, 18 to MealType.DINNER, 19 to MealType.DINNER, 20 to MealType.DINNER,
            21 to MealType.SNACK, 22 to MealType.SNACK, 23 to MealType.SNACK,
        )
        val actual = (0..23).associateWith { MealType.fromTimestamp(timestampForHour(it)) }
        assertEquals(expected, actual)
    }

    @Test fun `fromRaw round-trips displayName values`() {
        for (mt in MealType.entries) {
            assertEquals(mt, MealType.fromRaw(mt.displayName))
        }
    }

    @Test fun `fromRaw on null or unknown falls back to SNACK`() {
        assertEquals(MealType.SNACK, MealType.fromRaw(null))
        assertEquals(MealType.SNACK, MealType.fromRaw(""))
        assertEquals(MealType.SNACK, MealType.fromRaw("Brunch"))
    }

    // --- Shared cross-platform fixture ---
    // shared-fixtures/meal-type-by-hour/cases.json is the single source of truth.
    // iOS consumes the same file; if either platform's hour-bucketing drifts, this
    // test (and its iOS twin) fail on the divergent hour.
    @Test fun `shared fixture meal-type-by-hour maps every hour identically`() {
        val resource = javaClass.classLoader.getResource("meal-type-by-hour/cases.json")
            ?: error("Missing shared fixture resource. Did the sourceSets entry in app/build.gradle.kts get removed?")
        val root = JsonParser.parseString(resource.readText()).asJsonObject
        val byHour = root.getAsJsonObject("byHour")
        for (hour in 0..23) {
            val expectedRaw = byHour.get(hour.toString()).asString
            val expected = MealType.valueOf(expectedRaw)
            val actual = MealType.fromTimestamp(timestampForHour(hour))
            assertEquals("hour=$hour", expected, actual)
        }
    }
}
