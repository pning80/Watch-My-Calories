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
    var items: [EstimationItem]
    var totalCalories: Double {
        items.reduce(0) { $0 + $1.calories }
    }
}

struct GeminiModel: Identifiable, Codable, Hashable {
    var id: String { name }
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
    case apiError(String)
    case networkError(Error)
    
    var errorDescription: String? {
        switch self {
        case .invalidAPIKey: return "Invalid API Key. Please check settings."
        case .invalidResponse: return "Failed to parse response from Gemini."
        case .apiError(let msg): return "Gemini API Error: \(msg)"
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
            // Attempt to decode error
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorObj = errorJson["error"] as? [String: Any],
               let message = errorObj["message"] as? String {
                throw GeminiError.apiError(message)
            }
            throw GeminiError.networkError(URLError(.badServerResponse))
        }
        
        let listResponse = try JSONDecoder().decode(ModelListResponse.self, from: data)
        return listResponse.models.filter { $0.name.contains("gemini") }
    }
    
    func estimateCalories(images: [Data], model: String, apiKey: String) async throws -> EstimationResult {
        guard !apiKey.isEmpty else { throw GeminiError.invalidAPIKey }
        
        // Ensure model name format is correct (must start with "models/")
        let modelName = model.starts(with: "models/") ? model : "models/\(model)"
        
        let urlString = "https://generativelanguage.googleapis.com/v1beta/\(modelName):generateContent?key=\(apiKey)"
        guard let url = URL(string: urlString) else { throw GeminiError.networkError(URLError(.badURL)) }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Construct Request Body
        // Prompt refined to ensure strict JSON
        let prompt = """
        Analyze these food images. Identify the food items, estimate the portion size, and calculate the calories.
        Return ONLY a raw JSON object (no markdown, no code blocks) with this structure:
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
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw GeminiError.networkError(URLError(.badServerResponse))
        }
        
        guard httpResponse.statusCode == 200 else {
            // Detailed Error Handling
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorObj = errorJson["error"] as? [String: Any],
               let message = errorObj["message"] as? String {
                print("Gemini API Error: \(message)") // Log to console
                throw GeminiError.apiError(message)
            }
            let bodyString = String(data: data, encoding: .utf8) ?? "No body"
            print("Gemini API Error (Status \(httpResponse.statusCode)): \(bodyString)")
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
    func fetchAvailableModels(apiKey: String) async throws -> [GeminiModel] {
        return [
            GeminiModel(name: "models/gemini-1.5-flash", displayName: "Gemini 1.5 Flash", description: nil),
            GeminiModel(name: "models/gemini-2.0-flash-exp", displayName: "Gemini 2.0 Flash", description: nil)
        ]
    }
    
    func estimateCalories(images: [Data], model: String, apiKey: String) async throws -> EstimationResult {
        try await Task.sleep(nanoseconds: 1_000_000_000)
        return EstimationResult(items: [
            EstimationItem(name: "Mock Chicken", quantity: "150g", calories: 250, confidence: 0.95),
            EstimationItem(name: "Mock Rice", quantity: "1 cup", calories: 200, confidence: 0.90)
        ])
    }
}

