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
 * Most iOS tests target the `ScanMenuSheet` 3-option modal that Android does
 * not have (deviation D-002 — Android tab navigates straight to
 * `ScannedMenusScreen` whose only mutation entry point is a FAB → photo
 * library picker). Tests that depend on that sheet are skipped here.
 *
 * Skipped — D-002 (sheet not on Android):
 *   - testScanMenuSheetAccessibilityIDs
 *   - testScanMenuSheetDismissOnSwipe
 *   - testScanMenuSheetScanButtonOpensCamera (also D-003 — no menu camera)
 *   - testScanMenuSheetChooseFromLibraryButtonOpensPicker
 *
 * Skipped — D-003 (no menu camera on Android):
 *   - testMenuCameraScreenHasCaptureButton
 *   - testMenuCameraCancelDismissesToDashboard
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
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.SCAN_MENU).performClick()
        composeTestRule.waitForIdle()
    }

    /** Mirror of iOS `testStoredMenusShowsEmptyState` — Android shows empty state directly without the iOS sheet. */
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
