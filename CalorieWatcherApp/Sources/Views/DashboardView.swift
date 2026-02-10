import SwiftUI
import CoreData

struct DashboardView: View {
    @Environment(\.managedObjectContext) private var viewContext
    
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \FoodLog.timestamp, ascending: false)],
        predicate: NSPredicate(format: "timestamp >= %@", Calendar.current.startOfDay(for: Date()) as NSDate),
        animation: .default)
    private var todatsLogs: FetchedResults<FoodLog>
    
    @StateObject var healthKitService = HealthKitService()
    
    @State private var showingCamera = false
    @State private var capturedImages: [UIImage] = []
    @State private var showingResult = false
    @State private var showingSettings = false
    @State private var refreshID = UUID() // To force redraw if needed
    
    // User Stats (Defaults)
    @AppStorage("dailyCalorieGoal") private var dailyCalorieGoal: Double = 2000
    
    var caloriesConsumed: Double {
        todatsLogs.reduce(0) { $0 + Double($1.calories) }
    }
    
    var netHelper: Double {
        return (dailyCalorieGoal + healthKitService.activeEnergyBurned) - caloriesConsumed
    }
    
    var progress: Double {
        let total = dailyCalorieGoal // Fixed base for circle
        if total == 0 { return 0 }
        return min(caloriesConsumed / total, 1.0)
    }
    
    var body: some View {
        ZStack {
            Color.black.edgesIgnoringSafeArea(.all)
            
            VStack {
                // Header
                HStack {
                    VStack(alignment: .leading) {
                        Text("Good Day,")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                        Text("User")
                            .font(.largeTitle)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    }
                    Spacer()
                    Button(action: { showingSettings = true }) {
                        Image(systemName: "gearshape.fill")
                            .font(.title2)
                            .foregroundColor(.white)
                            .padding()
                            .background(Color.white.opacity(0.1))
                            .clipShape(Circle())
                    }
                }
                .padding()
                
                ScrollView {
                    VStack(spacing: 30) {
                        // Progress Ring
                        ZStack {
                            Circle()
                                .stroke(Color.white.opacity(0.1), lineWidth: 20)
                            
                            // Consumed Ring
                            Circle()
                                .trim(from: 0, to: progress)
                                .stroke(Color.mainGreen, style: StrokeStyle(lineWidth: 20, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                                .animation(.easeOut, value: progress)
                            
                            VStack {
                                Text("\(Int(netHelper))")
                                    .font(.system(size: 50, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                                Text("Kcal Left")
                                    .textCase(.uppercase)
                                    .font(.caption)
                                    .foregroundColor(.gray)
                                
                                if healthKitService.activeEnergyBurned > 0 {
                                    Text("+\(Int(healthKitService.activeEnergyBurned)) Burned")
                                        .font(.caption2)
                                        .foregroundColor(Color.orange)
                                        .padding(.top, 4)
                                }
                            }
                        }
                        .frame(width: 250, height: 250)
                        .padding(.vertical)
                        
                        // Recent Logs
                        VStack(alignment: .leading) {
                            Text("Today's Intake")
                                .font(.headline)
                                .foregroundColor(.gray)
                                .padding(.horizontal)
                            
                            if todatsLogs.isEmpty {
                                Text("No food logged today.")
                                    .foregroundColor(.gray)
                                    .frame(maxWidth: .infinity, alignment: .center)
                                    .padding()
                            } else {
                                ForEach(todatsLogs) { log in
                                    HStack {
                                        VStack(alignment: .leading) {
                                            Text(log.name ?? "Unknown")
                                                .fontWeight(.bold)
                                                .foregroundColor(.white)
                                            Text(log.timestamp ?? Date(), style: .time)
                                                .font(.caption)
                                                .foregroundColor(.gray)
                                        }
                                        Spacer()
                                        Text("\(log.calories)")
                                            .font(.title3)
                                            .fontWeight(.bold)
                                            .foregroundColor(.mainGreen)
                                    }
                                    .padding()
                                    .background(Color.white.opacity(0.05))
                                    .cornerRadius(16)
                                    .padding(.horizontal)
                                }
                            }
                        }
                    }
                    .padding(.bottom, 100) // Space for FAB
                }
            }
            
            // FAB
            VStack {
                Spacer()
                Button(action: {
                    capturedImages = []
                    showingCamera = true
                }) {
                    HStack {
                        Image(systemName: "camera.viewfinder")
                        Text("Scan Food")
                    }
                    .font(.headline)
                    .padding()
                    .padding(.horizontal)
                    .background(Color.mainGreen)
                    .foregroundColor(.black)
                    .cornerRadius(100)
                    .shadow(color: Color.mainGreen.opacity(0.4), radius: 10, x: 0, y: 5)
                }
                .padding(.bottom)
            }
        }
        .onAppear {
            healthKitService.requestAuthorization()
        }
        .fullScreenCover(isPresented: $showingCamera) {
            CameraView(capturedImages: $capturedImages)
                .onDisappear {
                    if !capturedImages.isEmpty {
                        showingResult = true
                    }
                }
        }
        .sheet(isPresented: $showingResult) {
            ResultView(images: capturedImages, healthKitService: healthKitService) {
                showingResult = false
                // Trigger refresh if needed, usually CoreData @FetchRequest handles it
            }
        }
        .sheet(isPresented: $showingSettings) {
            SettingsView()
        }
    }
}

struct SettingsView: View {
    @AppStorage("dailyCalorieGoal") private var dailyCalorieGoal: Double = 2000
    @StateObject private var geminiService = GeminiService()
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Goals")) {
                    HStack {
                        Text("Daily Calorie Goal")
                        Spacer()
                        TextField("2000", value: $dailyCalorieGoal, formatter: NumberFormatter())
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
                
                Section(header: Text("API Configuration")) {
                    SecureField("Gemini API Key", text: $geminiService.apiKey)
                    Link("Get API Key", destination: URL(string: "https://aistudio.google.com/")!)
                }
                
                Section {
                    Text("About Calorie Watcher v1.0")
                }
            }
            .navigationTitle("Settings")
        }
    }
}
