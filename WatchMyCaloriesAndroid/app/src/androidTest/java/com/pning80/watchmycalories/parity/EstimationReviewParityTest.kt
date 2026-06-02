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
 * The analysis success path now mirrors iOS exactly: the estimation auto-saves
 * and the screen goes straight to the read-only `SUCCESS_VIEW` ("Logged
 * Successfully!" + per-item cards + "Total Added"). The earlier Android-only
 * pre-save Review & Edit step (EDIT_VIEW / review SAVE_BUTTON) was dropped for
 * parity, so there is no Save tap — `DONE_BUTTON` returns to the dashboard.
 * Error and no-food views expose CANCEL_BUTTON (D-009).
 *
 * Bypasses the camera flow with `EXTRA_START_AT_ANALYSIS` — the activity boots
 * directly into the analysis route with a stub bitmap.
 */
class EstimationReviewParityTest : MainActivityComposeTest() {

    // --- Post-save success summary (auto-saved, read-only — strict iOS mirror) ---

    /** Success summary lists the estimated items (read-only). */
    @Test
    fun testSuccessScreenShowsItems() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        // SUCCESS fixture returns 3 items totalling 690 kcal; food names appear as cards.
        composeTestRule.onNodeWithText("Brown Rice", substring = true).assertIsDisplayed()
    }

    /** Mirror of iOS `testSuccessScreenShowsLoggedMessage`. */
    @Test
    fun testSuccessScreenShowsLoggedMessage() {
        launchStartAtAnalysisSuccess()
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
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.DONE_BUTTON).assertIsDisplayed()
    }

    /** Mirror of iOS `testDoneButtonReturnsToDashboard`. */
    @Test
    fun testDoneButtonReturnsToDashboard() {
        launchStartAtAnalysisSuccess()
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
