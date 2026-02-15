import SwiftUI

struct AppIconView: View {
    var body: some View {
        ZStack {
            // Background Gradient
            LinearGradient(
                colors: [Color.cwPrimary, Color.cwPrimary.opacity(0.8)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            
            // Decorative Ring Background
            Circle()
                .stroke(Color.black.opacity(0.1), lineWidth: 40)
                .padding(60)
            
            // Progress Ring Style
            Circle()
                .trim(from: 0, to: 0.75)
                .stroke(
                    LinearGradient(
                        colors: [Color.cwSecondary, Color.cwAccent],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    style: StrokeStyle(lineWidth: 40, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .padding(60)
                .shadow(color: Color.black.opacity(0.2), radius: 10, x: 0, y: 5)
            
            // Flame Icon
            Image(systemName: "flame.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 180)
                .foregroundStyle(Color.white)
                .shadow(color: Color.black.opacity(0.2), radius: 5, x: 0, y: 5)
        }
        // Standard App Icon Size for export (1024x1024), but scalable
        .aspectRatio(1, contentMode: .fit)
        .background(Color.cwPrimary) // Fallback
    }
}

#Preview {
    AppIconView()
        .frame(width: 200, height: 200)
        .clipShape(RoundedRectangle(cornerRadius: 40))
}
