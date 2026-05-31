import Foundation
import SwiftUI
import UIKit
import CoreLocation

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
    var totalProtein: Double { items.compactMap(\.protein).reduce(0, +) }
    var totalCarbs: Double { items.compactMap(\.carbs).reduce(0, +) }
    var totalFat: Double { items.compactMap(\.fat).reduce(0, +) }
}

// MARK: - Menu Analysis Models

struct MenuItemResult: Identifiable, Codable {
    var id = UUID()
    let name: String
    let description: String?
    let calories: Double
    let protein: Double?
    let carbs: Double?
    let fat: Double?

    init(name: String, description: String?, calories: Double, protein: Double?, carbs: Double?, fat: Double?) {
        self.id = UUID()
        self.name = name
        self.description = description
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = UUID()
        self.name = try container.decode(String.self, forKey: .name)
        self.description = try container.decodeIfPresent(String.self, forKey: .description)
        self.calories = try container.decode(Double.self, forKey: .calories)
        self.protein = try container.decodeIfPresent(Double.self, forKey: .protein)
        self.carbs = try container.decodeIfPresent(Double.self, forKey: .carbs)
        self.fat = try container.decodeIfPresent(Double.self, forKey: .fat)
    }

    private enum CodingKeys: String, CodingKey {
        case name, description, calories, protein, carbs, fat
    }
}

struct MenuAnalysisResult: Codable {
    let restaurantName: String?
    let items: [MenuItemResult]?
    let error: String?
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
    case rateLimited(retryAfter: Int?)

    var errorDescription: String? {
        switch self {
        case .missingBackendConfig: return "Backend URL or Key not configured. Please check Settings."
        case .invalidResponse: return "Failed to parse response from Gemini."
        case .apiError(let msg): return "API Error: \(msg)"
        case .networkError(let error): return "Network error: \(error.localizedDescription)"
        case .rateLimited(let retryAfter):
            if let seconds = retryAfter {
                return "Too many requests. Please try again in \(seconds) seconds."
            }
            return "Too many requests. Please wait a moment and try again."
        }
    }
}

final class GeminiService: EstimationService {

    // MARK: - Shared HTTP/Retry Infrastructure

    /// Sends a prompt + images to the Gemini backend and returns the cleaned JSON text from the response.
    private func sendGeminiRequest(prompt: String, images: [Data]) async throws -> String {
        let backendURL = BackendConfig.baseURL
        let backendKey = BackendConfig.apiKey

        guard !backendURL.isEmpty, !backendKey.isEmpty else {
            throw GeminiError.missingBackendConfig
        }

        let attestManager = AppAttestManager.shared
        try await attestManager.ensureAttested()

        let urlString = "\(backendURL)/v1beta/models/default:generateContent"
        guard let url = URL(string: urlString) else { throw GeminiError.networkError(URLError(.badURL)) }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("ios", forHTTPHeaderField: "X-App-Platform")
        request.addValue(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown", forHTTPHeaderField: "X-App-Version")

        if !attestManager.isSupported {
            request.addValue(backendKey, forHTTPHeaderField: "x-backend-key")
        }

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

        let maxAttempts = 3
        var data: Data!
        var httpResponse: HTTPURLResponse!

        for attempt in 1...maxAttempts {
            if attestManager.isSupported, let httpBody = request.httpBody {
                request.setValue(nil, forHTTPHeaderField: "X-App-Attest-Assertion")
                request.setValue(nil, forHTTPHeaderField: "X-App-Attest-KeyID")
                let headers = try await attestManager.assertionHeaders(for: httpBody)
                for (key, value) in headers {
                    request.setValue(value, forHTTPHeaderField: key)
                }
            }

            let responseData: Data
            let response: URLResponse
            do {
                (responseData, response) = try await URLSession.shared.data(for: request)
            } catch {
                if attempt < maxAttempts {
                    try await Task.sleep(for: .seconds(1 << (attempt - 1)))
                    continue
                }
                throw GeminiError.networkError(error)
            }

            guard let httpResp = response as? HTTPURLResponse else {
                throw GeminiError.networkError(URLError(.badServerResponse))
            }

            if httpResp.statusCode == 401 && attestManager.isSupported && attempt < maxAttempts {
                attestManager.handleAttestationRejected()
                try await attestManager.ensureAttested()
                continue
            }

            if httpResp.statusCode >= 500 && attempt < maxAttempts {
                try await Task.sleep(for: .seconds(1 << (attempt - 1)))
                continue
            }

            data = responseData
            httpResponse = httpResp
            break
        }

        if httpResponse.statusCode == 429 {
            let retryAfter = httpResponse.value(forHTTPHeaderField: "Retry-After").flatMap { Int($0) }
            throw GeminiError.rateLimited(retryAfter: retryAfter)
        }

        guard httpResponse.statusCode == 200 else {
            if httpResponse.statusCode == 401 && attestManager.isSupported {
                attestManager.handleAttestationRejected()
            }
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorObj = errorJson["error"] as? [String: Any],
               let message = errorObj["message"] as? String {
                throw GeminiError.apiError(message)
            }
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = errorJson["error"] as? String {
                throw GeminiError.apiError(message)
            }
            throw GeminiError.apiError("Server returned \(httpResponse.statusCode)")
        }

        // Parse Gemini response structure
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

        let cleanText = text
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        return cleanText
    }

    // MARK: - Food Estimation

    func estimateCalories(images: [Data]) async throws -> EstimationResult {
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

        let cleanText = try await sendGeminiRequest(prompt: prompt, images: images)
        guard let jsonData = cleanText.data(using: .utf8) else { throw GeminiError.invalidResponse }

        let result = try JSONDecoder().decode(EstimationResult.self, from: jsonData)

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

    // MARK: - Menu Analysis

    func analyzeMenu(image: Data, location: CLLocation?, locality: String?) async throws -> MenuAnalysisResult {
        var locationContext = ""
        if let location, let locality {
            locationContext = "\nThe user is located near \(locality) (approximately \(location.coordinate.latitude), \(location.coordinate.longitude)). Use this to help identify the restaurant and cuisine style for more accurate estimates.\n"
        } else if let location {
            locationContext = "\nThe user is located at approximately \(location.coordinate.latitude), \(location.coordinate.longitude). Use this to help identify the restaurant and cuisine style for more accurate estimates.\n"
        }

        let unitInstruction: String
        if SettingsStore.shared.unitSystem == .metric {
            unitInstruction = "Prefer metric units for quantities (g, kg, ml, L, pieces, slices) when possible."
        } else {
            unitInstruction = "Prefer US customary units for quantities. Use oz for weight, fl oz for liquid volume, and cups/tbsp/tsp for other volumes."
        }

        let prompt = """
        Analyze this photo of a restaurant menu. Identify the dishes listed and estimate the calorie content for each based on typical serving sizes.
        \(locationContext)
        \(unitInstruction)

        A restaurant menu includes printed menus, chalkboard specials, digital menu displays, drink lists, and similar documents listing food or drink items offered by a food service establishment. Receipts, grocery lists, nutrition labels, and non-food documents are NOT menus.

        If the image does NOT appear to be a restaurant menu, respond with ONLY:
        {"error": "not_a_menu"}

        Otherwise, return ONLY a raw JSON object (no markdown, no code blocks):
        {
          "restaurantName": "Name if visible or identifiable, otherwise null",
          "items": [
            {
              "name": "Dish Name",
              "description": "Brief description if visible on menu",
              "calories": 500,
              "protein": 30,
              "carbs": 50,
              "fat": 15
            }
          ]
        }
        """

        let cleanText = try await sendGeminiRequest(prompt: prompt, images: [image])
        guard let jsonData = cleanText.data(using: .utf8) else { throw GeminiError.invalidResponse }

        let result = try JSONDecoder().decode(MenuAnalysisResult.self, from: jsonData)

        // Clamp negative values in menu items
        if let items = result.items {
            let validatedItems = items.map { item in
                MenuItemResult(
                    name: item.name,
                    description: item.description,
                    calories: max(0, item.calories),
                    protein: item.protein.map { max(0, $0) },
                    carbs: item.carbs.map { max(0, $0) },
                    fat: item.fat.map { max(0, $0) }
                )
            }
            return MenuAnalysisResult(restaurantName: result.restaurantName, items: validatedItems, error: nil)
        }

        return result
    }
}

// MARK: - Mocks (Kept for fallback/testing)
final class MockEstimationService: EstimationService {
    enum Mode {
        case success
        case error
        case noFood
    }

    var mode: Mode

    init() {
        let args = ProcessInfo.processInfo.arguments
        if args.contains("--mock-estimation-error") {
            self.mode = .error
        } else if args.contains("--mock-estimation-no-food") {
            self.mode = .noFood
        } else {
            self.mode = .success
        }
    }

    func estimateCalories(images: [Data]) async throws -> EstimationResult {
        try await Task.sleep(nanoseconds: 1_000_000_000)
        switch mode {
        case .error:
            throw NSError(
                domain: "MockEstimationError",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Mock estimation error for UI tests"]
            )
        case .noFood:
            return EstimationResult(mealName: nil, items: [])
        case .success:
            return EstimationResult(mealName: "Mock Chicken and Rice", items: [
                EstimationItem(name: "Mock Chicken", quantity: "5 oz", calories: 250, confidence: 0.95),
                EstimationItem(name: "Mock Rice", quantity: "1 cup", calories: 200, confidence: 0.90)
            ])
        }
    }
}
