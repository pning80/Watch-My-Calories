import XCTest
@testable import WatchMyCalories

final class UnitConversionTests: XCTestCase {

    // MARK: - Metric to US

    func testGramsToOunces() {
        // 200 / 28.3495 = 7.055 -> "7.1 oz"
        XCTAssertEqual(UnitSystem.us.convertQuantity("200 g"), "7.1 oz")
    }

    func testMillilitersToFluidOunces() {
        // 250 / 29.5735 = 8.454 -> "8.5 fl oz"
        XCTAssertEqual(UnitSystem.us.convertQuantity("250 ml"), "8.5 fl oz")
    }

    func testLiterToCups() {
        // 1 * 4.22675 = 4.227 -> "4.2 cups"
        XCTAssertEqual(UnitSystem.us.convertQuantity("1 liter"), "4.2 cups")
    }

    // MARK: - US to Metric

    func testOuncesToGrams() {
        // 7 * 28.3495 = 198.45 -> >= 100, round to nearest 10 -> "200 g"
        XCTAssertEqual(UnitSystem.metric.convertQuantity("7 oz"), "200 g")
    }

    func testCupsToMilliliters() {
        // 1 * 236.588 = 236.6 -> >= 100, round to nearest 10 -> "240 ml"
        XCTAssertEqual(UnitSystem.metric.convertQuantity("1 cup"), "240 ml")
    }

    func testTablespoonsToMilliliters() {
        // 2 * 14.787 = 29.574 -> >= 10, round to whole -> "30 ml"
        XCTAssertEqual(UnitSystem.metric.convertQuantity("2 tbsp"), "30 ml")
    }

    func testGallonToMilliliters() {
        // 1 * 3785.41 = 3785.41 -> >= 100, round to nearest 10 -> "3790 ml"
        XCTAssertEqual(UnitSystem.metric.convertQuantity("1 gallon"), "3790 ml")
    }

    func testPintToMilliliters() {
        // 1 * 473.176 = 473.176 -> >= 100, round to nearest 10 -> "470 ml"
        XCTAssertEqual(UnitSystem.metric.convertQuantity("1 pint"), "470 ml")
    }

    // MARK: - Passthrough cases

    func testCountBasedUnitsUnchanged() {
        XCTAssertEqual(UnitSystem.us.convertQuantity("3 pieces"), "3 pieces")
        XCTAssertEqual(UnitSystem.metric.convertQuantity("1 slice"), "1 slice")
        XCTAssertEqual(UnitSystem.us.convertQuantity("2 eggs"), "2 eggs")
    }

    func testSameSystemNoConversion() {
        XCTAssertEqual(UnitSystem.metric.convertQuantity("200 g"), "200 g")
        XCTAssertEqual(UnitSystem.us.convertQuantity("6 oz"), "6 oz")
    }

    func testNoSpaceBetweenValueAndUnit() {
        XCTAssertEqual(UnitSystem.us.convertQuantity("200g"), "7.1 oz")
    }

    func testNoLeadingNumberPassthrough() {
        XCTAssertEqual(UnitSystem.us.convertQuantity("a handful"), "a handful")
    }

    func testEmptyStringPassthrough() {
        XCTAssertEqual(UnitSystem.us.convertQuantity(""), "")
    }
}
