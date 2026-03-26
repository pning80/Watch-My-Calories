import XCTest

final class EndToEndFlowTests: WatchMyCaloriesUITestBase {

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

        // Verify initial target is 2200 via goal stat row
        let goalElement = app.staticTexts["dashboard_goalValue"]
        XCTAssertTrue(goalElement.waitForExistence(timeout: 5))
        XCTAssertTrue(goalElement.label.contains("2200"))

        // Go to Settings via app menu
        let menuButton = app.buttons["appMenu_button"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 3))
        menuButton.tap()
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        // Calculate goal (~2531 after imperial conversion rounding)
        let calculateButton = app.buttons["settings_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        // Done (saves + dismisses since changes were made)
        app.buttons["settings_saveButton"].tap()
        // Handle unsaved changes alert
        let alert = app.alerts["Unsaved Changes"]
        if alert.waitForExistence(timeout: 2) {
            alert.buttons["Save"].tap()
        }

        // Should be back on dashboard with updated target
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 5))

        // Target should no longer be 2200
        let goalAfter = app.staticTexts["dashboard_goalValue"]
        XCTAssertTrue(goalAfter.waitForExistence(timeout: 3))
        XCTAssertFalse(goalAfter.label.contains("2200"), "Goal should have changed from 2200")
    }

    func testMultipleEntriesAccumulateCalories() {
        launchEmpty()

        addManualEntry(name: "Apple", calories: "95", quantity: "1 medium")
        addManualEntry(name: "Banana", calories: "105", quantity: "1 medium")

        // Hero card should show accumulated total of 200
        let consumed = app.otherElements["dashboard_consumedCalories"]
        XCTAssertTrue(consumed.waitForExistence(timeout: 3))
        XCTAssertTrue(consumed.label.contains("200"), "Consumed should show 200")
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

    // MARK: - Multiple Meal Types

    func testEntriesInMultipleMealSections() {
        launchEmpty()

        addManualEntry(name: "Toast", calories: "150", quantity: "2 slices", mealType: "Breakfast")
        addManualEntry(name: "Sandwich", calories: "400", quantity: "1 whole", mealType: "Lunch")
        addManualEntry(name: "Pasta", calories: "600", quantity: "1 plate", mealType: "Dinner")

        // All three meal sections should be visible
        XCTAssertTrue(app.staticTexts["Breakfast"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Lunch"].exists)

        // Scroll down to see Dinner
        app.swipeUp()
        XCTAssertTrue(app.staticTexts["Dinner"].waitForExistence(timeout: 3))
    }

    // MARK: - Calorie Accumulation Across Meals

    func testCaloriesAccumulateAcrossMealTypes() {
        launchEmpty()

        addManualEntry(name: "Eggs", calories: "200", quantity: "2 large", mealType: "Breakfast")
        addManualEntry(name: "Salad", calories: "300", quantity: "1 bowl", mealType: "Lunch")

        // Hero card should show total of 500
        let consumed = app.otherElements["dashboard_consumedCalories"]
        XCTAssertTrue(consumed.waitForExistence(timeout: 3))
        XCTAssertTrue(consumed.label.contains("500"), "Consumed should show 500")
    }

    // MARK: - Settings Goal Persists Across Tabs

    func testSettingsGoalPersistsOnReturn() {
        launchEmpty()

        // Go to Settings via app menu and set a target
        let menuButton = app.buttons["appMenu_button"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 3))
        menuButton.tap()
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))
        targetField.tap()
        targetField.typeText("1800")

        // Done — triggers unsaved changes alert
        app.buttons["settings_saveButton"].tap()
        let alert = app.alerts["Unsaved Changes"]
        if alert.waitForExistence(timeout: 2) {
            alert.buttons["Save"].tap()
        }

        // Should be on dashboard — verify target is 1800
        let addButton = app.buttons["dashboard_addButton"]
        XCTAssertTrue(addButton.waitForExistence(timeout: 3))
        let goalElement = app.staticTexts["dashboard_goalValue"]
        XCTAssertTrue(goalElement.waitForExistence(timeout: 3))
        XCTAssertTrue(goalElement.label.contains("1800"), "Goal should show 1800")
    }

    // MARK: - Manual Entry Visible in Both Dashboard and History

    func testEntryVisibleOnDashboardAndHistory() {
        launchEmpty()

        addManualEntry(name: "Granola Bar", calories: "180", quantity: "1 bar", mealType: "Snack")

        // Verify on dashboard
        XCTAssertTrue(app.staticTexts["Granola Bar"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Snack"].exists)

        // Switch to History and verify
        app.tabBars.buttons["History"].tap()
        XCTAssertTrue(app.staticTexts["180"].waitForExistence(timeout: 3))
    }

    // MARK: - Hero Card Updates After Delete

    func testHeroCardUpdatesAfterDelete() {
        launchWithSeedData()

        // Initial: 750 consumed
        let consumed = app.otherElements["dashboard_consumedCalories"]
        XCTAssertTrue(consumed.waitForExistence(timeout: 5))
        XCTAssertTrue(consumed.label.contains("750"))

        // Long press on an entry to delete
        let oatmeal = app.staticTexts["Oatmeal with Berries"]
        XCTAssertTrue(oatmeal.waitForExistence(timeout: 3))
        oatmeal.press(forDuration: 1.5)

        let deleteButton = app.buttons["Delete"]
        if deleteButton.waitForExistence(timeout: 3) {
            deleteButton.tap()

            // Hero card should now show 450 (750 - 300)
            let consumedAfter = app.otherElements["dashboard_consumedCalories"]
            XCTAssertTrue(consumedAfter.waitForExistence(timeout: 3))
            XCTAssertTrue(consumedAfter.label.contains("450"))
            XCTAssertFalse(consumedAfter.label.contains("750"))
        }
    }

    // MARK: - Remaining Calories Update

    func testRemainingCaloriesUpdateAfterEntry() {
        launchEmpty()

        // Go to Settings via app menu, set goal to 2000, save
        let menuButton = app.buttons["appMenu_button"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 3))
        menuButton.tap()
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))

        // Daily Goals section is below the fold — scroll to reveal
        app.swipeUp()

        let targetField = app.textFields["settings_targetCalories"]
        XCTAssertTrue(targetField.waitForExistence(timeout: 3))
        targetField.tap()
        targetField.typeText("2000")

        // Done — triggers unsaved changes alert
        app.buttons["settings_saveButton"].tap()
        let settingsAlert = app.alerts["Unsaved Changes"]
        if settingsAlert.waitForExistence(timeout: 2) {
            settingsAlert.buttons["Save"].tap()
        }
        XCTAssertTrue(app.buttons["dashboard_addButton"].waitForExistence(timeout: 3))

        // Remaining = effectiveTarget - consumed
        // effectiveTarget = 2000 (set goal) + 456 (simulator burned) = 2456
        // remaining = 2456 - 0 = 2456
        let remaining = app.staticTexts["dashboard_remainingValue"]
        XCTAssertTrue(remaining.waitForExistence(timeout: 3))
        XCTAssertTrue(remaining.label.contains("2456"), "Remaining should be 2456 with no entries (2000 + 456 burned)")

        // Add a 500 calorie entry
        addManualEntry(name: "Burger", calories: "500", quantity: "1 burger", mealType: "Lunch")

        // Remaining should now be 1956 (2456 - 500)
        let remainingAfter = app.staticTexts["dashboard_remainingValue"]
        XCTAssertTrue(remainingAfter.waitForExistence(timeout: 3))
        XCTAssertTrue(remainingAfter.label.contains("1956"), "Remaining should be 1956 (2456 - 500)")
    }
}
