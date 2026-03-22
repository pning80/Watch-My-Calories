import XCTest
import SwiftUI
@testable import WatchMyCalories

final class SettingsEnumTests: XCTestCase {

    // MARK: - AppTheme.colorScheme

    func testSystemThemeReturnsNil() {
        XCTAssertNil(AppTheme.system.colorScheme)
    }

    func testLightThemeReturnsLight() {
        XCTAssertEqual(AppTheme.light.colorScheme, .light)
    }

    func testDarkThemeReturnsDark() {
        XCTAssertEqual(AppTheme.dark.colorScheme, .dark)
    }

    // MARK: - GeminiError.errorDescription

    func testGeminiErrorDescriptions() {
        XCTAssertNotNil(GeminiError.missingBackendConfig.errorDescription)
        XCTAssertNotNil(GeminiError.invalidResponse.errorDescription)

        let apiError = GeminiError.apiError("test message")
        XCTAssertTrue(apiError.errorDescription?.contains("test message") ?? false)

        let networkError = GeminiError.networkError(URLError(.notConnectedToInternet))
        XCTAssertNotNil(networkError.errorDescription)
    }

    func testGeminiRateLimitedWithRetryAfter() {
        let error = GeminiError.rateLimited(retryAfter: 60)
        XCTAssertTrue(error.errorDescription?.contains("60 seconds") ?? false)
        XCTAssertTrue(error.errorDescription?.starts(with: "Too many requests") ?? false)
    }

    func testGeminiRateLimitedWithoutRetryAfter() {
        let error = GeminiError.rateLimited(retryAfter: nil)
        XCTAssertTrue(error.errorDescription?.contains("wait a moment") ?? false)
        XCTAssertTrue(error.errorDescription?.starts(with: "Too many requests") ?? false)
    }

    // MARK: - AppAttestError.errorDescription

    func testAppAttestErrorDescriptions() {
        XCTAssertNotNil(AppAttestError.invalidURL.errorDescription)
        XCTAssertNotNil(AppAttestError.challengeFetchFailed.errorDescription)
        XCTAssertNotNil(AppAttestError.invalidChallengeResponse.errorDescription)

        let verifyError = AppAttestError.attestationVerifyFailed("detail info")
        XCTAssertTrue(verifyError.errorDescription?.contains("detail info") ?? false)
    }
}
