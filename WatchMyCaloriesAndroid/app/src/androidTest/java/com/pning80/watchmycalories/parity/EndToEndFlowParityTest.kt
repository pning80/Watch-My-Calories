package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/EndToEndFlowTests.swift`.
 *
 * Covers the full manual-entry → dashboard → history loop plus settings-save
 * → dashboard-target propagation. All the AI/camera-dependent flows are
 * out of scope; those live in EstimationReviewParityTest.
 *
 * Known divergence handled inline:
 *   - Meal section headers are UPPERCASE on Android (D-006)
 *   - Health Connect typically reads 0 burned cals on Pixel 9a in test mode
 *     (vs iOS sim's 456). The remaining-calorie math is adapted accordingly.
 */
class EndToEndFlowParityTest : MainActivityComposeTest() {

    private fun addManualEntry(name: String, calories: String, quantity: String, mealType: String? = null) {
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.CAMERA).performClick()  // Log Food tab
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("logFood_logManually").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME).performTextInput(name)
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).performTextInput(calories)
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY).performTextInput(quantity)
        if (mealType != null) {
            composeTestRule.onNodeWithTag("mealType_$mealType").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.Dashboard.HERO_CARD).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun goToSettings() {
        composeTestRule.onNodeWithTag(AccessibilityTags.AppMenu.MENU_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SettingsTitle").assertIsDisplayed()
    }

    /** Mirror of iOS `testManualEntryAppearsInCorrectMealSection`. */
    @Test
    fun testManualEntryAppearsInCorrectMealSection() {
        launchEmpty()
        addManualEntry("Trail Mix", "200", "1 bag", mealType = "Snack")
        // SNACK section header appears (UPPERCASE on Android per D-006). Wait for the
        // dashboard to rebind after the manual-entry route pops.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Snack").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Snack").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trail Mix").assertIsDisplayed()
    }

    /** Mirror of iOS `testManualEntryAppearsInHistory`. */
    @Test
    fun testManualEntryAppearsInHistory() {
        launchEmpty()
        addManualEntry("Banana", "105", "1 medium")
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.HISTORY).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("105", substring = true).assertIsDisplayed()
    }

    /** Mirror of iOS `testMultipleEntriesAccumulateCalories`. */
    @Test
    fun testMultipleEntriesAccumulateCalories() {
        launchEmpty()
        addManualEntry("Apple", "95", "1 medium")
        addManualEntry("Banana", "105", "1 medium")
        // Hero card consumed value should show 200 total
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("200").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("200").assertIsDisplayed()
    }

    /** Mirror of iOS `testEmptyStateDisappearsAfterAddingEntry`. */
    @Test
    fun testEmptyStateDisappearsAfterAddingEntry() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.EMPTY_STATE_CARD).assertIsDisplayed()
        addManualEntry("Yogurt", "150", "1 cup")
        assert(composeTestRule.onAllNodesWithTag(AccessibilityTags.Dashboard.EMPTY_STATE_CARD).fetchSemanticsNodes().isEmpty()) {
            "Empty state card should be gone after adding an entry"
        }
        composeTestRule.onNodeWithText("Yogurt").assertIsDisplayed()
    }

    /** Mirror of iOS `testEntriesInMultipleMealSections`. */
    @Test
    fun testEntriesInMultipleMealSections() {
        launchEmpty()
        addManualEntry("Toast", "150", "2 slices", mealType = "Breakfast")
        addManualEntry("Sandwich", "400", "1 whole", mealType = "Lunch")
        addManualEntry("Pasta", "600", "1 plate", mealType = "Dinner")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Breakfast").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Breakfast").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dinner").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testCaloriesAccumulateAcrossMealTypes`. */
    @Test
    fun testCaloriesAccumulateAcrossMealTypes() {
        launchEmpty()
        addManualEntry("Eggs", "200", "2 large", mealType = "Breakfast")
        addManualEntry("Salad", "300", "1 bowl", mealType = "Lunch")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("500").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("500").assertIsDisplayed()
    }

    /** Mirror of iOS `testEntryVisibleOnDashboardAndHistory`. */
    @Test
    fun testEntryVisibleOnDashboardAndHistory() {
        launchEmpty()
        addManualEntry("Granola Bar", "180", "1 bar", mealType = "Snack")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Snack").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Granola Bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Snack").assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.HISTORY).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("180", substring = true).assertIsDisplayed()
    }

    /** Mirror of iOS `testHeroCardUpdatesAfterDelete` — long-press → Delete. */
    @Test
    fun testHeroCardUpdatesAfterDelete() {
        launchWithSeedData()
        // Initial: 750 consumed
        composeTestRule.onNodeWithText("750").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oatmeal with Berries").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        // After deleting 300 kcal Oatmeal, consumed flips to 450.
        // "450" may appear multiple times (hero total + meal-section subtotal);
        // any presence is enough — the prior "750" should be gone.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("450").fetchSemanticsNodes().isNotEmpty()
        }
        assert(composeTestRule.onAllNodesWithText("750").fetchSemanticsNodes().isEmpty()) {
            "Hero card should no longer show 750 after Oatmeal deletion"
        }
    }

    /**
     * Mirror of iOS `testSettingsSaveUpdatesHeroCardTarget`. Adapted for Android:
     * Save button is enabled-on-change, so we tap Calculate Goal first (which
     * changes targetCalories), then Save.
     */
    @Test
    fun testSettingsSaveUpdatesHeroCardTarget() {
        launchWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.GOAL_VALUE).assertIsDisplayed()
        composeTestRule.onNodeWithText("2200").assertIsDisplayed()  // seed target

        goToSettings()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.Dashboard.HERO_CARD).fetchSemanticsNodes().isNotEmpty()
        }
        // Goal should no longer be 2200 (Mifflin-St Jeor on seed profile is ~2530).
        assert(composeTestRule.onAllNodesWithText("2200").fetchSemanticsNodes().isEmpty()) {
            "Goal should have changed from 2200 after Calculate + Save"
        }
    }
}
