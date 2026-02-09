import SwiftUI

struct FoodResultView: View {
    @State var analysisResult: FoodAnalysisResult
    @Binding var isPresented: Bool
    
    var body: some View {
        NavigationStack {
            List {
                Section(header: Text("Summary")) {
                    HStack {
                        Text("Total Calories")
                        Spacer()
                        Text("\(analysisResult.totalCalories)")
                            .bold()
                            .foregroundColor(.green)
                    }
                }
                
                Section(header: Text("Detected Items")) {
                    ForEach(analysisResult.foodItems) { item in
                        VStack(alignment: .leading) {
                            HStack {
                                Text(item.name)
                                    .font(.headline)
                                Spacer()
                                Text("\(item.calories) kcal")
                            }
                            Text("\(item.quantity) • \(Int(item.protein))g P • \(Int(item.carbs))g C • \(Int(item.fat))g F")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            if item.confidence == "Low" {
                                Text("⚠️ Low Confidence - Verify Portion")
                                    .font(.caption2)
                                    .foregroundColor(.orange)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Analysis Result")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Discard") { isPresented = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save Log") {
                        // TODO: Save to SwiftData
                        isPresented = false
                    }
                }
            }
        }
    }
}
