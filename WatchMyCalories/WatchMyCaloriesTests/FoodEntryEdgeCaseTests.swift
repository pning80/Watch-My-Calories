import XCTest
@testable import WatchMyCalories

final class FoodEntryEdgeCaseTests: XCTestCase {

    private func dateWithHour(_ hour: Int) -> Date {
        var components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        components.hour = hour
        components.minute = 30
        components.second = 0
        return Calendar.current.date(from: components)!
    }

    // MARK: - Full macro storage

    func testEntryWithAllMacros() {
        let entry = FoodEntry(
            name: "Chicken Breast",
            calories: 280,
            quantity: "6 oz",
            protein: 52,
            carbs: 0,
            fat: 6
        )
        XCTAssertEqual(entry.protein, 52)
        XCTAssertEqual(entry.carbs, 0)
        XCTAssertEqual(entry.fat, 6)
    }

    func testEntryWithNoMacros() {
        let entry = FoodEntry(name: "Unknown Food", calories: 200, quantity: "1 serving")
        XCTAssertNil(entry.protein)
        XCTAssertNil(entry.carbs)
        XCTAssertNil(entry.fat)
    }

    // MARK: - Optional fields

    func testEntryWithImageID() {
        let imageID = UUID()
        let entry = FoodEntry(name: "Pizza", calories: 300, quantity: "1 slice", imageID: imageID)
        XCTAssertEqual(entry.imageID, imageID)
    }

    func testEntryWithMealName() {
        let entry = FoodEntry(name: "Rice", calories: 200, quantity: "1 cup", mealName: "Asian Lunch Bowl")
        XCTAssertEqual(entry.mealName, "Asian Lunch Bowl")
    }

    func testEntryWithoutImageIDIsNil() {
        let entry = FoodEntry(name: "Salad", calories: 150, quantity: "1 bowl")
        XCTAssertNil(entry.imageID)
    }

    // MARK: - MealType from all time ranges

    func testAllMealTypeTimeRanges() {
        // Early morning → Snack
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(6)).mealType, .snack)
        // Breakfast range: 7..<10
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(7)).mealType, .breakfast)
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(9)).mealType, .breakfast)
        // Gap → Snack
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(10)).mealType, .snack)
        // Lunch range: 11..<15
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(11)).mealType, .lunch)
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(14)).mealType, .lunch)
        // Gap → Snack
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(15)).mealType, .snack)
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(16)).mealType, .snack)
        // Dinner range: 17..<21
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(17)).mealType, .dinner)
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(20)).mealType, .dinner)
        // Late night → Snack
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(21)).mealType, .snack)
        XCTAssertEqual(FoodEntry(name: "A", calories: 1, quantity: "1", timestamp: dateWithHour(23)).mealType, .snack)
    }

    // MARK: - MealType raw value persistence

    func testMealTypeRawPersistsAllValues() {
        for mealType in MealType.allCases {
            let entry = FoodEntry(name: "Test", calories: 100, quantity: "1", mealType: mealType)
            XCTAssertEqual(entry.mealTypeRaw, mealType.rawValue)
            XCTAssertEqual(entry.mealType, mealType)
        }
    }

    func testInvalidMealTypeRawDefaultsToSnack() {
        let entry = FoodEntry(name: "Test", calories: 100, quantity: "1")
        entry.mealTypeRaw = "Brunch"
        XCTAssertEqual(entry.mealType, .snack)
    }
}
