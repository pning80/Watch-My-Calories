import XCTest

final class EndToEndFlowTests: CalorieWatcherUITestBase {

    // MARK: - Helpers

    private func addManualEntry(name: String, calories: String, quantity: String, mealType: String? = nil) {
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
        addButton.tap()

        let nameField = app.textFields["manualEntry_foodName"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        nameField.tap()
        nameField.typeText(name)

        let caloriesField = app.textFields["manualEntry_calories"]
        caloriesField.tap()
        caloriesField.typeText(calories)

        let quantityField = app.textFields["manualEntry_quantity"]
        quantityField.tap()
        quantityField.typeText(quantity)

        if let mealType {
            app.buttons[mealType].tap()
        }

        let saveButton = app.buttons["manualEntry_saveButton"]
        XCTAssertTrue(saveButton.isEnabled)
        saveButton.tap()

        // Wait for sheet to dismiss
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
    }

    // MARK: - Tests

    func testManualEntryAppearsInCorrectMealSection() {
        launchEmpty()

        addManualEntry(name: "Trail Mix", calories: "200", quantity: "1 bag", mealType: "Snack")

        // Verify "Snack" section appears on dashboard
        XCTAssertTrue(app.staticTexts["Snack"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Trail Mix"].exists)
    }

    func testManualEntryAppearsInHistory() {
        launchEmpty()

        addManualEntry(name: "Banana", calories: "105", quantity: "1 medium")

        // Switch to History tab
        app.tabBars.buttons["History"].tap()

        // Verify calorie total shown
        XCTAssertTrue(app.staticTexts["105"].waitForExistence(timeout: 3))
    }

    func testSettingsSaveUpdatesHeroCardTarget() {
        launchWithSeedData()

        // Verify initial target is 2200
        XCTAssertTrue(app.staticTexts["2200"].waitForExistence(timeout: 5))

        // Go to Settings
        app.tabBars.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))

        // Calculate goal (~2531 after imperial conversion rounding)
        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        // Save
        let saveButton = app.buttons["settings_saveButton"]
        saveButton.tap()

        // Should be back on dashboard with updated target
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 5))

        // Target should no longer be 2200
        XCTAssertFalse(app.staticTexts["2200"].exists)
    }

    func testMultipleEntriesAccumulateCalories() {
        launchEmpty()

        addManualEntry(name: "Apple", calories: "95", quantity: "1 medium")
        addManualEntry(name: "Banana", calories: "105", quantity: "1 medium")

        // Hero card should show accumulated total of 200
        XCTAssertTrue(app.staticTexts["200"].waitForExistence(timeout: 3))
    }

    func testEmptyStateDisappearsAfterAddingEntry() {
        launchEmpty()

        // Verify empty state is shown
        let emptyCard = app.buttons["dashboard_emptyStateCard"]
        XCTAssertTrue(emptyCard.waitForExistence(timeout: 3))

        addManualEntry(name: "Yogurt", calories: "150", quantity: "1 cup")

        // Empty state should be gone
        XCTAssertFalse(app.buttons["dashboard_emptyStateCard"].waitForExistence(timeout: 2))

        // Entry should be visible
        XCTAssertTrue(app.staticTexts["Yogurt"].waitForExistence(timeout: 3))
    }
}
