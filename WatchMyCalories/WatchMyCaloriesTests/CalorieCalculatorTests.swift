import XCTest
@testable import WatchMyCalories

final class CalorieCalculatorTests: XCTestCase {

    func testMaleBMRModeratelyActive() {
        // BMR = (10*70) + (6.25*175) - (5*30) + 5 = 1648.75
        // TDEE = 1648.75 * 1.55 = 2555.5625, rounded = 2556
        let result = CalorieCalculator.recommended(
            heightCm: 175, weightKg: 70, age: 30,
            gender: .male, activityLevel: .moderatelyActive
        )
        XCTAssertEqual(result, 2556)
    }

    func testFemaleBMRLightlyActive() {
        // BMR = (10*55) + (6.25*160) - (5*25) - 161 = 1264
        // TDEE = 1264 * 1.375 = 1738
        let result = CalorieCalculator.recommended(
            heightCm: 160, weightKg: 55, age: 25,
            gender: .female, activityLevel: .lightlyActive
        )
        XCTAssertEqual(result, 1738)
    }

    func testOtherGenderUsesFemaleFormula() {
        let female = CalorieCalculator.recommended(
            heightCm: 170, weightKg: 65, age: 40,
            gender: .female, activityLevel: .sedentary
        )
        let other = CalorieCalculator.recommended(
            heightCm: 170, weightKg: 65, age: 40,
            gender: .other, activityLevel: .sedentary
        )
        XCTAssertEqual(female, other)
    }

    func testActivityLevelsAreIncreasing() {
        let levels: [ActivityLevel] = [.sedentary, .lightlyActive, .moderatelyActive, .veryActive]
        var previous: Double = 0
        for level in levels {
            let result = CalorieCalculator.recommended(
                heightCm: 175, weightKg: 70, age: 30,
                gender: .male, activityLevel: level
            )
            XCTAssertGreaterThan(result, previous, "\(level.rawValue) should exceed previous level")
            previous = result
        }
    }

    func testResultIsRounded() {
        let result = CalorieCalculator.recommended(
            heightCm: 175, weightKg: 70, age: 30,
            gender: .male, activityLevel: .sedentary
        )
        XCTAssertEqual(result, result.rounded(), "Result should be a whole number")
    }
}
