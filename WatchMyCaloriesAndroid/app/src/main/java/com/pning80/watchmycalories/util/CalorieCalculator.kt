package com.pning80.watchmycalories.util

import com.pning80.watchmycalories.data.model.ActivityLevel
import com.pning80.watchmycalories.data.model.Gender

object CalorieCalculator {
    fun calculateBMR(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender
    ): Double {
        // Mifflin-St Jeor Equation
        return when (gender) {
            Gender.MALE -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
            Gender.FEMALE -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
            else -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5 // Default to Male formula
        }
    }

    fun calculateTDEE(bmr: Double, activityLevel: ActivityLevel): Double {
        val multiplier = when (activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2
            ActivityLevel.LIGHTLY_ACTIVE -> 1.375
            ActivityLevel.MODERATELY_ACTIVE -> 1.55
            ActivityLevel.VERY_ACTIVE -> 1.725
            ActivityLevel.EXTRA_ACTIVE -> 1.9
        }
        return bmr * multiplier
    }
}
