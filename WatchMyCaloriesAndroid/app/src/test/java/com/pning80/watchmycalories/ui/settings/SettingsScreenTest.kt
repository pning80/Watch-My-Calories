package com.pning80.watchmycalories.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pning80.watchmycalories.BaseComposeTest
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.utils.AccessibilityTags
import androidx.compose.ui.semantics.getOrNull
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

        // Sections - Use assertExists since they might be in a scrollable column off-screen
        composeTestRule.onNodeWithText("App Appearance").assertExists()
        composeTestRule.onNodeWithText("Theme").assertExists()
        composeTestRule.onNodeWithText("Unit System").assertExists()
        composeTestRule.onNodeWithText("Profile").assertExists()
        composeTestRule.onNodeWithText("Daily Goals").assertExists()
        composeTestRule.onNodeWithText("Privacy").assertExists()
        composeTestRule.onNodeWithText("About & Support").assertExists()
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
        composeTestRule.onNodeWithText("Discard changes?").assertDoesNotExist()

        // Make change
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()

        // Tap Cancel
        composeTestRule.onNodeWithTag("settings_cancel_button").performClick()

        // Dialog should show
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeTestRule.onNodeWithText("You have unsaved changes. Are you sure you want to discard them?").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Discard").performClick()

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

        // Initially Save is disabled because no changes exist
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON).assertIsNotEnabled()

        // Make change
        composeTestRule.onNodeWithTag(AccessibilityTags.Settings.CALCULATE_GOAL).performScrollTo().performClick()

        // Tap Save
        val saveButton = composeTestRule.onNodeWithTag(AccessibilityTags.Settings.SAVE_BUTTON)
        saveButton.assertIsEnabled()
        saveButton.performClick()

        assertTrue(savedProfile != null)
        assertEquals(1979.0, savedProfile!!.targetCalories, 0.0) // 175cm 70kg male 30y sedentary BMR = 1648.75 * 1.2 = 1978.5 => rounded 1979.
    }
}
