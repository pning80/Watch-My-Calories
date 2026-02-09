import Foundation
import UIKit

class GeminiService {
    private let endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent"
    
    func analyzeFood(images: [UIImage], userNotes: String?) async throws -> FoodAnalysisResult {
        guard let apiKey = KeychainService.load() else {
            throw GeminiError.missingAPIKey
        }
        
        var request = URLRequest(url: URL(string: "\(endpoint)?key=\(apiKey)")!)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let prompt = """
        You are an expert nutritionist using Gemini 2.0 Vision. Analyze these multiple frames of the same meal.
        1. **Reconstruct**: Use the different angles to infer 3D volume.
        2. **Refine**: Identify ingredients obscured in one angle but visible in another.
        3. **Output**: Detailed JSON with caloric breakdown and confidence.
        JSON Schema:
        {
          "foodItems": [
            {
              "name": "String",
              "calories": Int,
              "protein": Double,
              "carbs": Double,
              "fat": Double,
              "quantity": "String (e.g. 200g)",
              "confidence": "High/Low",
              "reasoning": "String"
            }
          ],
          "totalCalories": Int
        }
        """
        
        let imageParts = images.compactMap { image -> [String: Any]? in
            guard let data = image.jpegData(compressionQuality: 0.8) else { return nil }
            return [
                "inline_data": [
                    "mime_type": "image/jpeg",
                    "data": data.base64EncodedString()
                ]
            ]
        }
        
        let body: [String: Any] = [
            "contents": [
                [
                    "parts": [
                        ["text": prompt + (userNotes.map { "\nUser Notes: \($0)" } ?? "")]
                    ] + imageParts
                ]
            ]
        ]
        
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw GeminiError.apiError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? -1)
        }
        
        return try parseResponse(data)
    }
    
    private func parseResponse(_ data: Data) throws -> FoodAnalysisResult {
        // Simple JSON parsing logic here (omitted for brevity, assume mapped to model)
        // In a real app, use Codable with the specific Gemini response structure (candidates -> content -> parts -> text)
        // Then extract the JSON block from the text.
        return FoodAnalysisResult.mock // Placeholder
    }
}

enum GeminiError: Error {
    case missingAPIKey
    case apiError(statusCode: Int)
    case parsingError
}

struct FoodAnalysisResult: Codable {
    let foodItems: [FoodItemModel]
    let totalCalories: Int
    
    static let mock = FoodAnalysisResult(foodItems: [], totalCalories: 0)
}

struct FoodItemModel: Codable, Identifiable {
    var id = UUID()
    let name: String
    let calories: Int
    let protein: Double
    let carbs: Double
    let fat: Double
    let quantity: String
    let confidence: String // "High", "Low"
}
