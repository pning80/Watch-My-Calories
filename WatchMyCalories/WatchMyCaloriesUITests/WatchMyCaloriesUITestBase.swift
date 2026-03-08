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
}
