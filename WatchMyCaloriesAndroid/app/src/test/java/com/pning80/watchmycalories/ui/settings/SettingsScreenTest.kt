package com.pning80.watchmycalories.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pning80.watchmycalories.BaseComposeTest
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.utils.AccessibilityTags
import androidx.compose.ui.semantics.getOrNull
import androidx.datastore.preferences.core.edit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreenTest : BaseComposeTest() {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setup() {
        settingsDataStore = SettingsDataStore(RuntimeEnvironment.getApplication())
        // Clear any DataStore values from previous test classes to prevent pollution,
        // then pin metric so these tests are deterministic regardless of the
        // locale-based unit default (US locale → imperial would otherwise change the
        // rendered profile controls + goal-calc rounding and break value assertions).
        kotlinx.coroutines.runBlocking {
            RuntimeEnvironment.getApplication().dataStore.edit { it.clear() }
            settingsDataStore.setMetric(true)
        }
    }

    @Test
    fun testSettingsFieldsExist() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = null,
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        // Topbar Title
        composeTestRule.onNodeWithTag("SettingsTitle").assertIsDisplayed()

        // Sections - Use assertExists since they might be in a scrollable column off-screen.
        // ignoreCase: section headers render UPPERCASE to match iOS Form headers.
        composeTestRule.onNodeWithText("App Appearance", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Theme").assertExists()
        composeTestRule.onNodeWithText("Unit System").assertExists()
        composeTestRule.onNodeWithText("Profile", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Daily Goals", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Privacy", ignoreCase = true).assertExists()
        // "About & Support" section removed in PR C — About reached via Dashboard menu.
    }

    @Test
    fun testCalculateGoalSetsTargetCalories() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = null,
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        // Tap Calculate
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()

        // Check Target Calories text field has a value
        val targetNode = composeTestRule.onNodeWithTag(AccessibilityTags.Settings.TARGET_CALORIES).performScrollTo()
        targetNode.assertExists()
        // Default OTHER 30y 173cm 68kg sedentary suggested = 1740
        targetNode.assertTextContains("1740")
    }

    @Test
    fun testCancelShowsDiscardDialogWhenChanged() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 175.0,
                    weight = 70.0,
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        // No dialog initially
        composeTestRule.onNodeWithText("You have unsaved changes.").assertDoesNotExist()

        // Make change
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()

        // Tap Cancel
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()

        // Dialog should show
        composeTestRule.onNodeWithText("You have unsaved changes.").assertIsDisplayed()
    }

    @Test
    fun testDiscardChangesTriggersOnCancel() {
        var cancelTriggered = false
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 175.0,
                    weight = 70.0,
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = { cancelTriggered = true }
            )
        }

        // Make change
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()

        // Cancel -> Discard
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.onNodeWithText("Discard Changes").performClick()

        assertTrue("onCancel should be triggered after confirming discard", cancelTriggered)
    }

    @Test
    fun testSaveTriggersOnSave() {
        var savedProfile: UserProfile? = null
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 175.0,
                    weight = 70.0,
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = { savedProfile = it },
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        // Save is always enabled (iOS parity — the toolbar Save never disables;
        // SettingsView.swift:279 has no .disabled()).
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()

        // Make change
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()

        // Tap Save
        val saveButton = composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON)
        saveButton.assertIsEnabled()
        saveButton.performClick()

        assertTrue(savedProfile != null)
        assertEquals(1979.0, savedProfile!!.targetCalories, 0.0) // 175cm 70kg male 30y sedentary BMR = 1648.75 * 1.2 = 1978.5 => rounded 1979.
    }

    @Test
    fun testSaveButtonBehaviorInUSCustomaryMode() = kotlinx.coroutines.runBlocking {
        settingsDataStore.setMetric(false)

        var savedProfile: UserProfile? = null
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 172.72, // 5 ft 8 in (68 in)
                    weight = 68.0388, // 150 lbs
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = { savedProfile = it },
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        // Save is always enabled (iOS parity — SettingsView.swift:279, no .disabled()).
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()

        // Click Calculate Goal to trigger a change in target calories
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()

        // Save stays enabled; tapping it persists the profile.
        val saveButton = composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON)
        saveButton.assertIsEnabled()
        saveButton.performClick()

        assertTrue(savedProfile != null)
        assertEquals(172.72, savedProfile!!.height, 0.01)
    }

    @Test
    fun testCancelNoDialogWhenUnchanged() {
        var cancelTriggered = false
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 175.0,
                    weight = 70.0,
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = { cancelTriggered = true }
            )
        }

        // No dialog initially
        composeTestRule.onNodeWithText("You have unsaved changes.").assertDoesNotExist()

        // Tap Cancel without making any changes
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()

        // No dialog should appear, cancel should fire directly
        composeTestRule.onNodeWithText("You have unsaved changes.").assertDoesNotExist()
        assertTrue("onCancel should fire directly when no changes exist", cancelTriggered)
    }

    @Test
    fun testKeepEditingFromCancelDialog() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 175.0,
                    weight = 70.0,
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        // Wait for DataStore to load
        composeTestRule.waitForIdle()

        // Make a change by editing the target calories field
        val targetField = composeTestRule.onNodeWithTag(AccessibilityTags.Settings.TARGET_CALORIES)
        targetField.performScrollTo()
        targetField.performTextInput("999")

        // Tap Cancel
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()

        // Dialog should show
        composeTestRule.onNodeWithText("You have unsaved changes.").assertIsDisplayed()

        // Tap Keep Editing
        composeTestRule.onNodeWithText("Keep Editing").performClick()

        // Dialog should dismiss but we should still be on Settings
        composeTestRule.onNodeWithText("You have unsaved changes.").assertDoesNotExist()
        composeTestRule.onNodeWithTag("SettingsTitle").assertIsDisplayed()
    }

    @Test
    fun testProfileSectionFieldsVisible() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = null,
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Height").assertExists()
        composeTestRule.onNodeWithText("Weight").assertExists()
        composeTestRule.onNodeWithText("Age").assertExists()
        composeTestRule.onNodeWithText("Gender").assertExists()
        composeTestRule.onNodeWithText("Activity Level").assertExists()
    }

    @Test
    fun testTargetCaloriesFieldEditable() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = null,
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        val targetField = composeTestRule.onNodeWithTag(AccessibilityTags.Settings.TARGET_CALORIES)
        targetField.performScrollTo()
        targetField.performTextInput("1800")
        targetField.assertTextContains("1800")
    }

    @Test
    fun testPrivacySectionInfoText() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = null,
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("food photos are sent to Google Gemini", substring = true)
            .assertExists()
    }

    @Test
    fun testAIConsentToggleExists() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = null,
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.AI_CONSENT_TOGGLE)
            .assertExists()
    }

    // testAboutAndSupportSectionExists removed in PR C — About is now
    // reached via the Dashboard overflow menu, not from inside Settings.

    // Save is always enabled now (iOS parity), so dirty-tracking is verified via
    // the Cancel discard dialog instead of the Save button's enabled state.

    @Test
    fun testChangingThemeMarksUnsavedChanges() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 175.0,
                    weight = 70.0,
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        // Save is always enabled; no discard dialog before any edit.
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()
        composeTestRule.onNodeWithText("You have unsaved changes.").assertDoesNotExist()

        // Open InlineMenuPickerRow (theme), then tap a non-default option.
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.THEME_PICKER).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Light").performClick()
        composeTestRule.waitForIdle()

        // The change marks the form dirty → Cancel surfaces the discard dialog.
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.onNodeWithText("You have unsaved changes.").assertIsDisplayed()
    }

    @Test
    fun testChangingUnitSystemMarksUnsavedChanges() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 175.0,
                    weight = 70.0,
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()
        composeTestRule.onNodeWithText("You have unsaved changes.").assertDoesNotExist()

        // Open InlineMenuPickerRow (unit system), then tap US Customary.
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.UNIT_PICKER).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("US Customary").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.onNodeWithText("You have unsaved changes.").assertIsDisplayed()
    }

    @Test
    fun testChangingAiConsentMarksUnsavedChanges() {
        composeTestRule.setContent {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                currentProfile = UserProfile(
                    id = 1,
                    height = 175.0,
                    weight = 70.0,
                    age = 30,
                    genderRaw = "Male",
                    activityLevelRaw = "Sedentary",
                    targetCalories = 2200.0
                ),
                onSaveProfile = {},
                onNavigateToAbout = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsEnabled()
        composeTestRule.onNodeWithText("You have unsaved changes.").assertDoesNotExist()

        // Tap AI consent toggle
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.AI_CONSENT_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()
        composeTestRule.onNodeWithText("You have unsaved changes.").assertIsDisplayed()
    }
}

