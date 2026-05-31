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
 * Bypasses the camera flow with `EXTRA_START_AT_ANALYSIS` (added in the
 * MockGeminiRepository PR) — the activity boots directly into the analysis
 * route with a stub bitmap, and the mock repo returns the configured fixture.
 *
 * Skipped — parity GAPs and host-platform constraints:
 *   - testAIConsentSheetFiresWhenConsentMissing — Android start-at-analysis path
 *     pre-accepts AI consent in the test fixture. The consent-gate flow uses
 *     the regular Log-Food → Scan-Food path, which is camera-permission-bound
 *     on Pixel 9a and currently not exercised by parity tests.
 *   - testAIConsentDeclineDismissesEstimation — same camera-permission constraint.
 *   - testLoadingStateAppears — mock estimation completes in ~150ms; the loading
 *     view is too short-lived to assert reliably without a tighter mock delay knob.
 *     (The success/error/no-food terminal states cover the same code path.)
 *
 * Known divergences (Android specifics):
 *   - testDoneButtonReturnsToDashboard — when the activity boots at "analysis"
 *     as startDestination, popBackStack from Done is a no-op on the back stack
 *     root. Test asserts the dashboard hero card appears via Tab.DASHBOARD instead.
 */
class EstimationReviewParityTest : MainActivityComposeTest() {

    // --- Success path ---

    /** Mirror of iOS `testEstimationResultShowsActionButton` (success branch). */
    @Test
    fun testSuccessShowsDoneButton() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.DONE_BUTTON).assertIsDisplayed()
    }

    /** Mirror of iOS `testSuccessScreenShowsLoggedMessage` (asserts Android's analogous copy). */
    @Test
    fun testSuccessScreenShowsResultContent() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        // SUCCESS fixture returns 3 items totalling 690 kcal; food names appear
        // in the result list. "Brown Rice" is a stable substring.
        composeTestRule.onNodeWithText("Brown Rice", substring = true).assertIsDisplayed()
    }

    /**
     * iOS `testSuccessScreenShowsTotalAdded` asserts a rolled-up `"Total Added"`
     * row on the post-save success screen. Android's success view is a
     * Review & Edit step — per-item OutlinedTextFields, no rolled-up total.
     * Documented as **D-008** in `PORTING_DEVIATIONS.md`. This test pins the
     * Android shape by asserting the `"Review & Edit"` headline is present.
     */
    @Test
    fun testSuccessScreenShowsReviewAndEditHeading() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Review & Edit").assertIsDisplayed()
    }

    /** Mirror of iOS `testDoneButtonVisibleWithoutScrolling`. */
    @Test
    fun testDoneButtonReachableOnSuccess() {
        launchStartAtAnalysisSuccess()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.EstimationReview.DONE_BUTTON).assertIsDisplayed()
    }

    /**
     * Mirror of iOS `testDoneButtonReturnsToDashboard`. Adapted: because we bootstrap
     * AT the analysis route, Done's popBackStack lands on the dashboard via the
     * onSaveLog → navigate("dashboard") code path, not via popBackStack. The
     * dashboard hero card appearing is the reliable success signal.
     */
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

    // --- Error path ---

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

    /**
     * iOS `testErrorStateShowsCancelButton` asserts a Cancel button on the error
     * view. Android currently exposes only Try Again — see D-009 in
     * `PORTING_DEVIATIONS.md`. This test pins the current Android shape (no
     * Cancel testTag rendered on error). When the Cancel button lands on
     * Android, convert this back to a strict mirror.
     */
    @Test
    fun testErrorStateHasNoCancelButtonOnAndroid() {
        launchStartAtAnalysisError()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.ERROR_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        assert(composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.CANCEL_BUTTON).fetchSemanticsNodes().isEmpty()) {
            "Android error view should not yet expose CANCEL_BUTTON testTag (D-009)"
        }
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

    // --- No-food path ---

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

    /**
     * iOS `testNoFoodStateShowsCancelButton` asserts a Cancel button on no-food.
     * Android currently exposes only Try Again — same D-009 gap as the error
     * view. This test pins the no-cancel state on no-food.
     */
    @Test
    fun testNoFoodStateHasNoCancelButtonOnAndroid() {
        launchStartAtAnalysisNoFood()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.NO_FOOD_VIEW).fetchSemanticsNodes().isNotEmpty()
        }
        assert(composeTestRule.onAllNodesWithTag(AccessibilityTags.EstimationReview.CANCEL_BUTTON).fetchSemanticsNodes().isEmpty()) {
            "Android no-food view should not yet expose CANCEL_BUTTON testTag (D-009)"
        }
    }
}
