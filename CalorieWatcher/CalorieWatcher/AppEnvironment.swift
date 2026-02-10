import Foundation
import Combine

final class AppEnvironment: ObservableObject {
    static let shared = AppEnvironment()

    let estimationService: EstimationService

    private init() {
        // Switch to GeminiService for production
        // self.estimationService = GeminiService() 
        // For development/testing without API key, keep Mock:
        self.estimationService = GeminiService()
    }
}
