import XCTest

/// Log Food → Choose from Library. The simulator's photo library must contain
/// at least one image (`xcrun simctl addmedia <udid> <photo>`).
final class PhotoLibraryFlowTests: WatchMyCaloriesUITestBase {

    /// Opens Choose from Library and picks the first photo in the system picker.
    private func pickFirstLibraryPhoto() {
        app.tabBars.buttons["Log Food"].tap()
        let chooseFromLibrary = app.staticTexts["Choose from Library"]
        XCTAssertTrue(chooseFromLibrary.waitForExistence(timeout: 5))
        chooseFromLibrary.tap()

        // The system picker labels each cell "Photo, <date>".
        let photo = app.images.matching(NSPredicate(format: "label BEGINSWITH 'Photo'")).firstMatch
        XCTAssertTrue(photo.waitForExistence(timeout: 10), "Photo picker should list library photos")
        photo.tap()

        let disclaimerContinue = app.buttons["disclaimer_continueButton"]
        if disclaimerContinue.waitForExistence(timeout: 3) { disclaimerContinue.tap() }

        XCTAssertTrue(app.buttons["photoLibrary_usePhotoButton"].waitForExistence(timeout: 10))
    }

    private func assertBackOnDashboard() {
        XCTAssertTrue(app.staticTexts["Watch My Calories"].waitForExistence(timeout: 5), "Should return to the Dashboard")
        XCTAssertFalse(app.buttons["photoLibrary_usePhotoButton"].exists)
    }

    func testCancelFromPhotoReview() {
        launchWithAIConsentAccepted()
        pickFirstLibraryPhoto()

        let cancel = app.navigationBars.buttons["Cancel"]
        XCTAssertTrue(cancel.exists, "Photo review should offer Cancel")
        cancel.tap()
        assertBackOnDashboard()
    }

    /// After a failed analysis, Cancel returns to the photo review, which must
    /// still have a way out (regression: users were stuck there).
    func testCancelAfterFailedAnalysisReturnsToDashboard() {
        app.launchArguments.append("--mock-estimation-error")
        launchWithAIConsentAccepted()
        pickFirstLibraryPhoto()
        app.buttons["photoLibrary_usePhotoButton"].tap()

        let reviewCancel = app.buttons["review_cancelButton"]
        XCTAssertTrue(reviewCancel.waitForExistence(timeout: 30), "Mock error should show the error view")
        reviewCancel.tap()

        XCTAssertTrue(app.buttons["photoLibrary_usePhotoButton"].waitForExistence(timeout: 5))
        let cancel = app.navigationBars.buttons["Cancel"]
        XCTAssertTrue(cancel.exists, "Photo review should offer Cancel after a failed analysis")
        cancel.tap()
        assertBackOnDashboard()
    }
}
