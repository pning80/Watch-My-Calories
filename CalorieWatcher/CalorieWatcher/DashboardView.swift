import SwiftUI
import SwiftData

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \FoodEntry.timestamp, order: .forward) private var foodEntries: [FoodEntry]
    @Query private var userProfiles: [UserProfile]
    
    @StateObject private var healthKitManager = HealthKitManager()
    
    @Binding var selectedTab: ContentView.Tab
    @Binding var scrollToMeal: MealType?
    
    @State private var selectedImage: UIImage?
    @State private var showManualEntry = false
    @State private var pendingManualEntry: FoodEntry?
    @State private var entryToEdit: FoodEntry?
    @State private var entryToView: FoodEntry?

    init(selectedTab: Binding<ContentView.Tab>, scrollToMeal: Binding<MealType?> = .constant(nil)) {
        self._selectedTab = selectedTab
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
                                    Text("Calorie Watcher")
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

                                Button {
                                    showManualEntry = true
                                } label: {
                                    Image(systemName: "plus")
                                        .font(.system(size: 15, weight: .bold))
                                        .foregroundStyle(Color.white)
                                        .padding(10)
                                        .background(Circle().fill(Color.cwPrimary))
                                        .shadow(color: Color.cwPrimary.opacity(0.3), radius: 4, x: 0, y: 2)
                                }
                            }
                            .padding(.horizontal)
                            .padding(.top)
                            .id("top")
                            
                            HeroSummaryCard(
                                targetCalories: activeProfileTarget,
                                burnedCalories: healthKitManager.activeEnergyBurned,
                                entries: todayEntries
                            )
                            
                            if todayEntries.isEmpty {
                                VStack(spacing: 12) {
                                    Button {
                                        selectedTab = .camera
                                    } label: {
                                        EmptyStateCard()
                                    }
                                    .buttonStyle(.plain)

                                    Button {
                                        showManualEntry = true
                                    } label: {
                                        Label("or log manually", systemImage: "square.and.pencil")
                                            .font(.subheadline)
                                            .fontWeight(.medium)
                                            .foregroundStyle(Color.cwPrimary)
                                    }
                                }
                            } else {
                                VStack(alignment: .leading, spacing: 20) {
                                    ForEach(mealOrder, id: \.self) { mealType in
                                        if let entries = groupedMeals[mealType], !entries.isEmpty {
                                            MealSection(title: mealType.rawValue, entries: entries, onImageTap: { image in
                                                self.selectedImage = image
                                            }, onEdit: { entry in
                                                self.entryToEdit = entry
                                            }, onView: { entry in
                                                self.entryToView = entry
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
        }
        .fullScreenCover(item: Binding<ImageWrapper?>(
            get: { selectedImage.map { ImageWrapper(image: $0) } },
            set: { selectedImage = $0?.image }
        )) { wrapper in
            FullScreenImageView(image: wrapper.image)
        }
        .onAppear {
            healthKitManager.requestAuthorization()
        }
        .sheet(isPresented: $showManualEntry, onDismiss: {
            if let entry = pendingManualEntry {
                modelContext.insert(entry)
                pendingManualEntry = nil
            }
        }) {
            ManualEntryView(onSave: { entry in
                pendingManualEntry = entry
                showManualEntry = false
            })
        }
        .sheet(item: $entryToEdit) { entry in
            EditFoodEntryView(entry: entry)
        }
        .sheet(item: $entryToView) { entry in
            ViewFoodEntryView(entry: entry)
        }
    }
}

// MARK: - Manual Entry

private struct ManualEntryView: View {
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
        !name.trimmingCharacters(in: .whitespaces).isEmpty
        && Double(caloriesText) != nil && Double(caloriesText)! > 0
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

                                TextField("Calories", text: $caloriesText)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.decimalPad)

                                TextField("Quantity (e.g. 1 cup, 6 oz)", text: $quantity)
                                    .textFieldStyle(.roundedBorder)
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
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .fontWeight(.semibold)
                        .disabled(!canSave)
                }
            }
        }
    }

    private func save() {
        let calories = Double(caloriesText) ?? 0
        let protein = Double(proteinText)
        let carbs = Double(carbsText)
        let fat = Double(fatText)

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

