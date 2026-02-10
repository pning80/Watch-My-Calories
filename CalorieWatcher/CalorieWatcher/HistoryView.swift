import SwiftUI

struct HistoryView: View {
    var body: some View {
        NavigationStack {
            List {
                Section("Recent Days") {
                    ForEach(0..<7, id: \.self) { offset in
                        let date = Calendar.current.date(byAdding: .day, value: -offset, to: Date()) ?? Date()
                        HStack {
                            Text(date, style: .date)
                            Spacer()
                            Text("1234 kcal")
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("History")
        }
    }
}

#Preview {
    HistoryView()
}
