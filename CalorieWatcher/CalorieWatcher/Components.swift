import SwiftUI
import SwiftData

// Wrapper to make UIImage Identifiable for fullScreenCover
struct ImageWrapper: Identifiable {
    let id = UUID()
    let image: UIImage
}

struct FullScreenImageView: View {
    let image: UIImage
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .ignoresSafeArea()
            
            VStack {
                HStack {
                    Spacer()
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(Color.white, Color.black.opacity(0.5))
                            .padding()
                    }
                }
                Spacer()
            }
        }
    }
}

struct EmptyStateCard: View {
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "camera.viewfinder")
                .font(.system(size: 40))
                .foregroundStyle(Color.cwSecondary)
                .padding()
                .background(Circle().fill(Color.cwPrimary))
            
            Text("No meals tracked yet")
                .font(.headline)
                .foregroundStyle(Color.cwTextPrimary)
            
            Text("Tap the camera tab to scan your first meal.")
                .font(.subheadline)
                .foregroundStyle(Color.gray)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(40)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(Color.gray.opacity(0.3), style: StrokeStyle(lineWidth: 1, dash: [5]))
        )
        .padding(.horizontal)
    }
}

struct HeroSummaryCard: View {
    let targetCalories: Double
    var burnedCalories: Double = 0
    let entries: [FoodEntry]
    
    var consumed: Double {
        entries.reduce(0) { $0 + $1.calories }
    }
    
    var effectiveTarget: Double {
        targetCalories + burnedCalories
    }
    
    var progress: Double {
        guard effectiveTarget > 0 else { return 0 }
        return min(consumed / effectiveTarget, 1.0)
    }
    
    var remaining: Double {
        max(0, effectiveTarget - consumed)
    }
    var burnedProgress: Double {
        guard effectiveTarget > 0 else { return 0 }
        return min(burnedCalories / effectiveTarget, 1.0)
    }

    var body: some View {
        HStack(spacing: 20) {
            ZStack {
                // Background Ring (Total Budget / Remaining if seen as inverse)
                Circle()
                    .stroke(Color.cwSecondary, lineWidth: 15)
                
                // Burned Ring (Orange, starts at 0, goes to burnedProgress)
                // This is laid OUT UNDER the Consumed ring or stacked with it.
                // It represents the "bonus" or expanded part of the target.
                Circle()
                    .trim(from: 0, to: min(progress + burnedProgress, 1.0))
                    .stroke(
                        Color.cwAccent,
                        style: StrokeStyle(lineWidth: 15, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.spring(response: 1.0, dampingFraction: 0.7), value: progress + burnedProgress)

                // Foreground Ring (Consumed, Green, over the orange background)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(
                        Color.cwPrimary,
                        style: StrokeStyle(lineWidth: 15, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.spring(response: 1.0, dampingFraction: 0.7), value: progress)
                
                VStack(spacing: 0) {
                    Text("\(Int(consumed))")
                        .font(.system(size: 28, weight: .heavy, design: .rounded))
                        .foregroundStyle(Color.cwPrimary)
                    Text("kcal")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundStyle(Color.gray)
                }
            }
            .frame(width: 120, height: 120)
            
            VStack(alignment: .leading, spacing: 12) {
                StatRow(label: "Goal", value: "\(Int(targetCalories))", icon: "flag.fill", color: .gray)
                
                if burnedCalories > 0 {
                    StatRow(label: "Burned", value: "\(Int(burnedCalories))", icon: "flame.fill", color: .cwAccent)
                }
                
                // Remaining -> Light Green (cwSecondary)
                StatRow(label: "Remaining", value: "\(Int(remaining))", icon: "chart.bar.fill", color: .cwSecondary)
            }
        }
        .cwCard()
        .padding(.horizontal)
    }
}

struct StatRow: View {
    let label: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .font(.caption)
                // Explicitly use Color.cwPrimary to fix type inference error
                .foregroundStyle(color == .cwSecondary ? Color.cwPrimary : Color.white)
                .padding(6)
                .background(Circle().fill(color))
            
            VStack(alignment: .leading) {
                Text(label)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.cwTextPrimary.opacity(0.8))
                Text(value)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.cwTextPrimary)
            }
        }
    }
}

struct FoodEntryCard: View {
    let entry: FoodEntry
    var onThumbnailTap: ((UIImage) -> Void)? = nil
    @State private var thumbnail: UIImage?
    
    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.cwSecondary)
                    .frame(width: 48, height: 48)
                
                if let thumbnail = thumbnail {
                    Button(action: { onThumbnailTap?(thumbnail) }) {
                        Image(uiImage: thumbnail)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 40, height: 40)
                            .clipShape(Circle())
                            .shadow(radius: 1)
                    }
                    .buttonStyle(PlainButtonStyle())
                } else {
                    Text(String(entry.name.prefix(1)))
                        .font(.headline)
                        .foregroundStyle(Color.cwPrimary)
                }
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.cwTextPrimary)
                    .lineLimit(1)
                
                HStack(spacing: 4) {
                    Text(entry.timestamp, style: .time)
                        .font(.caption2)
                        .foregroundStyle(Color.gray)
                    
                    Text("•")
                        .font(.caption2)
                        .foregroundStyle(Color.gray)
                    
                    Text(entry.quantity)
                        .font(.caption2)
                        .foregroundStyle(Color.gray)
                }
            }
            
            Spacer()
            
            Text("\(Int(entry.calories))")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundStyle(Color.cwPrimary)
        }
        .padding(10)
        .background(Color.cwSurface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: Color.black.opacity(0.03), radius: 3, x: 0, y: 1)
        .padding(.horizontal)
        .onAppear {
            if let id = entry.imageID {
                self.thumbnail = ImageStorage.shared.load(id: id)
            }
        }
    }
}

// MARK: - Local Components
struct MealSection: View {
    let title: String
    let entries: [FoodEntry]
    var onImageTap: (UIImage) -> Void
    @Environment(\.modelContext) private var modelContext
    
    var totalCalories: Double {
        entries.reduce(0) { $0 + $1.calories }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.cwTextPrimary)
                
                Spacer()
                
                Text("\(Int(totalCalories)) kcal")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.cwPrimary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.cwSecondary)
                    .clipShape(Capsule())
            }
            .padding(.horizontal)
            
            VStack(spacing: 8) {
                ForEach(entries) { entry in
                    FoodEntryCard(entry: entry, onThumbnailTap: onImageTap)
                        .contextMenu {
                            Button(role: .destructive) {
                                modelContext.delete(entry)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                }
            }
        }
    }
}

