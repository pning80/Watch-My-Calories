import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var userProfiles: [UserProfile]
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var env: AppEnvironment
    
    @Binding var selectedTab: ContentView.Tab

    @StateObject private var store = SettingsStore.shared
    @State private var apiKey: String = ""
    @State private var selectedModel: String = ""
    @State private var availableModels: [GeminiModel] = []
    @State private var isLoadingModels: Bool = false
    
    // UI State (Imperial)
    @State private var heightFeet: Int = 5
    @State private var heightInchesPart: Int = 8
    @State private var weightLbs: Int = 150
    @State private var age: Int = 30
    
    @State private var gender: Gender = .male
    @State private var activityLevel: ActivityLevel = .sedentary
    @State private var targetCalories: Double? = nil
    
    // Toggle state for inline pickers
    @State private var isEditingWeight = false
    @State private var isEditingAge = false
    
    @FocusState private var focusedField: Field?
    
    enum Field {
        case apiKey, calories
    }
    
    init(selectedTab: Binding<ContentView.Tab> = .constant(.settings)) {
        self._selectedTab = selectedTab
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Gemini API")) {
                    SecureField("API Key", text: $apiKey)
                        .textContentType(.password)
                        .focused($focusedField, equals: .apiKey)
                        .onChange(of: apiKey) { newValue in
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
                                    Text(model.displayName).tag(model.name.replacingOccurrences(of: "models/", with: ""))
                                }
                            }
                        }
                    }
                    Link("Get an API key", destination: URL(string: "https://aistudio.google.com/app/apikey")!)
                }
                
                Section(header: Text("Profile")) {
                    // Height (Compact Inline)
                    HStack {
                        Text("Height")
                        Spacer()
                        Picker("Feet", selection: $heightFeet) {
                            ForEach(4...8, id: \.self) { ft in
                                Text("\(ft)'").tag(ft)
                            }
                        }
                        .labelsHidden()
                        .frame(width: 60)
                        .clipped()
                        
                        Picker("Inches", selection: $heightInchesPart) {
                            ForEach(0...11, id: \.self) { inch in
                                Text("\(inch)\"").tag(inch)
                            }
                        }
                        .labelsHidden()
                        .frame(width: 60)
                        .clipped()
                    }
                    
                    // Weight (Expandable Inline Picker)
                    DisclosureGroup(isExpanded: $isEditingWeight) {
                        Picker("Weight", selection: $weightLbs) {
                            ForEach(50...400, id: \.self) { lbs in
                                Text("\(lbs) lbs").tag(lbs)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(height: 150)
                    } label: {
                        HStack {
                            Text("Weight")
                                .foregroundStyle(Color.primary)
                            Spacer()
                            Text("\(weightLbs) lbs")
                                .foregroundStyle(isEditingWeight ? Color.accentColor : Color.secondary)
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            withAnimation {
                                isEditingWeight.toggle()
                                isEditingAge = false // Close others
                                focusedField = nil
                            }
                        }
                    }
                    
                    // Age (Expandable Inline Picker)
                    DisclosureGroup(isExpanded: $isEditingAge) {
                        Picker("Age", selection: $age) {
                            ForEach(1...100, id: \.self) { y in
                                Text("\(y)").tag(y)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(height: 150)
                    } label: {
                        HStack {
                            Text("Age")
                                .foregroundStyle(Color.primary)
                            Spacer()
                            Text("\(age)")
                                .foregroundStyle(isEditingAge ? Color.accentColor : Color.secondary)
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            withAnimation {
                                isEditingAge.toggle()
                                isEditingWeight = false
                                focusedField = nil
                            }
                        }
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
                        TextField("Not Set", value: $targetCalories, format: .number)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                            .focused($focusedField, equals: .calories)
                    }
                    
                    Button("Calculate Recommended Goal") {
                        calculateCalories()
                        focusedField = nil
                        isEditingWeight = false
                        isEditingAge = false
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
                        focusedField = nil
                        selectedTab = .dashboard
                    }
                    .disabled(targetCalories == nil)
                }
                
                ToolbarItem(placement: .keyboard) {
                    Button("Done") {
                        focusedField = nil
                    }
                }
            }
            .onAppear {
                loadSettings()
                focusedField = nil
                Task { await fetchModels() }
            }
            // Removed global .onTapGesture to fix responsiveness issues
        }
    }
    
    // ... helper methods same as before ...
    private func fetchModels() async {
        guard !apiKey.isEmpty else { return }
        isLoadingModels = true
        do {
            let models = try await env.estimationService.fetchAvailableModels(apiKey: apiKey)
            await MainActor.run {
                self.availableModels = models.sorted { $0.displayName < $1.displayName }
                self.isLoadingModels = false
                
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
        apiKey = store.apiKey
        selectedModel = store.selectedModel
        
        if let profile = userProfiles.first {
            let heightCm = profile.height
            let heightInchesTotal = heightCm / 2.54
            if heightInchesTotal > 0 {
                heightFeet = Int(heightInchesTotal) / 12
                heightInchesPart = Int(heightInchesTotal) % 12
            }
            let weightKg = profile.weight
            weightLbs = Int((weightKg * 2.20462).rounded())
            age = profile.age
            gender = profile.gender
            activityLevel = profile.activityLevel
            targetCalories = profile.targetCalories > 0 ? profile.targetCalories : nil
        }
    }
    
    private func saveSettings() {
        store.apiKey = apiKey
        store.selectedModel = selectedModel
        store.save()
        
        let totalInches = Double(heightFeet * 12 + heightInchesPart)
        let heightCm = totalInches * 2.54
        let weightKg = Double(weightLbs) / 2.20462
        let userAge = age
        let target = targetCalories ?? 2000
        
        if let profile = userProfiles.first {
            profile.height = heightCm
            profile.weight = weightKg
            profile.age = userAge
            profile.gender = gender
            profile.activityLevel = activityLevel
            profile.targetCalories = target
        } else {
            let newProfile = UserProfile(
                height: heightCm,
                weight: weightKg,
                age: userAge,
                gender: gender,
                activityLevel: activityLevel,
                targetCalories: target
            )
            modelContext.insert(newProfile)
        }
    }
    
    private func calculateCalories() {
        let totalInches = Double(heightFeet * 12 + heightInchesPart)
        let heightCm = totalInches * 2.54
        let weightKg = Double(weightLbs) * 0.453592
        var bmr: Double = (10 * weightKg) + (6.25 * heightCm) - (5 * Double(age))
        if gender == .male { bmr += 5 } else { bmr -= 161 }
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

