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
                    HStack(spacing: 4) {
                        Image(systemName: "camera")
                        Text("Take Photo (\(captureCount))")
                    }
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
