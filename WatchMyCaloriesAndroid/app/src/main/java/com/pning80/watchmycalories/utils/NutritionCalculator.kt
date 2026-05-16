package com.pning80.watchmycalories.utils

import kotlin.math.max
import kotlin.math.roundToInt

object NutritionCalculator {

    /**
     * Replicates exactly the Mifflin-St Jeor algorithmic baseline.
     * Male: (10 × weight in kg) + (6.25 × height in cm) - (5 × age in years) + 5
     * Female: (10 × weight in kg) + (6.25 × height in cm) - (5 × age in years) - 161
     */
    fun calculateTargetCalories(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        genderRaw: String,
        activityLevelRaw: String
    ): Double {
        var baseBmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age)
        
        baseBmr += if (genderRaw.equals("Male", ignoreCase = true)) {
            5.0
        } else {
            -161.0
        }

        // Standard Mifflin Activity Multipliers
        val multiplier = when (activityLevelRaw.lowercase()) {
            "sedentary" -> 1.2
            "lightly active" -> 1.375
            "moderately active" -> 1.55
            "very active" -> 1.725
            "extra active" -> 1.9
            else -> 1.2
        }

        return (baseBmr * multiplier).roundToInt().toDouble()
    }

    /**
     * Calculates visually mapped remaining bounds safe-guarded to 0 for the Dashboard target
     */
    fun calculateRemainingSafe(target: Double, burned: Double, consumed: Double): Double {
        val calculated = (target + burned) - consumed
        return max(0.0, calculated)
    }
}
