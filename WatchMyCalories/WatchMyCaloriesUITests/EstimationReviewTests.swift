import XCTest

final class EstimationReviewTests: WatchMyCaloriesUITestBase {

    /// Navigate to camera, capture, and tap Use to start a (mock, success-mode) estimation.
    /// AI consent is pre-accepted — without it the consent sheet blocks the estimation; the
    /// consent flow itself is covered by the `testAIConsent…` tests.
    private func startEstimation() {
        launchWithAIConsentAccepted()
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
        // SwiftUI state containers surface as different element types (and their identifier
        // is inherited by descendant Texts), so match any type.
        app.descendants(matching: .any)[identifier].firstMatch.waitForExistence(timeout: timeout)
    }

    /// Success mode: loading → "View Results" → success screen. Fails if any step is missing.
    private func advanceToSuccessUI() {
        let viewResults = app.buttons["ads_viewResultsButton"]
        XCTAssertTrue(viewResults.waitForExistence(timeout: 30),
                      "View Results should appear once the mock estimation completes")
        viewResults.tap()
        XCTAssertTrue(elementExists("review_success"), "Success screen should appear after View Results")
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
        advanceToSuccessUI()
        XCTAssertTrue(app.buttons["review_doneButton"].waitForExistence(timeout: 3),
                      "Done button should appear on success")
    }

    func testDoneButtonReturnsToDashboard() {
        startEstimation()
        advanceToSuccessUI()

        let doneButton = app.buttons["review_doneButton"]
        XCTAssertTrue(doneButton.waitForExistence(timeout: 5))
        doneButton.tap()

        // Back on Dashboard, with the two mock items (250 + 200) logged
        let heroCard = app.otherElements["dashboard_heroCard"]
        XCTAssertTrue(heroCard.waitForExistence(timeout: 5))
        let consumed = app.otherElements["dashboard_consumedCalories"]
        XCTAssertTrue(consumed.waitForExistence(timeout: 3))
        XCTAssertTrue(consumed.label.hasPrefix("450 of"), "Saved estimate should be logged; got: \(consumed.label)")
    }

    func testCancelButtonOnError() {
        startEstimationWithLaunchArgs(["--mock-estimation-error"])
        XCTAssertTrue(waitForErrorView(), "Mock-error mode should render the review_error view")

        let cancelButton = app.buttons["review_cancelButton"]
        XCTAssertTrue(cancelButton.waitForExistence(timeout: 3))
        cancelButton.tap()

        // Should return to camera
        let captureButton = app.buttons["camera_captureButton"]
        XCTAssertTrue(captureButton.waitForExistence(timeout: 5))
    }

    // MARK: - Success Screen Content

    func testSuccessScreenShowsLoggedMessage() {
        startEstimation()
        advanceToSuccessUI()
        XCTAssertTrue(
            app.staticTexts["Logged Successfully!"].exists,
            "Success screen should show 'Logged Successfully!' message"
        )
    }

    func testSuccessScreenShowsTotalAdded() {
        startEstimation()
        advanceToSuccessUI()
        XCTAssertTrue(
            app.staticTexts["Total Added"].exists,
            "Success screen should show 'Total Added' label"
        )
    }

    func testDoneButtonVisibleWithoutScrolling() {
        startEstimation()
        advanceToSuccessUI()

        let doneButton = app.buttons["review_doneButton"]
        XCTAssertTrue(doneButton.waitForExistence(timeout: 5))

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

    /// Wait for the error view (`review_error`) to appear.
    private func waitForErrorView(timeout: TimeInterval = 30) -> Bool {
        return elementExists("review_error", timeout: timeout)
    }

    func testErrorStateShowsTryAgainButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-error"])
        XCTAssertTrue(waitForErrorView(),
                      "Mock-error mode should render the review_error view")
        XCTAssertTrue(app.buttons["review_tryAgainButton"].exists,
                      "Try Again button must appear inside the error view")
        XCTAssertTrue(app.staticTexts["Analysis Failed"].exists,
                      "Error state should show 'Analysis Failed' headline")
    }

    func testErrorStateShowsCancelButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-error"])
        XCTAssertTrue(waitForErrorView())
        XCTAssertTrue(app.buttons["review_cancelButton"].exists,
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
        app.buttons["review_tryAgainButton"].tap()
        // After Try Again, the loading state re-fires and we end up back in error.
        XCTAssertTrue(waitForErrorView(timeout: 15))
    }

    /// In no-food mode the mock returns success-with-empty-items. The flow is:
    /// loading → "View Results" button appears → user taps it → no-food UI with Retake Photo/Cancel.
    private func advanceToNoFoodUI() {
        let viewResults = app.buttons["ads_viewResultsButton"]
        XCTAssertTrue(viewResults.waitForExistence(timeout: 30),
                      "View Results button must appear after empty-items estimation")
        viewResults.tap()
        // Now the no-food UI should render with its "No Food Detected" headline.
        XCTAssertTrue(app.staticTexts["No Food Detected"].waitForExistence(timeout: 5))
    }

    func testNoFoodStateShowsRetakeButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-no-food"])
        advanceToNoFoodUI()
        XCTAssertTrue(app.buttons["review_tryAgainButton"].exists,
                      "No-food UI should expose a Retake Photo button")
        XCTAssertEqual(app.buttons["review_tryAgainButton"].label, "Retake Photo")
    }

    func testNoFoodStateShowsCancelButton() {
        startEstimationWithLaunchArgs(["--mock-estimation-no-food"])
        advanceToNoFoodUI()
        XCTAssertTrue(app.buttons["review_cancelButton"].exists,
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
        XCTAssertTrue(declineButton.waitForExistence(timeout: 5), "AI consent sheet should offer Don't Allow")
        declineButton.tap()
        // After decline, should return to camera screen (or be cancelled)
        let backOnCamera = app.buttons["camera_captureButton"].waitForExistence(timeout: 5)
        let backOnDashboard = app.buttons["dashboard_emptyStateCard"].waitForExistence(timeout: 2)
        XCTAssertTrue(backOnCamera || backOnDashboard,
                      "After AI consent decline, user should return to camera or dashboard")
    }
}
