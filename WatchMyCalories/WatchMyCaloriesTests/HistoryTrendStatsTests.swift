import XCTest
@testable import WatchMyCalories

final class HistoryTrendStatsTests: XCTestCase {

    private var calendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "UTC")!
        return cal
    }

    /// Fixed "now" mid-afternoon so start-of-day normalization is exercised.
    private var now: Date {
        calendar.date(from: DateComponents(year: 2026, month: 10, day: 24, hour: 15))!
    }

    private func day(_ offset: Int) -> Date {
        calendar.date(byAdding: .day, value: -offset, to: calendar.startOfDay(for: now))!
    }

    private func stats(_ totals: [Int: Double], target: Double = 2000, days: Int = 7) -> HistoryTrendStats {
        HistoryTrendStats(
            dailyTotals: Dictionary(uniqueKeysWithValues: totals.map { (day($0.key), $0.value) }),
            target: target,
            days: days,
            today: now,
            calendar: calendar
        )
    }

    // MARK: - Series

    func testSeriesIsZeroFilledOldestFirstEndingToday() {
        let s = stats([0: 1500, 2: 1800]).series
        XCTAssertEqual(s.count, 7)
        XCTAssertEqual(s.first?.date, day(6))
        XCTAssertEqual(s.last?.date, day(0))
        XCTAssertEqual(s.map(\.total), [0, 0, 0, 0, 1800, 0, 1500])
    }

    func testMonthRangeHasThirtyPoints() {
        let s = stats([29: 900, 30: 1200], days: 30).series
        XCTAssertEqual(s.count, 30)
        XCTAssertEqual(s.first?.date, day(29))
        XCTAssertEqual(s.first?.total, 900)
        // Day 30 is outside the window and must not leak in.
        XCTAssertEqual(s.map(\.total).reduce(0, +), 900)
    }

    // MARK: - Average

    func testAverageIgnoresUnloggedDays() {
        // (1500 + 2500) / 2 logged days — the 5 empty days don't count.
        XCTAssertEqual(stats([0: 1500, 3: 2500]).avgPerDay, 2000)
    }

    func testAverageIsZeroWithNoEntries() {
        XCTAssertEqual(stats([:]).avgPerDay, 0)
    }

    func testAverageExcludesDaysOutsideRange() {
        XCTAssertEqual(stats([0: 1000, 10: 5000]).avgPerDay, 1000)
    }

    // MARK: - Days on target

    func testDaysOnTargetCountsLoggedDaysAtOrUnderTarget() {
        // 2000 == target counts; 2001 is over; 0 (unlogged) never counts.
        let s = stats([0: 2000, 1: 2001, 2: 1200, 4: 0])
        XCTAssertEqual(s.daysOnTarget, 2)
        XCTAssertEqual(s.loggedTotals.count, 3)
    }

    func testDaysOnTargetIsZeroWithoutTarget() {
        XCTAssertEqual(stats([0: 1000, 1: 1500], target: 0).daysOnTarget, 0)
    }

    // MARK: - Streak

    func testStreakCountsConsecutiveDaysEndingToday() {
        XCTAssertEqual(stats([0: 1000, 1: 1000, 2: 1000, 4: 1000]).streak, 3)
    }

    func testStreakIsZeroWhenTodayHasNoEntries() {
        XCTAssertEqual(stats([1: 1000, 2: 1000]).streak, 0)
    }

    func testStreakExtendsBeyondChartRange() {
        let totals = Dictionary(uniqueKeysWithValues: (0..<10).map { ($0, 1000.0) })
        XCTAssertEqual(stats(totals, days: 7).streak, 10)
    }
}
