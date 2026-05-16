package com.pning80.watchmycalories

import com.pning80.watchmycalories.utils.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    @Test
    fun `isToday returns true when passed identical timestamps`() {
        val now = System.currentTimeMillis()
        assertTrue(TimeUtils.isToday(now, now))
    }

    @Test
    fun `isToday returns false when passed 48 hours ago`() {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (48 * 60 * 60 * 1000)
        assertFalse(TimeUtils.isToday(twoDaysAgo, now))
    }

    @Test
    fun `autoAssignMeal returns Breakfast for 8 AM`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }
        val meal = TimeUtils.autoAssignMeal(calendar.timeInMillis)
        assertEquals("Breakfast", meal)
    }

    @Test
    fun `autoAssignMeal returns Lunch for 1 PM`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13) // 1:00 PM
            set(Calendar.MINUTE, 0)
        }
        val meal = TimeUtils.autoAssignMeal(calendar.timeInMillis)
        assertEquals("Lunch", meal)
    }

    @Test
    fun `autoAssignMeal returns Snack for 11 PM`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23) // 11 PM
            set(Calendar.MINUTE, 0)
        }
        val meal = TimeUtils.autoAssignMeal(calendar.timeInMillis)
        assertEquals("Snack", meal)
    }
}
