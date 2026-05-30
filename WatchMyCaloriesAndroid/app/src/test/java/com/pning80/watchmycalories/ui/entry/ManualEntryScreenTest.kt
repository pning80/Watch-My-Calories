package com.pning80.watchmycalories.ui.entry

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pning80.watchmycalories.BaseComposeTest
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ManualEntryScreenTest : BaseComposeTest() {

    @get:Rule
    val composeTestRule = createComposeRule()

    // MARK: - Form Fields

    @Test
    fun testManualEntryFieldsExist() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME).assertExists()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).assertExists()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY).assertExists()
    }

    @Test
    fun testSaveButtonDisabledWhenEmpty() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun testCancelDismissesSheet() {
        var cancelTriggered = false
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = { cancelTriggered = true }
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CANCEL_BUTTON).performClick()
        assertTrue("Cancel callback should be triggered", cancelTriggered)
    }

    @Test
    fun testCanSaveEntry() {
        var savedEntry: com.pning80.watchmycalories.data.FoodEntry? = null
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = { savedEntry = it },
                onCancel = {}
            )
        }

        // Fill name
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME)
            .performTextInput("Apple")

        // Fill calories
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES)
            .performTextInput("95")

        // Fill quantity
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY)
            .performTextInput("1 medium")

        // Save should be enabled
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON)
            .assertIsEnabled()
            .performClick()

        assertTrue("onSave should be called", savedEntry != null)
        assertEquals("Apple", savedEntry!!.name)
        assertEquals(95.0, savedEntry!!.calories, 0.0)
        assertEquals("1 medium", savedEntry!!.quantity)
    }

    // MARK: - Meal Type Picker

    @Test
    fun testAllMealTypeSegmentsExist() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithTag("mealType_Breakfast").assertExists()
        composeTestRule.onNodeWithTag("mealType_Lunch").assertExists()
        composeTestRule.onNodeWithTag("mealType_Dinner").assertExists()
        composeTestRule.onNodeWithTag("mealType_Snack").assertExists()
    }

    @Test
    fun testMealTypePickerInteraction() {
        // Use initialEntry with a known Breakfast state
        val initialBreakfastEntry = com.pning80.watchmycalories.data.FoodEntry(
            id = "test-1", name = "Test Food", calories = 100.0, quantity = "1 portion",
            protein = null, carbs = null, fat = null,
            timestamp = System.currentTimeMillis(),
            imageID = null, mealName = null, mealTypeRaw = "Breakfast"
        )
        composeTestRule.setContent {
            ManualEntryScreen(
                initialEntry = initialBreakfastEntry,
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        // Verify Breakfast is initially selected (SegmentedButton selected state)
        composeTestRule.onNodeWithTag("mealType_Breakfast").assertIsSelected()

        // Verify the meal type picker tag is present
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.MEAL_PICKER).assertExists()
    }

    // MARK: - Validation

    @Test
    fun testZeroCaloriesDisablesSave() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME)
            .performTextInput("Water")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES)
            .performTextInput("0")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY)
            .performTextInput("1 glass")

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun testSaveButtonEnabledWhenAllFieldsFilled() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME)
            .performTextInput("Banana")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES)
            .performTextInput("105")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY)
            .performTextInput("1 medium")

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON)
            .assertIsEnabled()
    }

    @Test
    fun testMissingNameDisablesSave() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        // Fill calories and quantity but NOT name
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES)
            .performTextInput("200")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY)
            .performTextInput("1 cup")

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun testMissingQuantityDisablesSave() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        // Fill name and calories but NOT quantity
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME)
            .performTextInput("Rice")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES)
            .performTextInput("200")

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON)
            .assertIsNotEnabled()
    }

    // MARK: - Nutrition Details

    @Test
    fun testNutritionDisclosureGroupExpands() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        // The nutrition toggle button is inside a scrollable Column — scroll to it
        val toggleNode = composeTestRule.onNodeWithText("Add Nutrition Details (optional)")
        toggleNode.performScrollTo()
        toggleNode.assertExists()

        // Tap to expand
        toggleNode.performClick()

        // Advance clock to allow AnimatedVisibility to complete
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()

        // After expanding, the toggle text should have changed
        composeTestRule.onNodeWithText("Hide Nutrition Details").assertExists()
    }

    // MARK: - Navigation Title

    @Test
    fun testManualEntryNavigationTitle() {
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Log Food").assertExists()
    }

    // MARK: - Meal Picker Persistence

    @Test
    fun testSaveWithSnackMealType() {
        var savedEntry: com.pning80.watchmycalories.data.FoodEntry? = null
        composeTestRule.setContent {
            ManualEntryScreen(
                isMetric = true,
                onSave = { savedEntry = it },
                onCancel = {}
            )
        }

        // Select Snack
        composeTestRule.onNodeWithTag("mealType_Snack").performScrollTo().performClick()

        // Fill required fields
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME)
            .performTextInput("Trail Mix")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES)
            .performTextInput("200")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY)
            .performTextInput("1 bag")

        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).performClick()

        assertTrue("onSave should be called", savedEntry != null)
        assertEquals("Snack", savedEntry!!.mealTypeRaw)
    }
}
