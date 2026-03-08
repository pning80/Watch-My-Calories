package com.pning80.watchmycalories.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Gender {
    MALE, FEMALE, OTHER
}

enum class ActivityLevel {
    SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTRA_ACTIVE
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 0, // Single row, always 0
    val heightCm: Double,
    val weightKg: Double,
    val age: Int,
    val gender: Gender,
    val activityLevel: ActivityLevel,
    val targetCalories: Double
)
