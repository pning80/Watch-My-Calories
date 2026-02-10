import SwiftUI

// MARK: - Color Palette
extension Color {
    static let cwPrimary = Color(red: 0.18, green: 0.42, blue: 0.31) // Deep Forest Green
    static let cwSecondary = Color(red: 0.85, green: 0.95, blue: 0.86) // Pale Mint
    static let cwAccent = Color(red: 1.0, green: 0.62, blue: 0.11) // Energetic Orange
    static let cwBackground = Color(red: 0.98, green: 0.98, blue: 0.98) // Soft Off-White
    static let cwSurface = Color.white
    static let cwTextPrimary = Color(red: 0.1, green: 0.1, blue: 0.1)
}

// MARK: - Typography Modifiers
struct CWTitleModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .font(.system(.largeTitle, design: .serif))
            .fontWeight(.bold)
            .foregroundStyle(Color.cwPrimary)
    }
}

struct CWCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding()
            .background(Color.cwSurface)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 4)
    }
}

extension View {
    func cwTitle() -> some View {
        modifier(CWTitleModifier())
    }
    
    func cwCard() -> some View {
        modifier(CWCardModifier())
    }
}
