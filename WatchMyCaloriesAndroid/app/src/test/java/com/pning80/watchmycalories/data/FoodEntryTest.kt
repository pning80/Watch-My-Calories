package com.pning80.watchmycalories.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class FoodEntryTest {

    @Test
    fun testInitSetsAllProperties() {
        val timestamp = System.currentTimeMillis()
        val entry = FoodEntry(
            id = "123",
            name = "Eggs",
            calories = 150.0,
            quantity = "2 eggs",
            timestamp = timestamp,
            protein = 12.0,
            carbs = 1.0,
            fat = 10.0,
            imageID = "img123",
            mealName = "Morning Eggs",
            mealTypeRaw = "Breakfast"
        )
        assertEquals("123", entry.id)
        assertEquals("Eggs", entry.name)
        assertEquals(150.0, entry.calories, 0.0)
        assertEquals("2 eggs", entry.quantity)
        assertEquals(timestamp, entry.timestamp)
        assertEquals(12.0, entry.protein!!, 0.0)
        assertEquals(1.0, entry.carbs!!, 0.0)
        assertEquals(10.0, entry.fat!!, 0.0)
        assertEquals("img123", entry.imageID)
        assertEquals("Morning Eggs", entry.mealName)
        assertEquals("Breakfast", entry.mealTypeRaw)
    }

    @Test
    fun testInitGeneratesUniqueID() {
        val entry1 = FoodEntry(
            name = "Apple",
            calories = 95.0,
            quantity = "1 medium",
            timestamp = System.currentTimeMillis(),
            protein = null,
            carbs = null,
            fat = null,
            imageID = null,
            mealName = null,
            mealTypeRaw = "Snack"
        )
        val entry2 = FoodEntry(
            name = "Banana",
            calories = 105.0,
            quantity = "1 medium",
            timestamp = System.currentTimeMillis(),
            protein = null,
            carbs = null,
            fat = null,
            imageID = null,
            mealName = null,
            mealTypeRaw = "Snack"
        )
        assertNotEquals(entry1.id, entry2.id)
        // Verify they are valid UUID strings
        UUID.fromString(entry1.id)
        UUID.fromString(entry2.id)
    }

    @Test
    fun testEntryWithNoMacros() {
        val entry = FoodEntry(
            name = "Water",
            calories = 0.0,
            quantity = "500 ml",
            timestamp = System.currentTimeMillis(),
            protein = null,
            carbs = null,
            fat = null,
            imageID = null,
            mealName = null,
            mealTypeRaw = "Snack"
        )
        assertNull(entry.protein)
        assertNull(entry.carbs)
        assertNull(entry.fat)
    }

    @Test
    fun testOptionalFieldsImageAndMealName() {
        val entryWithOptional = FoodEntry(
            name = "Salad",
            calories = 120.0,
            quantity = "1 plate",
            timestamp = System.currentTimeMillis(),
            protein = 2.0,
            carbs = 8.0,
            fat = 9.0,
            imageID = "img-salad-456",
            mealName = "Lunch Green Salad",
            mealTypeRaw = "Lunch"
        )
        assertEquals("img-salad-456", entryWithOptional.imageID)
        assertEquals("Lunch Green Salad", entryWithOptional.mealName)

        val entryWithoutOptional = FoodEntry(
            name = "Coffee",
            calories = 5.0,
            quantity = "1 cup",
            timestamp = System.currentTimeMillis(),
            protein = null,
            carbs = null,
            fat = null,
            imageID = null,
            mealName = null,
            mealTypeRaw = "Snack"
        )
        assertNull(entryWithoutOptional.imageID)
        assertNull(entryWithoutOptional.mealName)
    }

    @Test
    fun testMealTypeEnumHelperRoundTrip() {
        for (mealType in MealType.entries) {
            val entry = FoodEntry(
                name = "Test Food",
                calories = 100.0,
                quantity = "1 serving",
                timestamp = System.currentTimeMillis(),
                protein = null,
                carbs = null,
                fat = null,
                imageID = null,
                mealName = null,
                mealTypeRaw = mealType.displayName
            )
            val parsedType = MealType.fromRaw(entry.mealTypeRaw)
            assertEquals(mealType, parsedType)
            assertEquals(mealType.displayName, entry.mealTypeRaw)
        }
    }
}
