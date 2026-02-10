import Foundation

// MARK: - Gemini API Service

actor GeminiEstimationService: EstimationService {
    func estimateCalories(images: [Data], model: String, apiKey: String) async throws -> EstimationResult {
        guard !apiKey.isEmpty else {
            throw GeminiError.missingAPIKey
        }
        
        let urlString = "https://generativelanguage.googleapis.com/v1beta/models/\(model):generateContent?key=\(apiKey)"
        guard let url = URL(string: urlString) else {
            throw GeminiError.invalidURL
        }

        // Construct the multipart request manually or use a simple JSON payload if images are base64 encoded.
        // For simplicity and reliability with standard JSON APIs, we'll base64 encode the images.
        
        let prompt = """
        Analyze these images of food. Identify each distinct food item, estimate its quantity (e.g., cups, grams, pieces), and estimate the calories.
        Return ONLY a JSON object with this structure:
        {
          "items": [
            { "name": "Food Name", "quantity": "estimated amount", "calories": 123.0, "confidence": 0.9 }
          ]
        }
        Do not include markdown formatting like ```json. Just the raw JSON.
        """
        
        var parts: [[String: Any]] = [
            ["text": prompt]
        ]
        
        for imageData in images {
            let base64 = imageData.base64EncodedString()
            parts.append([
                "inline_data": [
                    "mime_type": "image/jpeg",
                    "data": base64
                ]
            ])
        }
        
        let requestBody: [String: Any] = [
            "contents": [
                ["parts": parts]
            ]
        ]
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            // Try to parse error message from body
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let errorObj = errorJson["error"] as? [String: Any],
               let message = errorObj["message"] as? String {
                throw GeminiError.apiError(message)
            }
            throw GeminiError.serverError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? 0)
        }
        
        return try parseGeminiResponse(data)
    }
    
    private func parseGeminiResponse(_ data: Data) throws -> EstimationResult {
        // Decode the outer Gemini response structure
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
        
        // Clean up markdown if present
        let cleanJson = text
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
            
        guard let jsonData = cleanJson.data(using: .utf8) else {
            throw GeminiError.invalidResponse
        }
        
        return try JSONDecoder().decode(EstimationResult.self, from: jsonData)
    }
}

enum GeminiError: LocalizedError {
    case missingAPIKey
    case invalidURL
    case serverError(statusCode: Int)
    case apiError(String)
    case invalidResponse
    
    var errorDescription: String? {
        switch self {
        case .missingAPIKey: return "Please set your Gemini API Key in Settings."
        case .invalidURL: return "Invalid API URL."
        case .serverError(let code): return "Server returned error code: \(code)"
        case .apiError(let msg): return "Gemini API Error: \(msg)"
        case .invalidResponse: return "Could not understand the AI's response."
        }
    }
}
