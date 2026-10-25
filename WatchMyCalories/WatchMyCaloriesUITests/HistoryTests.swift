import XCTest

final class HistoryTests: WatchMyCaloriesUITestBase {

    func testHistoryEmptyState() {
        launchEmpty()

        app.tabBars.buttons["History"].tap()

        // The empty state card contains "No meals tracked yet"
        XCTAssertTrue(app.staticTexts["No meals tracked yet"].waitForExistence(timeout: 3))
    }

    func testHistoryShowsDayCardWithSeedData() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        let historyTitle = app.staticTexts["history_title"]
        XCTAssertTrue(historyTitle.waitForExistence(timeout: 3))

        // A day card with total calories should appear (300 + 450 = 750)
        XCTAssertTrue(app.staticTexts["750"].waitForExistence(timeout: 3))
    }

    func testExpandDayCardShowsEntries() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        // Tap on the day card to expand it
        let caloriesText = app.staticTexts["750"]
        XCTAssertTrue(caloriesText.waitForExistence(timeout: 3))
        caloriesText.tap()

        // After expanding, individual food entries should be visible
        XCTAssertTrue(app.staticTexts["Oatmeal with Berries"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Chicken Salad"].exists)
    }

    // MARK: - Multi-Day History

    func testHistoryShowsMultipleDays() {
        launchWithHistoryData()

        app.tabBars.buttons["History"].tap()

        let historyTitle = app.staticTexts["history_title"]
        XCTAssertTrue(historyTitle.waitForExistence(timeout: 5))

        // Today's entries: 300 + 450 = 750
        XCTAssertTrue(app.staticTexts["750"].waitForExistence(timeout: 3))

        // Scroll to see older day cards
        app.swipeUp()

        // Yesterday's entries: 600 + 150 = 750 (second "750" card)
        // 2-days-ago entry: 400 kcal
        // Verify at least one of the history entries is visible
        let has400 = app.staticTexts["400"].waitForExistence(timeout: 3)
        // Expand a day card to verify older entries exist
        if !has400 {
            app.swipeUp()
        }
        XCTAssertTrue(app.staticTexts["400"].waitForExistence(timeout: 3),
                       "Expected 2-days-ago entry with 400 kcal")
    }

    // MARK: - Delete Entry

    func testDeleteEntryFromHistory() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        // Expand the day card
        let caloriesText = app.staticTexts["750"]
        XCTAssertTrue(caloriesText.waitForExistence(timeout: 3))
        caloriesText.tap()

        // Wait for entries to appear
        let oatmeal = app.staticTexts["Oatmeal with Berries"]
        XCTAssertTrue(oatmeal.waitForExistence(timeout: 3))

        // Long press to trigger context menu
        oatmeal.press(forDuration: 1.5)

        // Look for Delete button in context menu
        let deleteButton = app.buttons["Delete"]
        XCTAssertTrue(deleteButton.waitForExistence(timeout: 3), "context menu should offer Delete")
        deleteButton.tap()

        // After deleting 300kcal oatmeal, total should be 450
        XCTAssertTrue(app.staticTexts["450"].waitForExistence(timeout: 3))
    }

    // MARK: - History Title Accessibility

    func testHistoryTitleAccessibilityID() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        let title = app.staticTexts["history_title"]
        XCTAssertTrue(title.waitForExistence(timeout: 3))
    }

    // MARK: - Meal Sections in Expanded Day Card

    func testExpandedDayCardShowsMealSections() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        // Expand the day card
        let caloriesText = app.staticTexts["750"]
        XCTAssertTrue(caloriesText.waitForExistence(timeout: 3))
        caloriesText.tap()

        // Seed data has Breakfast and Lunch entries
        // .textCase(.uppercase) renders visually but accessibility label uses the raw value
        XCTAssertTrue(app.staticTexts["BREAKFAST"].waitForExistence(timeout: 3) ||
                       app.staticTexts["Breakfast"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["LUNCH"].exists || app.staticTexts["Lunch"].exists)
    }

    // MARK: - History with Multiple Day Entries

    func testHistoryDayCardShowsYesterdayEntries() {
        launchWithHistoryData()

        app.tabBars.buttons["History"].tap()

        XCTAssertTrue(app.staticTexts["history_title"].waitForExistence(timeout: 5))

        // Scroll to see all day cards (today=750, yesterday=750, 2-days-ago=400)
        app.swipeUp()

        // Verify the 2-days-ago card with 400 kcal exists (unique value, confirms multi-day)
        XCTAssertTrue(app.staticTexts["400"].waitForExistence(timeout: 3),
                       "Expected 2-days-ago entry with 400 kcal")

        // Tap the 400 card to expand and verify its entry
        app.staticTexts["400"].tap()
        XCTAssertTrue(app.staticTexts["Turkey Sandwich"].waitForExistence(timeout: 3),
                       "Expected Turkey Sandwich in 2-days-ago card")
    }

    // MARK: - Day Card Macro Row

    func testDayCardShowsMacroRow() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        // Seed data has protein/carbs/fat, so macro row should appear
        let macroRow = app.descendants(matching: .any)["history_dayCard_macros"].firstMatch
        XCTAssertTrue(macroRow.waitForExistence(timeout: 3))
    }

    func testDayCardMacroRowShowsPercentages() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        XCTAssertTrue(app.staticTexts["750"].waitForExistence(timeout: 3))

        // Seed data: Oatmeal (P:10 C:50 F:6) + Salad (P:35 C:20 F:18)
        // Total: P=45g C=70g F=24g
        // CompactMacroRow shows "P: 45g", "C: 70g", "F: 24g" with percentages
        let macroRow = app.descendants(matching: .any)["history_dayCard_macros"].firstMatch
        XCTAssertTrue(macroRow.waitForExistence(timeout: 3))

        // Verify the macro row contains protein gram text
        let hasMacroText = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'P: 45g'")).firstMatch
        XCTAssertTrue(hasMacroText.waitForExistence(timeout: 3))
    }

    func testDayCardMacroRowHiddenWhenNoMacros() {
        launchEmpty()

        app.tabBars.buttons["History"].tap()

        // No entries at all, so no macro row
        let macroRow = app.descendants(matching: .any)["history_dayCard_macros"].firstMatch
        XCTAssertFalse(macroRow.waitForExistence(timeout: 2))
    }

    // MARK: - Meal Card Macro Display in Expanded Day Card

    func testExpandedMealCardShowsProportionalBarNotGramLabels() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        // Expand the day card
        let caloriesText = app.staticTexts["750"]
        XCTAssertTrue(caloriesText.waitForExistence(timeout: 3))
        caloriesText.tap()

        // Wait for entries to appear
        XCTAssertTrue(app.staticTexts["Oatmeal with Berries"].waitForExistence(timeout: 3))

        // The collapsed meal entry cards should NOT show gram labels (replaced by proportional bar).
        // "P: 10g" was the old InlineMacroRow text for Oatmeal's protein at the card level.
        XCTAssertFalse(app.staticTexts["P: 10g"].exists,
                        "Meal entry card should show proportional bar, not gram labels")
    }

    // MARK: - Day Card Accessibility ID

    func testHistoryDayCardAccessibilityID() {
        launchWithSeedData()

        app.tabBars.buttons["History"].tap()

        let dayCard = app.descendants(matching: .any)["history_dayCard"].firstMatch
        XCTAssertTrue(dayCard.waitForExistence(timeout: 3))
    }

    // MARK: - Empty State Accessibility ID

    func testHistoryEmptyStateAccessibilityID() {
        launchEmpty()

        app.tabBars.buttons["History"].tap()

        let emptyState = app.descendants(matching: .any)["history_emptyState"].firstMatch
        XCTAssertTrue(emptyState.waitForExistence(timeout: 3))
    }

    // MARK: - Parity audit (2026-05-30) — group card / image / context menu coverage

    func testHistoryMultiItemMealGroupAppears() {
        launchWithMultiItemMeal()
        app.tabBars.buttons["History"].tap()
        // History day card collapsed; tap to expand
        let dayCard = app.buttons["history_dayCard"]
        XCTAssertTrue(dayCard.waitForExistence(timeout: 5), "History should show a day card")
        dayCard.tap()
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label == %@", "Mock Bento Box")).element(boundBy: 0).waitForExistence(timeout: 5))
    }

    func testHistoryMultiItemMealGroupExpandsItems() {
        launchWithMultiItemMeal()
        app.tabBars.buttons["History"].tap()
        let dayCard = app.buttons["history_dayCard"]
        XCTAssertTrue(dayCard.waitForExistence(timeout: 5), "History should show a day card")
        dayCard.tap()
        let mealGroup = app.staticTexts.matching(NSPredicate(format: "label == %@", "Mock Bento Box")).element(boundBy: 0)
        XCTAssertTrue(mealGroup.waitForExistence(timeout: 5))
        mealGroup.tap()
        XCTAssertTrue(app.staticTexts["Brown Rice"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Teriyaki Chicken"].exists)
    }

    func testHistoryMultiItemMealGroupLongPressShowsContextMenu() {
        launchWithMultiItemMeal()
        app.tabBars.buttons["History"].tap()
        let dayCard = app.buttons["history_dayCard"]
        XCTAssertTrue(dayCard.waitForExistence(timeout: 5), "History should show a day card")
        dayCard.tap()
        let mealGroup = app.staticTexts.matching(NSPredicate(format: "label == %@", "Mock Bento Box")).element(boundBy: 0)
        XCTAssertTrue(mealGroup.waitForExistence(timeout: 5))
        mealGroup.press(forDuration: 1.2)
        let anyContextItem = app.buttons["View"].waitForExistence(timeout: 3)
            || app.buttons["Edit"].exists
            || app.buttons["Delete"].exists
        XCTAssertTrue(anyContextItem, "Long-press on a meal group in history should reveal a context menu")
    }

    func testHistorySubItemLongPressShowsContextMenu() {
        launchWithMultiItemMeal()
        app.tabBars.buttons["History"].tap()
        let dayCard = app.buttons["history_dayCard"]
        XCTAssertTrue(dayCard.waitForExistence(timeout: 5), "History should show a day card")
        dayCard.tap()
        let mealGroup = app.staticTexts.matching(NSPredicate(format: "label == %@", "Mock Bento Box")).element(boundBy: 0)
        XCTAssertTrue(mealGroup.waitForExistence(timeout: 5))
        mealGroup.tap()
        let subItem = app.staticTexts.matching(NSPredicate(format: "label == %@", "Brown Rice")).element(boundBy: 0)
        XCTAssertTrue(subItem.waitForExistence(timeout: 3))
        subItem.press(forDuration: 1.2)
        let anyContextItem = app.buttons["View"].waitForExistence(timeout: 3)
            || app.buttons["Edit"].exists
            || app.buttons["Delete"].exists
        XCTAssertTrue(anyContextItem)
    }

    func testHistoryThumbnailTapOpensFullScreenImage() throws {
        // Skipped: same XCUITest limitation as DashboardTests.testThumbnailTapOpensFullScreenImage —
        // the unlabeled thumbnail Image inside a PlainButtonStyle Button isn't reliably queryable
        // by accessibility identifier, so the tap can't be dispatched. Feature verified manually.
        try XCTSkipIf(true, "Thumbnail Image not reliably queryable by XCUITest; feature verified manually")
        launchWithImage()
        app.tabBars.buttons["History"].tap()
        let dayCard = app.buttons["history_dayCard"]
        XCTAssertTrue(dayCard.waitForExistence(timeout: 5), "History should show a day card")
        dayCard.tap()
        XCTAssertTrue(app.staticTexts["Mock Lunch with Photo"].waitForExistence(timeout: 5))
        let firstImage = app.images.firstMatch
        XCTAssertTrue(firstImage.waitForExistence(timeout: 3))
        firstImage.tap()
        // FullScreenImageView close button is `Image(systemName: "xmark.circle.fill")` —
        // exposed as a Button labeled "xmark.circle.fill" without an explicit a11y label.
        let closeButton = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] %@", "xmark")).firstMatch
        XCTAssertTrue(closeButton.waitForExistence(timeout: 3),
                      "Full-screen image cover should expose an xmark.circle.fill close button")
    }

    // MARK: - Trend card (chart + stat chips)

    /// Any element whose accessibility label starts with `prefix` (stat chips and the chart
    /// are single combined elements, e.g. "Avg / day, 633 kcal").
    private func element(labelPrefix prefix: String) -> XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "label BEGINSWITH %@", prefix))
            .firstMatch
    }

    private func assertLabel(_ prefix: String, equals expected: String, file: StaticString = #filePath, line: UInt = #line) {
        let el = element(labelPrefix: prefix)
        XCTAssertTrue(el.waitForExistence(timeout: 5), "Missing element labeled '\(prefix)…'", file: file, line: line)
        XCTAssertEqual(el.label, expected, file: file, line: line)
    }

    func testTrendStatChipsShowSeededHistory() {
        // Seed: today 750, yesterday 750, 2 days ago 400; target 2200.
        launchWithHistoryData()
        app.tabBars.buttons["History"].tap()

        assertLabel("Avg / day", equals: "Avg / day, 633 kcal")   // (750 + 750 + 400) / 3
        assertLabel("On target", equals: "On target, 3/3")
        assertLabel("Day streak", equals: "Day streak, 3")
    }

    func testTrendChartIsExposedToVoiceOver() {
        launchWithHistoryData()
        app.tabBars.buttons["History"].tap()

        let chart = element(labelPrefix: "Daily calories")
        XCTAssertTrue(chart.waitForExistence(timeout: 5))
        XCTAssertEqual(chart.label, "Daily calories, last 7 days")
        XCTAssertEqual(chart.value as? String, "3 of 7 days logged, none over goal")
    }

    func testTrendRangeSwitchTo30Days() {
        launchWithHistoryData()
        app.tabBars.buttons["History"].tap()

        let month = app.buttons["30D"]
        XCTAssertTrue(month.waitForExistence(timeout: 5))
        month.tap()
        XCTAssertTrue(month.isSelected, "30D segment should be selected after tapping")

        let chart = element(labelPrefix: "Daily calories")
        XCTAssertTrue(chart.waitForExistence(timeout: 3))
        XCTAssertEqual(chart.label, "Daily calories, last 30 days")
        XCTAssertEqual(chart.value as? String, "3 of 30 days logged, none over goal")
        // Same three logged days fall in both windows, so the stats are unchanged.
        assertLabel("Avg / day", equals: "Avg / day, 633 kcal")
        assertLabel("On target", equals: "On target, 3/3")
        assertLabel("Day streak", equals: "Day streak, 3")
    }

    func testTrendShowsOverGoalDay() {
        // Today 2000 kcal vs 1200 target (the trend uses the base target, not burned-adjusted).
        launchWithOverGoal()
        app.tabBars.buttons["History"].tap()

        assertLabel("On target", equals: "On target, 0/1")
        let chart = element(labelPrefix: "Daily calories")
        XCTAssertTrue(chart.waitForExistence(timeout: 3))
        XCTAssertEqual(chart.value as? String, "1 of 7 days logged, 1 over goal")
    }

    func testDeleteEntryUpdatesTrendStats() {
        // Today only: 300 + 450 = 750. Deleting the 300 kcal entry must update the chips
        // live while the chart is on screen.
        launchWithSeedData()
        app.tabBars.buttons["History"].tap()

        assertLabel("Avg / day", equals: "Avg / day, 750 kcal")

        let total = app.staticTexts["750"]
        XCTAssertTrue(total.waitForExistence(timeout: 3))
        total.tap()

        let oatmeal = app.staticTexts["Oatmeal with Berries"]
        XCTAssertTrue(oatmeal.waitForExistence(timeout: 3))
        oatmeal.press(forDuration: 1.5)

        let deleteButton = app.buttons["Delete"]
        XCTAssertTrue(deleteButton.waitForExistence(timeout: 3), "context menu should offer Delete")
        deleteButton.tap()

        XCTAssertTrue(app.staticTexts["450"].waitForExistence(timeout: 3))
        assertLabel("Avg / day", equals: "Avg / day, 450 kcal")
        assertLabel("Day streak", equals: "Day streak, 1")
    }
}
