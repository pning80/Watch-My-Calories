import Foundation
import SwiftUI

// Protocol for calorie estimation service
protocol EstimationService {
    func estimateCalories(images: [Data], model: String, apiKey: String) async throws -> EstimationResult
}

// Minimal result model for estimation
struct EstimationItem: Identifiable, Codable {
    let id: UUID = UUID()
    var name: String
    var quantity: String
    var calories: Double
    var confidence: Double
}

struct EstimationResult: Codable {
    var items: [EstimationItem]
    var totalCalories: Double {
        items.reduce(0) { $0 + $1.calories }
    }
}

// Protocol for daily summary service
protocol DailySummaryService {
    func caloriesConsumed(on date: Date) throws -> Double
    func goalCalories(for date: Date) throws -> Double
}

// Mock implementations for development
final class MockEstimationService: EstimationService {
    func estimateCalories(images: [Data], model: String, apiKey: String) async throws -> EstimationResult {
        try await Task.sleep(nanoseconds: 400_000_000)
        return EstimationResult(items: [
            EstimationItem(name: "Grilled Chicken", quantity: "150 g", calories: 248, confidence: 0.78),
            EstimationItem(name: "Rice", quantity: "1 cup", calories: 200, confidence: 0.65)
        ])
    }
}

final class MockDailySummaryService: DailySummaryService {
    func caloriesConsumed(on date: Date) throws -> Double { 800 }
    func goalCalories(for date: Date) throws -> Double { 2000 }
}
