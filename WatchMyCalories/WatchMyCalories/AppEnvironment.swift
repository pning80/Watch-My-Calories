import Foundation
import Combine

final class AppEnvironment: ObservableObject {
    static let shared = AppEnvironment()

    private(set) var estimationService: EstimationService

    private init() {
        self.estimationService = GeminiService()
    }

    func swapService(_ service: EstimationService) {
        self.estimationService = service
    }
}
