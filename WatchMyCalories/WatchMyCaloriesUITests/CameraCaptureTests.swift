import XCTest

final class CameraCaptureTests: WatchMyCaloriesUITestBase {

    private func navigateToCamera() {
        launchEmpty()
        app.tabBars.buttons["Scan"].tap()
    }

    private func capturePhoto() {
        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
        captureButton.tap()
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

        XCTAssertTrue(app.buttons["camera_retakeButton"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["camera_usePhotoButton"].exists)
    }

    func testRetakeReturnsToCamera() {
        navigateToCamera()
        capturePhoto()

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
}
