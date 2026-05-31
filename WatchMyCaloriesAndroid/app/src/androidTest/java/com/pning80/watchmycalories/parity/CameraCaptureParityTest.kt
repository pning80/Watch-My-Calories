package com.pning80.watchmycalories.parity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.pning80.watchmycalories.utils.AccessibilityTags
import org.junit.Rule
import org.junit.Test

/**
 * Parity mirror of iOS `WatchMyCaloriesUITests/CameraCaptureTests.swift`.
 *
 * Most iOS CameraCapture tests drive the full capture → review → use → estimation
 * loop. On Android, simulating a CameraX capture in an instrumented test is
 * hardware-dependent (real camera or stub) and brittle — we don't replicate that
 * here. What we DO mirror is the structural surface: granting CAMERA permission
 * up front and asserting the capture button renders.
 *
 * Skipped — require a real or stubbed camera capture (out-of-scope for parity audit):
 *   - testCaptureShowsReviewButtons · testRetakeReturnsToCamera ·
 *     testMealTypePickerAppearsOnReview · testMealTypePickerSelectionChanges ·
 *     testUsePhotoNavigatesToEstimation ·
 *     testDisclaimer{AppearsOnFirstCapture,ReappearsWhenNotDismissedPermanently,
 *     DoesNotReappearAfterDontShowAgain}
 *   The Disclaimer + Review screens are exercised through the camera-bypass path
 *   in EstimationReviewParityTest already, so the parity audit's evidence for
 *   those code paths is recorded there.
 */
class CameraCaptureParityTest : MainActivityComposeTest() {

    @get:Rule
    val cameraPermission: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    /** Mirror of iOS `testCaptureButtonExists`. */
    @Test
    fun testCaptureButtonExists() {
        launchEmpty()
        // Open Log Food sheet, tap Scan Food to land on the camera screen.
        composeTestRule.onNodeWithTag(AccessibilityTags.Tab.CAMERA).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Scan Food").performClick()
        composeTestRule.waitForIdle()
        // CameraX preview + capture button should render now that the permission is granted.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(AccessibilityTags.Camera.CAPTURE_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(AccessibilityTags.Camera.CAPTURE_BUTTON).assertIsDisplayed()
    }
}

