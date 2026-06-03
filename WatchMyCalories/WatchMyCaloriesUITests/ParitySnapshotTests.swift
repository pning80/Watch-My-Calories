import XCTest

/// Parity snapshot harness — the iOS half of the iOS↔Android paired pixel diff.
///
/// XCUITest is itself the simulator's tap-injection path (it synthesizes touch
/// events inside the sim via XCTest's automation framework), so these tests
/// navigate each screen on seeded data the same way a person would and capture
/// a full-screen screenshot as a `keepAlways` attachment.
///
/// Run (per appearance) and extract the PNGs:
///   xcrun simctl ui booted appearance dark      # or light
///   xcodebuild test -project WatchMyCalories.xcodeproj -scheme WatchMyCalories \
///     -destination "id=<SIM_UDID>" \
///     -only-testing:WatchMyCaloriesUITests/ParitySnapshotTests \
///     -resultBundlePath /tmp/parity-loop/ios.xcresult
///   xcrun xcresulttool export attachments \
///     --path /tmp/parity-loop/ios.xcresult --output-path /tmp/parity-loop/ios
///
/// Test-only; touches no app code (the app's theme follows the sim appearance,
/// which the launch fixtures inherit — no `--force-theme` arg is added).
final class ParitySnapshotTests: WatchMyCaloriesUITestBase {

    /// Capture the whole screen as a named, always-kept attachment.
    private func snap(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// Dashboard — seeded profile + 2 food entries.
    func testSnapDashboard() {
        launchWithSeedData()
        XCTAssertTrue(app.staticTexts["Watch My Calories"].waitForExistence(timeout: 5))
        snap("01-dashboard-seeded")
    }

    /// Dashboard — multi-item meal group card.
    func testSnapDashboardMultiItem() {
        launchWithMultiItemMeal()
        XCTAssertTrue(app.staticTexts["Watch My Calories"].waitForExistence(timeout: 5))
        snap("02-dashboard-multi-item")
    }

    /// History — multi-day seed.
    func testSnapHistory() {
        launchWithHistoryData()
        app.tabBars.buttons["History"].tap()
        XCTAssertTrue(app.staticTexts["history_title"].waitForExistence(timeout: 5))
        snap("03-history")
    }

    /// Settings — top of the form and scrolled down to the profile section
    /// (the screen that was source-compare-only before tap injection).
    func testSnapSettings() {
        launchWithSeedData()
        app.buttons["appMenu_button"].tap()
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        snap("04-settings-top")
        app.swipeUp()
        snap("04b-settings-profile")
    }

    /// Scan Menu sheet — the 3-option modal.
    func testSnapScanMenuSheet() {
        launchEmpty()
        app.tabBars.buttons["Scan Menu"].tap()
        XCTAssertTrue(app.staticTexts["Scan Menu"].waitForExistence(timeout: 5))
        snap("05-scanmenu-sheet")
    }

    /// Stored/Scanned Menus list — menu-scan seed.
    func testSnapScannedMenus() {
        launchWithMenuScans()
        app.tabBars.buttons["Scan Menu"].tap()
        let stored = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(stored.waitForExistence(timeout: 5))
        stored.tap()
        snap("06-scanned-menus")
    }

    /// About screen.
    func testSnapAbout() {
        launchEmpty()
        app.buttons["appMenu_button"].tap()
        app.buttons["About"].tap()
        XCTAssertTrue(app.navigationBars["About"].waitForExistence(timeout: 5))
        snap("07-about")
    }

    /// Analysis / EstimationReview success summary (auto-saved read-only state).
    /// Drives the capture→Use flow like EstimationReviewTests; the default
    /// MockEstimationService (no --mock-estimation-* arg) returns a success result.
    func testSnapAnalysisSuccess() {
        launchWithAIConsentAccepted()
        app.tabBars.buttons["Log Food"].tap()
        let scanFood = app.staticTexts["Scan Food"]
        XCTAssertTrue(scanFood.waitForExistence(timeout: 5))
        scanFood.tap()
        let capture = app.buttons["camera_captureButton"]
        XCTAssertTrue(capture.waitForExistence(timeout: 5))
        capture.tap()
        let disclaimerContinue = app.buttons["disclaimer_continueButton"]
        if disclaimerContinue.waitForExistence(timeout: 3) { disclaimerContinue.tap() }
        let use = app.buttons["camera_usePhotoButton"]
        XCTAssertTrue(use.waitForExistence(timeout: 5))
        use.tap()
        // After estimation an "Analysis complete! / View Results" gate appears
        // (the ad surface, suppressed under --uitesting but the CTA remains).
        // Tap it by label to reach the read-only success summary.
        let viewResults = app.buttons["View Results"]
        if viewResults.waitForExistence(timeout: 15) { viewResults.tap() }
        _ = app.staticTexts["Logged Successfully!"].waitForExistence(timeout: 12)
        snap("08-analysis-success")
    }
}
