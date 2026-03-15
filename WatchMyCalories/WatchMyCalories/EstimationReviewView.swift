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
    @State private var showDetails: Bool = false
    @State private var showConsentSheet: Bool = false

    // Auto-save state
    @State private var isSaved: Bool = false

    // Ad + deferred results state
    @StateObject private var adLoader = NativeAdLoader()
    @State private var estimationComplete: Bool = false
    @State private var pendingResult: EstimationResult? = nil
    @State private var pendingError: String? = nil

    var body: some View {
        Group {
            if isLoading {
                VStack(spacing: 0) {
                    // Top: App heading
                    HStack(spacing: 12) {
                        Image("MiniAppIcon")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 32, height: 32)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                        Text("Watch My Calories")
                            .font(.system(.title3, design: .serif, weight: .bold))
                            .foregroundStyle(Color.cwPrimary)
                    }
                    .padding(.top, 24)
                    .padding(.bottom, 20)

                    // Middle: Native ad area
                    if let nativeAd = adLoader.nativeAd {
                        NativeAdContentView(nativeAd: nativeAd)
                            .frame(maxWidth: .infinity)
                            .frame(minHeight: 250)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 4)
                            .padding(.horizontal)
                            .accessibilityIdentifier(AccessibilityID.Ads.native)
                    } else if !AdManager.isUITestingMode && AdManager.shared.canRequestAds {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color.cwSurface)
                            .frame(maxWidth: .infinity)
                            .frame(height: 250)
                            .overlay(
                                ProgressView()
                                    .tint(Color.gray.opacity(0.5))
                            )
                            .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 4)
                            .padding(.horizontal)
                    }

                    Spacer()

                    // Bottom: Progress / View Results / Inline Error
                    VStack(spacing: 10) {
                        if !estimationComplete {
                            ProgressView()
                                .controlSize(.large)
                                .tint(Color.cwPrimary)
                            Text("Analyzing food...")
                                .font(.headline)
                                .foregroundStyle(Color.gray)
                        } else if pendingError != nil {
                            // Inline error UI
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .font(.system(size: 24))
                                    .foregroundStyle(Color.orange)

                                Text("Analysis Failed")
                                    .font(.headline)
                                    .foregroundStyle(Color.cwTextPrimary)
                            }

                            Text("We couldn't analyze the image. Please try again.")
                                .font(.subheadline)
                                .multilineTextAlignment(.center)
                                .foregroundStyle(Color.gray)

                            if showDetails, let errorText = pendingError {
                                ScrollView {
                                    Text(errorText)
                                        .font(.caption)
                                        .fontDesign(.monospaced)
                                        .foregroundStyle(Color.red)
                                        .padding()
                                        .background(Color.red.opacity(0.1))
                                        .cornerRadius(8)
                                }
                                .frame(maxHeight: 120)
                                .padding(.horizontal)
                            }

                            Button(showDetails ? "Hide Details" : "Show Details") {
                                withAnimation { showDetails.toggle() }
                            }
                            .font(.footnote)
                            .foregroundStyle(Color.blue)

                            HStack(spacing: 16) {
                                Button("Try Again") { Task { await estimate() } }
                                    .buttonStyle(.borderedProminent)
                                    .tint(Color.cwPrimary)
                                    .accessibilityIdentifier(AccessibilityID.EstimationReview.tryAgainButton)

                                Button("Cancel") { dismiss() }
                                    .foregroundStyle(Color.gray)
                                    .accessibilityIdentifier(AccessibilityID.EstimationReview.cancelButton)
                            }
                        } else {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 40))
                                .foregroundStyle(Color.cwPrimary)
                            Text("Analysis complete!")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            Button {
                                result = pendingResult
                                isLoading = false
                            } label: {
                                Text("View Results")
                                    .font(.headline)
                                    .foregroundStyle(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(Color.cwPrimary)
                                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            }
                            .padding(.horizontal, 40)
                            .accessibilityIdentifier(AccessibilityID.Ads.viewResultsButton)
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                        }
                    }
                    .frame(minHeight: 140)
                    .padding(.bottom, 40)
                    .animation(.easeInOut(duration: 0.3), value: estimationComplete)
                }
                .accessibilityIdentifier(AccessibilityID.EstimationReview.loadingView)
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
                                .accessibilityIdentifier(AccessibilityID.EstimationReview.tryAgainButton)

                            Button("Cancel") {
                                onDone()
                                dismiss()
                            }
                                .foregroundStyle(Color.gray)
                                .accessibilityIdentifier(AccessibilityID.EstimationReview.cancelButton)
                        }
                        .padding(.top, 8)
                    }
                    .padding()
                    .accessibilityIdentifier(AccessibilityID.EstimationReview.noFoodView)
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
                        .accessibilityIdentifier(AccessibilityID.EstimationReview.doneButton)
                    }
                    .padding()
                    .transition(.opacity)
                    .accessibilityIdentifier(AccessibilityID.EstimationReview.successView)
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .navigationBarBackButtonHidden(true)
        .task {
            if AdManager.shared.canRequestAds {
                adLoader.loadAd()
            }

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
        pendingError = nil
        estimationComplete = false
        showDetails = false
        do {
            let estimation = try await env.estimationService.estimateCalories(images: images)

            await MainActor.run {
                self.pendingResult = estimation
                if !estimation.items.isEmpty {
                    saveToHistory(estimation)
                    self.isSaved = true
                }
                self.estimationComplete = true
            }
        } catch {
            await MainActor.run {
                self.pendingError = error.localizedDescription
                self.estimationComplete = true
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



