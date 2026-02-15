import SwiftUI

struct AppIconView: View {
    var body: some View {
        Image("MiniAppIcon")
            .resizable()
            .scaledToFit()
            .background(Color.white) // Fallback background if transparent
    }
}

#Preview {
    AppIconView()
        .frame(width: 200, height: 200)
        .clipShape(RoundedRectangle(cornerRadius: 40))
}
