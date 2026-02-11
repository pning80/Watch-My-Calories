import SwiftUI
import SwiftData

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \FoodEntry.timestamp, order: .forward) private var foodEntries: [FoodEntry]
    @Query private var userProfiles: [UserProfile]
    
    @Binding var scrollToMeal: MealType?
    
    @State private var selectedImage: UIImage?
    
    init(scrollToMeal: Binding<MealType?> = .constant(nil)) {
        self._scrollToMeal = scrollToMeal
    }
    
    var todayEntries: [FoodEntry] {
        let calendar = Calendar.current
        return foodEntries.filter { calendar.isDateInToday($0.timestamp) }
    }
    
    var groupedMeals: [MealType: [FoodEntry]] {
        Dictionary(grouping: todayEntries) { entry in
            MealType.from(date: entry.timestamp)
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
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(Date(), format: .dateTime.weekday(.wide).day().month())
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                        .textCase(.uppercase)
                                        .foregroundStyle(Color.gray)
                                        .kerning(1)
                                    
                                    Text("Calorie Watcher")
                                        .cwTitle()
                                }
                                Spacer()
                            }
                            .padding(.horizontal)
                            .padding(.top)
                            .id("top")
                            
                            HeroSummaryCard(targetCalories: activeProfileTarget, entries: todayEntries)
                            
                            if todayEntries.isEmpty {
                                EmptyStateCard()
                            } else {
                                VStack(alignment: .leading, spacing: 20) {
                                    ForEach(mealOrder, id: \.self) { mealType in
                                        if let entries = groupedMeals[mealType], !entries.isEmpty {
                                            MealSection(title: mealType.rawValue, entries: entries, onImageTap: { image in
                                                self.selectedImage = image
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
    }
}

// MARK: - Local Components
struct MealSection: View {
    let title: String
    let entries: [FoodEntry]
    var onImageTap: (UIImage) -> Void
    @Environment(\.modelContext) private var modelContext
    
    var totalCalories: Double {
        entries.reduce(0) { $0 + $1.calories }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.cwTextPrimary)
                
                Spacer()
                
                Text("\(Int(totalCalories)) kcal")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.cwPrimary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.cwSecondary)
                    .clipShape(Capsule())
            }
            .padding(.horizontal)
            
            VStack(spacing: 8) {
                ForEach(entries) { entry in
                    FoodEntryCard(entry: entry, onThumbnailTap: onImageTap)
                        .contextMenu {
                            Button(role: .destructive) {
                                modelContext.delete(entry)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                }
            }
        }
    }
}

