import SwiftUI
import UIKit // Required for UIImage
import CoreData

struct ResultView: View {
    let images: [UIImage]
    @Environment(\.dismiss) var dismiss
    @Environment(\.managedObjectContext) private var viewContext
    
    @StateObject private var geminiService = GeminiService()
    @ObservedObject var healthKitService: HealthKitService
    
    @State private var foodName: String = "Analyzing..."
    @State private var calories: String = "0"
    @State private var protein: String = "0"
    @State private var carbs: String = "0"
    @State private var fat: String = "0"
    @State private var description: String = "Please wait while we identify the food."
    @State private var isAnalyzing = true
    @State private var confidence: String = ""
    
    var onSave: () -> Void
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Image Carousel
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(images, id: \.self) { img in
                            Image(uiImage: img)
                                .resizable()
                                .scaledToFill()
                                .frame(width: 200, height: 200)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                    .padding()
                }
                
                if isAnalyzing {
                    VStack {
                        ProgressView()
                            .scaleEffect(1.5)
                            .tint(.mainGreen)
                        Text("Gemini is analyzing your food...")
                            .font(.caption)
                            .foregroundColor(.gray)
                            .padding(.top)
                    }
                    .frame(height: 100)
                } else {
                    // Result Card
                    VStack(alignment: .leading, spacing: 16) {
                        if !confidence.isEmpty {
                            Text(confidence)
                                .font(.caption)
                                .fontWeight(.bold)
                                .padding(6)
                                .background(Color.mainGreen.opacity(0.2))
                                .foregroundColor(.mainGreen)
                                .cornerRadius(8)
                        }
                        
                        TextField("Food Name", text: $foodName)
                            .font(.title).fontWeight(.bold)
                            .foregroundColor(.white)
                        
                        Divider().background(Color.gray.opacity(0.3))
                        
                        HStack(spacing: 20) {
                            MacroView(label: "KCAL", value: $calories, color: .mainGreen)
                            MacroView(label: "PROTEIN", value: $protein, color: .orange)
                            MacroView(label: "CARBS", value: $carbs, color: .blue)
                            MacroView(label: "FAT", value: $fat, color: .purple)
                        }
                        
                        Divider().background(Color.gray.opacity(0.3))
                        
                        Text(description)
                            .font(.body)
                            .foregroundColor(.gray)
                    }
                    .padding()
                    .background(Color.white.opacity(0.05))
                    .cornerRadius(24)
                    .padding(.horizontal)
                    
                    Button(action: saveLog) {
                        Text("Add to Log")
                            .font(.headline)
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.mainGreen)
                            .cornerRadius(100)
                    }
                    .padding()
                }
            }
        }
        .background(Color.black.edgesIgnoringSafeArea(.all))
        .onAppear {
            analyze()
        }
    }
    
    func analyze() {
        Task {
            do {
                let result = try await geminiService.analyzeImages(images)
                DispatchQueue.main.async {
                    self.foodName = result.foodName
                    self.calories = String(result.calories)
                    self.protein = String(result.protein)
                    self.carbs = String(result.carbs)
                    self.fat = String(result.fat)
                    self.confidence = "\(result.confidence) Confidence"
                    self.description = result.reasoning
                    self.isAnalyzing = false
                }
            } catch {
                DispatchQueue.main.async {
                    self.description = "Error: \(error.localizedDescription)\nCheck API Key in Settings."
                    self.isAnalyzing = false
                }
            }
        }
    }
    
    func saveLog() {
        // Core Data
        let newItem = FoodLog(context: viewContext)
        newItem.id = UUID()
        newItem.timestamp = Date()
        newItem.name = foodName
        newItem.calories = Int64(calories) ?? 0
        newItem.protein = Int64(protein) ?? 0
        newItem.carbs = Int64(carbs) ?? 0
        newItem.fat = Int64(fat) ?? 0
        newItem.note = description
        
        // Save images locally
        var paths: [String] = []
        for img in images {
            if let data = img.jpegData(compressionQuality: 0.8) {
                let filename = UUID().uuidString + ".jpg"
                let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(filename)
                try? data.write(to: url)
                paths.append(filename)
            }
        }
        // Simple JSON encoding for array of strings
        if let data = try? JSONEncoder().encode(paths), let str = String(data: data, encoding: .utf8) {
            newItem.imagePaths = str
        }

        do {
            try viewContext.save()
            // Write to HealthKit
            healthKitService.saveFood(
                calories: Double(newItem.calories) ?? 0, // FIXED: Handle optional defaults safely
                protein: Double(newItem.protein),
                carbs: Double(newItem.carbs),
                fat: Double(newItem.fat)
            )
            onSave()
        } catch {
            print("Error saving: \(error)")
        }
    }
}

struct MacroView: View {
    let label: String
    @Binding var value: String
    let color: Color
    
    var body: some View {
        VStack {
            TextField("0", text: $value)
                .font(.title2).fontWeight(.bold)
                .foregroundColor(color)
                .multilineTextAlignment(.center)
                .keyboardType(.numberPad)
            Text(label)
                .font(.caption)
                .foregroundColor(.gray)
        }
    }
}
