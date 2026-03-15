import XCTest
@testable import WatchMyCalories

final class BackendConfigTests: XCTestCase {

    func testAPIKeyIsNotEmpty() {
        XCTAssertFalse(BackendConfig.apiKey.isEmpty, "XOR-deobfuscated API key should not be empty")
    }

    func testAPIKeyLengthMatchesObfuscatedArrayLength() {
        // Both obfuscatedKey and keyMask are 64 bytes → 64-char UTF-8 key
        XCTAssertEqual(BackendConfig.apiKey.count, 64)
    }

    func testBaseURLIsValidHTTPS() {
        let url = BackendConfig.baseURL
        XCTAssertTrue(url.hasPrefix("https://"), "Backend URL should use HTTPS")
        XCTAssertFalse(url.hasSuffix("/"), "Backend URL should not have a trailing slash")
        XCTAssertNotNil(URL(string: url), "Backend URL should be a valid URL")
    }
}
