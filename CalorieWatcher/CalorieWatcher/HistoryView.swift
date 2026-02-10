import SwiftUI
import SwiftData

struct HistoryView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \FoodEntry.timestamp, order: .reverse) private var foodEntries: [FoodEntry]
    
    // Group entries by date
    var groupedEntries: [Date: [FoodEntry]] {
        let calendar = Calendar.current
        return Dictionary(grouping: foodEntries) { entry in
            calendar.startOfDay(for: entry.timestamp)
        }
    }
    
    var sortedDates: [Date] {
        groupedEntries.keys.sorted(by: >)
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 20) {
                        HStack {
                            Text("History")
                                .cwTitle()
                            Spacer()
                        }
                        .padding(.horizontal)
                        .padding(.top)
                        
                        if sortedDates.isEmpty {
                            EmptyStateCard() // Replaced ContentUnavailableView
                                .padding(.top, 40)
                        } else {
                            ForEach(sortedDates, id: \.self) { date in
                                HistoryDayCard(date: date, entries: groupedEntries[date] ?? [])
                            }
                        }
                    }
                    .padding(.bottom, 100)
                }
            }
        }
    }
}

struct HistoryDayCard: View {
    let date: Date
    let entries: [FoodEntry]
    
    @Environment(\.modelContext) private var modelContext
    
    var totalCalories: Double {
        entries.reduce(0) { $0 + $1.calories }
    }
    
    @State private var isExpanded = false
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            Button(action: { withAnimation(.snappy) { isExpanded.toggle() } }) {
                HStack {
                    VStack(alignment: .leading) {
                        Text(date, format: .dateTime.day().month(.wide))
                            .font(.headline)
                            .foregroundStyle(Color.cwTextPrimary)
                        Text(date, format: .dateTime.weekday(.wide))
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundStyle(Color.cwTextPrimary.opacity(0.7))
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing) {
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
            
            // Expanded List
            if isExpanded {
                Divider()
                VStack(spacing: 0) {
                    ForEach(entries) { entry in
                        HStack {
                            Text(entry.name)
                                .font(.subheadline)
                                .foregroundStyle(Color.cwTextPrimary)
                            Spacer()
                            Text("\(Int(entry.calories))")
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundStyle(Color.cwTextPrimary.opacity(0.8))
                        }
                        .padding()
                        .background(Color.cwSurface.opacity(0.95))
                        .contextMenu { // Context menu for deletion
                            Button(role: .destructive) {
                                withAnimation {
                                    modelContext.delete(entry)
                                }
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                        
                        if entry.id != entries.last?.id {
                            Divider().padding(.leading)
                        }
                    }
                }
                .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
        .padding(.horizontal)
    }
}

