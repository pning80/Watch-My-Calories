import SwiftUI
import SwiftData

struct EstimationReviewView: View {
    @EnvironmentObject private var env: AppEnvironment
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    let images: [Data]

    @State private var result: EstimationResult? = nil
    @State private var isLoading: Bool = true
    @State private var errorMessage: String? = nil
    
    // Auto-save state
    @State private var isSaved: Bool = false

    var body: some View {
        Group {
            if isLoading {
                VStack(spacing: 20) {
                    ProgressView()
                        .controlSize(.large)
                        .tint(Color.cwPrimary)
                    Text("Analyzing Food...")
                        .font(.headline)
                        .foregroundStyle(Color.gray)
                }
            } else if let errorMessage {
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundStyle(Color.orange)
                    Text("Analysis Failed")
                        .font(.headline)
                        .foregroundStyle(Color.cwTextPrimary)
                    Text(errorMessage)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(Color.gray)
                        .padding()
                    Button("Try Again") { Task { await estimate() } }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.cwPrimary)
                }
            } else if let result {
                VStack(spacing: 24) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 64))
                        .foregroundStyle(Color.cwPrimary)
                        .transition(.scale.combined(with: .opacity))
                    
                    Text("Logged Successfully!")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundStyle(Color.cwTextPrimary)
                    
                    VStack(spacing: 16) {
                        ForEach(result.items.indices, id: \.self) { idx in
                            HStack {
                                Text(result.items[idx].name)
                                    .font(.headline)
                                Spacer()
                                Text("\(Int(result.items[idx].calories)) kcal")
                                    .fontWeight(.bold)
                                    .foregroundStyle(Color.cwPrimary)
                            }
                            .padding()
                            .background(Color.cwSurface)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
                        }
                        
                        Divider()
                        
                        HStack {
                            Text("Total Added")
                                .font(.headline)
                            Spacer()
                            Text("\(Int(result.totalCalories)) kcal")
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundStyle(Color.cwAccent)
                        }
                        .padding(.top, 8)
                    }
                    .padding(.horizontal)
                    
                    Button("Done") {
                        dismiss()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.cwPrimary)
                    .padding(.top)
                }
                .padding()
                .transition(.opacity)
            }
        }
        .navigationTitle("Review")
        .navigationBarBackButtonHidden(true) // Prevent going back during auto-process
        .task { await estimate() }
    }

    private func estimate() async {
        isLoading = true
        errorMessage = nil
        do {
            let store = SettingsStore.shared
            let estimation = try await env.estimationService.estimateCalories(images: images, model: store.selectedModel, apiKey: store.apiKey)
            
            // Artificial delay for smooth UX transition if API is too fast
            try? await Task.sleep(nanoseconds: 500_000_000)
            
            await MainActor.run {
                self.result = estimation
                self.isLoading = false
                
                // Auto-save immediately
                saveToHistory(estimation)
                self.isSaved = true
                
                // Optional: Auto-dismiss after a few seconds?
                // For now, let user click "Done" to acknowledge the result.
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
    
    private func saveToHistory(_ result: EstimationResult) {
        for item in result.items {
            let entry = FoodEntry(
                name: item.name,
                calories: item.calories,
                quantity: item.quantity,
                timestamp: Date(),
                protein: item.protein,
                carbs: item.carbs,
                fat: item.fat
            )
            modelContext.insert(entry)
        }
    }
}

