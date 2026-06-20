package com.pning80.watchmycalories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pning80.watchmycalories.parity.MainActivityComposeTest
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Top-level navigation smoke: Dashboard ↔ History ↔ Settings.
 *
 * Uses the parity launch harness ([MainActivityComposeTest.launchEmpty]) so the
 * Activity launches with `EXTRA_UI_TESTING` — that gates the Health Connect
 * permission request, which otherwise launches a system permission Activity that
 * backgrounds MainActivity and detaches the Compose tree ("No compose hierarchies
 * found"). Onboarding is pre-completed by the harness, so there's no onboarding step.
 */
class EndToEndFlowTest : MainActivityComposeTest() {

    @Test
    fun verifyBottomNavigationTabs() {
        launchEmpty()

        // App starts on Dashboard ("Today"). Navigate to History via its bottom-nav tab.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("History").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("History")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("HistoryTitle").fetchSemanticsNodes().isNotEmpty()
        }
        // Title now lives on MainActivity's TopAppBar.
        composeTestRule.onNodeWithTag("HistoryTitle").assertIsDisplayed()

        // Settings is no longer a bottom-nav tab; reach it via the TopAppBar gear
        // (AppMenu.MENU_BUTTON) → "Settings" menu item, mirroring the iOS toolbar gear.
        composeTestRule.onNodeWithTag(AccessibilityTags.AppMenu.MENU_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("SettingsTitle").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("SettingsTitle").assertIsDisplayed()

        // Return to Dashboard.
        composeTestRule.onAllNodesWithText("Today")[0].performClick()
        composeTestRule.waitForIdle()
    }
}
