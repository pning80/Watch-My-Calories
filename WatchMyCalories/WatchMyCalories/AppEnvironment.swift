import Foundation
import Combine

final class AppEnvironment: ObservableObject {
    static let shared = AppEnvironment()

    private(set) var estimationService: EstimationService
    private(set) var menuAnalysisService: MenuAnalysisService

    private init() {
        let gemini = GeminiService()
        self.estimationService = gemini
        self.menuAnalysisService = gemini
    }

    func swapService(_ service: EstimationService) {
        self.estimationService = service
    }

    func swapMenuAnalysisService(_ service: MenuAnalysisService) {
        self.menuAnalysisService = service
    }
}
