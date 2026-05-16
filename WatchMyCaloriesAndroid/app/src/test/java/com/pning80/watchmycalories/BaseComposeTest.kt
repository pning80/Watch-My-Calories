package com.pning80.watchmycalories

import com.pning80.watchmycalories.data.FoodEntry
import org.junit.Before

abstract class BaseComposeTest {
    
    fun getSampleEntries() = listOf(
        FoodEntry(
            id = "1",
            name = "Oatmeal with Berries",
            calories = 300.0,
            quantity = "1 bowl",
            protein = 10.0,
            carbs = 50.0,
            fat = 6.0,
            timestamp = System.currentTimeMillis(),
            imageId = null,
            mealName = null,
            mealTypeRaw = "Breakfast"
        ),
        FoodEntry(
            id = "2",
            name = "Chicken Salad",
            calories = 450.0,
            quantity = "1 plate",
            protein = 35.0,
            carbs = 20.0,
            fat = 18.0,
            timestamp = System.currentTimeMillis(),
            imageId = null,
            mealName = null,
            mealTypeRaw = "Lunch"
        )
    )
}
