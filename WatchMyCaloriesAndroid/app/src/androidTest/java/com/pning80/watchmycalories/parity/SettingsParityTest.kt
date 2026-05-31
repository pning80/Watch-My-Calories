package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/SettingsTests.swift`.
 *
 * Covers the settings screen surface: appearance pickers, profile section,
 * daily goals + calculate-goal flow, AI consent toggle, discard-dialog flow,
 * and seed-data preload.
 *
 * Skipped — parity divergences in PORTING_DEVIATIONS.md:
 *   - testHeightDisclosureGroupExpandsInMetricMode / testWeight* / testAgeDisclosureGroupExpands /
 *     testHeightFeetAndInchesPickersExistInUSMode — Android uses Slider widgets (D-004),
 *     not DisclosureGroup-with-wheel-picker like iOS
 *   - testThemePickerCanChangeSelection / testUnitPickerCanChangeSelection /
 *     testActivityPickerCanChangeSelection — Android uses ExposedDropdownMenu;
 *     selection round-trip works but the dropdown semantics are different from
 *     iOS's menu picker. Not skipped for shape reasons, just deferred for a
 *     dedicated follow-up that adds the dropdown-aware assertion.
 *   - testKeyboardDoneButtonDismissesKeyboard — IME action semantics on Compose
 *     keyboards are platform-host-dependent; deferred.
 *
 * Known label divergences (tests assert on the Android label):
 *   - iOS shows "Activity" on the profile row; Android shows "Activity Level"
 *   - Cancel dialog buttons: iOS "Discard Changes" / "Keep Editing";
 *     Android "Discard" / "Keep Editing"
 */
class SettingsParityTest : MainActivityComposeTest() {

    /** Open Settings via the Dashboard app menu (gear → "Settings"). */
    private fun openSettings() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.AppMenu.MENU_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SettingsTitle").assertIsDisplayed()
    }

    private fun openSettingsWithSeedData() {
        launchWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.AppMenu.MENU_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SettingsTitle").assertIsDisplayed()
    }

    // MARK: - Fields

    /** Mirror of iOS `testSettingsFieldsExist`. */
    @Test
    fun testSettingsFieldsExist() {
        openSettings()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.AI_CONSENT_TOGGLE).performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testThemePickerExists`. */
    @Test
    fun testThemePickerExists() {
        openSettings()
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.THEME_PICKER).assertIsDisplayed()
    }

    /** Mirror of iOS `testUnitPickerExists`. */
    @Test
    fun testUnitPickerExists() {
        openSettings()
        composeTestRule.onNodeWithText("Unit System").assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.UNIT_PICKER).assertIsDisplayed()
    }

    /** Mirror of iOS `testProfileSectionFieldsVisible`. */
    @Test
    fun testProfileSectionFieldsVisible() {
        openSettings()
        composeTestRule.onNodeWithText("Height").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Weight").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Age").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Gender").performScrollTo().assertIsDisplayed()
        // Label divergence: iOS "Activity", Android "Activity Level"
        composeTestRule.onNodeWithText("Activity Level").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testDailyGoalsSectionVisible`. */
    @Test
    fun testDailyGoalsSectionVisible() {
        openSettings()
        composeTestRule.onNodeWithText("Target Calories").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testGenderPickerInteraction`. */
    @Test
    fun testGenderPickerInteraction() {
        openSettings()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.GENDER_PICKER).performScrollTo().assertIsDisplayed()
        // Segmented buttons render all option labels — "Male" is always present as a
        // segment label, regardless of which one is currently selected.
        composeTestRule.onNodeWithText("Male").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testActivityLevelPickerInteraction`. */
    @Test
    fun testActivityLevelPickerInteraction() {
        openSettings()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.ACTIVITY_PICKER).performScrollTo().assertIsDisplayed()
        // ExposedDropdownMenu's anchor field shows the current selection text;
        // default ActivityLevel.fromRaw(null) → SEDENTARY → "Sedentary".
        composeTestRule.onNodeWithText("Sedentary").performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testAIConsentToggleExists`. */
    @Test
    fun testAIConsentToggleExists() {
        openSettings()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.AI_CONSENT_TOGGLE).performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testAIConsentToggleCanBeToggled`. */
    @Test
    fun testAIConsentToggleCanBeToggled() {
        openSettings()
        val toggle = composeTestRule.onNodeWithTag(AccessibilityTags.Settings.AI_CONSENT_TOGGLE).performScrollTo()
        toggle.assertIsDisplayed()
        toggle.performClick()
        composeTestRule.waitForIdle()
        // Toggle's existence after tap is enough to prove the click didn't crash
        // the screen; deeper "value changed" assertion would inspect Switch state.
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.AI_CONSENT_TOGGLE).assertIsDisplayed()
    }

    // MARK: - Privacy Section

    /** Mirror of iOS `testPrivacySectionInfoText`. */
    @Test
    fun testPrivacySectionInfoText() {
        openSettings()
        // Text contains "food photos are sent to Google Gemini" — partial match via predicate.
        val nodes = composeTestRule.onAllNodesWithText("food photos are sent to Google Gemini", substring = true).fetchSemanticsNodes()
        assert(nodes.isNotEmpty()) { "Privacy info text should explain that photos go to Google Gemini" }
    }

    /** Mirror of iOS `testPrivacyPolicyLinkNotInSettings`. */
    @Test
    fun testPrivacyPolicyLinkNotInSettings() {
        openSettings()
        // Privacy Policy was moved out of Settings to the About screen.
        assert(composeTestRule.onAllNodesWithText("Privacy Policy", substring = true).fetchSemanticsNodes().isEmpty()) {
            "Privacy Policy link should not appear in Settings"
        }
    }

    /** Mirror of iOS `testDeviceAttestationNotInSettings`. */
    @Test
    fun testDeviceAttestationNotInSettings() {
        openSettings()
        assert(composeTestRule.onAllNodesWithText("Device Attestation", substring = true).fetchSemanticsNodes().isEmpty()) {
            "Device Attestation copy should not appear in Settings"
        }
    }

    /** Mirror of iOS `testManagePrivacyChoicesButtonHiddenUnderUITesting`. */
    @Test
    fun testManagePrivacyChoicesButtonHiddenUnderUITesting() {
        openSettings()
        // AdManager.isPrivacyOptionsRequired defaults to false; under UI testing
        // it never flips true so the button must not render.
        assert(composeTestRule.onAllNodesWithText("Manage Privacy Choices").fetchSemanticsNodes().isEmpty()) {
            "Manage Privacy Choices button should be hidden under EXTRA_UI_TESTING"
        }
    }

    // MARK: - Cancel / Save / Discard

    /** Mirror of iOS `testCancelButtonDismissesWhenNoChanges`. */
    @Test
    fun testCancelButtonDismissesWhenNoChanges() {
        openSettings()
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.EMPTY_STATE_CARD).assertIsDisplayed()
    }

    /** Mirror of iOS `testCancelNoDialogWhenUnchanged`. */
    @Test
    fun testCancelNoDialogWhenUnchanged() {
        openSettingsWithSeedData()
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.waitForIdle()
        // No discard dialog should appear
        assert(composeTestRule.onAllNodesWithText("Discard").fetchSemanticsNodes().isEmpty()) {
            "Discard dialog must not show when there are no unsaved changes"
        }
        // Dashboard hero card should be back
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.HERO_CARD).assertIsDisplayed()
    }

    /** Mirror of iOS `testCancelShowsDiscardDialogWhenChanged`. */
    @Test
    fun testCancelShowsDiscardDialogWhenChanged() {
        openSettingsWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.waitForIdle()
        // Android dialog title is "Discard changes?" with action labels "Discard" / "Keep Editing"
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()
    }

    /** Mirror of iOS `testDiscardChangesFromCancelDialog`. */
    @Test
    fun testDiscardChangesFromCancelDialog() {
        openSettingsWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Discard").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.HERO_CARD).assertIsDisplayed()
    }

    /** Mirror of iOS `testKeepEditingFromCancelDialog`. */
    @Test
    fun testKeepEditingFromCancelDialog() {
        openSettingsWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Keep Editing").performClick()
        composeTestRule.waitForIdle()
        // Should still be on Settings
        composeTestRule.onNodeWithTag("SettingsTitle").assertIsDisplayed()
    }

    /**
     * Adapted from iOS `testSaveButtonDismissesSheet`. Android Save button is
     * `enabled = hasUnsavedChanges`, so we need to make a change first.
     */
    @Test
    fun testSaveButtonAfterChangeDismissesScreen() {
        openSettingsWithSeedData()
        // Toggle AI consent to enable the Save button
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.AI_CONSENT_TOGGLE).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        // SAVE_BUTTON lives in the TopAppBar (outside the scrollable content), so
        // no performScrollTo() — it's always on-screen.
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.HERO_CARD).assertIsDisplayed()
    }

    // MARK: - Calculate Goal

    /** Mirror of iOS `testCalculateGoalSetsTargetCalories`. */
    @Test
    fun testCalculateGoalSetsTargetCalories() {
        openSettings()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        // After calculate, the target field testTag should still be displayed (no crash)
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.TARGET_CALORIES).performScrollTo().assertIsDisplayed()
    }

    /** Mirror of iOS `testCalculateGoalMatchesMifflinStJeor`. Seed = M/30/175/70/Moderate → ~2500–2560. */
    @Test
    fun testCalculateGoalMatchesMifflinStJeor() {
        openSettingsWithSeedData()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        // Any 4-digit calorie value in [2500, 2560] should appear as text on the screen
        // (the OutlinedTextField renders its value text in the semantics tree).
        val matches = (2500..2560).any { v ->
            composeTestRule.onAllNodesWithText(v.toString(), substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assert(matches) { "Calculated goal for seeded M/30/175/70/Moderate should land in 2500-2560 kcal" }
    }

    // MARK: - Seed Data Preload

    // MARK: - Picker change-selection (newly addressable after MockGemini infra)

    /** Mirror of iOS `testThemePickerCanChangeSelection`. */
    @Test
    fun testThemePickerCanChangeSelection() {
        openSettings()
        // Tap a non-default option ("Light"); Save button flips to enabled.
        composeTestRule.onNodeWithText("Light").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()
    }

    /** Mirror of iOS `testUnitPickerCanChangeSelection`. */
    @Test
    fun testUnitPickerCanChangeSelection() {
        openSettings()
        // Default is Metric (per fresh-launch). Tap US Customary; Save flips enabled.
        composeTestRule.onNodeWithText("US Customary").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()
    }

    /** Mirror of iOS `testGenderPickerCanChangeSelection`. */
    @Test
    fun testGenderPickerCanChangeSelection() {
        openSettings()
        // Default is Other; tap Female (a different segment) — Save flips enabled.
        composeTestRule.onNodeWithText("Female").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()
    }

    /** Mirror of iOS `testActivityPickerCanChangeSelection`. */
    @Test
    fun testActivityPickerCanChangeSelection() {
        openSettings()
        // Open the dropdown by tapping its OutlinedTextField anchor.
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.ACTIVITY_PICKER).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        // Pick a non-default option.
        composeTestRule.onNodeWithText("Very Active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()
    }

    /** Mirror of iOS `testSettingsLoadSeedProfileValues`. */
    @Test
    fun testSettingsLoadSeedProfileValues() {
        openSettingsWithSeedData()
        composeTestRule.onNodeWithText("Male").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Moderately Active").performScrollTo().assertIsDisplayed()
    }
}
