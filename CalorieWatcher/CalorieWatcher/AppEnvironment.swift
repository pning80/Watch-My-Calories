import Foundation

final class AppEnvironment: ObservableObject {
    static let shared = AppEnvironment()

    let dailySummaryService: DailySummaryService
    let estimationService: EstimationService

    private init(
        dailySummaryService: DailySummaryService = MockDailySummaryService(),
        estimationService: EstimationService = MockEstimationService()
    ) {
        self.dailySummaryService = dailySummaryService
        self.estimationService = estimationService
    }
}
