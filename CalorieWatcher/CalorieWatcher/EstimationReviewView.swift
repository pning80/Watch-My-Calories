import SwiftUI

struct EstimationReviewView: View {
    @EnvironmentObject private var env: AppEnvironment
    @Environment(\.dismiss) private var dismiss

    let images: [Data]

    @State private var result: EstimationResult? = nil
    @State private var isLoading: Bool = true
    @State private var errorMessage: String? = nil

    var body: some View {
        Group {
            if isLoading {
                ProgressView("Estimating…")
            } else if let errorMessage {
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.largeTitle)
                        .foregroundStyle(.orange)
                    Text(errorMessage)
                        .multilineTextAlignment(.center)
                    Button("Try Again") { Task { await estimate() } }
                }
            } else if let result {
                List {
                    Section("Detected Items") {
                        ForEach(result.items.indices, id: \.self) { idx in
                            HStack {
                                VStack(alignment: .leading) {
                                    TextField("Name", text: binding(for: idx, keyPath: \.name))
                                        .font(.headline)
                                    TextField("Quantity", text: binding(for: idx, keyPath: \.quantity))
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Stepper(value: bindingDouble(for: idx, keyPath: \.calories), in: 0...2000, step: 10) {
                                    Text("\(Int(result.items[idx].calories)) kcal")
                                }
                            }
                        }
                    }

                    Section {
                        HStack {
                            Text("Total")
                            Spacer()
                            Text("\(Int(result.totalCalories)) kcal")
                                .bold()
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save") {
                            // TODO: Persist to Core Data when entities exist
                            dismiss()
                        }
                    }
                }
            } else {
                Text("No result")
            }
        }
        .navigationTitle("Review")
        .task { await estimate() }
    }

    private func estimate() async {
        isLoading = true
        errorMessage = nil
        do {
            let store = SettingsStore.shared
            let estimation = try await env.estimationService.estimateCalories(images: images, model: store.selectedModel, apiKey: store.apiKey)
            await MainActor.run {
                self.result = estimation
                self.isLoading = false
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    private func binding(for index: Int, keyPath: WritableKeyPath<EstimationItem, String>) -> Binding<String> {
        Binding<String>(
            get: { result?.items[index][keyPath: keyPath] ?? "" },
            set: { newValue in
                if var r = result {
                    r.items[index][keyPath: keyPath] = newValue
                    result = r
                }
            }
        )
    }

    private func bindingDouble(for index: Int, keyPath: WritableKeyPath<EstimationItem, Double>) -> Binding<Double> {
        Binding<Double>(
            get: { result?.items[index][keyPath: keyPath] ?? 0 },
            set: { newValue in
                if var r = result {
                    r.items[index][keyPath: keyPath] = newValue
                    result = r
                }
            }
        )
    }
}

#Preview {
    NavigationStack {
        EstimationReviewView(images: [])
            .environmentObject(AppEnvironment.shared)
    }
}
