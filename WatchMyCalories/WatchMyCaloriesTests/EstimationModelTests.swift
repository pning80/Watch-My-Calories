import XCTest
@testable import WatchMyCalories

final class EstimationModelTests: XCTestCase {

    func testTotalCaloriesSumsItems() {
        let result = EstimationResult(items: [
            EstimationItem(name: "Apple", quantity: "1 medium", calories: 95, confidence: 0.9),
            EstimationItem(name: "Banana", quantity: "1 medium", calories: 105, confidence: 0.85),
            EstimationItem(name: "Orange", quantity: "1 medium", calories: 62, confidence: 0.88),
        ])
        XCTAssertEqual(result.totalCalories, 262)
    }

    func testTotalCaloriesEmptyItems() {
        let result = EstimationResult(items: [])
        XCTAssertEqual(result.totalCalories, 0)
    }

    func testDecodeValidJSON() throws {
        let json = """
        {
            "items": [
                {"name": "Rice", "quantity": "1 cup", "calories": 200, "confidence": 0.9, "protein": 4, "carbs": 45, "fat": 0.5}
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertEqual(result.items.count, 1)
        XCTAssertEqual(result.items[0].name, "Rice")
        XCTAssertEqual(result.items[0].calories, 200)
        XCTAssertEqual(result.items[0].protein, 4)
        XCTAssertEqual(result.items[0].carbs, 45)
        XCTAssertEqual(result.items[0].fat, 0.5)
    }

    func testDecodeMissingConfidenceDefaultsToZero() throws {
        let json = """
        {
            "items": [
                {"name": "Bread", "quantity": "2 slices", "calories": 150}
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertEqual(result.items[0].confidence, 0.0)
    }

    func testDecodeWithMealName() throws {
        let json = """
        {
            "mealName": "Grilled Chicken Lunch",
            "items": [
                {"name": "Chicken", "quantity": "6 oz", "calories": 280, "confidence": 0.95}
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertEqual(result.mealName, "Grilled Chicken Lunch")
    }

    func testDecodeOptionalMacrosAbsent() throws {
        let json = """
        {
            "items": [
                {"name": "Mystery", "quantity": "1 serving", "calories": 300, "confidence": 0.5}
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertNil(result.items[0].protein)
        XCTAssertNil(result.items[0].carbs)
        XCTAssertNil(result.items[0].fat)
    }
}
