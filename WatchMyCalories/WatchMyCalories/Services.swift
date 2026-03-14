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

        // Ensure App Attest key is generated and attested (no-op on simulator)
        let attestManager = AppAttestManager.shared
        try await attestManager.ensureAttested()

        // Model name in the path is ignored by the backend — it always uses its configured model
        let urlString = "\(backendURL)/v1beta/models/default:generateContent"
        guard let url = URL(string: urlString) else { throw GeminiError.networkError(URLError(.badURL)) }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        // Use legacy key as fallback when App Attest is unavailable (simulator, pre-A12 devices)
        if !attestManager.isSupported {
            request.addValue(backendKey, forHTTPHeaderField: "x-backend-key")
        }

        // Construct Request Body
        let unitInstruction: String
        let quantityExample: String
        if SettingsStore.shared.unitSystem == .metric {
            unitInstruction = "Prefer metric units for quantities (g, kg, ml, L, pieces, slices) when possible."
            quantityExample = "Estimated Quantity (e.g. 200 g, 250 ml, 2 pieces)"
        } else {
            unitInstruction = "Prefer US customary units for quantities. Use oz for weight, fl oz for liquid volume, and cups/tbsp/tsp for other volumes. Examples: 6 oz, 8 fl oz, 1 cup, 2 pieces."
            quantityExample = "Estimated Quantity (e.g. 1 cup, 6 oz, 2 pieces)"
        }

        let prompt = """
        Analyze these food images. Identify the food items, estimate the portion size, and calculate the calories.
        \(unitInstruction)
        Return ONLY a raw JSON object (no markdown, no code blocks) with this structure:
        {
          "items": [
            {
              "name": "Food Name",
              "quantity": "\(quantityExample)",
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
        request.timeoutInterval = 30

        // Retry loop: automatically re-attest and retry on 401 attestation rejection
        let maxAttempts = 3
        var data: Data!
        var httpResponse: HTTPURLResponse!

        for attempt in 1...maxAttempts {
            // Attach App Attest assertion headers (signs the request body)
            if attestManager.isSupported, let httpBody = request.httpBody {
                // Strip old assertion headers before regenerating
                request.setValue(nil, forHTTPHeaderField: "X-App-Attest-Assertion")
                request.setValue(nil, forHTTPHeaderField: "X-App-Attest-KeyID")
                let headers = try await attestManager.assertionHeaders(for: httpBody)
                for (key, value) in headers {
                    request.setValue(value, forHTTPHeaderField: key)
                }
            }

            let (responseData, response) = try await URLSession.shared.data(for: request)

            guard let httpResp = response as? HTTPURLResponse else {
                throw GeminiError.networkError(URLError(.badServerResponse))
            }

            // On 401 with App Attest, re-attest and retry transparently
            if httpResp.statusCode == 401 && attestManager.isSupported && attempt < maxAttempts {
                attestManager.handleAttestationRejected()
                try await attestManager.ensureAttested()
                continue
            }

            data = responseData
            httpResponse = httpResp
            break
        }

        guard httpResponse.statusCode == 200 else {
            // Clear attestation state on final 401 so next user-initiated retry starts fresh
            if httpResponse.statusCode == 401 && attestManager.isSupported {
                attestManager.handleAttestationRejected()
            }
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorObj = errorJson["error"] as? [String: Any],
               let message = errorObj["message"] as? String {
                throw GeminiError.apiError(message)
            }
            // Check for plain-text error from backend
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = errorJson["error"] as? String {
                throw GeminiError.apiError(message)
            }
            throw GeminiError.apiError("Server returned \(httpResponse.statusCode)")
        }

        // Parse Gemini Response Structure
        // Note: text is optional because thinking models (e.g. gemini-3-flash)
        // may include thought-only parts without a text field.
        struct GeminiResponse: Decodable {
            struct Candidate: Decodable {
                struct Content: Decodable {
                    struct Part: Decodable {
                        let text: String?
                    }
                    let parts: [Part]
                }
                let content: Content
            }
            let candidates: [Candidate]?
        }

        let geminiResponse = try JSONDecoder().decode(GeminiResponse.self, from: data)
        guard let text = geminiResponse.candidates?.first?.content.parts.compactMap(\.text).first else {
            throw GeminiError.invalidResponse
        }

        // Clean up text (remove markdown if present)
        let cleanText = text
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard let jsonData = cleanText.data(using: .utf8) else { throw GeminiError.invalidResponse }

        let result = try JSONDecoder().decode(EstimationResult.self, from: jsonData)

        // Clamp negative calorie/macro values to zero
        let validatedItems = result.items.map { item in
            EstimationItem(
                id: item.id,
                name: item.name,
                quantity: item.quantity,
                calories: max(0, item.calories),
                confidence: item.confidence,
                protein: item.protein.map { max(0, $0) },
                carbs: item.carbs.map { max(0, $0) },
                fat: item.fat.map { max(0, $0) }
            )
        }

        return EstimationResult(mealName: result.mealName, items: validatedItems)
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
