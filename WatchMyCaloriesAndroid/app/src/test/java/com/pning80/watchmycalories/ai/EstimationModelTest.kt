package com.pning80.watchmycalories.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class EstimationModelTest {

    @Test
    fun testTotalCaloriesSumsItems() {
        val result = EstimationResult(
            mealName = "Fruity Breakfast",
            items = listOf(
                EstimationItem(name = "Apple", quantity = "1 medium", calories = 95.0, confidence = 0.9),
                EstimationItem(name = "Banana", quantity = "1 medium", calories = 105.0, confidence = 0.85),
                EstimationItem(name = "Orange", quantity = "1 medium", calories = 62.0, confidence = 0.88)
            )
        )
        assertEquals(262.0, result.totalCalories, 0.0)
    }

    @Test
    fun testTotalCaloriesEmptyItems() {
        val result = EstimationResult(
            mealName = null,
            items = emptyList()
        )
        assertEquals(0.0, result.totalCalories, 0.0)
    }

    @Test
    fun testTotalMacrosSumsItems() {
        val result = EstimationResult(
            mealName = "High Protein Lunch",
            items = listOf(
                EstimationItem(name = "Chicken", quantity = "6 oz", calories = 280.0, confidence = 0.9, protein = 40.0, carbs = 0.0, fat = 12.0),
                EstimationItem(name = "Rice", quantity = "1 cup", calories = 200.0, confidence = 0.85, protein = 4.0, carbs = 45.0, fat = 0.5)
            )
        )
        assertEquals(44.0, result.totalProtein, 0.0)
        assertEquals(45.0, result.totalCarbs, 0.0)
        assertEquals(12.5, result.totalFat, 0.0)
    }

    @Test
    fun testTotalMacrosWithNilValues() {
        val result = EstimationResult(
            mealName = null,
            items = listOf(
                EstimationItem(name = "Apple", quantity = "1 medium", calories = 95.0, confidence = 0.9, protein = 0.5, carbs = 25.0, fat = 0.3),
                EstimationItem(name = "Mystery", quantity = "1 serving", calories = 300.0, confidence = 0.5, protein = null, carbs = null, fat = null)
            )
        )
        assertEquals(0.5, result.totalProtein, 0.0)
        assertEquals(25.0, result.totalCarbs, 0.0)
        assertEquals(0.3, result.totalFat, 0.0)
    }

    @Test
    fun testTotalMacrosEmptyItems() {
        val result = EstimationResult(items = emptyList())
        assertEquals(0.0, result.totalProtein, 0.0)
        assertEquals(0.0, result.totalCarbs, 0.0)
        assertEquals(0.0, result.totalFat, 0.0)
    }

    @Test
    fun testTotalMacrosAllNil() {
        val result = EstimationResult(
            items = listOf(
                EstimationItem(name = "A", quantity = "1", calories = 100.0, confidence = 0.5, protein = null, carbs = null, fat = null),
                EstimationItem(name = "B", quantity = "1", calories = 200.0, confidence = 0.5, protein = null, carbs = null, fat = null)
            )
        )
        assertEquals(0.0, result.totalProtein, 0.0)
        assertEquals(0.0, result.totalCarbs, 0.0)
        assertEquals(0.0, result.totalFat, 0.0)
    }
}
