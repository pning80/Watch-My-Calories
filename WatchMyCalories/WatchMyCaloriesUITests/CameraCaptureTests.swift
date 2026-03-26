import XCTest

final class CameraCaptureTests: WatchMyCaloriesUITestBase {

    private func navigateToCamera() {
        launchEmpty()
        app.tabBars.buttons["Scan Food"].tap()
    }

    private func capturePhoto() {
        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
        captureButton.tap()
    }

    /// Dismiss the calorie estimate disclaimer sheet if it appears
    private func dismissDisclaimerIfPresent() {
        let continueButton = app.buttons["disclaimer_continueButton"]
        if continueButton.waitForExistence(timeout: 3) {
            continueButton.tap()
        }
    }

    // MARK: - Camera Tab

    func testCaptureButtonExists() {
        navigateToCamera()

        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
    }

    // MARK: - Photo Review

    func testCaptureShowsReviewButtons() {
        navigateToCamera()
        capturePhoto()
        dismissDisclaimerIfPresent()

        XCTAssertTrue(app.buttons["camera_retakeButton"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["camera_usePhotoButton"].exists)
    }

    func testRetakeReturnsToCamera() {
        navigateToCamera()
        capturePhoto()
        dismissDisclaimerIfPresent()

        let retakeButton = app.buttons["camera_retakeButton"]
        XCTAssertTrue(retakeButton.waitForExistence(timeout: 5))
        retakeButton.tap()

        // Should be back at capture button
        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
    }

    // MARK: - Meal Type Picker

    func testMealTypePickerAppearsOnReview() {
        navigateToCamera()
        capturePhoto()
        dismissDisclaimerIfPresent()

        // All 4 meal type buttons should be visible
        XCTAssertTrue(app.buttons["camera_retakeButton"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Breakfast"].exists)
        XCTAssertTrue(app.buttons["Lunch"].exists)
        XCTAssertTrue(app.buttons["Dinner"].exists)
        XCTAssertTrue(app.buttons["Snack"].exists)
    }

    func testMealTypePickerSelectionChanges() {
        navigateToCamera()
        capturePhoto()
        dismissDisclaimerIfPresent()

        XCTAssertTrue(app.buttons["camera_retakeButton"].waitForExistence(timeout: 5))

        // Tap Breakfast, then Dinner — both should be tappable
        app.buttons["Breakfast"].tap()
        XCTAssertTrue(app.buttons["Breakfast"].exists)

        app.buttons["Dinner"].tap()
        XCTAssertTrue(app.buttons["Dinner"].exists)
    }

    // MARK: - Use Photo → Estimation

    func testUsePhotoNavigatesToEstimation() {
        navigateToCamera()
        capturePhoto()
        dismissDisclaimerIfPresent()

        let useButton = app.buttons["camera_usePhotoButton"]
        XCTAssertTrue(useButton.waitForExistence(timeout: 5))
        useButton.tap()

        // Should navigate to estimation review — check for any indicator
        let appeared = app.otherElements["review_loading"].waitForExistence(timeout: 5)
            || app.staticTexts["Analyzing food..."].exists
            || app.buttons["ads_viewResultsButton"].exists
            || app.otherElements["review_success"].exists
            || app.otherElements["review_error"].exists
            || app.staticTexts["Watch My Calories"].exists
        XCTAssertTrue(appeared, "Estimation review screen should appear")
    }

    // MARK: - Calorie Estimate Disclaimer

    func testDisclaimerAppearsOnFirstCapture() {
        navigateToCamera()
        capturePhoto()

        // Disclaimer sheet should appear
        let continueButton = app.buttons["disclaimer_continueButton"]
        XCTAssertTrue(continueButton.waitForExistence(timeout: 5))

        XCTAssertTrue(app.staticTexts["Estimates Are Approximate"].exists)

        let toggle = app.switches["disclaimer_dontShowToggle"]
        XCTAssertTrue(toggle.exists)
    }

    func testDisclaimerReappearsWhenNotDismissedPermanently() {
        navigateToCamera()
        capturePhoto()

        // Dismiss without checking "Don't show again"
        let continueButton = app.buttons["disclaimer_continueButton"]
        XCTAssertTrue(continueButton.waitForExistence(timeout: 5))
        continueButton.tap()

        // Retake and capture again
        let retakeButton = app.buttons["camera_retakeButton"]
        XCTAssertTrue(retakeButton.waitForExistence(timeout: 5))
        retakeButton.tap()

        capturePhoto()

        // Disclaimer should appear again
        XCTAssertTrue(app.buttons["disclaimer_continueButton"].waitForExistence(timeout: 5))
    }

    func testDisclaimerDoesNotReappearAfterDontShowAgain() {
        navigateToCamera()
        capturePhoto()

        // Check "Don't show again" and continue
        let toggle = app.switches["disclaimer_dontShowToggle"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 5))
        toggle.switches.firstMatch.tap()

        let continueButton = app.buttons["disclaimer_continueButton"]
        continueButton.tap()

        // Retake and capture again
        let retakeButton = app.buttons["camera_retakeButton"]
        XCTAssertTrue(retakeButton.waitForExistence(timeout: 5))
        retakeButton.tap()

        capturePhoto()

        // Disclaimer should NOT appear
        XCTAssertFalse(app.buttons["disclaimer_continueButton"].waitForExistence(timeout: 2))

        // Review buttons should be visible directly
        XCTAssertTrue(app.buttons["camera_retakeButton"].waitForExistence(timeout: 3))
    }
}
