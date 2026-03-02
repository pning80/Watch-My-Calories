import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var userProfiles: [UserProfile]
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var env: AppEnvironment

    @Binding var selectedTab: ContentView.Tab
    @Binding var hasUnsavedChanges: Bool

    // Use ObservedObject for singletons to avoid lifecycle conflicts
    @ObservedObject private var store = SettingsStore.shared

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
        case calories
    }

    init(selectedTab: Binding<ContentView.Tab> = .constant(.settings), hasUnsavedChanges: Binding<Bool> = .constant(false)) {
        self._selectedTab = selectedTab
        self._hasUnsavedChanges = hasUnsavedChanges
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack {
                        Spacer()
                        VStack(spacing: 12) {
                            AppIconView()
                                .frame(width: 100, height: 100)
                                .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                                .shadow(radius: 5)

                            Text("Calorie Watcher")
                                .font(.headline)
                                .foregroundStyle(Color.cwPrimary)

                            Text("Version 1.0.0")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .listRowBackground(Color.clear)
                }

                Section(header: Text("App Appearance")) {
                    Picker("Theme", selection: $store.appTheme) {
                        ForEach(AppTheme.allCases) { theme in
                            Text(theme.rawValue).tag(theme)
                        }
                    }
                    .pickerStyle(.menu)
                }

                Section(header: Text("Profile")) {
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
                                isEditingAge = false
                                focusedField = nil
                            }
                        }
                    }

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
                    Text("Calorie Watcher keeps your data on-device. Only food images are sent to the backend for analysis.")
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
                checkUnsaved()
            }
            .onChange(of: heightFeet) { _, _ in checkUnsaved() }
            .onChange(of: heightInchesPart) { _, _ in checkUnsaved() }
            .onChange(of: weightLbs) { _, _ in checkUnsaved() }
            .onChange(of: age) { _, _ in checkUnsaved() }
            .onChange(of: gender) { _, _ in checkUnsaved() }
            .onChange(of: activityLevel) { _, _ in checkUnsaved() }
            .onChange(of: targetCalories) { _, _ in checkUnsaved() }
            .onChange(of: store.appTheme) { _, _ in checkUnsaved() }
            .onReceive(NotificationCenter.default.publisher(for: Notification.Name("DiscardSettings"))) { _ in
                loadSettings()
                checkUnsaved()
            }
        }
        .preferredColorScheme(store.appTheme.colorScheme)
    }

    private func checkUnsaved() {
        let target = targetCalories ?? 2000
        let totalInches = Double(heightFeet * 12 + heightInchesPart)
        let heightCm = totalInches * 2.54
        let weightKg = Double(weightLbs) / 2.20462

        var isUnsaved = false

        if store.appTheme != store.savedAppTheme { isUnsaved = true }
        else if let p = userProfiles.first {
            if abs(p.height - heightCm) > 0.1 ||
               abs(p.weight - weightKg) > 0.1 ||
               p.age != age ||
               p.gender != gender ||
               p.activityLevel != activityLevel ||
               p.targetCalories != target {
                isUnsaved = true
            }
        } else {
            if heightFeet != 5 || heightInchesPart != 8 || weightLbs != 150 || age != 30 || gender != .male || activityLevel != .sedentary || targetCalories != nil {
                isUnsaved = true
            }
        }

        if hasUnsavedChanges != isUnsaved {
            hasUnsavedChanges = isUnsaved
        }
    }

    private func loadSettings() {
        store.appTheme = store.savedAppTheme

        if let profile = userProfiles.first {
            // Metric (Stored) -> Imperial (UI)
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
        // 1. Save to Store (UserDefaults)
        store.save()

        // 2. Prepare Profile Data
        // Imperial (UI) -> Metric (Storage)
        let totalInches = Double(heightFeet * 12 + heightInchesPart)
        let heightCm = totalInches * 2.54
        let weightKg = Double(weightLbs) / 2.20462

        let userAge = age
        // Fallback to 2000 if user didn't set a target
        let target = targetCalories ?? 2000

        // 3. Save to SwiftData
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

        // 4. Commit Changes with Error Handling
        do {
            try modelContext.save()
            print("Settings saved successfully.")
        } catch {
            print("CRITICAL ERROR: Failed to save user profile: \(error)")
        }
    }

    private func calculateCalories() {
        let totalInches = Double(heightFeet * 12 + heightInchesPart)
        let heightCm = totalInches * 2.54
        let weightKg = Double(weightLbs) * 0.453592

        var bmr: Double = (10 * weightKg) + (6.25 * heightCm) - (5 * Double(age))

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
