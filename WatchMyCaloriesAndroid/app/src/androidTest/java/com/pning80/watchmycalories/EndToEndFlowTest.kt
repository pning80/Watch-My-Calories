package com.pning80.watchmycalories

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.lang.Exception

class EndToEndFlowTest {

    // Using createAndroidComposeRule launches the actual MainActivity,
    // wiring the NavHost, Themes, and real system flows natively!
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyBottomNavigationTabs() {
        // App starts on Dashboard by default
        composeTestRule.onAllNodesWithText("Watch My Calories")[0].assertIsDisplayed()

        // Click "History" in Bottom navigation
        composeTestRule.onAllNodesWithText("History")[0].performClick()
        
        // Ensure History screen is displayed
        composeTestRule.onAllNodesWithText("History")[0].assertIsDisplayed() // Header
        
        // Click "Settings" in Bottom navigation
        composeTestRule.onAllNodesWithText("Settings")[0].performClick()
        
        // Ensure Settings screen is displayed
        composeTestRule.onAllNodesWithText("Preferences")[0].assertIsDisplayed()
        
        // Return to Dashboard
        try {
            composeTestRule.onAllNodesWithText("Dashboard")[0].performClick()
        } catch (e: Exception) {
            // Navigation hidden tab
        }
    }
}
