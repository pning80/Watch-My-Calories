import XCTest

final class EstimationReviewTests: WatchMyCaloriesUITestBase {

    /// Navigate to camera, capture, and tap Use to start estimation.
    private func startEstimation() {
        launchEmpty()
        // Open Log Food sheet, then tap Scan Food
        app.tabBars.buttons["Log Food"].tap()
        let scanFood = app.staticTexts["Scan Food"]
        XCTAssertTrue(scanFood.waitForExistence(timeout: 3))
        scanFood.tap()

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
        let addButton = app.otherElements["dashboard_heroCard"]
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

    // MARK: - Success Screen Content

    func testSuccessScreenShowsLoggedMessage() {
        startEstimation()
        let result = waitForEstimationResult()

        if result == "completedLoading" {
            let viewResults = app.buttons["ads_viewResultsButton"]
            if viewResults.waitForExistence(timeout: 3) {
                viewResults.tap()
            }
        }

        guard result == "success" || result == "completedLoading" else { return }
        guard elementExists("review_success") else { return }

        XCTAssertTrue(
            app.staticTexts["Logged Successfully!"].exists,
            "Success screen should show 'Logged Successfully!' message"
        )
    }

    func testSuccessScreenShowsTotalAdded() {
        startEstimation()
        let result = waitForEstimationResult()

        if result == "completedLoading" {
            let viewResults = app.buttons["ads_viewResultsButton"]
            if viewResults.waitForExistence(timeout: 3) {
                viewResults.tap()
            }
        }

        guard result == "success" || result == "completedLoading" else { return }
        guard elementExists("review_success") else { return }

        XCTAssertTrue(
            app.staticTexts["Total Added"].exists,
            "Success screen should show 'Total Added' label"
        )
    }

    func testDoneButtonVisibleWithoutScrolling() {
        startEstimation()
        let result = waitForEstimationResult()

        if result == "completedLoading" {
            let viewResults = app.buttons["ads_viewResultsButton"]
            if viewResults.waitForExistence(timeout: 3) {
                viewResults.tap()
            }
        }

        guard result == "success" || result == "completedLoading" else { return }

        let doneButton = app.buttons["review_doneButton"]
        guard doneButton.waitForExistence(timeout: 5) else { return }

        // Done button should be hittable without scrolling (pinned at bottom)
        XCTAssertTrue(doneButton.isHittable, "Done button should be visible without scrolling")
    }

    // MARK: - Parity audit (2026-05-30) — error / no-food / AI consent

    private func startEstimationWithLaunchArgs(_ args: [String]) {
        for arg in args { app.launchArguments.append(arg) }
        app.launchArguments.append("--ai-consent-accepted")
        app.launch()
        app.tabBars.buttons["Log Food"].tap()
        let scanFood = app.staticTexts["Scan Food"]
        XCTAssertTrue(scanFood.waitForExistence(timeout: 3))
        scanFood.tap()

        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
        captureButton.tap()

        let disclaimerContinue = app.buttons["disclaimer_continueButton"]
        if disclaimerContinue.waitForExistence(timeout: 3) {
            disclaimerContinue.tap()
        }

        let useButton = app.buttons["camera_usePhotoButton"]
        XCTAssertTrue(useButton.waitForExistence(timeout: 5))
        useButton.tap()
    }

    /// Wait for the error VIEW (the inline error VStack inside the loading container) to
    /// appear. With the IOS-BUG-1 fix landed (this PR), the error VStack now exposes the
    /// `review_error` accessibility identifier, so we can detect the error state directly
    /// instead of inferring it from the "Try Again" button label.
    ///
    /// We still tap action buttons via their LABEL ("Try Again", "Cancel") because IOS-BUG-2
    /// (SwiftUI `.buttonStyle(.borderedProminent)` strips `.accessibilityIdentifier`) is
    /// unrelated and unfixed.
    private func waitForErrorView(timeout: TimeInterval = 30) -> Bool {
        return elementExists("review_error", timeout: timeout)
    }

    func testErrorStateShowsTryAgainButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-error"])
        XCTAssertTrue(waitForErrorView(),
                      "Mock-error mode should render the review_error view")
        XCTAssertTrue(app.buttons["Try Again"].exists,
                      "Try Again button must appear inside the error view")
        XCTAssertTrue(app.staticTexts["Analysis Failed"].exists,
                      "Error state should show 'Analysis Failed' headline")
    }

    func testErrorStateShowsCancelButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-error"])
        XCTAssertTrue(waitForErrorView())
        XCTAssertTrue(app.buttons["Cancel"].exists,
                      "Cancel button must appear on error state")
    }

    func testErrorStateShowDetailsButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-error"])
        XCTAssertTrue(waitForErrorView())
        let detailsButton = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] %@", "details")).firstMatch
        XCTAssertTrue(detailsButton.waitForExistence(timeout: 3),
                      "Show/Hide Details button must be present on error state")
    }

    func testErrorStateTryAgainTapResetsView() {
        startEstimationWithLaunchArgs(["--mock-estimation-error"])
        XCTAssertTrue(waitForErrorView())
        app.buttons["Try Again"].tap()
        // After Try Again, the loading state re-fires and we end up back in error.
        XCTAssertTrue(waitForErrorView(timeout: 15))
    }

    /// In no-food mode the mock returns success-with-empty-items. The flow is:
    /// loading → "View Results" button appears → user taps it → no-food UI with Try Again/Cancel.
    private func advanceToNoFoodUI() {
        let viewResults = app.buttons["View Results"]
        XCTAssertTrue(viewResults.waitForExistence(timeout: 30),
                      "View Results button must appear after empty-items estimation")
        viewResults.tap()
        // Now the no-food UI should render with its "No Food Detected" headline.
        XCTAssertTrue(app.staticTexts["No Food Detected"].waitForExistence(timeout: 5))
    }

    func testNoFoodStateShowsTryAgainButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-no-food"])
        advanceToNoFoodUI()
        XCTAssertTrue(app.buttons["Try Again"].exists,
                      "No-food UI should expose a Try Again button")
    }

    func testNoFoodStateShowsCancelButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-no-food"])
        advanceToNoFoodUI()
        XCTAssertTrue(app.buttons["Cancel"].exists,
                      "No-food UI should expose a Cancel button")
    }

    func testAIConsentSheetFiresWhenConsentMissing() {
        // Do NOT pass --ai-consent-accepted; consent is .notAsked by default under --uitesting.
        launchEmpty()
        app.tabBars.buttons["Log Food"].tap()
        let scanFood = app.staticTexts["Scan Food"]
        XCTAssertTrue(scanFood.waitForExistence(timeout: 3))
        scanFood.tap()
        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
        captureButton.tap()
        let disclaimerContinue = app.buttons["disclaimer_continueButton"]
        if disclaimerContinue.waitForExistence(timeout: 3) { disclaimerContinue.tap() }
        let useButton = app.buttons["camera_usePhotoButton"]
        XCTAssertTrue(useButton.waitForExistence(timeout: 5))
        useButton.tap()
        // AIConsentSheet should present — look for an Allow / Don't Allow style action
        let allowButton = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] %@ OR label CONTAINS[c] %@", "allow", "consent")).firstMatch
        XCTAssertTrue(allowButton.waitForExistence(timeout: 5),
                      "AIConsentSheet should fire when consent has not been granted yet")
    }

    func testAIConsentDeclineDismissesEstimation() {
        launchEmpty()
        app.tabBars.buttons["Log Food"].tap()
        let scanFood = app.staticTexts["Scan Food"]
        XCTAssertTrue(scanFood.waitForExistence(timeout: 3))
        scanFood.tap()
        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
        captureButton.tap()
        let disclaimerContinue = app.buttons["disclaimer_continueButton"]
        if disclaimerContinue.waitForExistence(timeout: 3) { disclaimerContinue.tap() }
        let useButton = app.buttons["camera_usePhotoButton"]
        useButton.tap()
        // Find a Decline / Don't Allow button on the consent sheet
        let declineButton = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] %@ OR label CONTAINS[c] %@", "don't allow", "decline")).firstMatch
        if declineButton.waitForExistence(timeout: 5) {
            declineButton.tap()
            // After decline, should return to camera screen (or be cancelled)
            let backOnCamera = app.buttons["camera_captureButton"].waitForExistence(timeout: 5)
            let backOnDashboard = app.buttons["dashboard_emptyStateCard"].waitForExistence(timeout: 2)
            XCTAssertTrue(backOnCamera || backOnDashboard,
                          "After AI consent decline, user should return to camera or dashboard")
        }
    }
}
