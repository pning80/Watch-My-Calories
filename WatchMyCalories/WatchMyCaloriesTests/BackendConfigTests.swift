import XCTest
@testable import WatchMyCalories

final class BackendConfigTests: XCTestCase {

    func testAPIKeyMatchesInfoPlistSource() {
        // The dev legacy key is injected from Backend/.env.dev (the single source
        // of truth) via the xcconfig → Info.plist (AppBackendApiKey). It may be
        // empty when not configured (e.g. CI / fresh clone); the contract is only
        // that apiKey mirrors that injected value in DEBUG.
        #if DEBUG
        let expected = Bundle.main.infoDictionary?["AppBackendApiKey"] as? String ?? ""
        XCTAssertEqual(BackendConfig.apiKey, expected,
                       "apiKey should be sourced from Info.plist AppBackendApiKey")
        #else
        XCTAssertTrue(BackendConfig.apiKey.isEmpty,
                      "RELEASE builds carry no legacy key — production uses App Attest")
        #endif
    }

    func testBaseURLIsValidHTTPS() {
        let url = BackendConfig.baseURL
        XCTAssertTrue(url.hasPrefix("https://"), "Backend URL should use HTTPS")
        XCTAssertFalse(url.hasSuffix("/"), "Backend URL should not have a trailing slash")
        XCTAssertNotNil(URL(string: url), "Backend URL should be a valid URL")
    }
}
