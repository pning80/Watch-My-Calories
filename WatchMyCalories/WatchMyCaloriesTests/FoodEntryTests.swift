import XCTest
@testable import WatchMyCalories

final class FoodEntryTests: XCTestCase {

    private func dateWithHour(_ hour: Int) -> Date {
        var components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        components.hour = hour
        components.minute = 30
        components.second = 0
        return Calendar.current.date(from: components)!
    }

    func testAutoAssignsMealTypeFromTimestamp() {
        let entry = FoodEntry(name: "Eggs", calories: 150, quantity: "2 eggs", timestamp: dateWithHour(8))
        XCTAssertEqual(entry.mealType, .breakfast)
    }

    func testExplicitMealTypeOverridesTimestamp() {
        let entry = FoodEntry(name: "Pizza", calories: 300, quantity: "2 slices", timestamp: dateWithHour(8), mealType: .dinner)
        XCTAssertEqual(entry.mealType, .dinner)
    }

    func testInitGeneratesUniqueID() {
        let entry1 = FoodEntry(name: "A", calories: 100, quantity: "1")
        let entry2 = FoodEntry(name: "B", calories: 200, quantity: "2")
        XCTAssertNotEqual(entry1.id, entry2.id)
    }
}
