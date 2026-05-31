import SwiftUI
import SwiftData
import CoreLocation

struct MenuAnalysisView: View {
    @EnvironmentObject private var env: AppEnvironment
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    let imageData: Data
    var onDone: () -> Void = {}
    var onScanAgain: () -> Void = {}

    @ObservedObject private var store = SettingsStore.shared

    @State private var result: MenuAnalysisResult?
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showDetails = false
    @State private var showConsentSheet = false
    @State private var analysisTask: Task<Void, Never>?

    // Ad state
    @StateObject private var adLoader = NativeAdLoader()

    private var isRateLimited: Bool {
        errorMessage?.starts(with: "Too many requests") ?? false
    }

    var body: some View {
        Group {
            if isLoading {
                loadingView
            } else if let error = result?.error, error == "not_a_menu" {
                notAMenuView
            } else if let errorMessage {
                errorView(errorMessage)
            } else if let items = result?.items, !items.isEmpty {
                successView(items: items)
            } else {
                errorView("No items found in the menu.")
            }
        }
        .navigationBarBackButtonHidden(true)
        .onDisappear {
            analysisTask?.cancel()
            analysisTask = nil
        }
        .task {
            if AdManager.shared.canRequestAds {
                adLoader.loadAd()
            }

            switch store.aiConsent {
            case .accepted:
                analysisTask = Task { await analyze() }
            case .notAsked, .declined:
                showConsentSheet = true
            }
        }
        .sheet(isPresented: $showConsentSheet) {
            AIConsentSheet(
                onAccept: {
                    store.saveAIConsent(.accepted)
                    showConsentSheet = false
                    isLoading = true
                    analysisTask?.cancel()
                    analysisTask = Task { await analyze() }
                },
                onDecline: {
                    store.saveAIConsent(.declined)
                    showConsentSheet = false
                    onDone()
                }
            )
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 0) {
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

            // Native ad area
            if let nativeAd = adLoader.nativeAd {
                NativeAdContentView(nativeAd: nativeAd)
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: 250)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 4)
                    .padding(.horizontal)
            } else if !AdManager.isUITestingMode && AdManager.shared.canRequestAds {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color.cwSurface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 250)
                    .overlay(ProgressView().tint(Color.gray.opacity(0.5)))
                    .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 4)
                    .padding(.horizontal)
            }

            Spacer()

            VStack(spacing: 10) {
                ProgressView()
                    .scaleEffect(1.2)
                Text("Analyzing menu...")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(.bottom, 60)

            Spacer()
        }
    }

    // MARK: - Success

    private func successView(items: [MenuItemResult]) -> some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 16) {
                    // Header
                    HStack(spacing: 8) {
                        Image(systemName: "fork.knife")
                            .font(.title2)
                            .foregroundStyle(Color.cwPrimary)
                        Text("Menu Analysis")
                            .font(.system(.title2, design: .serif, weight: .bold))
                            .foregroundStyle(Color.cwPrimary)
                    }
                    .padding(.top, 20)

                    // Restaurant name
                    if let name = result?.restaurantName {
                        Text("Looks like \(name)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }

                    // Items
                    LazyVStack(spacing: 12) {
                        ForEach(items) { item in
                            menuItemCard(item)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 16)
                }
            }

            // Fixed bottom buttons
            HStack(spacing: 12) {
                Button(action: onScanAgain) {
                    Text("Scan Again")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(Color.cwPrimary)
                        .padding(.vertical, 10)
                        .frame(maxWidth: .infinity)
                        .background(Color.cwPrimary.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }

                Button(action: onDone) {
                    Text("Done")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(.white)
                        .padding(.vertical, 10)
                        .frame(maxWidth: .infinity)
                        .background(Color.cwPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 10)
            .background(Color.cwBackground)
        }
        .background(Color.cwBackground)
    }

    private func menuItemCard(_ item: MenuItemResult) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(item.name)
                    .font(.headline)
                Spacer()
                Text("~\(Int(item.calories)) cal")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.cwPrimary)
            }

            if let desc = item.description, !desc.isEmpty {
                Text(desc)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 16) {
                if let p = item.protein { macroLabel("Protein", value: p) }
                if let c = item.carbs { macroLabel("Carbs", value: c) }
                if let f = item.fat { macroLabel("Fat", value: f) }
            }
            .padding(.top, 4)
        }
        .padding()
        .background(Color.cwSurface)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
    }

    private func macroLabel(_ label: String, value: Double) -> some View {
        VStack(spacing: 2) {
            Text("\(Int(value))g")
                .font(.caption)
                .fontWeight(.semibold)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Not a Menu

    private var notAMenuView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "doc.questionmark")
                .font(.system(size: 64))
                .foregroundStyle(Color.cwPrimary.opacity(0.6))

            Text("Not a Menu")
                .font(.title2)
                .fontWeight(.bold)

            Text("This doesn't appear to be a restaurant menu.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: onScanAgain) {
                Text("Try Again")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.cwPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .padding(.horizontal, 40)

            Spacer()
        }
    }

    // MARK: - Error

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 64))
                .foregroundStyle(.orange)

            Text("Analysis Failed")
                .font(.title2)
                .fontWeight(.bold)

            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            if showDetails, let fullError = errorMessage {
                Text(fullError)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding()
                    .background(Color.cwSurface)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding(.horizontal)
            }

            Button("Show Details") {
                showDetails.toggle()
            }
            .font(.caption)
            .foregroundStyle(Color.cwPrimary)

            HStack(spacing: 16) {
                Button(action: {
                    isLoading = true
                    errorMessage = nil
                    result = nil
                    analysisTask?.cancel()
                    analysisTask = Task { await analyze() }
                }) {
                    Text("Try Again")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.cwPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }

                Button(action: onDone) {
                    Text("Cancel")
                        .font(.headline)
                        .foregroundStyle(Color.cwPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.cwPrimary.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
            }
            .padding(.horizontal, 40)

            Spacer()
        }
    }

    // MARK: - Analysis Logic

    private func analyze() async {
        isLoading = true
        errorMessage = nil
        result = nil

        // Fetch location with timeout
        let locationResult = await withTimeout(seconds: 3) {
            await LocationManager.shared.getCurrentLocation()
        }

        let location = locationResult?.location
        let locality = locationResult?.locality

        do {
            let analysisResult = try await env.menuAnalysisService.analyzeMenu(
                image: imageData,
                location: location,
                locality: locality
            )
            guard !Task.isCancelled else { return }

            self.result = analysisResult
            self.isLoading = false

            // Auto-save if recognized as a menu
            if analysisResult.error == nil, let items = analysisResult.items, !items.isEmpty {
                saveMenuScan(analysisResult)
            }
        } catch {
            guard !Task.isCancelled else { return }
            self.errorMessage = error.localizedDescription
            self.isLoading = false
        }
    }

    private func saveMenuScan(_ result: MenuAnalysisResult) {
        let imageID = UUID()
        _ = ImageStorage.shared.save(imageData, id: imageID)

        let scan = MenuScan(
            restaurantName: result.restaurantName,
            imageID: imageID,
            timestamp: Date(),
            items: result.items ?? []
        )
        modelContext.insert(scan)
    }

    private func withTimeout<T>(seconds: Double, operation: @escaping () async -> T) async -> T? {
        await withTaskGroup(of: T?.self) { group in
            group.addTask { await operation() }
            group.addTask {
                try? await Task.sleep(for: .seconds(seconds))
                return nil
            }

            if let first = await group.next() {
                group.cancelAll()
                return first
            }
            return nil
        }
    }
}
