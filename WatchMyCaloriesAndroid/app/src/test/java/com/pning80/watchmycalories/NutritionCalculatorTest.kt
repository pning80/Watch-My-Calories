package com.pning80.watchmycalories

import com.pning80.watchmycalories.utils.NutritionCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun `Mifflin formula strictly maps biological metrics for healthy adult male`() {
        // Standard profile: 80 kg, 180 cm, 30 years old, Male, Sedentary
        // Math: (10 * 80) + (6.25 * 180) - (5 * 30) + 5 = 800 + 1125 - 150 + 5 = 1780 BMR
        // Target = 1780 * 1.2 (Sedentary) = 2136.0
        val target = NutritionCalculator.calculateTargetCalories(
            weightKg = 80.0,
            heightCm = 180.0,
            age = 30,
            genderRaw = "Male",
            activityLevelRaw = "Sedentary"
        )
        
        assertEquals(2136.0, target, 0.1) // 0.1 delta tolerance bridging floating point precision
    }

    @Test
    fun `Mifflin formula maps bounds for female highly active profile`() {
        // Standard profile: 60 kg, 160 cm, 25 years old, Female, Very Active
        // Math: (10 * 60) + (6.25 * 160) - (5 * 25) - 161 = 600 + 1000 - 125 - 161 = 1314 BMR
        // Target = 1314 * 1.725 (Very Active) = 2266.65 -> Rounded = 2267.0
        val target = NutritionCalculator.calculateTargetCalories(
            weightKg = 60.0,
            heightCm = 160.0,
            age = 25,
            genderRaw = "Female",
            activityLevelRaw = "Very active"
        )

        assertEquals(2267.0, target, 0.1)
    }

    @Test
    fun `Remaining calories never drops strictly below zero`() {
        val remainingNormal = NutritionCalculator.calculateRemainingSafe(target = 2000.0, burned = 500.0, consumed = 1500.0)
        assertEquals(1000.0, remainingNormal, 0.1) // 2000 + 500 - 1500 = 1000

        val remainingDebt = NutritionCalculator.calculateRemainingSafe(target = 2000.0, burned = 0.0, consumed = 4000.0)
        assertEquals(0.0, remainingDebt, 0.1) // Should cap at exactly 0.0 floor
    }
}
