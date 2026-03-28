import SwiftUI
import SwiftData

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \FoodEntry.timestamp, order: .forward) private var foodEntries: [FoodEntry]
    @Query private var userProfiles: [UserProfile]
    
    @StateObject private var healthKitManager = HealthKitManager()
    
    var onLogFood: () -> Void
    @Binding var scrollToMeal: MealType?

    @State private var selectedImage: UIImage?
    @State private var entryToEdit: FoodEntry?
    @State private var entryToView: FoodEntry?
    @State private var groupToEdit: FoodEntryGroupEdit?
    @State private var groupToView: FoodEntryGroupEdit?

    init(onLogFood: @escaping () -> Void = {}, scrollToMeal: Binding<MealType?> = .constant(nil)) {
        self.onLogFood = onLogFood
        self._scrollToMeal = scrollToMeal
    }
    
    var todayEntries: [FoodEntry] {
        let calendar = Calendar.current
        return foodEntries.filter { calendar.isDateInToday($0.timestamp) }
    }
    
    var groupedMeals: [MealType: [FoodEntry]] {
        Dictionary(grouping: todayEntries) { entry in
            entry.mealType
        }
    }
    
    let mealOrder: [MealType] = [.breakfast, .lunch, .dinner, .snack]
    
    var activeProfileTarget: Double {
        if let profile = userProfiles.first {
            return profile.targetCalories > 0 ? profile.targetCalories : 2000
        }
        return 2000
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()
                
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(spacing: 24) {
                            HStack(alignment: .center, spacing: 14) {
                                Image("MiniAppIcon")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 38, height: 38)
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    .shadow(color: Color.black.opacity(0.1), radius: 2, x: 0, y: 1)
                                    
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Watch My Calories")
                                        .font(.system(.title2, design: .serif, weight: .bold))
                                        .foregroundStyle(Color.cwPrimary)
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.8)
                                    
                                    Text(Date(), format: .dateTime.weekday(.wide).day().month())
                                        .font(.caption)
                                        .fontWeight(.medium)
                                        .textCase(.uppercase)
                                        .foregroundStyle(Color.gray)
                                        .kerning(0.5)
                                }
                                Spacer()

                                AppMenuButton()
                            }
                            .padding(.horizontal)
                            .padding(.top)
                            .id("top")
                            
                            HeroSummaryCard(
                                targetCalories: activeProfileTarget,
                                burnedCalories: healthKitManager.activeEnergyBurned,
                                entries: todayEntries
                            )

                            BannerAdView()

                            if todayEntries.isEmpty {
                                VStack(spacing: 12) {
                                    Button {
                                        onLogFood()
                                    } label: {
                                        EmptyStateCard()
                                    }
                                    .buttonStyle(.plain)
                                    .accessibilityIdentifier(AccessibilityID.Dashboard.emptyStateCard)

                                    Button {
                                        onLogFood()
                                    } label: {
                                        Label("or log manually", systemImage: "square.and.pencil")
                                            .font(.subheadline)
                                            .fontWeight(.medium)
                                            .foregroundStyle(Color.cwPrimary)
                                    }
                                    .accessibilityIdentifier(AccessibilityID.Dashboard.manualEntryLink)
                                }
                            } else {
                                VStack(alignment: .leading, spacing: 20) {
                                    ForEach(mealOrder, id: \.self) { mealType in
                                        if let entries = groupedMeals[mealType], !entries.isEmpty {
                                            MealSection(title: mealType.rawValue, entries: entries, onImageTap: { image in
                                                self.selectedImage = image
                                            }, onEdit: { entry in
                                                self.entryToEdit = entry
                                            }, onEditGroup: { items in
                                                self.groupToEdit = FoodEntryGroupEdit(items: items)
                                            }, onView: { entry in
                                                self.entryToView = entry
                                            }, onViewGroup: { items in
                                                self.groupToView = FoodEntryGroupEdit(items: items)
                                            })
                                            .id(mealType)
                                        }
                                    }
                                }
                                .padding(.bottom)
                            }
                        }
                        .padding(.bottom, 100)
                    }
                    .refreshable {
                        healthKitManager.fetchTodayEnergyBurned()
                    }
                    .onChange(of: scrollToMeal) { oldVal, newVal in
                        if let meal = newVal {
                            withAnimation {
                                proxy.scrollTo(meal, anchor: .top)
                            }
                        }
                    }
                }
            }
            .toolbar(.hidden, for: .navigationBar)
        }
        .fullScreenCover(item: Binding<ImageWrapper?>(
            get: { selectedImage.map { ImageWrapper(image: $0) } },
            set: { selectedImage = $0?.image }
        )) { wrapper in
            FullScreenImageView(image: wrapper.image)
        }
        .onAppear {
            if !WatchMyCaloriesApp.isUITesting {
                healthKitManager.requestAuthorization()
            }
        }
        .sheet(item: $entryToEdit) { entry in
            EditFoodEntryView(entry: entry)
        }
        .sheet(item: $groupToEdit) { group in
            EditMealGroupView(entries: group.items)
        }
        .sheet(item: $entryToView) { entry in
            ViewFoodEntryView(entry: entry)
        }
        .sheet(item: $groupToView) { group in
            ViewMealGroupView(entries: group.items)
        }
    }

}

// MARK: - Manual Entry

struct ManualEntryView: View {
    @Environment(\.dismiss) private var dismiss
    var onSave: (FoodEntry) -> Void

    @State private var name = ""
    @State private var caloriesText = ""
    @State private var quantity = ""
    @State private var mealType: MealType = MealType.from(date: Date())
    @State private var proteinText = ""
    @State private var carbsText = ""
    @State private var fatText = ""
    @State private var showNutrition = false

    private var canSave: Bool {
        let trimmedCalories = caloriesText.trimmingCharacters(in: .whitespaces)
        return !name.trimmingCharacters(in: .whitespaces).isEmpty
        && !trimmedCalories.isEmpty && (Double(trimmedCalories) ?? 0) > 0
        && !quantity.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Food info section
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Food Details")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            VStack(spacing: 12) {
                                TextField("Food name", text: $name)
                                    .textFieldStyle(.roundedBorder)
                                    .accessibilityIdentifier(AccessibilityID.ManualEntry.foodName)

                                TextField("Calories", text: $caloriesText)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.decimalPad)
                                    .accessibilityIdentifier(AccessibilityID.ManualEntry.calories)

                                TextField(SettingsStore.shared.unitSystem == .metric ? "Quantity (e.g. 200 g, 250 ml)" : "Quantity (e.g. 1 cup, 6 oz)", text: $quantity)
                                    .textFieldStyle(.roundedBorder)
                                    .accessibilityIdentifier(AccessibilityID.ManualEntry.quantity)
                            }
                        }
                        .cwCard()
                        .padding(.horizontal)

                        // Meal type picker
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Meal")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            Picker("Meal", selection: $mealType) {
                                ForEach(MealType.allCases, id: \.self) { type in
                                    Text(type.rawValue).tag(type)
                                }
                            }
                            .pickerStyle(.segmented)
                            .accessibilityIdentifier(AccessibilityID.ManualEntry.mealPicker)
                        }
                        .cwCard()
                        .padding(.horizontal)

                        // Optional nutrition
                        VStack(alignment: .leading, spacing: 12) {
                            DisclosureGroup("Nutrition Details (optional)", isExpanded: $showNutrition) {
                                VStack(spacing: 12) {
                                    HStack(spacing: 12) {
                                        NutrientField(label: "Protein (g)", text: $proteinText)
                                        NutrientField(label: "Carbs (g)", text: $carbsText)
                                        NutrientField(label: "Fat (g)", text: $fatText)
                                    }
                                }
                                .padding(.top, 8)
                            }
                            .font(.headline)
                            .foregroundStyle(Color.cwTextPrimary)
                        }
                        .cwCard()
                        .padding(.horizontal)
                    }
                    .padding(.top)
                }
            }
            .navigationTitle("Log Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .accessibilityIdentifier(AccessibilityID.ManualEntry.cancelButton)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .fontWeight(.semibold)
                        .disabled(!canSave)
                        .accessibilityIdentifier(AccessibilityID.ManualEntry.saveButton)
                }
            }
        }
    }

    private func save() {
        let calories = Double(caloriesText.trimmingCharacters(in: .whitespaces)) ?? 0
        let protein = Double(proteinText.trimmingCharacters(in: .whitespaces))
        let carbs = Double(carbsText.trimmingCharacters(in: .whitespaces))
        let fat = Double(fatText.trimmingCharacters(in: .whitespaces))

        let entry = FoodEntry(
            name: name.trimmingCharacters(in: .whitespaces),
            calories: calories,
            quantity: quantity.trimmingCharacters(in: .whitespaces),
            protein: protein,
            carbs: carbs,
            fat: fat
        )
        entry.mealType = mealType
        onSave(entry)
    }
}

private struct NutrientField: View {
    let label: String
    @Binding var text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Color.gray)
            TextField("—", text: $text)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.decimalPad)
        }
    }
}


