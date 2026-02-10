import SwiftUI
import SwiftData

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \FoodEntry.timestamp, order: .reverse) private var foodEntries: [FoodEntry]
    @Query private var userProfiles: [UserProfile]
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Header
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
                        
                        if let profile = userProfiles.first {
                            HeroSummaryCard(profile: profile, entries: foodEntries)
                        } else {
                            ContentUnavailableView("Setup Profile", systemImage: "person.crop.circle.badge.plus")
                        }
                        
                        // Recent Entries Section
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Recent Meals")
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundStyle(Color.cwTextPrimary)
                                .padding(.horizontal)
                            
                            if foodEntries.isEmpty {
                                EmptyStateCard()
                            } else {
                                ForEach(foodEntries) { entry in
                                    FoodEntryCard(entry: entry)
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
                    .padding(.bottom, 100) // Space for TabBar
                }
            }
        }
    }
}

// MARK: - Components

struct HeroSummaryCard: View {
    let profile: UserProfile
    let entries: [FoodEntry]
    
    var consumed: Double {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        return entries
            .filter { calendar.startOfDay(for: $0.timestamp) == today }
            .reduce(0) { $0 + $1.calories }
    }
    
    var progress: Double {
        guard profile.targetCalories > 0 else { return 0 }
        return min(consumed / profile.targetCalories, 1.0)
    }
    
    var body: some View {
        HStack(spacing: 20) {
            // Circular Progress
            ZStack {
                Circle()
                    .stroke(Color.cwSecondary, lineWidth: 15)
                
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(
                        AngularGradient(
                            colors: [Color.cwPrimary, Color.cwAccent],
                            center: .center,
                            startAngle: .degrees(0),
                            endAngle: .degrees(360)
                        ),
                        style: StrokeStyle(lineWidth: 15, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.spring(response: 1.0, dampingFraction: 0.7), value: progress)
                
                VStack(spacing: 0) {
                    Text("\(Int(consumed))")
                        .font(.system(size: 28, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color.cwPrimary)
                    Text("kcal")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundStyle(Color.gray)
                }
            }
            .frame(width: 120, height: 120)
            
            // Stats
            VStack(alignment: .leading, spacing: 12) {
                StatRow(label: "Goal", value: "\(Int(profile.targetCalories))", icon: "flag.fill", color: .gray)
                StatRow(label: "Remaining", value: "\(Int(max(0, profile.targetCalories - consumed)))", icon: "flame.fill", color: .cwAccent)
            }
        }
        .cwCard()
        .padding(.horizontal)
    }
}

struct StatRow: View {
    let label: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(.white)
                .padding(6)
                .background(Circle().fill(color))
            
            VStack(alignment: .leading) {
                Text(label)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.cwTextPrimary.opacity(0.8))
                Text(value)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.cwTextPrimary)
            }
        }
    }
}

struct FoodEntryCard: View {
    let entry: FoodEntry
    
    var body: some View {
        HStack(spacing: 16) {
            // Icon Placeholder
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.cwSecondary)
                    .frame(width: 60, height: 60)
                
                Text(String(entry.name.prefix(1)))
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.cwPrimary)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(entry.name)
                    .font(.headline)
                    .foregroundStyle(Color.cwTextPrimary)
                
                Text(entry.quantity)
                    .font(.subheadline)
                    .foregroundStyle(Color.gray)
            }
            
            Spacer()
            
            Text("\(Int(entry.calories))")
                .font(.system(.title3, design: .rounded))
                .fontWeight(.bold)
                .foregroundStyle(Color.cwPrimary)
        }
        .cwCard()
        .padding(.horizontal)
    }
}

// Removed EmptyStateCard definition here as it is moved to Components.swift

