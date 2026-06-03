package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/ScanMenuTests.swift`.
 *
 * D-002 + D-003 closed in PR #22 — ScanMenuSheet now exists and a Menu-mode
 * camera path is wired via CaptureMode.Menu. Sheet-related iOS tests now
 * have strict Android mirrors below.
 *
 * Skipped — Android does not have an Edit/Done toolbar toggle (Material idiom):
 *   - testScannedMenusEditButtonTogglesMode
 *
 * Mirrored (5):
 *   - testScannedMenusShowsEmptyState
 *   - testScannedMenusListShowsRowsWithSeedData
 *   - testScannedMenusTappingRowOpensDetail
 *   - testMenuScanDetailDeleteButtonShowsConfirmation
 *   - testMenuScanDetailDeleteDialogCancelKeepsEntry (Android extra — confirms
 *     the dialog Cancel button keeps the entry)
 */
class ScanMenuParityTest : MainActivityComposeTest() {

    private fun openScannedMenus() {
        // D-002: tab tap now opens ScanMenuSheet; tap "Stored Menus" to reach the list.
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.SCAN_MENU).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.ScanMenuSheet.STORED_MENUS_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    // MARK: - ScanMenuSheet (D-002 closed — strict iOS mirrors)

    /** Mirror of iOS `testScanMenuSheetAccessibilityIDs`. */
    @Test
    fun testScanMenuSheetAccessibilityIDs() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.SCAN_MENU).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.ScanMenuSheet.SCAN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.ScanMenuSheet.CHOOSE_FROM_LIBRARY_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AccessibilityTags.ScanMenuSheet.STORED_MENUS_BUTTON).assertIsDisplayed()
    }

    /** Mirror of iOS `testScanMenuSheetScanButtonOpensCamera`. */
    @Test
    fun testScanMenuSheetScanButtonOpensCamera() {
        launchEmpty()
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.SCAN_MENU).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AccessibilityTags.ScanMenuSheet.SCAN_BUTTON).performClick()
        composeTestRule.waitForIdle()
        // CameraScreen renders the CAPTURE_BUTTON once permission is granted.
        // (CameraCaptureParityTest covers the granted-permission case; here we
        // verify the navigation happens — the sheet dismisses + camera route is reached.)
        assert(composeTestRule.onAllNodesWithTag(AccessibilityTags.ScanMenuSheet.SCAN_BUTTON).fetchSemanticsNodes().isEmpty()) {
            "Sheet should be dismissed after tapping Scan with Camera"
        }
    }

    /** Mirror of iOS `testStoredMenusShowsEmptyState` (D-002 closed — sheet flow). */
    @Test
    fun testScannedMenusShowsEmptyState() {
        launchEmpty()
        openScannedMenus()
        composeTestRule.onNodeWithTag(AccessibilityTags.ScannedMenus.EMPTY_STATE).assertIsDisplayed()
        composeTestRule.onNodeWithText("No scanned menus yet").assertIsDisplayed()
    }

    /** Mirror of iOS `testScannedMenusListShowsRowsWithSeedData`. */
    @Test
    fun testScannedMenusListShowsRowsWithSeedData() {
        launchWithMenuScans()
        openScannedMenus()
        composeTestRule.onNodeWithText("Mock Italian Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mock Sushi Bar").assertIsDisplayed()
    }

    /** Mirror of iOS `testScannedMenusTappingRowOpensDetail`. */
    @Test
    fun testScannedMenusTappingRowOpensDetail() {
        launchWithMenuScans()
        openScannedMenus()
        composeTestRule.onNodeWithText("Mock Italian Place").performClick()
        composeTestRule.waitForIdle()
        // Detail screen header is "Menu Analysis"; one of the seeded items is "Margherita Pizza".
        composeTestRule.onNodeWithText("Margherita Pizza").assertIsDisplayed()
    }

    /** Mirror of iOS `testMenuScanDetailDeleteButtonShowsConfirmation`. */
    @Test
    fun testMenuScanDetailDeleteButtonShowsConfirmation() {
        launchWithMenuScans()
        openScannedMenus()
        composeTestRule.onNodeWithText("Mock Italian Place").performClick()
        composeTestRule.waitForIdle()
        // Tap the trash icon in the TopAppBar (contentDescription = "Delete").
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete this scanned menu?").assertIsDisplayed()
    }

    /** Mirror of iOS `testScannedMenusSwipeToDeleteRevealsAction` (D-010 closed). */
    @Test
    fun testScannedMenusSwipeToDeleteRemovesRow() {
        launchWithMenuScans()
        openScannedMenus()
        composeTestRule.onNodeWithText("Mock Italian Place").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mock Italian Place").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Mock Italian Place").fetchSemanticsNodes().isEmpty()
        }
    }

    /** Android extra: Cancel in the delete dialog keeps the entry — verifies the dialog dismiss path. */
    @Test
    fun testMenuScanDetailDeleteDialogCancelKeepsEntry() {
        launchWithMenuScans()
        openScannedMenus()
        composeTestRule.onNodeWithText("Mock Italian Place").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete this scanned menu?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        // Dialog dismissed, still on detail screen — the trash icon and seeded item remain.
        composeTestRule.onNodeWithText("Margherita Pizza").assertIsDisplayed()
    }
}
