import Foundation
import SwiftUI
import UIKit

// MARK: - Models

struct EstimationItem: Identifiable, Codable {
    var id: UUID
    var name: String
    var quantity: String
    var calories: Double
    var confidence: Double
    var protein: Double?
    var carbs: Double?
    var fat: Double?

    init(id: UUID = UUID(), name: String, quantity: String, calories: Double, confidence: Double, protein: Double? = nil, carbs: Double? = nil, fat: Double? = nil) {
        self.id = id
        self.name = name
        self.quantity = quantity
        self.calories = calories
        self.confidence = confidence
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = UUID()
        self.name = try container.decode(String.self, forKey: .name)
        self.quantity = try container.decode(String.self, forKey: .quantity)
        self.calories = try container.decode(Double.self, forKey: .calories)
        self.confidence = try container.decodeIfPresent(Double.self, forKey: .confidence) ?? 0.0
        self.protein = try container.decodeIfPresent(Double.self, forKey: .protein)
        self.carbs = try container.decodeIfPresent(Double.self, forKey: .carbs)
        self.fat = try container.decodeIfPresent(Double.self, forKey: .fat)
    }

    private enum CodingKeys: String, CodingKey {
        case name, quantity, calories, confidence, protein, carbs, fat
    }
}

struct EstimationResult: Codable {
    var mealName: String?
    var items: [EstimationItem]
    var totalCalories: Double {
        items.reduce(0) { $0 + $1.calories }
    }
}

// MARK: - Protocols

protocol EstimationService {
    func estimateCalories(images: [Data]) async throws -> EstimationResult
}

// MARK: - Gemini Implementation

enum GeminiError: Error, LocalizedError {
    case missingBackendConfig
    case invalidResponse
    case apiError(String)
    case networkError(Error)

    var errorDescription: String? {
        switch self {
        case .missingBackendConfig: return "Backend URL or Key not configured. Please check Settings."
        case .invalidResponse: return "Failed to parse response from Gemini."
        case .apiError(let msg): return "API Error: \(msg)"
        case .networkError(let error): return "Network error: \(error.localizedDescription)"
        }
    }
}

final class GeminiService: EstimationService {

    func estimateCalories(images: [Data]) async throws -> EstimationResult {
        let backendURL = BackendConfig.baseURL
        let backendKey = BackendConfig.apiKey

        guard !backendURL.isEmpty, !backendKey.isEmpty else {
            throw GeminiError.missingBackendConfig
        }

        // Model name in the path is ignored by the backend — it always uses its configured model
        let urlString = "\(backendURL)/v1beta/models/default:generateContent"
        guard let url = URL(string: urlString) else { throw GeminiError.networkError(URLError(.badURL)) }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(backendKey, forHTTPHeaderField: "x-backend-key")

        // Construct Request Body
        let prompt = """
        Analyze these food images. Identify the food items, estimate the portion size, and calculate the calories.
        Always use US customary units for quantities (oz, lbs, cups, tbsp, tsp, pieces, slices). Never use metric units like grams or milliliters.
        Return ONLY a raw JSON object (no markdown, no code blocks) with this structure:
        {
          "items": [
            {
              "name": "Food Name",
              "quantity": "Estimated Quantity in US customary units (e.g. 1 cup, 6 oz, 2 pieces)",
              "calories": 150,
              "protein": 10,
              "carbs": 20,
              "fat": 5,
              "confidence": 0.95
            }
          ]
        }
        """

        let parts: [[String: Any]] = [
            ["text": prompt]
        ] + images.map { data in
            [
                "inline_data": [
                    "mime_type": "image/jpeg",
                    "data": data.base64EncodedString()
                ]
            ]
        }

        let body: [String: Any] = [
            "contents": [
                ["parts": parts]
            ]
        ]

        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw GeminiError.networkError(URLError(.badServerResponse))
        }

        guard httpResponse.statusCode == 200 else {
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorObj = errorJson["error"] as? [String: Any],
               let message = errorObj["message"] as? String {
                print("API Error: \(message)")
                throw GeminiError.apiError(message)
            }
            // Check for plain-text error from backend
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = errorJson["error"] as? String {
                throw GeminiError.apiError(message)
            }
            let bodyString = String(data: data, encoding: .utf8) ?? "No body"
            print("API Error (Status \(httpResponse.statusCode)): \(bodyString)")
            throw GeminiError.apiError("Server returned \(httpResponse.statusCode)")
        }

        // Parse Gemini Response Structure
        struct GeminiResponse: Decodable {
            struct Candidate: Decodable {
                struct Content: Decodable {
                    struct Part: Decodable {
                        let text: String
                    }
                    let parts: [Part]
                }
                let content: Content
            }
            let candidates: [Candidate]?
        }

        let geminiResponse = try JSONDecoder().decode(GeminiResponse.self, from: data)
        guard let text = geminiResponse.candidates?.first?.content.parts.first?.text else {
            throw GeminiError.invalidResponse
        }

        // Clean up text (remove markdown if present)
        let cleanText = text
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard let jsonData = cleanText.data(using: .utf8) else { throw GeminiError.invalidResponse }

        return try JSONDecoder().decode(EstimationResult.self, from: jsonData)
    }
}

// MARK: - Mocks (Kept for fallback/testing)
final class MockEstimationService: EstimationService {
    func estimateCalories(images: [Data]) async throws -> EstimationResult {
        try await Task.sleep(nanoseconds: 1_000_000_000)
        return EstimationResult(mealName: "Mock Chicken and Rice", items: [
            EstimationItem(name: "Mock Chicken", quantity: "5 oz", calories: 250, confidence: 0.95),
            EstimationItem(name: "Mock Rice", quantity: "1 cup", calories: 200, confidence: 0.90)
        ])
    }
}
