import SwiftUI
import SwiftData

struct OnboardingView: View {
    @Environment(\.modelContext) private var modelContext
    @ObservedObject private var store = SettingsStore.shared

    @State private var currentStep = 0

    // Profile state
    @State private var heightFeet: Int = 5
    @State private var heightInchesPart: Int = 8
    @State private var weightLbs: Int = 150
    @State private var heightCmUI: Int = 173
    @State private var weightKgUI: Int = 68
    @State private var age: Int = 30
    @State private var gender: Gender = .male
    @State private var activityLevel: ActivityLevel = .sedentary
    @State private var targetCaloriesText: String = ""
    @FocusState private var isCaloriesFieldFocused: Bool

    var body: some View {
        ZStack(alignment: .topTrailing) {
            TabView(selection: $currentStep) {
                welcomeStep.tag(0)
                profileStep.tag(1)
                goalsStep.tag(2)
                permissionsStep.tag(3)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(.easeInOut, value: currentStep)
            .onChange(of: currentStep) { _, _ in
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            }
            .onChange(of: store.unitSystem) { _, _ in
                // Cross-sync UI values when unit system changes
                if store.unitSystem == .metric {
                    let totalInches = Double(heightFeet * 12 + heightInchesPart)
                    heightCmUI = Int((totalInches * 2.54).rounded())
                    weightKgUI = Int((Double(weightLbs) / 2.20462).rounded())
                } else {
                    let totalInches = Double(heightCmUI) / 2.54
                    heightFeet = Int(totalInches) / 12
                    heightInchesPart = Int(totalInches) % 12
                    weightLbs = Int((Double(weightKgUI) * 2.20462).rounded())
                }
            }

            Button("Skip") {
                store.completeOnboarding()
            }
            .font(.subheadline.weight(.medium))
            .foregroundStyle(Color.cwPrimary)
            .padding(.trailing, 24)
            .padding(.top, 16)
            .accessibilityIdentifier(AccessibilityID.Onboarding.skipButton)
        }
    }

    // MARK: - Step 0: Welcome

    private var welcomeStep: some View {
        VStack(spacing: 24) {
            Spacer()

            AppIconView()
                .frame(width: 120, height: 120)
                .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                .shadow(radius: 10)

            Text("Watch My Calories")
                .cwTitle()

            Text("Track your meals with AI-powered calorie estimation")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Spacer()

            Label("Your data stays on your device", systemImage: "lock.shield")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .accessibilityIdentifier("onboarding_privacyNote")

            Button {
                withAnimation { currentStep = 1 }
            } label: {
                Text("Get Started")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.cwPrimary)
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
            .accessibilityIdentifier(AccessibilityID.Onboarding.getStartedButton)
        }
    }

    // MARK: - Step 1: Profile

    private var profileStep: some View {
        VStack(spacing: 20) {
            Text("About You")
                .font(.title.bold())
                .foregroundStyle(Color.cwPrimary)
                .padding(.top, 40)

            Form {
                if store.unitSystem == .us {
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

                    Picker("Weight", selection: $weightLbs) {
                        ForEach(50...400, id: \.self) { lbs in
                            Text("\(lbs) lbs").tag(lbs)
                        }
                    }
                } else {
                    Picker("Height", selection: $heightCmUI) {
                        ForEach(100...250, id: \.self) { cm in
                            Text("\(cm) cm").tag(cm)
                        }
                    }

                    Picker("Weight", selection: $weightKgUI) {
                        ForEach(20...200, id: \.self) { kg in
                            Text("\(kg) kg").tag(kg)
                        }
                    }
                }

                Picker("Age", selection: $age) {
                    ForEach(1...100, id: \.self) { y in
                        Text("\(y)").tag(y)
                    }
                }

                Picker("Gender", selection: $gender) {
                    ForEach(Gender.allCases) { g in
                        Text(g.rawValue).tag(g)
                    }
                }
            }
            .scrollContentBackground(.hidden)

            progressDots(current: 1, total: 3)

            nextButton {
                withAnimation { currentStep = 2 }
            }
            .padding(.bottom, 40)
        }
    }

    // MARK: - Step 2: Goals

    private var goalsStep: some View {
        VStack(spacing: 20) {
            Text("Your Goals")
                .font(.title.bold())
                .foregroundStyle(Color.cwPrimary)
                .padding(.top, 40)
                .onTapGesture { isCaloriesFieldFocused = false }

            Form {
                Section {
                    Picker("Activity Level", selection: $activityLevel) {
                        ForEach(ActivityLevel.allCases) { level in
                            Text(level.rawValue).tag(level)
                        }
                    }
                }

                Section(header: Text("Target Calories")) {
                    Button("Calculate Recommended Goal") {
                        calculateRecommended()
                    }
                    .accessibilityIdentifier(AccessibilityID.Onboarding.calculateGoalButton)

                    HStack {
                        Text("Target Calories")
                        Spacer()
                        TextField("Not Set", text: $targetCaloriesText)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                            .focused($isCaloriesFieldFocused)
                            .accessibilityIdentifier(AccessibilityID.Onboarding.targetCaloriesField)
                    }
                }

                Section(header: Text("Unit System")) {
                    Picker("Unit System", selection: $store.unitSystem) {
                        ForEach(UnitSystem.allCases) { unit in
                            Text(unit.rawValue).tag(unit)
                        }
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                }
            }
            .scrollContentBackground(.hidden)
            .scrollDismissesKeyboard(.immediately)

            progressDots(current: 2, total: 3)

            nextButton {
                isCaloriesFieldFocused = false
                withAnimation { currentStep = 3 }
            }
            .padding(.bottom, 40)
        }
    }

    // MARK: - Step 3: Permissions & Done

    private var permissionsStep: some View {
        VStack(spacing: 20) {
            Text("Almost Done")
                .font(.title.bold())
                .foregroundStyle(Color.cwPrimary)
                .padding(.top, 40)

            Form {
                Section(header: Text("Privacy")) {
                    Toggle("AI Photo Analysis", isOn: Binding(
                        get: { store.aiConsent == .accepted },
                        set: { store.saveAIConsent($0 ? .accepted : .declined) }
                    ))
                    .tint(Color.cwPrimary)

                    Text("When enabled, food photos are sent to Google Gemini for calorie estimation. All other data stays on-device.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Section(header: Text("Health")) {
                    Button {
                        HealthKitManager().requestAuthorization()
                    } label: {
                        Label("Enable Health Tracking", systemImage: "heart.fill")
                    }
                    .accessibilityIdentifier(AccessibilityID.Onboarding.enableHealthButton)

                    Text("Syncs active calories burned from Apple Health to adjust your daily goal.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .scrollContentBackground(.hidden)

            progressDots(current: 3, total: 3)

            Button {
                saveProfileAndFinish()
            } label: {
                Text("Start Tracking")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.cwPrimary)
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
            .accessibilityIdentifier(AccessibilityID.Onboarding.finishButton)
        }
    }

    // MARK: - Helpers

    private func nextButton(action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text("Next")
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
        }
        .buttonStyle(.borderedProminent)
        .tint(Color.cwPrimary)
        .padding(.horizontal, 32)
        .accessibilityIdentifier(AccessibilityID.Onboarding.nextButton)
    }

    private func progressDots(current: Int, total: Int) -> some View {
        HStack(spacing: 8) {
            ForEach(1...total, id: \.self) { step in
                Circle()
                    .fill(step == current ? Color.cwPrimary : Color.cwPrimary.opacity(0.3))
                    .frame(width: 8, height: 8)
            }
        }
    }

    private func calculateRecommended() {
        isCaloriesFieldFocused = false
        let heightCm: Double
        let weightKg: Double
        if store.unitSystem == .us {
            let totalInches = Double(heightFeet * 12 + heightInchesPart)
            heightCm = totalInches * 2.54
            weightKg = Double(weightLbs) / 2.20462
        } else {
            heightCm = Double(heightCmUI)
            weightKg = Double(weightKgUI)
        }
        let cal = CalorieCalculator.recommended(
            heightCm: heightCm, weightKg: weightKg, age: age,
            gender: gender, activityLevel: activityLevel
        )
        targetCaloriesText = "\(Int(cal))"
    }

    private func saveProfileAndFinish() {
        let heightCm: Double
        let weightKg: Double
        if store.unitSystem == .us {
            let totalInches = Double(heightFeet * 12 + heightInchesPart)
            heightCm = totalInches * 2.54
            weightKg = Double(weightLbs) / 2.20462
        } else {
            heightCm = Double(heightCmUI)
            weightKg = Double(weightKgUI)
        }

        let target = Double(targetCaloriesText) ?? 2000

        let profile = UserProfile(
            height: heightCm,
            weight: weightKg,
            age: age,
            gender: gender,
            activityLevel: activityLevel,
            targetCalories: target
        )
        modelContext.insert(profile)
        try? modelContext.save()

        store.save()
        store.completeOnboarding()
    }
}
