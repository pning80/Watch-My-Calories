import SwiftUI
import SwiftData

struct ProfileOnboardingView: View {
    @Environment(\.modelContext) private var modelContext
    @Binding var isProfileComplete: Bool
    
    @State private var age: Double = 30
    @State private var height: Double = 170
    @State private var weight: Double = 70
    @State private var gender: Gender = .male
    @State private var activityLevel: ActivityLevel = .moderate
    @State private var goal: Goal = .maintain
    
    var body: some View {
        Form {
            Section(header: Text("Personal Details")) {
                Picker("Gender", selection: $gender) {
                    ForEach(Gender.allCases, id: \.self) { gender in
                        Text(gender.rawValue.capitalized).tag(gender)
                    }
                }
                
                VStack {
                    HStack {
                        Text("Age")
                        Spacer()
                        Text("\(Int(age))")
                    }
                    Slider(value: $age, in: 18...100, step: 1)
                }
                
                VStack {
                    HStack {
                        Text("Height (cm)")
                        Spacer()
                        Text("\(Int(height))")
                    }
                    Slider(value: $height, in: 140...220, step: 1)
                }
                
                VStack {
                    HStack {
                        Text("Weight (kg)")
                        Spacer()
                        Text("\(Int(weight))")
                    }
                    Slider(value: $weight, in: 40...150, step: 1)
                }
            }
            
            Section(header: Text("Lifestyle & Goals")) {
                Picker("Activity Level", selection: $activityLevel) {
                    ForEach(ActivityLevel.allCases, id: \.self) { level in
                        Text(level.rawValue.capitalized).tag(level)
                    }
                }
                
                Picker("Goal", selection: $goal) {
                    ForEach(Goal.allCases, id: \.self) { goal in
                        Text(goal.rawValue.capitalized).tag(goal)
                    }
                }
            }
            
            Section {
                Button("Save Profile") {
                    let profile = UserProfile(
                        age: Int(age),
                        gender: gender,
                        height: height,
                        weight: weight,
                        activityLevel: activityLevel,
                        goal: goal
                    )
                    modelContext.insert(profile)
                    try? modelContext.save()
                    isProfileComplete = true
                }
            }
        }
        .navigationTitle("Your Profile")
    }
}
