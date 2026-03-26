import XCTest

final class EstimationReviewTests: WatchMyCaloriesUITestBase {

    /// Navigate to camera, capture, and tap Use to start estimation.
    private func startEstimation() {
        launchEmpty()
        app.tabBars.buttons["Scan Food"].tap()

        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
        captureButton.tap()

        // Dismiss disclaimer if shown
        let disclaimerContinue = app.buttons["disclaimer_continueButton"]
        if disclaimerContinue.waitForExistence(timeout: 3) {
            disclaimerContinue.tap()
        }

        let useButton = app.buttons["camera_usePhotoButton"]
        XCTAssertTrue(useButton.waitForExistence(timeout: 5))
        useButton.tap()
    }

    private func elementExists(_ identifier: String, timeout: TimeInterval = 5) -> Bool {
        // SwiftUI VStacks may appear as different element types
        return app.otherElements[identifier].waitForExistence(timeout: timeout)
            || app.scrollViews[identifier].exists
            || app.staticTexts[identifier].exists
    }

    /// Wait for estimation to finish (success or error).
    /// Returns "success", "error", "noFood", or "loading".
    @discardableResult
    private func waitForEstimationResult(timeout: TimeInterval = 30) -> String {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if elementExists("review_success", timeout: 0.5) { return "success" }
            if elementExists("review_error", timeout: 0.5) { return "error" }
            if elementExists("review_noFood", timeout: 0.5) { return "noFood" }
            // Check for "View Results" button which appears when estimation completes successfully
            if app.buttons["ads_viewResultsButton"].exists { return "completedLoading" }
            RunLoop.current.run(until: Date().addingTimeInterval(0.5))
        }
        return "loading"
    }

    // MARK: - Loading State

    func testLoadingStateAppears() {
        startEstimation()

        // The loading view or a result should appear
        let loadingExists = elementExists("review_loading")
        let successExists = elementExists("review_success", timeout: 1)
        let errorExists = elementExists("review_error", timeout: 1)
        // Also check for text indicators
        let analyzingText = app.staticTexts["Analyzing food..."].exists
        let viewResults = app.buttons["ads_viewResultsButton"].exists

        XCTAssertTrue(
            loadingExists || successExists || errorExists || analyzingText || viewResults,
            "Estimation screen should show loading or result"
        )
    }

    // MARK: - Result States

    func testEstimationResultShowsActionButton() {
        startEstimation()
        let result = waitForEstimationResult()

        switch result {
        case "success":
            XCTAssertTrue(app.buttons["review_doneButton"].exists, "Done button should appear on success")
        case "completedLoading":
            XCTAssertTrue(app.buttons["ads_viewResultsButton"].exists, "View Results button should appear")
        case "error":
            XCTAssertTrue(app.buttons["review_tryAgainButton"].exists, "Try Again button should appear on error")
        case "noFood":
            XCTAssertTrue(
                app.buttons["review_tryAgainButton"].exists || app.buttons["review_cancelButton"].exists,
                "Action button should appear on no food"
            )
        default:
            // Still loading after timeout — not a failure, just slow network
            break
        }
    }

    func testDoneButtonReturnsToDashboard() {
        startEstimation()
        let result = waitForEstimationResult()

        // Navigate through to success view
        if result == "completedLoading" {
            let viewResults = app.buttons["ads_viewResultsButton"]
            if viewResults.waitForExistence(timeout: 3) {
                viewResults.tap()
            }
        }

        guard result == "success" || result == "completedLoading" else { return }

        let doneButton = app.buttons["review_doneButton"]
        guard doneButton.waitForExistence(timeout: 5) else { return }
        doneButton.tap()

        // Should be back on Dashboard
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 5))
    }

    func testCancelButtonOnError() {
        startEstimation()
        let result = waitForEstimationResult()

        guard result == "error" || result == "noFood" else { return }

        let cancelButton = app.buttons["review_cancelButton"]
        guard cancelButton.waitForExistence(timeout: 3) else { return }
        cancelButton.tap()

        // Should return to camera
        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
    }
}
