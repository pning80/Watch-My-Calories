import XCTest
@testable import WatchMyCalories

final class MenuAnalysisModelTests: XCTestCase {

    // MARK: - MenuItemResult Decoding

    func testDecodeMenuItemWithAllFields() throws {
        let json = """
        {
            "name": "Margherita Pizza",
            "description": "Classic tomato and mozzarella",
            "calories": 850,
            "protein": 35,
            "carbs": 90,
            "fat": 38
        }
        """.data(using: .utf8)!

        let item = try JSONDecoder().decode(MenuItemResult.self, from: json)
        XCTAssertEqual(item.name, "Margherita Pizza")
        XCTAssertEqual(item.description, "Classic tomato and mozzarella")
        XCTAssertEqual(item.calories, 850)
        XCTAssertEqual(item.protein, 35)
        XCTAssertEqual(item.carbs, 90)
        XCTAssertEqual(item.fat, 38)
    }

    func testDecodeMenuItemWithOptionalFieldsMissing() throws {
        let json = """
        {
            "name": "House Salad",
            "calories": 200
        }
        """.data(using: .utf8)!

        let item = try JSONDecoder().decode(MenuItemResult.self, from: json)
        XCTAssertEqual(item.name, "House Salad")
        XCTAssertNil(item.description)
        XCTAssertEqual(item.calories, 200)
        XCTAssertNil(item.protein)
        XCTAssertNil(item.carbs)
        XCTAssertNil(item.fat)
    }

    func testDecodedMenuItemGetsUniqueID() throws {
        let json = """
        {"name": "Item", "calories": 100}
        """.data(using: .utf8)!

        let item1 = try JSONDecoder().decode(MenuItemResult.self, from: json)
        let item2 = try JSONDecoder().decode(MenuItemResult.self, from: json)
        XCTAssertNotEqual(item1.id, item2.id)
    }

    func testMenuItemMemberwiseInit() {
        let item = MenuItemResult(
            name: "Pasta Carbonara",
            description: "Creamy egg and bacon pasta",
            calories: 720,
            protein: 28,
            carbs: 65,
            fat: 38
        )
        XCTAssertEqual(item.name, "Pasta Carbonara")
        XCTAssertEqual(item.description, "Creamy egg and bacon pasta")
        XCTAssertEqual(item.calories, 720)
    }

    // MARK: - MenuAnalysisResult Decoding

    func testDecodeSuccessfulMenuAnalysis() throws {
        let json = """
        {
            "restaurantName": "Olive Garden",
            "items": [
                {"name": "Breadsticks", "calories": 140},
                {"name": "Chicken Alfredo", "description": "Creamy pasta", "calories": 1010, "protein": 50, "carbs": 85, "fat": 52}
            ]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(MenuAnalysisResult.self, from: json)
        XCTAssertEqual(result.restaurantName, "Olive Garden")
        XCTAssertEqual(result.items?.count, 2)
        XCTAssertNil(result.error)
        XCTAssertEqual(result.items?.first?.name, "Breadsticks")
    }

    func testDecodeNotAMenuError() throws {
        let json = """
        {"error": "not_a_menu"}
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(MenuAnalysisResult.self, from: json)
        XCTAssertEqual(result.error, "not_a_menu")
        XCTAssertNil(result.items)
        XCTAssertNil(result.restaurantName)
    }

    func testDecodeMenuWithNullRestaurantName() throws {
        let json = """
        {
            "restaurantName": null,
            "items": [{"name": "Special of the Day", "calories": 600}]
        }
        """.data(using: .utf8)!

        let result = try JSONDecoder().decode(MenuAnalysisResult.self, from: json)
        XCTAssertNil(result.restaurantName)
        XCTAssertEqual(result.items?.count, 1)
    }

    // MARK: - MenuScan Model

    func testMenuScanInit() {
        let items = [
            MenuItemResult(name: "Burger", description: "Beef patty", calories: 700, protein: 40, carbs: 50, fat: 35),
            MenuItemResult(name: "Fries", description: nil, calories: 400, protein: 5, carbs: 50, fat: 20)
        ]
        let imageID = UUID()
        let scan = MenuScan(restaurantName: "Five Guys", imageID: imageID, timestamp: Date(), items: items)

        XCTAssertEqual(scan.restaurantName, "Five Guys")
        XCTAssertEqual(scan.imageID, imageID)
        XCTAssertNotNil(scan.id)
    }

    func testMenuScanItemsRoundTrip() {
        let items = [
            MenuItemResult(name: "Tacos", description: "Three street tacos", calories: 450, protein: 25, carbs: 40, fat: 20),
            MenuItemResult(name: "Guacamole", description: nil, calories: 150, protein: 2, carbs: 8, fat: 13)
        ]
        let scan = MenuScan(restaurantName: "Taqueria", imageID: nil, timestamp: Date(), items: items)

        // Items should round-trip through JSON encoding/decoding
        let decoded = scan.items
        XCTAssertEqual(decoded.count, 2)
        XCTAssertEqual(decoded[0].name, "Tacos")
        XCTAssertEqual(decoded[0].calories, 450)
        XCTAssertEqual(decoded[1].name, "Guacamole")
        XCTAssertEqual(decoded[1].calories, 150)
    }

    func testMenuScanEmptyItems() {
        let scan = MenuScan(restaurantName: nil, imageID: nil, timestamp: Date(), items: [])
        XCTAssertTrue(scan.items.isEmpty)
        XCTAssertNil(scan.restaurantName)
    }

    func testMenuScanItemsPreservesDescription() {
        let items = [
            MenuItemResult(name: "Steak", description: "8oz ribeye with chimichurri", calories: 900, protein: 60, carbs: 0, fat: 70)
        ]
        let scan = MenuScan(restaurantName: nil, imageID: nil, timestamp: Date(), items: items)

        let decoded = scan.items
        XCTAssertEqual(decoded.first?.description, "8oz ribeye with chimichurri")
    }

    func testMenuScanItemsPreservesNilMacros() {
        let items = [
            MenuItemResult(name: "Mystery Dish", description: nil, calories: 500, protein: nil, carbs: nil, fat: nil)
        ]
        let scan = MenuScan(restaurantName: nil, imageID: nil, timestamp: Date(), items: items)

        let decoded = scan.items
        XCTAssertNil(decoded.first?.protein)
        XCTAssertNil(decoded.first?.carbs)
        XCTAssertNil(decoded.first?.fat)
    }
}
