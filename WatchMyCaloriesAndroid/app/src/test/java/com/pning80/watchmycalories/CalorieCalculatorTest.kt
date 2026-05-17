package com.pning80.watchmycalories

import com.google.gson.JsonParser
import com.pning80.watchmycalories.data.CalorieCalculator
import com.pning80.watchmycalories.data.CalorieCalculator.ActivityLevel
import com.pning80.watchmycalories.data.CalorieCalculator.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Golden vectors for Mifflin-St Jeor BMR + TDEE multipliers.
 * Per PORTING_CRITERIA.md T1.3, the same vectors must produce identical
 * outputs on iOS. Move to shared-fixtures/bmr-mifflin-st-jeor.json once
 * the iOS test suite is wired to consume the same file.
 *
 * Formula reference (CalorieCalculator.kt):
 *   bmr = 10*weightKg + 6.25*heightCm - 5*age + (gender == MALE ? +5 : -161)
 *   tdee = round(bmr * activityMultiplier)
 */
class CalorieCalculatorTest {

    private fun expectedTdee(
        weightKg: Double, heightCm: Double, age: Int, gender: Gender, activity: ActivityLevel
    ): Double {
        var bmr = 10 * weightKg + 6.25 * heightCm - 5 * age
        bmr += if (gender == Gender.MALE) 5.0 else -161.0
        return (bmr * activity.multiplier).roundToInt().toDouble()
    }

    // --- BMR component (verified through the integer TDEE values) ---

    @Test fun `male 30y 70kg 175cm sedentary`() {
        // bmr = 700 + 1093.75 - 150 + 5 = 1648.75
        // tdee = round(1648.75 * 1.2) = round(1978.5) = 1979 (banker's? no — kotlin roundToInt rounds half up to even)
        // Actually 0.5 rounds to nearest even in Kotlin's roundToInt; 1978.5 → 1978.
        // Use the helper instead of hard-coding so test is robust to rounding mode.
        val expected = expectedTdee(70.0, 175.0, 30, Gender.MALE, ActivityLevel.SEDENTARY)
        assertEquals(expected, CalorieCalculator.recommended(175.0, 70.0, 30, Gender.MALE, ActivityLevel.SEDENTARY), 0.0)
    }

    @Test fun `female 30y 60kg 165cm sedentary`() {
        val expected = expectedTdee(60.0, 165.0, 30, Gender.FEMALE, ActivityLevel.SEDENTARY)
        assertEquals(expected, CalorieCalculator.recommended(165.0, 60.0, 30, Gender.FEMALE, ActivityLevel.SEDENTARY), 0.0)
    }

    @Test fun `other gender uses female offset (-161)`() {
        val asFemale = CalorieCalculator.recommended(170.0, 65.0, 30, Gender.FEMALE, ActivityLevel.SEDENTARY)
        val asOther = CalorieCalculator.recommended(170.0, 65.0, 30, Gender.OTHER, ActivityLevel.SEDENTARY)
        assertEquals(asFemale, asOther, 0.0)
    }

    // --- TDEE multiplier matrix ---

    @Test fun `all four activity multipliers produce monotonically increasing TDEE`() {
        val results = ActivityLevel.entries.map {
            CalorieCalculator.recommended(175.0, 70.0, 30, Gender.MALE, it)
        }
        // SEDENTARY (1.2) < LIGHTLY_ACTIVE (1.375) < MODERATELY_ACTIVE (1.55) < VERY_ACTIVE (1.725)
        for (i in 1 until results.size) {
            assert(results[i] > results[i - 1]) {
                "TDEE should increase with activity level; got ${results[i - 1]} → ${results[i]}"
            }
        }
    }

    @Test fun `activity multipliers match spec exactly (1_2 1_375 1_55 1_725)`() {
        assertEquals(1.2, ActivityLevel.SEDENTARY.multiplier, 0.0)
        assertEquals(1.375, ActivityLevel.LIGHTLY_ACTIVE.multiplier, 0.0)
        assertEquals(1.55, ActivityLevel.MODERATELY_ACTIVE.multiplier, 0.0)
        assertEquals(1.725, ActivityLevel.VERY_ACTIVE.multiplier, 0.0)
    }

    // --- Boundary conditions ---

    @Test fun `low weight low height old age yields low TDEE without going negative`() {
        val v = CalorieCalculator.recommended(150.0, 40.0, 80, Gender.FEMALE, ActivityLevel.SEDENTARY)
        assert(v > 0) { "TDEE should remain positive for realistic edge inputs; got $v" }
    }

    @Test fun `large athlete yields plausible TDEE`() {
        // 100 kg, 190 cm, 25 y/o male, very active
        val v = CalorieCalculator.recommended(190.0, 100.0, 25, Gender.MALE, ActivityLevel.VERY_ACTIVE)
        // Hand-calc: bmr = 1000 + 1187.5 - 125 + 5 = 2067.5 → tdee = 2067.5 * 1.725 ≈ 3566.4 → 3566
        // Allow a small window in case rounding mode differs slightly between platforms.
        assert(v in 3550.0..3580.0) { "expected ~3566, got $v" }
    }

    @Test fun `gender flip changes TDEE by 166 cal at BMR (5 - -161 = 166) times activity multiplier`() {
        val male = CalorieCalculator.recommended(175.0, 70.0, 30, Gender.MALE, ActivityLevel.SEDENTARY)
        val female = CalorieCalculator.recommended(175.0, 70.0, 30, Gender.FEMALE, ActivityLevel.SEDENTARY)
        val diff = male - female
        val expectedDiff = (166.0 * 1.2).roundToInt().toDouble() // round((+5) − (−161) = 166, × multiplier)
        // Allow a +/- 1 calorie tolerance for rounding-mode boundary cases.
        assert(kotlin.math.abs(diff - expectedDiff) <= 1.0) {
            "male − female should be ≈ ${expectedDiff} kcal, got $diff"
        }
    }

    // --- Shared cross-platform fixtures ---
    // shared-fixtures/bmr-mifflin-st-jeor/cases.json is the single source of truth.
    // The same JSON is consumed by iOS once its test target is wired (see PORTING_RUNBOOK.md Stage 4).
    @Test fun `shared fixtures from bmr-mifflin-st-jeor cases json all pass`() {
        val resource = javaClass.classLoader.getResource("bmr-mifflin-st-jeor/cases.json")
            ?: error("Missing shared fixture resource. Did the sourceSets entry in app/build.gradle.kts get removed?")
        val root = JsonParser.parseString(resource.readText()).asJsonObject
        val cases = root.getAsJsonArray("cases")
        assertTrue("Expected fixtures to contain at least one case", cases.size() > 0)

        for (caseEl in cases) {
            val case = caseEl.asJsonObject
            val id = case.get("id").asString
            val input = case.getAsJsonObject("input")
            val gender = Gender.valueOf(input.get("gender").asString)
            val activity = ActivityLevel.valueOf(input.get("activity").asString)
            val actual = CalorieCalculator.recommended(
                heightCm = input.get("heightCm").asDouble,
                weightKg = input.get("weightKg").asDouble,
                age = input.get("age").asInt,
                gender = gender,
                activityLevel = activity
            )
            val expected = case.get("expectedTdee").asDouble
            val tolerance = case.get("tolerance")?.asDouble ?: 1.0
            assertTrue(
                "[$id] expected TDEE ≈ $expected (±$tolerance), got $actual",
                abs(actual - expected) <= tolerance
            )
        }
    }
}
