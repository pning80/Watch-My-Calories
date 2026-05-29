package com.pning80.watchmycalories

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EndToEndFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyBottomNavigationTabs() {
        // Wait for the initial compose tree to stabilize
        composeTestRule.waitForIdle()

        // The app may start with onboarding (DataStore defaults to false for new installs).
        // Skip it if visible, then wait for navigation to re-compose.
        val skipNodes = composeTestRule.onAllNodesWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).fetchSemanticsNodes()
        if (skipNodes.isNotEmpty()) {
            composeTestRule.onNodeWithTag(AccessibilityTags.Onboarding.SKIP_BUTTON).performClick()
            composeTestRule.waitForIdle()
            // After skip, the onboarding writes to DataStore and calls onComplete.
            // Give the recomposition time to settle.
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithText("Dashboard").fetchSemanticsNodes().isNotEmpty()
            }
        }

        // App should be on Dashboard now
        composeTestRule.waitForIdle()

        // Click "History" in Bottom navigation
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("History").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("History")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Ensure History screen is displayed (title now lives on MainActivity's TopAppBar)
        composeTestRule.onNodeWithTag("HistoryTitle").assertIsDisplayed()

        // Settings is no longer in the bottom nav; reach it via the gear icon in the
        // TopAppBar (AppMenu.MENU_BUTTON), mirroring the iOS toolbar gear affordance.
        composeTestRule.onNodeWithTag(AccessibilityTags.AppMenu.MENU_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Ensure Settings screen is displayed
        composeTestRule.onNodeWithTag("SettingsTitle").assertIsDisplayed()
        
        // Return to Dashboard
        composeTestRule.onAllNodesWithText("Dashboard")[0].performClick()
        composeTestRule.waitForIdle()
    }
}
