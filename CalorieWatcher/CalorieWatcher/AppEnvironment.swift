import Foundation
import Combine

final class AppEnvironment: ObservableObject {
    static let shared = AppEnvironment()

    let estimationService: EstimationService

    private init() {
        self.estimationService = GeminiService()
    }
}
