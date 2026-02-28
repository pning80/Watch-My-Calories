import SwiftUI
import SwiftData

struct HistoryView: View {
    // Changed order to .forward so items inside the day card are chronological
    @Query(sort: \FoodEntry.timestamp, order: .forward) private var foodEntries: [FoodEntry]
    
    @State private var selectedImage: UIImage?
    @State private var entryToEdit: FoodEntry?
    @State private var entryToView: FoodEntry?
    
    var groupedEntries: [Date: [FoodEntry]] {
        let calendar = Calendar.current
        return Dictionary(grouping: foodEntries) { entry in
            calendar.startOfDay(for: entry.timestamp)
        }
    }
    
    // Sort dates descending (Newest day at top), but items inside will be .forward (chronological)
    var sortedDates: [Date] {
        groupedEntries.keys.sorted(by: >)
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 16) {
                        HStack {
                            Text("History")
                                .cwTitle()
                            Spacer()
                        }
                        .padding(.horizontal)
                        .padding(.top)
                        
                        if sortedDates.isEmpty {
                            EmptyStateCard()
                                .padding(.top, 40)
                        } else {
                            ForEach(sortedDates, id: \.self) { date in
                                HistoryDayCard(date: date, entries: groupedEntries[date] ?? [], onImageTap: { image in
                                    self.selectedImage = image
                                }, onEdit: { entry in
                                    self.entryToEdit = entry
                                }, onView: { entry in
                                    self.entryToView = entry
                                })
                            }
                        }
                    }
                    .padding(.bottom, 100)
                }
            }
        }
        .fullScreenCover(item: Binding<ImageWrapper?>(
            get: { selectedImage.map { ImageWrapper(image: $0) } },
            set: { selectedImage = $0?.image }
        )) { wrapper in
            FullScreenImageView(image: wrapper.image)
        }
        .sheet(item: $entryToEdit) { entry in
            EditFoodEntryView(entry: entry)
        }
        .sheet(item: $entryToView) { entry in
            ViewFoodEntryView(entry: entry)
        }
    }
}

// HistoryDayCard remains unchanged (it just displays the entries array it is given)
struct HistoryDayCard: View {
    let date: Date
    let entries: [FoodEntry]
    var onImageTap: (UIImage) -> Void
    var onEdit: ((FoodEntry) -> Void)? = nil
    var onView: ((FoodEntry) -> Void)? = nil
    
    @Environment(\.modelContext) private var modelContext
    
    var totalCalories: Double {
        entries.reduce(0) { $0 + $1.calories }
    }
    
    @State private var isExpanded = false
    
    var mealGroupedEntries: [MealType: [FoodEntry]] {
        Dictionary(grouping: entries) { entry in
            entry.mealType
        }
    }
    
    let mealOrder: [MealType] = [.breakfast, .lunch, .dinner, .snack]
    
    var body: some View {
        VStack(spacing: 0) {
            Button(action: { withAnimation(.snappy) { isExpanded.toggle() } }) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(date, format: .dateTime.day().month(.wide))
                            .font(.headline)
                            .foregroundStyle(Color.cwTextPrimary)
                        Text(date, format: .dateTime.weekday(.wide))
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundStyle(Color.gray)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 0) {
                        Text("\(Int(totalCalories))")
                            .font(.system(.title3, design: .rounded))
                            .fontWeight(.bold)
                            .foregroundStyle(Color.cwPrimary)
                        Text("kcal")
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundStyle(Color.gray)
                    }
                    
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(Color.gray)
                        .rotationEffect(.degrees(isExpanded ? 90 : 0))
                        .padding(.leading, 8)
                }
                .padding()
                .background(Color.cwSurface)
            }
            
            if isExpanded {
                Divider()
                VStack(spacing: 0) {
                    ForEach(mealOrder, id: \.self) { mealType in
                        if let mealEntries = mealGroupedEntries[mealType], !mealEntries.isEmpty {
                            HStack {
                                Text(mealType.rawValue)
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundStyle(Color.cwPrimary)
                                    .textCase(.uppercase)
                                    .padding(.vertical, 4)
                                Spacer()
                            }
                            .padding(.horizontal)
                            .background(Color.cwBackground)
                            
                            ForEach(mealEntries) { entry in
                                // Assumes FoodEntryCard is defined in Components.swift or locally
                                FoodEntryCard(entry: entry, onThumbnailTap: onImageTap)
                                    .contextMenu {
                                        Button {
                                            onView?(entry)
                                        } label: {
                                            Label("View", systemImage: "eye")
                                        }
                                        Button {
                                            onEdit?(entry)
                                        } label: {
                                            Label("Edit", systemImage: "pencil")
                                        }
                                        Button(role: .destructive) {
                                            withAnimation {
                                                modelContext.delete(entry)
                                            }
                                        } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                                
                                if entry.id != mealEntries.last?.id {
                                    Divider()
                                        .padding(.leading, 56)
                                }
                            }
                        }
                    }
                }
                .background(Color.cwSurface)
                .transition(.opacity)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 1)
        .padding(.horizontal)
    }
}

