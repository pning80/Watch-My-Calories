import SwiftUI
import SwiftData

struct ContentView: View {
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text("Hello, Calorie Watcher!")
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
