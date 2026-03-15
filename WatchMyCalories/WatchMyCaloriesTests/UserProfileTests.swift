import XCTest
@testable import WatchMyCalories

final class UserProfileTests: XCTestCase {

    func testInitSetsAllProperties() {
        let profile = UserProfile(height: 175, weight: 70, age: 30, gender: .male, activityLevel: .moderatelyActive, targetCalories: 2200)
        XCTAssertEqual(profile.height, 175)
        XCTAssertEqual(profile.weight, 70)
        XCTAssertEqual(profile.age, 30)
        XCTAssertEqual(profile.gender, .male)
        XCTAssertEqual(profile.activityLevel, .moderatelyActive)
        XCTAssertEqual(profile.targetCalories, 2200)
    }

    func testGenderComputedPropertyReadsRawValue() {
        let profile = UserProfile(height: 160, weight: 55, age: 25, gender: .female, activityLevel: .sedentary, targetCalories: 1600)
        XCTAssertEqual(profile.genderRaw, "Female")
        XCTAssertEqual(profile.gender, .female)
    }

    func testGenderComputedPropertySetUpdatesRaw() {
        let profile = UserProfile(height: 160, weight: 55, age: 25, gender: .female, activityLevel: .sedentary, targetCalories: 1600)
        profile.gender = .other
        XCTAssertEqual(profile.genderRaw, "Other")
        XCTAssertEqual(profile.gender, .other)
    }

    func testInvalidGenderRawDefaultsToOther() {
        let profile = UserProfile(height: 160, weight: 55, age: 25, gender: .male, activityLevel: .sedentary, targetCalories: 1600)
        profile.genderRaw = "InvalidGender"
        XCTAssertEqual(profile.gender, .other)
    }

    func testActivityLevelComputedPropertyRoundTrips() {
        let profile = UserProfile(height: 180, weight: 80, age: 35, gender: .male, activityLevel: .veryActive, targetCalories: 2800)
        XCTAssertEqual(profile.activityLevelRaw, "Very Active")
        XCTAssertEqual(profile.activityLevel, .veryActive)
    }

    func testInvalidActivityLevelRawDefaultsToSedentary() {
        let profile = UserProfile(height: 180, weight: 80, age: 35, gender: .male, activityLevel: .veryActive, targetCalories: 2800)
        profile.activityLevelRaw = "Super Active"
        XCTAssertEqual(profile.activityLevel, .sedentary)
    }

    func testCreatedAtIsSetAutomatically() {
        let before = Date()
        let profile = UserProfile(height: 170, weight: 65, age: 28, gender: .female, activityLevel: .lightlyActive, targetCalories: 1800)
        let after = Date()
        XCTAssertGreaterThanOrEqual(profile.createdAt, before)
        XCTAssertLessThanOrEqual(profile.createdAt, after)
    }
}
