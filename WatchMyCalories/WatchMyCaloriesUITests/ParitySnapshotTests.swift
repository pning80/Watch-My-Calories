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

    /// Settings in METRIC mode (Unit System → Metric): metric Height/Weight wheels
    /// + cm/kg units, vs the default US-customary ft-in/lbs. Switches the unit
    /// picker (a .menu Picker, settings_unitPicker) then snaps top + profile.
    func testSnapSettingsMetric() {
        launchWithSeedData()
        app.buttons["appMenu_button"].tap()
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        app.buttons["settings_unitPicker"].tap()
        let metric = app.buttons["Metric"]
        if metric.waitForExistence(timeout: 3) { metric.tap() }
        snap("04c-settings-metric-top")
        app.swipeUp()
        snap("04d-settings-metric-profile")
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
        // Stored Menus presents over the dashboard; wait for its Done button so
        // the snap doesn't catch the dashboard mid-transition.
        _ = app.buttons["Done"].waitForExistence(timeout: 5)
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

    /// Onboarding Welcome + Privacy steps. Onboarding is gated out under
    /// --uitesting (WatchMyCaloriesApp.swift:92 short-circuits to ContentView),
    /// so launch a FRESH instance with only --reset-onboarding to render it.
    func testSnapOnboarding() {
        let ob = XCUIApplication()
        ob.launchArguments = ["--reset-onboarding"]
        ob.launch()
        XCTAssertTrue(ob.buttons["onboarding_getStartedButton"].waitForExistence(timeout: 10))
        snap("09-onboarding-welcome")
        ob.buttons["onboarding_getStartedButton"].tap()
        XCTAssertTrue(ob.buttons["onboarding_connectHealth"].waitForExistence(timeout: 5))
        snap("09b-onboarding-privacy")
    }

    /// Drives Log Food → Scan Food → capture → Use to kick off estimation.
    private func goToEstimation() {
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
    }

    /// Analysis error state (--mock-estimation-error).
    func testSnapAnalysisError() {
        app.launchArguments.append("--ai-consent-accepted")
        app.launchArguments.append("--mock-estimation-error")
        app.launch()
        goToEstimation()
        // Error view settles on a Try Again button (soft wait, then snap).
        _ = app.buttons["review_tryAgainButton"].waitForExistence(timeout: 15)
        snap("10-analysis-error")
    }

    /// Analysis no-food state (--mock-estimation-no-food). Like success, iOS
    /// interposes the "View Results" ad gate before the no-food message.
    func testSnapAnalysisNoFood() {
        app.launchArguments.append("--ai-consent-accepted")
        app.launchArguments.append("--mock-estimation-no-food")
        app.launch()
        goToEstimation()
        let viewResults = app.buttons["View Results"]
        if viewResults.waitForExistence(timeout: 15) { viewResults.tap() }
        _ = app.staticTexts["No Food Detected"].waitForExistence(timeout: 12)
        snap("11-analysis-nofood")
    }

    /// History with the first day card expanded (per-entry meal-grouped rows).
    func testSnapHistoryExpanded() {
        launchWithHistoryData()
        app.tabBars.buttons["History"].tap()
        let card = app.descendants(matching: .any)["history_dayCard"].firstMatch
        XCTAssertTrue(card.waitForExistence(timeout: 5))
        card.tap()
        snap("12-history-expanded")
    }

    /// Onboarding Goal step (step 3) — Welcome → Get Started → Privacy → Next → Goal.
    func testSnapOnboardingGoal() {
        let ob = XCUIApplication()
        ob.launchArguments = ["--reset-onboarding"]
        ob.launch()
        XCTAssertTrue(ob.buttons["onboarding_getStartedButton"].waitForExistence(timeout: 10))
        ob.buttons["onboarding_getStartedButton"].tap()
        let next = ob.buttons["onboarding_nextButton"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        next.tap()
        XCTAssertTrue(ob.buttons["onboarding_finishButton"].waitForExistence(timeout: 5))
        snap("09c-onboarding-goal")
    }
    /// Manual food entry form (Log Food → Log Manually).
    func testSnapManualEntry() {
        launchEmpty()
        app.tabBars.buttons["Log Food"].tap()
        let logManually = app.staticTexts["Log Manually"]
        XCTAssertTrue(logManually.waitForExistence(timeout: 5))
        logManually.tap()
        XCTAssertTrue(app.textFields["manualEntry_foodName"].waitForExistence(timeout: 5))
        snap("13-manual-entry")
    }
    /// Menu Scan detail (ScannedMenus → tap a menu → MenuScanDetailView).
    func testSnapMenuScanDetail() {
        launchWithMenuScans()
        app.tabBars.buttons["Scan Menu"].tap()
        let stored = app.descendants(matching: .any)["scanMenuSheet_storedMenus"].firstMatch
        XCTAssertTrue(stored.waitForExistence(timeout: 5))
        stored.tap()
        let row = app.staticTexts["Mock Italian Place"]
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        row.tap()
        XCTAssertTrue(app.staticTexts["Margherita Pizza"].waitForExistence(timeout: 5))
        snap("14-menu-detail")
    }
    /// Camera post-capture review (Log Food → Scan Food → capture → meal picker + Retake/Use).
    func testSnapCameraReview() {
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
        // Post-capture review: photo + meal picker + Retake/Use Photo.
        XCTAssertTrue(app.buttons["camera_usePhotoButton"].waitForExistence(timeout: 5))
        snap("15-camera-review")
    }
    /// Multi-item meal GROUP edit (analysis-success saves a shared-imageID group
    /// → Dashboard grouped card → long-press → Edit group → EditMealGroupView).
    /// The mock estimation saves "Mock Chicken and Rice" (Mock Chicken + Mock Rice)
    /// with one shared imageID (EstimationReviewView.swift:396-413), so the
    /// dashboard renders a grouped card titled by that mealName.
    func testSnapEditMealGroup() {
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
        let viewResults = app.buttons["View Results"]
        if viewResults.waitForExistence(timeout: 15) { viewResults.tap() }
        let done = app.buttons["Done"]
        XCTAssertTrue(done.waitForExistence(timeout: 12))
        done.tap()
        // Dashboard now shows the grouped "Mock Chicken and Rice" card.
        let group = app.staticTexts["Mock Chicken and Rice"]
        XCTAssertTrue(group.waitForExistence(timeout: 5))
        group.press(forDuration: 1.1)
        let edit = app.buttons["Edit"]
        if edit.waitForExistence(timeout: 3) { edit.tap() }
        // EditMealGroupView exposes the editable "Meal Name" field.
        _ = app.staticTexts["Meal Name"].waitForExistence(timeout: 5)
        snap("19-edit-meal-group")
    }
    /// Empty Dashboard state (no entries) — EmptyStateCard + "or log manually".
    func testSnapDashboardEmpty() {
        launchEmpty()
        XCTAssertTrue(app.buttons["dashboard_emptyStateCard"].waitForExistence(timeout: 5))
        snap("18-dashboard-empty")
    }
    /// Single-entry edit sheet (Dashboard → long-press a food entry → Edit →
    /// EditFoodEntryView). The context menu is the same affordance Android uses
    /// (combinedClickable long-press → Edit). Seeded "today" entries are
    /// "Oatmeal with Berries" + "Chicken Salad" (WatchMyCaloriesApp seedData).
    func testSnapEditFoodEntry() {
        launchWithSeedData()
        XCTAssertTrue(app.staticTexts["Watch My Calories"].waitForExistence(timeout: 5))
        let entry = app.staticTexts["Oatmeal with Berries"]
        XCTAssertTrue(entry.waitForExistence(timeout: 5))
        entry.press(forDuration: 1.1)
        let edit = app.buttons["Edit"]
        if edit.waitForExistence(timeout: 3) { edit.tap() }
        // EditFoodEntryView settles on its "Item Name" field.
        _ = app.staticTexts["Item Name"].waitForExistence(timeout: 5)
        snap("17-edit-food-entry")
    }
    // NOTE: a live Menu Analysis snapshot (Scan Menu → menu camera → Analyze Menu →
    // MenuAnalysisView) is NOT viable on the simulator. The menu camera's
    // simulator stub (MenuCameraView.swift:219, simulatorPhotos = ["MenuPhoto1",
    // "MenuPhoto2"]) fails — takePhoto() raises a "Camera Error" alert instead of
    // returning a stub frame (the food camera's stub works, the menu one doesn't).
    // So the live result can't be paired-pixel-diffed from the sim; compare the
    // Android render against MenuAnalysisView.swift as the source spec, or capture
    // iOS on a physical device. The saved-scan equivalent is testSnapMenuScanDetail.
}
