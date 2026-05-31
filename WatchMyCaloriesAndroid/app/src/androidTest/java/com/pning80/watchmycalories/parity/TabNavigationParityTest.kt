package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/TabNavigationTests.swift`.
 *
 * Covers the four bottom-nav buttons (Dashboard / Log Food / Scan Menu / History)
 * and the basic navigation round-trip. Settings is intentionally NOT in the
 * bottom nav on either platform — gear icon on Android, toolbar on iOS.
 *
 * Skipped — parity divergences, recorded in PORTING_DEVIATIONS.md:
 *   - testScanMenuTabOpensSheetWithOptions (iOS-only `ScanMenuSheet`, D-002)
 *   - testScannedMenusDoneReturnsToDashboard (iOS-only Done-to-dismiss flow, D-002)
 *   - testCameraRootCancelReturnsToDashboard (CameraX permission prompt on
 *     Pixel 9a — not yet wired into TestSeed; documented as a future hook)
 */
class TabNavigationParityTest : MainActivityComposeTest() {

    /** Mirror of iOS `testAllTabsAreVisible`. */
    @Test
    fun testAllTabsAreVisible() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.DASHBOARD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.CAMERA).assertIsDisplayed()    // Log Food
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.SCAN_MENU).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.HISTORY).assertIsDisplayed()
    }

    /** Mirror of iOS `testSettingsTabDoesNotExist`. */
    @Test
    fun testSettingsTabDoesNotExist() {
        launchEmpty()
        // Settings is reachable via the TopAppBar gear icon, not as a bottom-nav tab.
        assert(composeTestRule.onAllNodesWithTag(AccessibilityTags.Tab.SETTINGS).fetchSemanticsNodes().isEmpty()) {
            "Settings should not appear as a bottom-nav tab"
        }
    }

    /** Mirror of iOS `testTappingHistoryTabShowsHistory`. */
    @Test
    fun testTappingHistoryTabShowsHistory() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.HISTORY).performClick()
        composeTestRule.waitForIdle()
        // TopAppBar title "History" appears with testTag "HistoryTitle" (see MainActivity.kt:217).
        composeTestRule.onNodeWithTag("HistoryTitle").assertIsDisplayed()
    }

    /** Mirror of iOS `testTappingTodayTabReturnsToDashboard`. */
    @Test
    fun testTappingTodayTabReturnsToDashboard() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.HISTORY).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.DASHBOARD).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.EMPTY_STATE_CARD).assertIsDisplayed()
    }

    /** Mirror of iOS `testTappingLogFoodTabShowsSheet`. */
    @Test
    fun testTappingLogFoodTabShowsSheet() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.CAMERA).performClick()
        composeTestRule.waitForIdle()
        // LogFoodSheet exposes a "Log Manually" option as a stable text marker.
        composeTestRule.onNodeWithText("Log Manually").assertIsDisplayed()
    }

    /**
     * Android-adapted mirror. iOS tests `testTappingScanMenuTabShowsSheet` because
     * iOS shows the `ScanMenuSheet` (3 options) when tapping the tab. Android
     * navigates directly to the Scanned Menus screen per deviation D-002 —
     * asserting the Scanned Menus TopAppBar title is the parity-aware check.
     */
    @Test
    fun testTappingScanMenuTabNavigatesToScannedMenus() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.SCAN_MENU).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ScannedMenusTitle").assertIsDisplayed()
    }

    /** Mirror of iOS `testRoundTripTabNavigation`. */
    @Test
    fun testRoundTripTabNavigation() {
        launchEmpty()
        // History
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.HISTORY).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("HistoryTitle").assertIsDisplayed()
        // Back to Dashboard
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.DASHBOARD).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.Dashboard.EMPTY_STATE_CARD).assertIsDisplayed()
    }
}
