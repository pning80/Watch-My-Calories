import XCTest
@testable import WatchMyCalories

final class SettingsStoreTests: XCTestCase {

    private let defaults = UserDefaults.standard
    private let store = SettingsStore.shared

    // Keys must match SettingsStore's private constants
    private let themeKey = "appTheme"
    private let unitSystemKey = "unitSystem"
    private let aiConsentKey = "aiConsentStatus"
    private let onboardingKey = "hasCompletedOnboarding"

    private var originalTheme: String?
    private var originalUnit: String?
    private var originalConsent: String?
    private var originalOnboarding: Bool = false

    override func setUpWithError() throws {
        // Snapshot original values to restore after test
        originalTheme = defaults.string(forKey: themeKey)
        originalUnit = defaults.string(forKey: unitSystemKey)
        originalConsent = defaults.string(forKey: aiConsentKey)
        originalOnboarding = defaults.bool(forKey: onboardingKey)
    }

    override func tearDownWithError() throws {
        // Restore original values
        if let t = originalTheme { defaults.set(t, forKey: themeKey) } else { defaults.removeObject(forKey: themeKey) }
        if let u = originalUnit { defaults.set(u, forKey: unitSystemKey) } else { defaults.removeObject(forKey: unitSystemKey) }
        if let c = originalConsent { defaults.set(c, forKey: aiConsentKey) } else { defaults.removeObject(forKey: aiConsentKey) }
        defaults.set(originalOnboarding, forKey: onboardingKey)
        defaults.synchronize()
        store.load()
    }

    // MARK: - save()

    func testSavePersistsThemeAndUnitSystem() {
        store.appTheme = .dark
        store.unitSystem = .metric
        store.save()

        XCTAssertEqual(defaults.string(forKey: themeKey), "Dark")
        XCTAssertEqual(defaults.string(forKey: unitSystemKey), "Metric")
    }

    // MARK: - saveAIConsent()

    func testSaveAIConsentPersistsToDisk() {
        store.saveAIConsent(.accepted)
        XCTAssertEqual(defaults.string(forKey: aiConsentKey), "accepted")
    }

    func testSaveAIConsentUpdatesPublishedProperty() {
        store.saveAIConsent(.declined)
        XCTAssertEqual(store.aiConsent, .declined)
    }

    // MARK: - completeOnboarding()

    func testCompleteOnboardingPersists() {
        store.completeOnboarding()
        XCTAssertTrue(defaults.bool(forKey: onboardingKey))
        XCTAssertTrue(store.hasCompletedOnboarding)
    }

    // MARK: - load()

    func testLoadRestoresPersistedValues() {
        defaults.set("Light", forKey: themeKey)
        defaults.set("US Customary", forKey: unitSystemKey)
        defaults.set("declined", forKey: aiConsentKey)
        defaults.set(true, forKey: onboardingKey)
        defaults.synchronize()

        store.load()

        XCTAssertEqual(store.appTheme, .light)
        XCTAssertEqual(store.unitSystem, .us)
        XCTAssertEqual(store.aiConsent, .declined)
        XCTAssertTrue(store.hasCompletedOnboarding)
    }
}
