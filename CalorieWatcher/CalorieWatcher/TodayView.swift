import SwiftUI

struct TodayView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                ProgressView(value: 0.4) {
                    Text("Today's Progress")
                }
                .progressViewStyle(.linear)

                HStack(spacing: 16) {
                    VStack {
                        Text("Consumed")
                            .font(.caption)
                        Text("800 kcal")
                            .font(.title3)
                            .bold()
                    }
                    Divider()
                    VStack {
                        Text("Goal")
                            .font(.caption)
                        Text("2000 kcal")
                            .font(.title3)
                            .bold()
                    }
                }
                .frame(maxWidth: .infinity)

                Button {
                    // Start capture flow
                } label: {
                    Label("Add from Camera", systemImage: "camera")
                }
                .buttonStyle(.borderedProminent)

                Button {
                    // Add manual entry
                } label: {
                    Label("Add Manual Entry", systemImage: "plus")
                }
                .buttonStyle(.bordered)

                Spacer()
            }
            .padding()
            .navigationTitle("Today")
        }
    }
}

#Preview {
    TodayView()
}
