import XCTest

final class OnboardingTests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        // Do NOT pass --uitesting so onboarding is shown
        app.launchArguments.append("--reset-onboarding")
    }

    override func tearDownWithError() throws {
        app = nil
    }

    // MARK: - Welcome Step

    func testWelcomeScreenShowsPrivacyNoteAndNoUnitPicker() {
        app.launch()

        // Privacy note should be visible (Label renders as a combined element, use identifier)
        XCTAssertTrue(app.otherElements["onboarding_privacyNote"].waitForExistence(timeout: 5)
                      || app.staticTexts["onboarding_privacyNote"].waitForExistence(timeout: 2))

        // Unit picker should NOT be on the welcome screen
        XCTAssertFalse(app.buttons["US Customary"].exists)
        XCTAssertFalse(app.buttons["Metric"].exists)

        // Get Started button should exist
        let getStarted = app.buttons["onboarding_getStartedButton"]
        XCTAssertTrue(getStarted.exists)

        // Skip button should exist
        XCTAssertTrue(app.buttons["onboarding_skipButton"].exists)
    }

    // MARK: - Full Flow

    func testOnboardingFlowHasUnitPickerOnGoalsStep() {
        app.launch()

        // Step 0: Welcome - tap Get Started
        let getStarted = app.buttons["onboarding_getStartedButton"]
        XCTAssertTrue(getStarted.waitForExistence(timeout: 5))
        getStarted.tap()

        // Step 1: Profile - tap Next
        let nextButton = app.buttons["onboarding_nextButton"]
        XCTAssertTrue(nextButton.waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["About You"].exists)
        nextButton.tap()

        // Step 2: Goals - unit picker should be here
        XCTAssertTrue(app.staticTexts["Your Goals"].waitForExistence(timeout: 5))

        // Scroll down to see the unit picker section
        app.swipeUp()

        // Unit system picker should exist on this step
        XCTAssertTrue(app.buttons["US Customary"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["Metric"].exists)

        // Section header text should exist (SwiftUI may render as "UNIT SYSTEM" or "Unit System")
        XCTAssertTrue(
            app.staticTexts["UNIT SYSTEM"].exists || app.staticTexts["Unit System"].exists
        )
    }

    // MARK: - Keyboard Dismissal

    func testKeyboardDismissesWhenTappingOutsideTargetCalories() {
        app.launch()

        // Navigate to Goals step
        let getStarted = app.buttons["onboarding_getStartedButton"]
        XCTAssertTrue(getStarted.waitForExistence(timeout: 5))
        getStarted.tap()

        let nextButton = app.buttons["onboarding_nextButton"]
        XCTAssertTrue(nextButton.waitForExistence(timeout: 5))
        nextButton.tap()

        XCTAssertTrue(app.staticTexts["Your Goals"].waitForExistence(timeout: 5))

        // Scroll down and tap the target calories field to bring up the keyboard
        app.swipeUp()
        let caloriesField = app.textFields["onboarding_targetCalories"]
        XCTAssertTrue(caloriesField.waitForExistence(timeout: 3))
        caloriesField.tap()

        // Keyboard should be visible
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 3))

        // Tap on the title area to dismiss the keyboard
        app.swipeDown()
        let goalsTitle = app.staticTexts["Your Goals"]
        XCTAssertTrue(goalsTitle.waitForExistence(timeout: 3))
        goalsTitle.tap()

        // Keyboard should be dismissed
        XCTAssertFalse(app.keyboards.firstMatch.waitForExistence(timeout: 2))
    }

    func testKeyboardDismissesWhenNavigatingToNextStep() {
        app.launch()

        // Navigate to Goals step
        let getStarted = app.buttons["onboarding_getStartedButton"]
        XCTAssertTrue(getStarted.waitForExistence(timeout: 5))
        getStarted.tap()

        let nextButton = app.buttons["onboarding_nextButton"]
        XCTAssertTrue(nextButton.waitForExistence(timeout: 5))
        nextButton.tap()

        XCTAssertTrue(app.staticTexts["Your Goals"].waitForExistence(timeout: 5))

        // Scroll down and tap the target calories field to bring up the keyboard
        app.swipeUp()
        let caloriesField = app.textFields["onboarding_targetCalories"]
        XCTAssertTrue(caloriesField.waitForExistence(timeout: 3))
        caloriesField.tap()

        // Keyboard should be visible
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 3))

        // Tap Next to go to the Almost Done step
        nextButton.tap()

        // Should be on the permissions step
        XCTAssertTrue(app.staticTexts["Almost Done"].waitForExistence(timeout: 5))

        // Keyboard should be dismissed
        XCTAssertFalse(app.keyboards.firstMatch.waitForExistence(timeout: 2))
    }

    // MARK: - Calculate Recommended Goal

    func testCalculateRecommendedGoalPopulatesTargetCalories() {
        app.launch()

        // Step 0: Welcome → tap Get Started
        let getStarted = app.buttons["onboarding_getStartedButton"]
        XCTAssertTrue(getStarted.waitForExistence(timeout: 5))
        getStarted.tap()

        // Step 1: Profile → tap Next
        let nextButton = app.buttons["onboarding_nextButton"]
        XCTAssertTrue(nextButton.waitForExistence(timeout: 5))
        nextButton.tap()

        // Step 2: Goals
        XCTAssertTrue(app.staticTexts["Your Goals"].waitForExistence(timeout: 5))

        // Verify target calories field is initially empty
        let caloriesField = app.textFields["onboarding_targetCalories"]
        XCTAssertTrue(caloriesField.waitForExistence(timeout: 3))
        XCTAssertEqual(caloriesField.value as? String, "Not Set")

        // Tap Calculate Recommended Goal
        let calculateButton = app.buttons["onboarding_calculateGoal"]
        XCTAssertTrue(calculateButton.waitForExistence(timeout: 3))
        calculateButton.tap()

        // Wait for the target calories field value to change from placeholder
        let predicate = NSPredicate(format: "value != %@", "Not Set")
        expectation(for: predicate, evaluatedWith: caloriesField, handler: nil)
        waitForExpectations(timeout: 5, handler: nil)

        // Target calories field should now have a numeric value
        let fieldValue = caloriesField.value as? String ?? ""
        XCTAssertFalse(fieldValue.isEmpty, "Target calories should be populated after calculation")
        XCTAssertNotNil(Int(fieldValue), "Target calories should be a valid number, got: \(fieldValue)")
    }

    // MARK: - Skip Button

    func testSkipButtonCompletesOnboarding() {
        app.launch()

        let skipButton = app.buttons["onboarding_skipButton"]
        XCTAssertTrue(skipButton.waitForExistence(timeout: 5))
        skipButton.tap()

        // Should now see the main app (tab bar)
        XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout: 5))
    }
}
