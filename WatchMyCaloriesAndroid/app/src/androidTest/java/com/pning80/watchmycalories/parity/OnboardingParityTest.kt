package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/OnboardingTests.swift`.
 *
 * Uses `launchResetOnboarding()` which sets `EXTRA_RESET_ONBOARDING` so
 * `TestSeed.applyIfTesting` calls `setOnboardingCompleted(false)` — mirror of
 * the iOS `--reset-onboarding` launch arg added in PR #1.
 *
 * Skipped — host-platform constraints (not feature gaps):
 *   - testKeyboardDismissesWhenTappingOutsideTargetCalories /
 *     testKeyboardDismissesWhenTappingFinishButton — IME action semantics
 *     are platform-host-dependent.
 *
 * Goal step Height/Weight/Age picker tests are adapted: Android uses Slider
 * widgets (D-004), so the picker-exists assertions become slider-row-label
 * visibility checks instead.
 */
class OnboardingParityTest : MainActivityComposeTest() {

    // --- Navigation helpers ---

    private fun advanceToPrivacyStep() {
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your Privacy").assertIsDisplayed()
    }

    private fun advanceToGoalStep() {
        advanceToPrivacyStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.NEXT_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your Goal").assertIsDisplayed()
    }

    // --- Welcome step ---

    /** Mirror of iOS `testWelcomeScreenShowsPrivacyNoteAndNoUnitPicker`. */
    @Test
    fun testWelcomeScreenShowsGetStartedAndSkip() {
        launchResetOnboarding()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).assertIsDisplayed()
        // Unit picker (US / Metric) should not appear on the welcome screen
        assert(composeTestRule.onAllNodesWithText("US Customary").fetchSemanticsNodes().isEmpty()) {
            "Welcome screen should not show a unit picker"
        }
        assert(composeTestRule.onAllNodesWithText("Metric").fetchSemanticsNodes().isEmpty()) {
            "Welcome screen should not show a unit picker"
        }
    }

    /** Mirror of iOS `testGetStartedButtonAdvancesFromWelcome`. */
    @Test
    fun testGetStartedButtonAdvancesFromWelcome() {
        launchResetOnboarding()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your Privacy").assertIsDisplayed()
    }

    /** Mirror of iOS `testSkipButtonCompletesOnboarding`. */
    @Test
    fun testSkipButtonCompletesOnboarding() {
        launchResetOnboarding()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).performClick()
        composeTestRule.waitForIdle()
        // After skip, bottom nav appears — Dashboard tab is the marker.
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.DASHBOARD).assertIsDisplayed()
    }

    // --- Privacy step ---

    /** Mirror of iOS `testPermissionsStepShowsAIToggle`. */
    @Test
    fun testPrivacyStepShowsAIToggle() {
        launchResetOnboarding()
        advanceToPrivacyStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.AI_CONSENT_TOGGLE).assertIsDisplayed()
        // Google Gemini mentioned in the explanatory copy
        assert(composeTestRule.onAllNodesWithText("Google Gemini", substring = true).fetchSemanticsNodes().isNotEmpty()) {
            "Privacy step should mention Google Gemini in the AI consent copy"
        }
    }

    /** Mirror of iOS `testPermissionsStepShowsHealthButton`. */
    @Test
    fun testPrivacyStepShowsHealthButton() {
        launchResetOnboarding()
        advanceToPrivacyStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.CONNECT_HEALTH_BUTTON).assertIsDisplayed()
        // "active calories" substring appears in the explanatory copy
        assert(composeTestRule.onAllNodesWithText("active calories", substring = true).fetchSemanticsNodes().isNotEmpty()) {
            "Privacy step Health card should mention 'active calories'"
        }
    }

    /** Mirror of iOS `testPermissionsStepShowsNextButton`. */
    @Test
    fun testPrivacyStepShowsNextButton() {
        launchResetOnboarding()
        advanceToPrivacyStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.NEXT_BUTTON).assertIsDisplayed()
    }

    /** Mirror of iOS `testAIToggleIsInteractive`. */
    @Test
    fun testAIToggleIsInteractive() {
        launchResetOnboarding()
        advanceToPrivacyStep()
        val toggle = composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.AI_CONSENT_TOGGLE)
        toggle.assertIsDisplayed()
        toggle.performClick()
        composeTestRule.waitForIdle()
        // Tap didn't crash; toggle is still present
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.AI_CONSENT_TOGGLE).assertIsDisplayed()
    }

    /** Mirror of iOS `testNextButtonAdvancesFromPermissions`. */
    @Test
    fun testNextButtonAdvancesFromPermissions() {
        launchResetOnboarding()
        advanceToPrivacyStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.NEXT_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Your Goal").assertIsDisplayed()
    }

    /** Mirror of iOS `testSkipFromPermissionsStep`. */
    @Test
    fun testSkipFromPrivacyStep() {
        launchResetOnboarding()
        advanceToPrivacyStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.DASHBOARD).assertIsDisplayed()
    }

    // --- Goal step ---

    /** Mirror of iOS `testGoalStepShowsFormElements`. */
    @Test
    fun testGoalStepShowsFormElements() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithText("Height").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Weight").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Age").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Gender").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Activity Level").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.FINISH_BUTTON).performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testGoalStepShowsActivityLevelPicker`. */
    @Test
    fun testGoalStepShowsActivityLevelPicker() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithText("Activity Level").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sedentary").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testGoalStepShowsFinishButton`. */
    @Test
    fun testGoalStepShowsFinishButton() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.FINISH_BUTTON).performScrollTo().assertIsDisplayed()
    }

    /**
     * iOS `testGoalStepHeightPickerExists` asserts a menu-picker button.
     * Android renders Height as a Slider (D-004), so this verifies the
     * Height row label is visible — slider semantics aren't directly
     * queryable as a single node.
     */
    @Test
    fun testGoalStepHeightSliderRowExists() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithText("Height").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testGoalStepWeightPickerExists` (Android Slider per D-004). */
    @Test
    fun testGoalStepWeightSliderRowExists() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithText("Weight").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testGoalStepAgePickerExists` (Android Slider per D-004). */
    @Test
    fun testGoalStepAgeSliderRowExists() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithText("Age").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testCalculateRecommendedGoalPopulatesTargetCalories`. */
    @Test
    fun testCalculateRecommendedGoalPopulatesTargetCalories() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.CALCULATE_GOAL_BUTTON).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        // After calculation, the field testTag still exists. A 4-digit calorie value
        // around BMR × Sedentary multiplier should appear in the semantics tree.
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.TARGET_CALORIES_FIELD).performScrollTo().assertIsDisplayed()
        // Default profile (cm=173, kg=68, age=30, Male, Sedentary) → roughly 2030 kcal
        val hasReasonableCalorieValue = (1800..2400).any { v ->
            composeTestRule.onAllNodesWithText(v.toString(), substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assert(hasReasonableCalorieValue) {
            "Calculated goal for default profile should land in 1800–2400 kcal"
        }
    }

    /** Mirror of iOS `testCompleteOnboardingFlowShowsDashboard`. */
    @Test
    fun testCompleteOnboardingFlowShowsDashboard() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.FINISH_BUTTON).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.DASHBOARD).assertIsDisplayed()
    }

    /** Mirror of iOS `testSkipFromGoalStep`. */
    @Test
    fun testSkipFromGoalStep() {
        launchResetOnboarding()
        advanceToGoalStep()
        composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.DASHBOARD).assertIsDisplayed()
    }
}
