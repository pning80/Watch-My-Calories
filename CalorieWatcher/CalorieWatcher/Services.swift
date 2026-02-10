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
    
    // Explicit init for manual creation / mocks
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
    
    // Custom decoding to generate UUID locally since API doesn't provide it
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = UUID() // Generate local ID
        self.name = try container.decode(String.self, forKey: .name)
        self.quantity = try container.decode(String.self, forKey: .quantity)
        self.calories = try container.decode(Double.self, forKey: .calories)
        // Handle optional confidence, default to 0 if missing
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
    var items: [EstimationItem]
    var totalCalories: Double {
        items.reduce(0) { $0 + $1.calories }
    }
}

struct GeminiModel: Identifiable, Codable, Hashable {
    var id: String { name } // The API returns "models/gemini-1.5-flash" as name
    let name: String
    let displayName: String
    let description: String?
}

struct ModelListResponse: Codable {
    let models: [GeminiModel]
}

// MARK: - Protocols

protocol EstimationService {
    func estimateCalories(images: [Data], model: String, apiKey: String) async throws -> EstimationResult
    func fetchAvailableModels(apiKey: String) async throws -> [GeminiModel]
}

// MARK: - Gemini Implementation

enum GeminiError: Error, LocalizedError {
    case invalidAPIKey
    case invalidResponse
    case networkError(Error)
    
    var errorDescription: String? {
        switch self {
        case .invalidAPIKey: return "Invalid API Key. Please check settings."
        case .invalidResponse: return "Failed to parse response from Gemini."
        case .networkError(let error): return "Network error: \(error.localizedDescription)"
        }
    }
}

final class GeminiService: EstimationService {
    
    func fetchAvailableModels(apiKey: String) async throws -> [GeminiModel] {
        guard !apiKey.isEmpty else { throw GeminiError.invalidAPIKey }
        
        let urlString = "https://generativelanguage.googleapis.com/v1beta/models?key=\(apiKey)"
        guard let url = URL(string: urlString) else { throw GeminiError.networkError(URLError(.badURL)) }
        
        let (data, response) = try await URLSession.shared.data(from: url)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw GeminiError.networkError(URLError(.badServerResponse))
        }
        
        let listResponse = try JSONDecoder().decode(ModelListResponse.self, from: data)
        // Filter for Gemini models only
        return listResponse.models.filter { $0.name.contains("gemini") }
    }
    
    func estimateCalories(images: [Data], model: String, apiKey: String) async throws -> EstimationResult {
        guard !apiKey.isEmpty else { throw GeminiError.invalidAPIKey }
        
        // Handle model name (API returns models/gemini-x, but generateContent expects models/gemini-x:generateContent)
        // Usually we pass just the name. The API returns full resource name like "models/gemini-1.5-flash"
        // If the user selected just "gemini-1.5-flash", we need to adjust, but let's assume we use what fetch returns or the default.
        // The generateContent URL format is: https://generativelanguage.googleapis.com/v1beta/{model=models/*}:generateContent
        
        // Ensure model name doesn't double "models/" if already present
        let modelName = model.starts(with: "models/") ? model : "models/\(model)"
        
        let urlString = "https://generativelanguage.googleapis.com/v1beta/\(modelName):generateContent?key=\(apiKey)"
        guard let url = URL(string: urlString) else { throw GeminiError.networkError(URLError(.badURL)) }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Construct Request Body
        let prompt = """
        Analyze these food images. Identify the food items, estimate the portion size, and calculate the calories.
        Return ONLY a JSON object with this structure:
        {
          "items": [
            {
              "name": "Food Name",
              "quantity": "Estimated Quantity (e.g. 1 cup, 200g)",
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
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            // Attempt to decode error message
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                print("Gemini API Error: \(errorJson)")
            }
            throw GeminiError.networkError(URLError(.badServerResponse))
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
        
        // Extract JSON from text (sometimes Gemini adds markdown ```json blocks)
        let jsonString = text.replacingOccurrences(of: "```json", with: "").replacingOccurrences(of: "```", with: "")
        guard let jsonData = jsonString.data(using: .utf8) else { throw GeminiError.invalidResponse }
        
        return try JSONDecoder().decode(EstimationResult.self, from: jsonData)
    }
}

// MARK: - Mocks

final class MockEstimationService: EstimationService {
    func fetchAvailableModels(apiKey: String) async throws -> [GeminiModel] {
        return [
            GeminiModel(name: "models/gemini-1.5-flash", displayName: "Gemini 1.5 Flash", description: "Fast and versatile"),
            GeminiModel(name: "models/gemini-2.0-flash-exp", displayName: "Gemini 2.0 Flash", description: "Next gen speed")
        ]
    }
    
    func estimateCalories(images: [Data], model: String, apiKey: String) async throws -> EstimationResult {
        try await Task.sleep(nanoseconds: 1_000_000_000) // 1s delay
        return EstimationResult(items: [
            EstimationItem(name: "Grilled Chicken Breast", quantity: "150g", calories: 248, confidence: 0.95, protein: 46, carbs: 0, fat: 5),
            EstimationItem(name: "Brown Rice", quantity: "1 cup", calories: 216, confidence: 0.90, protein: 5, carbs: 45, fat: 1.8),
            EstimationItem(name: "Steamed Broccoli", quantity: "100g", calories: 34, confidence: 0.88, protein: 2.8, carbs: 7, fat: 0.4)
        ])
    }
}

