package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/EstimationReviewTests.swift`.
 *
 * Updated for the D-008 + D-009 feature work:
 *  - `SUCCESS_VIEW` now means the post-save confirmation screen
 *    ("Logged Successfully!" + "Total Added"), matching iOS semantics
 *  - `EDIT_VIEW` is the pre-save Review & Edit screen (Android-specific extra)
 *  - `SAVE_BUTTON` is on Review & Edit; `DONE_BUTTON` is on the success screen
 *  - Error and no-food views now expose CANCEL_BUTTON (D-009)
 *
 * Bypasses the camera flow with `EXTRA_START_AT_ANALYSIS` — the activity boots
 * directly into the analysis route with a stub bitmap.
 */
class EstimationReviewParityTest : MainActivityComposeTest() {

    // --- Review & Edit (pre-save) ---

    /** Android extra: pre-save Review & Edit screen exposes editable items. */
    @Test
    fun testReviewAndEditScreenShowsItems() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.EDIT_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        // SUCCESS fixture returns 3 items totalling 690 kcal; food names appear in editable rows.
        composeTestRule.onNodeWithText("Brown Rice", substring = true).assertIsDisplayed()
    }

    /** Android extra: Save button is visible and clickable on Review & Edit. */
    @Test
    fun testSaveButtonVisibleOnReviewAndEdit() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.EDIT_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.SAVE_BUTTON).assertIsDisplayed()
    }

    // --- Post-save success confirmation (D-008 — strict iOS mirrors) ---

    /** Mirror of iOS `testSuccessScreenShowsLoggedMessage`. */
    @Test
    fun testSuccessScreenShowsLoggedMessage() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.EDIT_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.SAVE_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Logged Successfully!").assertIsDisplayed()
    }

    /** Mirror of iOS `testSuccessScreenShowsTotalAdded`. */
    @Test
    fun testSuccessScreenShowsTotalAdded() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.EDIT_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.SAVE_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Total Added").assertIsDisplayed()
        // SUCCESS fixture: 220 + 350 + 120 = 690 kcal total.
        composeTestRule.onNodeWithText("690", substring = true).assertIsDisplayed()
    }

    /** Mirror of iOS `testEstimationResultShowsActionButton` (success branch). */
    @Test
    fun testSuccessScreenShowsDoneButton() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.EDIT_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.SAVE_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.DONE_BUTTON).assertIsDisplayed()
    }

    /** Mirror of iOS `testDoneButtonReturnsToDashboard`. */
    @Test
    fun testDoneButtonReturnsToDashboard() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.EDIT_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.SAVE_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.DONE_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.Tab.DASHBOARD).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // --- Error path (D-009 — Cancel buttons now exist) ---

    /** Mirror of iOS `testErrorStateShowsTryAgainButton`. */
    @Test
    fun testErrorStateShowsTryAgainButton() {
        launchStartAtAnalysisError()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.ERROR_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.TRY_AGAIN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText("Analysis Failed").assertIsDisplayed()
    }

    /** Mirror of iOS `testErrorStateShowsCancelButton` (D-009 closed). */
    @Test
    fun testErrorStateShowsCancelButton() {
        launchStartAtAnalysisError()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.ERROR_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.CANCEL_BUTTON).assertIsDisplayed()
    }

    /** Mirror of iOS `testErrorStateTryAgainTapResetsView`. */
    @Test
    fun testErrorStateTryAgainTapResetsView() {
        launchStartAtAnalysisError()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.ERROR_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.TRY_AGAIN_BUTTON).performClick()
        composeTestRule.waitForIdle()
        // After Try Again the mock re-fires and lands in error again.
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.ERROR_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.ERROR_VIEW).assertIsDisplayed()
    }

    // --- No-food path (D-009 — Cancel button now exists) ---

    /** Mirror of iOS `testNoFoodStateShowsTryAgainButton`. */
    @Test
    fun testNoFoodStateShowsTryAgainButton() {
        launchStartAtAnalysisNoFood()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.NO_FOOD_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No Food Detected").assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.TRY_AGAIN_BUTTON).assertIsDisplayed()
    }

    /** Mirror of iOS `testNoFoodStateShowsCancelButton` (D-009 closed). */
    @Test
    fun testNoFoodStateShowsCancelButton() {
        launchStartAtAnalysisNoFood()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.NO_FOOD_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.CANCEL_BUTTON).assertIsDisplayed()
    }
}
