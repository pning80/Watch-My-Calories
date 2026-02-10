import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var userProfiles: [UserProfile]
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var env: AppEnvironment
    
    // Binding to control Tab selection
    @Binding var selectedTab: ContentView.Tab

    // API Key State
    @StateObject private var store = SettingsStore.shared
    @State private var apiKey: String = ""
    @State private var selectedModel: String = ""
    @State private var availableModels: [GeminiModel] = []
    @State private var isLoadingModels: Bool = false
    
    // Profile State
    @State private var height: Double = 170
    @State private var weight: Double = 70
    @State private var age: Double = 30
    @State private var gender: Gender = .male
    @State private var activityLevel: ActivityLevel = .sedentary
    @State private var targetCalories: Double = 2000
    
    // Add an init to provide default binding for preview/compatibility if needed, 
    // though usually strictly required is better.
    init(selectedTab: Binding<ContentView.Tab> = .constant(.settings)) {
        self._selectedTab = selectedTab
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Gemini API")) {
                    SecureField("API Key", text: $apiKey)
                        .textContentType(.password)
                        .onChange(of: apiKey) { newValue in
                            // Refresh models when API key changes if valid length
                            if newValue.count > 10 {
                                Task { await fetchModels() }
                            }
                        }
                    
                    if isLoadingModels {
                        HStack {
                            Text("Loading models...")
                            Spacer()
                            ProgressView()
                        }
                    } else {
                        Picker("Model", selection: $selectedModel) {
                            if availableModels.isEmpty {
                                Text("Gemini 2.0 Flash Exp").tag("gemini-2.0-flash-exp")
                                Text("Gemini 1.5 Flash").tag("gemini-1.5-flash")
                            } else {
                                ForEach(availableModels, id: \.name) { model in
                                    // Remove "models/" prefix for cleaner display if desired, or keep as is.
                                    // Tag should match what we save.
                                    Text(model.displayName).tag(model.name.replacingOccurrences(of: "models/", with: ""))
                                }
                            }
                        }
                    }
                    
                    Link("Get an API key", destination: URL(string: "https://aistudio.google.com/app/apikey")!)
                }
                
                Section(header: Text("Profile")) {
                    HStack {
                        Text("Height (cm)")
                        Spacer()
                        TextField("Height", value: $height, format: .number)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Weight (kg)")
                        Spacer()
                        TextField("Weight", value: $weight, format: .number)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Age")
                        Spacer()
                        TextField("Age", value: $age, format: .number)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                    Picker("Gender", selection: $gender) {
                        ForEach(Gender.allCases) { gender in
                            Text(gender.rawValue).tag(gender)
                        }
                    }
                    Picker("Activity", selection: $activityLevel) {
                        ForEach(ActivityLevel.allCases) { level in
                            Text(level.rawValue).tag(level)
                        }
                    }
                }
                
                Section(header: Text("Daily Goals")) {
                    HStack {
                        Text("Target Calories")
                        Spacer()
                        TextField("Calories", value: $targetCalories, format: .number)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                    Button("Calculate Recommended Goal") {
                        calculateCalories()
                    }
                }

                Section {
                    Text("Calorie Watcher keeps your data on-device.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saveSettings()
                        // Navigate to Dashboard
                        selectedTab = .dashboard
                    }
                }
            }
            .onAppear {
                loadSettings()
                Task { await fetchModels() }
            }
        }
    }
    
    private func fetchModels() async {
        guard !apiKey.isEmpty else { return }
        isLoadingModels = true
        do {
            let models = try await env.estimationService.fetchAvailableModels(apiKey: apiKey)
            await MainActor.run {
                self.availableModels = models.sorted { $0.displayName < $1.displayName }
                self.isLoadingModels = false
                
                // If current selection is not in list, update it
                // Prefer 2.0 Flash if available
                let currentModelExists = availableModels.contains { $0.name.contains(selectedModel) }
                
                if !currentModelExists {
                    if let flash2 = availableModels.first(where: { $0.name.contains("gemini-2.0-flash") }) {
                        self.selectedModel = flash2.name.replacingOccurrences(of: "models/", with: "")
                    } else if let flash15 = availableModels.first(where: { $0.name.contains("gemini-1.5-flash") }) {
                        self.selectedModel = flash15.name.replacingOccurrences(of: "models/", with: "")
                    } else if let first = availableModels.first {
                        self.selectedModel = first.name.replacingOccurrences(of: "models/", with: "")
                    }
                }
            }
        } catch {
            print("Failed to fetch models: \(error)")
            await MainActor.run { self.isLoadingModels = false }
        }
    }
    
    private func loadSettings() {
        // Load API settings
        apiKey = store.apiKey
        selectedModel = store.selectedModel
        
        // Load Profile
        if let profile = userProfiles.first {
            height = profile.height
            weight = profile.weight
            age = Double(profile.age)
            gender = profile.gender
            activityLevel = profile.activityLevel
            targetCalories = profile.targetCalories
        }
    }
    
    private func saveSettings() {
        // Save API settings
        store.apiKey = apiKey
        store.selectedModel = selectedModel
        store.save()
        
        // Save Profile
        if let profile = userProfiles.first {
            profile.height = height
            profile.weight = weight
            profile.age = Int(age)
            profile.gender = gender
            profile.activityLevel = activityLevel
            profile.targetCalories = targetCalories
        } else {
            let newProfile = UserProfile(
                height: height,
                weight: weight,
                age: Int(age),
                gender: gender,
                activityLevel: activityLevel,
                targetCalories: targetCalories
            )
            modelContext.insert(newProfile)
        }
    }
    
    private func calculateCalories() {
        // Mifflin-St Jeor Equation
        var bmr: Double = (10 * weight) + (6.25 * height) - (5 * age)
        
        if gender == .male {
            bmr += 5
        } else {
            bmr -= 161
        }
        
        let multiplier: Double
        switch activityLevel {
        case .sedentary: multiplier = 1.2
        case .lightlyActive: multiplier = 1.375
        case .moderatelyActive: multiplier = 1.55
        case .veryActive: multiplier = 1.725
        }
        
        targetCalories = (bmr * multiplier).rounded()
    }
}

