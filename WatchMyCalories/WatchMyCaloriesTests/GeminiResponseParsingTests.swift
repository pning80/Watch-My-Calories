import XCTest
@testable import WatchMyCalories

final class GeminiResponseParsingTests: XCTestCase {

    // MARK: - Markdown cleanup (simulates the text cleaning in GeminiService)

    private func cleanGeminiText(_ text: String) -> String {
        text
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    func testCleanTextStripsMarkdownCodeFence() throws {
        let raw = """
        ```json
        {"items":[{"name":"Rice","quantity":"1 cup","calories":200}]}
        ```
        """
        let cleaned = cleanGeminiText(raw)
        let data = cleaned.data(using: .utf8)!
        let result = try JSONDecoder().decode(EstimationResult.self, from: data)
        XCTAssertEqual(result.items.count, 1)
        XCTAssertEqual(result.items[0].name, "Rice")
    }

    func testCleanTextHandlesRawJSON() throws {
        let raw = """
        {"items":[{"name":"Apple","quantity":"1 medium","calories":95}]}
        """
        let cleaned = cleanGeminiText(raw)
        let data = cleaned.data(using: .utf8)!
        let result = try JSONDecoder().decode(EstimationResult.self, from: data)
        XCTAssertEqual(result.items[0].calories, 95)
    }

    // MARK: - Decoding edge cases

    func testDecodeWithExtraUnknownFieldsSucceeds() throws {
        let json = """
        {
            "items": [
                {"name": "Pasta", "quantity": "1 plate", "calories": 400, "confidence": 0.8, "unknownField": true}
            ],
            "extraTopLevel": "ignored"
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertEqual(result.items.count, 1)
        XCTAssertEqual(result.items[0].name, "Pasta")
    }

    func testDecodeSingleItemResult() throws {
        let json = """
        {"items":[{"name":"Banana","quantity":"1 large","calories":121,"confidence":0.92,"protein":1.5,"carbs":31,"fat":0.4}]}
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertEqual(result.items.count, 1)
        XCTAssertEqual(result.totalCalories, 121)
        XCTAssertEqual(result.items[0].protein, 1.5)
        XCTAssertEqual(result.items[0].carbs, 31)
        XCTAssertEqual(result.items[0].fat, 0.4)
    }

    func testDecodeMultipleItemsTotalCalories() throws {
        let json = """
        {
            "items": [
                {"name": "Eggs", "quantity": "2 large", "calories": 140, "confidence": 0.9},
                {"name": "Toast", "quantity": "1 slice", "calories": 80, "confidence": 0.85},
                {"name": "Coffee", "quantity": "1 cup", "calories": 5, "confidence": 0.95}
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertEqual(result.items.count, 3)
        XCTAssertEqual(result.totalCalories, 225)
    }

    func testDecodeWithNegativeValuesDoesNotCrash() throws {
        // GeminiService clamps negatives post-decode — verify decoding itself succeeds
        let json = """
        {
            "items": [
                {"name": "Item", "quantity": "1", "calories": -10, "confidence": 0.5, "protein": -1, "carbs": -2, "fat": -3}
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertEqual(result.items[0].calories, -10)
        XCTAssertEqual(result.items[0].protein, -1)
    }

    func testDecodeFailsOnMissingRequiredField() {
        let json = """
        {"items":[{"name":"Missing calories","quantity":"1 cup"}]}
        """.data(using: .utf8)!

        XCTAssertThrowsError(try JSONDecoder().decode(EstimationResult.self, from: json))
    }

    // MARK: - EstimationItem identity

    func testDecodedItemsGetUniqueIDs() throws {
        let json = """
        {
            "items": [
                {"name": "A", "quantity": "1", "calories": 100},
                {"name": "B", "quantity": "2", "calories": 200}
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(EstimationResult.self, from: json)
        XCTAssertNotEqual(result.items[0].id, result.items[1].id)
    }

    // MARK: - Negative value clamping (mirrors GeminiService logic)

    func testNegativeValueClampingLogic() {
        let item = EstimationItem(
            name: "Test",
            quantity: "1",
            calories: -50,
            confidence: 0.5,
            protein: -5,
            carbs: -10,
            fat: -3
        )
        // Simulate the clamping GeminiService applies
        let clamped = EstimationItem(
            id: item.id,
            name: item.name,
            quantity: item.quantity,
            calories: max(0, item.calories),
            confidence: item.confidence,
            protein: item.protein.map { max(0, $0) },
            carbs: item.carbs.map { max(0, $0) },
            fat: item.fat.map { max(0, $0) }
        )
        XCTAssertEqual(clamped.calories, 0)
        XCTAssertEqual(clamped.protein, 0)
        XCTAssertEqual(clamped.carbs, 0)
        XCTAssertEqual(clamped.fat, 0)
    }
}
