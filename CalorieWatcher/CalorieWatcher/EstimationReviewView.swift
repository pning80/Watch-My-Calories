import SwiftUI
import SwiftData

struct EstimationReviewView: View {
    @EnvironmentObject private var env: AppEnvironment
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    let images: [Data]
    let captureDate: Date
    var onDone: () -> Void = {}

    @ObservedObject private var store = SettingsStore.shared

    @State private var result: EstimationResult? = nil
    @State private var isLoading: Bool = true
    @State private var errorMessage: String? = nil
    @State private var showDetails: Bool = false // State for error details
    @State private var showConsentSheet: Bool = false

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
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 60))
                        .foregroundStyle(Color.orange)
                        .padding()
                    
                    Text("Analysis Failed")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundStyle(Color.cwTextPrimary)
                    
                    Text("We couldn't analyze the image. Please try again.")
                        .multilineTextAlignment(.center)
                        .foregroundStyle(Color.gray)
                    
                    if showDetails {
                        ScrollView {
                            Text(errorMessage)
                                .font(.caption)
                                .fontDesign(.monospaced)
                                .foregroundStyle(Color.red)
                                .padding()
                                .background(Color.red.opacity(0.1))
                                .cornerRadius(8)
                        }
                        .frame(maxHeight: 150)
                    }
                    
                    Button(showDetails ? "Hide Details" : "Show Details") {
                        withAnimation {
                            showDetails.toggle()
                        }
                    }
                    .font(.footnote)
                    .foregroundStyle(Color.blue)
                    .padding(.bottom, 8)
                    
                    VStack(spacing: 12) {
                        Button("Try Again") { Task { await estimate() } }
                            .buttonStyle(.borderedProminent)
                            .tint(Color.cwPrimary)
                        
                        Button("Cancel") {
                            dismiss()
                        }
                        .foregroundStyle(Color.gray)
                    }
                }
                .padding()
            } else if let result {
                if result.items.isEmpty {
                    // No food detected
                    VStack(spacing: 16) {
                        Image(systemName: "fork.knife.circle")
                            .font(.system(size: 64))
                            .foregroundStyle(Color.gray)

                        Text("No Food Detected")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundStyle(Color.cwTextPrimary)

                        Text("We couldn't identify any food items in this photo. Try taking a clearer photo.")
                            .multilineTextAlignment(.center)
                            .foregroundStyle(Color.gray)

                        VStack(spacing: 12) {
                            Button("Try Again") { dismiss() }
                                .buttonStyle(.borderedProminent)
                                .tint(Color.cwPrimary)

                            Button("Cancel") {
                                onDone()
                                dismiss()
                            }
                                .foregroundStyle(Color.gray)
                        }
                        .padding(.top, 8)
                    }
                    .padding()
                } else {
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
                                        .foregroundStyle(Color.cwTextPrimary)
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
                                    .foregroundStyle(Color.cwTextPrimary)
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
                            onDone()
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
        }
        .navigationTitle("Review")
        .navigationBarBackButtonHidden(true)
        .task {
            switch store.aiConsent {
            case .accepted:
                await estimate()
            case .notAsked:
                showConsentSheet = true
            case .declined:
                showConsentSheet = true
            }
        }
        .sheet(isPresented: $showConsentSheet) {
            AIConsentSheet(
                onAccept: {
                    store.saveAIConsent(.accepted)
                    showConsentSheet = false
                    isLoading = true
                    Task { await estimate() }
                },
                onDecline: {
                    store.saveAIConsent(.declined)
                    showConsentSheet = false
                    dismiss()
                }
            )
        }
    }

    private func estimate() async {
        isLoading = true
        errorMessage = nil
        showDetails = false
        do {
            let estimation = try await env.estimationService.estimateCalories(images: images)
            
            try? await Task.sleep(nanoseconds: 500_000_000)
            
            await MainActor.run {
                self.result = estimation
                self.isLoading = false
                if !estimation.items.isEmpty {
                    saveToHistory(estimation)
                    self.isSaved = true
                }
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
    
    private func saveToHistory(_ result: EstimationResult) {
        var savedImageID: UUID? = nil
        if let firstImageData = images.first {
            let id = UUID()
            ImageStorage.shared.save(firstImageData, id: id)
            savedImageID = id
        }
        
        for item in result.items {
            let entry = FoodEntry(
                name: item.name,
                calories: item.calories,
                quantity: item.quantity,
                timestamp: captureDate,
                protein: item.protein,
                carbs: item.carbs,
                fat: item.fat,
                imageID: savedImageID,
                mealName: result.mealName
            )
            modelContext.insert(entry)
        }
    }
}

