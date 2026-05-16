package com.pning80.watchmycalories.data

import kotlin.math.roundToInt

/**
 * Mifflin-St Jeor equation with TDEE activity multiplier.
 * Matches the iOS CalorieCalculator exactly.
 */
object CalorieCalculator {

    enum class Gender(val displayName: String) {
        MALE("Male"),
        FEMALE("Female"),
        OTHER("Other");

        companion object {
            fun fromRaw(raw: String?): Gender =
                entries.find { it.displayName == raw } ?: OTHER
        }
    }

    enum class ActivityLevel(val displayName: String, val multiplier: Double) {
        SEDENTARY("Sedentary", 1.2),
        LIGHTLY_ACTIVE("Lightly Active", 1.375),
        MODERATELY_ACTIVE("Moderately Active", 1.55),
        VERY_ACTIVE("Very Active", 1.725);

        companion object {
            fun fromRaw(raw: String?): ActivityLevel =
                entries.find { it.displayName == raw } ?: SEDENTARY
        }
    }

    /**
     * Calculate recommended daily calorie intake.
     * @param heightCm Height in centimeters
     * @param weightKg Weight in kilograms
     * @param age Age in years
     * @param gender Gender enum
     * @param activityLevel Activity level enum
     * @return Recommended daily calories (rounded to nearest whole number)
     */
    fun recommended(
        heightCm: Double,
        weightKg: Double,
        age: Int,
        gender: Gender,
        activityLevel: ActivityLevel
    ): Double {
        // Mifflin-St Jeor BMR
        var bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age.toDouble())
        bmr += if (gender == Gender.MALE) 5.0 else -161.0

        return (bmr * activityLevel.multiplier).roundToInt().toDouble()
    }
}
