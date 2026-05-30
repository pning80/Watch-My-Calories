import XCTest

class WatchMyCaloriesUITestBase: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments.append("--uitesting")
    }

    override func tearDownWithError() throws {
        app = nil
    }

    /// Launch with seed data (UserProfile + 2 FoodEntries).
    func launchWithSeedData() {
        app.launchArguments.append("--seed-data")
        app.launch()
    }

    /// Launch with seed data + multi-day history entries.
    func launchWithHistoryData() {
        app.launchArguments.append("--seed-history")
        app.launch()
    }

    /// Launch with an empty database.
    func launchEmpty() {
        app.launch()
    }

    // MARK: - Parity-audit launch fixtures (added 2026-05-30)

    /// Launch with seeded menu scans (2 entries) for ScannedMenusView coverage.
    func launchWithMenuScans() {
        app.launchArguments.append("--seed-menu-scans")
        app.launch()
    }

    /// Launch with a multi-item meal group (3 entries sharing `mealName`).
    func launchWithMultiItemMeal() {
        app.launchArguments.append("--seed-multi-item-meal")
        app.launch()
    }

    /// Launch with one FoodEntry that has an attached image on disk.
    func launchWithImage() {
        app.launchArguments.append("--seed-with-image")
        app.launch()
    }

    /// Launch with MockEstimationService set to throw on every call.
    func launchWithEstimationError() {
        app.launchArguments.append("--mock-estimation-error")
        app.launch()
    }

    /// Launch with MockEstimationService returning an empty result (no food detected).
    func launchWithEstimationNoFood() {
        app.launchArguments.append("--mock-estimation-no-food")
        app.launch()
    }

    /// Launch with AI consent pre-accepted so the AIConsentSheet does not fire.
    func launchWithAIConsentAccepted() {
        app.launchArguments.append("--ai-consent-accepted")
        app.launch()
    }
}
