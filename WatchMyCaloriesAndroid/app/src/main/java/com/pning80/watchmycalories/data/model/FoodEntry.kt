package com.pning80.watchmycalories.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Double,
    val quantity: String,
    val timestamp: Long,
    val imagePath: String?,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val mealType: MealType
)
