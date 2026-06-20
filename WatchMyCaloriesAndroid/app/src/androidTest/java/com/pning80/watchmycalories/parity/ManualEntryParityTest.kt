package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/ManualEntryTests.swift`.
 *
 * Phase 5 of `PARITY_FIX_PLAN.md` — first Android-side mirror suite that
 * exercises the Phase 3 test infrastructure end-to-end. Manual Entry is
 * the cleanest starting point: pure form, no AI/camera dependencies,
 * 17 iOS tests with full Robolectric counterpart coverage.
 *
 * Each test below mirrors the iOS test of the same name (or a clearly
 * derivable variant). Navigation path: launch empty → Log Food tab →
 * Log Manually button → form.
 */
class ManualEntryParityTest : MainActivityComposeTest() {

    /** Open the Manual Entry sheet from the Log Food bottom-nav tab. */
    private fun openManualEntry() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.CAMERA).performClick()  // "Log Food" tab
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("logFood_logManually").performClick()
        composeTestRule.waitForIdle()
    }

    /** Mirror of iOS `testManualEntryFieldsExist`. */
    @Test
    fun testManualEntryFieldsExist() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY).assertIsDisplayed()
    }

    /** Mirror of iOS `testSaveButtonDisabledWhenEmpty`. */
    @Test
    fun testSaveButtonDisabledWhenEmpty() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).assertIsNotEnabled()
    }

    /** Mirror of iOS `testCancelDismissesSheet`. */
    @Test
    fun testCancelDismissesSheet() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CANCEL_BUTTON).performClick()
        composeTestRule.waitForIdle()
        // After cancel, dashboard re-shows the empty-state card.
        // Mirrors iOS assertion on `dashboard_emptyStateCard` accessibility identifier.
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.EMPTY_STATE_CARD).assertIsDisplayed()
    }

    /** Mirror of iOS `testCanSaveEntry`. */
    @Test
    fun testCanSaveEntry() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME).performTextInput("Apple")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).performTextInput("95")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY).performTextInput("1 medium")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Apple").assertIsDisplayed()
    }

    /** Mirror of iOS `testZeroCaloriesDisablesSave`. */
    @Test
    fun testZeroCaloriesDisablesSave() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME).performTextInput("Water")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).performTextInput("0")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY).performTextInput("1 glass")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).assertIsNotEnabled()
    }

    /** Mirror of iOS `testSaveButtonEnabledWhenAllFieldsFilled`. */
    @Test
    fun testSaveButtonEnabledWhenAllFieldsFilled() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME).performTextInput("Banana")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).performTextInput("105")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY).performTextInput("1 medium")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).assertIsEnabled()
    }

    /** Mirror of iOS `testMissingNameDisablesSave`. */
    @Test
    fun testMissingNameDisablesSave() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).performTextInput("200")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY).performTextInput("1 cup")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).assertIsNotEnabled()
    }

    /** Mirror of iOS `testMissingQuantityDisablesSave`. */
    @Test
    fun testMissingQuantityDisablesSave() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME).performTextInput("Rice")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).performTextInput("200")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).assertIsNotEnabled()
    }

    /** Mirror of iOS `testAllMealTypeSegmentsExist`. */
    @Test
    fun testAllMealTypeSegmentsExist() {
        openManualEntry()
        composeTestRule.onNodeWithTag("mealType_Breakfast").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mealType_Lunch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mealType_Dinner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mealType_Snack").assertIsDisplayed()
    }

    /** Mirror of iOS `testMealTypePickerInteraction`. */
    @Test
    fun testMealTypePickerInteraction() {
        openManualEntry()
        composeTestRule.onNodeWithTag("mealType_Snack").assertHasClickAction().performClick()
        composeTestRule.onNodeWithTag("mealType_Dinner").assertHasClickAction().performClick()
    }

    /** Mirror of iOS `testSaveWithDinnerMealType`. */
    @Test
    fun testSaveWithDinnerMealType() {
        openManualEntry()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.FOOD_NAME).performTextInput("Steak")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.CALORIES).performTextInput("500")
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.QUANTITY).performTextInput("8 oz")
        composeTestRule.onNodeWithTag("mealType_Dinner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.ManualEntry.SAVE_BUTTON).performClick()
        // Assert the Dinner meal section header appears on the dashboard after save.
        // The dashboard header is title-case displayName ("Dinner"); poll because the
        // dashboard rebinds after the manual-entry route pops.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Dinner").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Dinner").performScrollTo().assertIsDisplayed()
    }
}
