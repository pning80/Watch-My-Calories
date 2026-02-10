import Foundation
import UIKit
import Combine

class GeminiService: ObservableObject {
    @Published var apiKey: String = UserDefaults.standard.string(forKey: "GeminiAPIKey") ?? "" {
        didSet {
            UserDefaults.standard.set(apiKey, forKey: "GeminiAPIKey")
        }
    }
    
    @Published var availableModels: [String] = []
    @Published var selectedModel: String = UserDefaults.standard.string(forKey: "GeminiSelectedModel") ?? "" {
        didSet {
            UserDefaults.standard.set(selectedModel, forKey: "GeminiSelectedModel")
        }
    }
    
    struct AnalysisResult: Codable {
        let foodName: String
        let calories: Int
        let protein: Int
        let carbs: Int
        let fat: Int
        let confidence: String
        let reasoning: String
    }
    
    // Response structures for Gemini API
    struct GeminiResponse: Codable {
        let candidates: [Candidate]?
    }
    
    struct Candidate: Codable {
        let content: ContentResponse?
    }
    
    struct ContentResponse: Codable {
        let parts: [PartResponse]?
    }
    
    struct PartResponse: Codable {
        let text: String?
    }
    
    // Models API Response
    struct ModelListResponse: Codable {
        let models: [ModelInfo]?
    }
    
    struct ModelInfo: Codable {
        let name: String
        let displayName: String?
        let supportedGenerationMethods: [String]?
    }
    
    func fetchModels() async {
        guard !apiKey.isEmpty else { return }
        
        let urlString = "https://generativelanguage.googleapis.com/v1beta/models?key=\(apiKey)"
        guard let url = URL(string: urlString) else { return }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(ModelListResponse.self, from: data)
            
            let models = response.models?
                .filter { $0.supportedGenerationMethods?.contains("generateContent") == true }
                .map { $0.name.replacingOccurrences(of: "models/", with: "") } ?? []
            
            DispatchQueue.main.async {
                self.availableModels = models
                
                // Set default if not selected or invalid
                if self.selectedModel.isEmpty || !models.contains(self.selectedModel) {
                     // Prefer flash if available, otherwise first
                    if let flash = models.first(where: { $0.contains("flash") }) {
                        self.selectedModel = flash
                    } else if let first = models.first {
                        self.selectedModel = first
                    }
                }
            }
        } catch {
            print("Error fetching models: \(error)")
        }
    }
    
    func analyzeImages(_ images: [UIImage]) async throws -> AnalysisResult {
        guard !apiKey.isEmpty else {
            throw NSError(domain: "GeminiService", code: 401, userInfo: [NSLocalizedDescriptionKey: "API Key not configured."])
        }
        
        // Use selected model or fallback
        let modelToUse = selectedModel.isEmpty ? "gemini-1.5-flash" : selectedModel
        
        let urlString = "https://generativelanguage.googleapis.com/v1beta/models/\(modelToUse):generateContent?key=\(apiKey)"
        guard let url = URL(string: urlString) else {
            throw NSError(domain: "GeminiService", code: 400, userInfo: [NSLocalizedDescriptionKey: "Invalid URL."])
        }
        
        // Prepare Request
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Prepare Parts
        var parts: [[String: Any]] = [
            ["text": """
            Analyze these images of a meal. Identify the food items. 
            Estimate the total calories and macronutrients (protein, carbs, fat). 
            Return the result in JSON format: 
            { 
                "foodName": String, 
                "calories": Int, 
                "protein": Int, 
                "carbs": Int, 
                "fat": Int, 
                "confidence": "Low|Medium|High", 
                "reasoning": String 
            }
            
            Provide only the JSON. Do not include markdown formatting.
            """]
        ]
        
        for image in images {
            if let jpegData = image.jpegData(compressionQuality: 0.8) {
                let base64 = jpegData.base64EncodedString()
                let imagePart: [String: Any] = [
                    "inline_data": [
                        "mime_type": "image/jpeg",
                        "data": base64
                    ]
                ]
                parts.append(imagePart)
            }
        }
        
        let requestBody: [String: Any] = [
            "contents": [
                [
                    "parts": parts
                ]
            ]
        ]
        
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorMsg = String(data: data, encoding: .utf8) ?? "Unknown Error"
            throw NSError(domain: "GeminiService", code: 500, userInfo: [NSLocalizedDescriptionKey: "API Error: \(errorMsg). Model: \(modelToUse)"])
        }
        
        let geminiResponse = try JSONDecoder().decode(GeminiResponse.self, from: data)
        guard let text = geminiResponse.candidates?.first?.content?.parts?.first?.text else {
            throw NSError(domain: "GeminiService", code: 500, userInfo: [NSLocalizedDescriptionKey: "No text in response."])
        }
        
        // Cleanup response
        let cleanText = text.replacingOccurrences(of: "```json", with: "").replacingOccurrences(of: "```", with: "")
        
        guard let jsonData = cleanText.data(using: .utf8) else {
             throw NSError(domain: "GeminiService", code: 500, userInfo: [NSLocalizedDescriptionKey: "Failed to convert response to data."])
        }
        
        let result = try JSONDecoder().decode(AnalysisResult.self, from: jsonData)
        return result
    }
}
