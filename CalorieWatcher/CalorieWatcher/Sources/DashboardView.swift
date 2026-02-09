import SwiftUI
import SwiftData
import Charts

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \UserProfile.age) var profiles: [UserProfile]
    @StateObject private var healthManager = HealthKitManager()
    
    // Mock data for eaten calories (replace with SwiftData query)
    @State private var caloriesEaten: Int = 0
    
    var userProfile: UserProfile? {
        profiles.first
    }
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    if let profile = userProfile {
                        // Ring Chart
                        ZStack {
                            Circle()
                                .stroke(Color.gray.opacity(0.2), lineWidth: 20)
                            
                            let goal = Double(profile.dailyCalorieTarget)
                            let current = Double(caloriesEaten)
                            let progress = min(current / goal, 1.0)
                            
                            Circle()
                                .trim(from: 0, to: progress)
                                .stroke(Color.blue, style: StrokeStyle(lineWidth: 20, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                                .animation(.spring, value: progress)
                            
                            VStack {
                                Text("\(Int(goal - current + healthManager.activeEnergyBurned))")
                                    .font(.system(size: 40, weight: .bold))
                                Text("Remaining")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .frame(height: 250)
                        .padding()
                        
                        // Stats Grid
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 20) {
                            StatCard(title: "Base Goal", value: "\(profile.dailyCalorieTarget)", icon: "target")
                            StatCard(title: "Food Eaten", value: "\(caloriesEaten)", icon: "fork.knife", color: .blue)
                            StatCard(title: "Active Burn", value: "\(Int(healthManager.activeEnergyBurned))", icon: "flame.fill", color: .orange)
                            StatCard(title: "Net Balance", value: "\(caloriesEaten - Int(healthManager.activeEnergyBurned))", icon: "scalemass.fill", color: .green)
                        }
                        .padding()
                        
                        // Recent Meals Stub
                        HStack {
                            Text("Recent Meals")
                                .font(.headline)
                            Spacer()
                        }
                        .padding(.horizontal)
                        
                        // Add list here
                    } else {
                        Text("Please complete your profile")
                    }
                }
            }
            .navigationTitle("Dashboard")
        }
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    var color: Color = .primary
    
    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Text(value)
                .font(.title2)
                .bold()
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}
