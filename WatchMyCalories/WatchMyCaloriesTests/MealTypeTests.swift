import XCTest
@testable import WatchMyCalories

final class MealTypeTests: XCTestCase {

    private func dateWithHour(_ hour: Int) -> Date {
        var components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        components.hour = hour
        components.minute = 0
        components.second = 0
        return Calendar.current.date(from: components)!
    }

    func testMidnightIsSnack() {
        XCTAssertEqual(MealType.from(date: dateWithHour(0)), .snack)
    }

    func testEarlyMorningIsSnack() {
        XCTAssertEqual(MealType.from(date: dateWithHour(6)), .snack)
    }

    func testBreakfastLowerBound() {
        XCTAssertEqual(MealType.from(date: dateWithHour(7)), .breakfast)
    }

    func testBreakfastUpperBound() {
        XCTAssertEqual(MealType.from(date: dateWithHour(9)), .breakfast)
    }

    func testHour10IsSnack() {
        XCTAssertEqual(MealType.from(date: dateWithHour(10)), .snack)
    }

    func testLunchLowerBound() {
        XCTAssertEqual(MealType.from(date: dateWithHour(11)), .lunch)
    }

    func testLunchUpperBound() {
        XCTAssertEqual(MealType.from(date: dateWithHour(14)), .lunch)
    }

    func testGapBetweenLunchAndDinnerIsSnack() {
        XCTAssertEqual(MealType.from(date: dateWithHour(15)), .snack)
        XCTAssertEqual(MealType.from(date: dateWithHour(16)), .snack)
    }

    func testDinnerLowerBound() {
        XCTAssertEqual(MealType.from(date: dateWithHour(17)), .dinner)
    }

    func testDinnerUpperBound() {
        XCTAssertEqual(MealType.from(date: dateWithHour(20)), .dinner)
    }

    func testHour21IsSnack() {
        XCTAssertEqual(MealType.from(date: dateWithHour(21)), .snack)
    }

    func testLateNightIsSnack() {
        XCTAssertEqual(MealType.from(date: dateWithHour(23)), .snack)
    }
}
