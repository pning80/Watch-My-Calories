import SwiftUI

struct CaptureView: View {
    @State private var captureCount: Int = 0

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Image(systemName: "camera.viewfinder")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 180)
                    .foregroundStyle(.secondary)

                Text("Capture your meal")
                    .font(.title3)
                    .bold()
                Text("Take 1–3 photos from different angles to improve estimation accuracy.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)

                Button {
                    captureCount = min(captureCount + 1, 3)
                } label: {
                    Label("Take Photo (") + Text("\(captureCount)") + Label(")", systemImage: "camera")
                }
                .buttonStyle(.borderedProminent)

                if captureCount > 0 {
                    Button {
                        // Proceed to estimation review
                    } label: {
                        Label("Review & Estimate", systemImage: "checkmark.circle")
                    }
                }

                Spacer()
            }
            .padding()
            .navigationTitle("Capture")
        }
    }
}

#Preview {
    CaptureView()
}
