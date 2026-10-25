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
    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .scaleEffect(scale)
                .offset(offset)
                .gesture(
                    MagnifyGesture()
                        .onChanged { value in
                            scale = lastScale * value.magnification
                        }
                        .onEnded { _ in
                            lastScale = scale
                            if scale < 1.0 {
                                withAnimation(.easeOut(duration: 0.2)) {
                                    scale = 1.0
                                    lastScale = 1.0
                                    offset = .zero
                                    lastOffset = .zero
                                }
                            }
                        }
                        .simultaneously(with:
                            DragGesture()
                                .onChanged { value in
                                    if scale > 1.0 {
                                        offset = CGSize(
                                            width: lastOffset.width + value.translation.width,
                                            height: lastOffset.height + value.translation.height
                                        )
                                    }
                                }
                                .onEnded { _ in
                                    lastOffset = offset
                                }
                        )
                )
                .onTapGesture(count: 2) {
                    withAnimation(.easeOut(duration: 0.2)) {
                        if scale > 1.0 {
                            scale = 1.0
                            lastScale = 1.0
                            offset = .zero
                            lastOffset = .zero
                        } else {
                            scale = 3.0
                            lastScale = 3.0
                        }
                    }
                }
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
    @ScaledMetric(relativeTo: .largeTitle) private var glyphSize: CGFloat = 40
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "camera.viewfinder")
                .font(.system(size: glyphSize))
                .foregroundStyle(Color.cwSecondary)
                .padding()
                .background(Circle().fill(Color.cwPrimary))
            
            Text("No meals tracked yet")
                .font(.headline)
                .foregroundStyle(Color.cwTextPrimary)
            
            Text("Tap here to log your first meal.")
                .font(.subheadline)
                .foregroundStyle(Color.cwTextSecondary)
                .multilineTextAlignment(.center)

            Label("Log food", systemImage: "plus.circle.fill")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color.cwPrimary)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(40)
        .background(
            RoundedRectangle(cornerRadius: CWRadius.hero)
                .strokeBorder(Color.cwTextTertiary.opacity(0.4), style: StrokeStyle(lineWidth: 1, dash: [5]))
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
    
    // Earned-calories model: HealthKit "burned" extends the day's calorie budget.
    var effectiveGoal: Double {
        max(targetCalories + burnedCalories, 0)
    }

    var isOver: Bool {
        effectiveGoal > 0 && consumed > effectiveGoal
    }

    /// One self-consistent color for the ring + center number across states.
    var ringColor: Color {
        isOver ? .cwOverGoal : .cwPrimary
    }

    var ringAccessibilityLabel: String {
        let goal = Int(effectiveGoal)
        let eaten = Int(consumed)
        if isOver {
            return "\(eaten) of \(goal) kilocalories eaten, over by \(Int(consumed - effectiveGoal))"
        }
        return "\(eaten) of \(goal) kilocalories eaten, \(Int(max(remaining, 0))) remaining"
    }
    
    var progress: Double {
        guard effectiveGoal > 0 else { return 0 }
        return min(consumed / effectiveGoal, 1.0)
    }
    
    /// Signed remaining — negative once the user is over the effective goal.
    var remaining: Double {
        effectiveGoal - consumed
    }

    // Macro totals
    var totalProtein: Double { entries.compactMap(\.protein).reduce(0, +) }
    var totalCarbs: Double { entries.compactMap(\.carbs).reduce(0, +) }
    var totalFat: Double { entries.compactMap(\.fat).reduce(0, +) }
    var hasMacroData: Bool { totalProtein > 0 || totalCarbs > 0 || totalFat > 0 }

    // Calorie contribution per macro (protein/carbs = 4 cal/g, fat = 9 cal/g)
    var proteinCals: Double { totalProtein * 4 }
    var carbsCals: Double { totalCarbs * 4 }
    var fatCals: Double { totalFat * 9 }
    var totalMacroCals: Double { proteinCals + carbsCals + fatCals }

    var body: some View {
        VStack(spacing: 0) {
        HStack(spacing: 20) {
            ZStack {
                // Track ring (full budget)
                Circle()
                    .stroke(Color.cwSecondary, lineWidth: 15)

                // Progress ring — one ring: cwPrimary under budget, cwOverGoal once over.
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(
                        ringColor,
                        style: StrokeStyle(lineWidth: 15, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.spring(response: 1.0, dampingFraction: 0.7), value: progress)
                    .animation(.easeInOut(duration: 0.3), value: isOver)

                VStack(spacing: 0) {
                    Text("\(Int(consumed))")
                        .cwNumeral(.hero)
                        .foregroundStyle(ringColor)
                        .contentTransition(.numericText())
                    Text("kcal")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundStyle(Color.cwTextSecondary)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(ringAccessibilityLabel)
                .accessibilityIdentifier(AccessibilityID.Dashboard.consumedCalories)
            }
            .frame(width: 120, height: 120)
            
            VStack(alignment: .leading, spacing: 12) {
                StatRow(label: "Goal", value: "\(Int(targetCalories))", icon: "flag.fill", color: .gray, accessibilityID: AccessibilityID.Dashboard.goalValue)

                if burnedCalories > 0 {
                    StatRow(label: "Burned", value: "+\(Int(burnedCalories))", icon: "flame.fill", color: .cwAccent)
                }

                if isOver {
                    StatRow(label: "Over by", value: "\(Int(consumed - effectiveGoal))", icon: "exclamationmark.triangle.fill", color: .cwOverGoal, accessibilityID: AccessibilityID.Dashboard.remainingValue)
                } else {
                    StatRow(label: "Remaining", value: "\(Int(remaining))", icon: "chart.bar.fill", color: .cwSecondary, accessibilityID: AccessibilityID.Dashboard.remainingValue)
                }
            }
        }

        // Macro breakdown
        if hasMacroData {
            Divider()
                .padding(.top, 12)
                .padding(.bottom, 8)

            // Labels + values
            HStack(spacing: 0) {
                macroLabel("Protein", grams: totalProtein, cals: proteinCals, color: .cwMacroProtein)
                Spacer()
                macroLabel("Carbs", grams: totalCarbs, cals: carbsCals, color: .cwMacroCarbs)
                Spacer()
                macroLabel("Fat", grams: totalFat, cals: fatCals, color: .cwMacroFat)
            }

            // Stacked proportional bar
            MacroProportionalBar(
                proteinCals: proteinCals,
                carbsCals: carbsCals,
                fatCals: fatCals
            )
            .padding(.top, 6)
        }
        } // VStack
        .frame(maxWidth: .infinity)
        .cwCard()
        .padding(.horizontal)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(AccessibilityID.Dashboard.heroCard)
    }

    private func macroLabel(_ label: String, grams: Double, cals: Double, color: Color) -> some View {
        let pct = totalMacroCals > 0 ? Int((cals / totalMacroCals * 100).rounded()) : 0
        return VStack(spacing: 2) {
            HStack(spacing: 4) {
                Circle()
                    .fill(color)
                    .frame(width: 8, height: 8)
                Text(label)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Text("\(Int(grams))g")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(Color.cwTextPrimary)
            Text("\(pct)%")
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
    }
}

// MARK: - Macro Proportional Bar

struct MacroProportionalBar: View {
    let proteinCals: Double
    let carbsCals: Double
    let fatCals: Double
    var height: CGFloat = 8

    private var macroAccessibilityLabel: String {
        let total = proteinCals + carbsCals + fatCals
        guard total > 0 else { return "No macro data" }
        let p = Int((proteinCals / total * 100).rounded())
        let c = Int((carbsCals / total * 100).rounded())
        let f = Int((fatCals / total * 100).rounded())
        return "Macros: protein \(p) percent, carbs \(c) percent, fat \(f) percent"
    }

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let total = max(proteinCals + carbsCals + fatCals, 1)
            let pWidth = w * (proteinCals / total)
            let cWidth = w * (carbsCals / total)
            let fWidth = w * (fatCals / total)

            HStack(spacing: 1.5) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color.cwMacroProtein)
                    .frame(width: max(pWidth, proteinCals > 0 ? 4 : 0))
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color.cwMacroCarbs)
                    .frame(width: max(cWidth, carbsCals > 0 ? 4 : 0))
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color.cwMacroFat)
                    .frame(width: max(fWidth, fatCals > 0 ? 4 : 0))
            }
            .animation(.easeInOut(duration: 0.5), value: total)
        }
        .frame(height: height)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(macroAccessibilityLabel)
    }
}

// MARK: - Compact Macro Row (History)

struct CompactMacroRow: View {
    let totalProtein: Double
    let totalCarbs: Double
    let totalFat: Double

    init(entries: [FoodEntry]) {
        self.totalProtein = entries.compactMap(\.protein).reduce(0, +)
        self.totalCarbs = entries.compactMap(\.carbs).reduce(0, +)
        self.totalFat = entries.compactMap(\.fat).reduce(0, +)
    }

    init(protein: Double?, carbs: Double?, fat: Double?) {
        self.totalProtein = protein ?? 0
        self.totalCarbs = carbs ?? 0
        self.totalFat = fat ?? 0
    }

    private var hasMacroData: Bool { totalProtein > 0 || totalCarbs > 0 || totalFat > 0 }
    private var proteinCals: Double { totalProtein * 4 }
    private var carbsCals: Double { totalCarbs * 4 }
    private var fatCals: Double { totalFat * 9 }
    private var totalMacroCals: Double { proteinCals + carbsCals + fatCals }

    private func pct(_ cals: Double) -> Int {
        totalMacroCals > 0 ? Int((cals / totalMacroCals * 100).rounded()) : 0
    }

    var body: some View {
        if hasMacroData {
            HStack(spacing: 12) {
                MacroProportionalBar(
                    proteinCals: proteinCals,
                    carbsCals: carbsCals,
                    fatCals: fatCals,
                    height: 6
                )

                HStack(spacing: 8) {
                    compactMacro("P", grams: totalProtein, pct: pct(proteinCals), color: .cwMacroProtein)
                    compactMacro("C", grams: totalCarbs, pct: pct(carbsCals), color: .cwMacroCarbs)
                    compactMacro("F", grams: totalFat, pct: pct(fatCals), color: .cwMacroFat)
                }
                .fixedSize()
            }
            .accessibilityIdentifier(AccessibilityID.History.dayCardMacros)
        }
    }

    private func compactMacro(_ label: String, grams: Double, pct: Int, color: Color) -> some View {
        HStack(alignment: .top, spacing: 2) {
            Circle()
                .fill(color)
                .frame(width: 6, height: 6)
                .padding(.top, 3)
            VStack(alignment: .leading, spacing: 0) {
                Text("\(label): \(Int(grams))g")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text("\(pct)%")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
    }
}

struct InlineMacroRow: View {
    let protein: Double?
    let carbs: Double?
    let fat: Double?

    private var hasMacroData: Bool {
        (protein ?? 0) > 0 || (carbs ?? 0) > 0 || (fat ?? 0) > 0
    }

    init(protein: Double?, carbs: Double?, fat: Double?) {
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
    }

    init(entry: FoodEntry) {
        self.protein = entry.protein
        self.carbs = entry.carbs
        self.fat = entry.fat
    }

    init(entries: [FoodEntry]) {
        let p = entries.compactMap(\.protein).reduce(0, +)
        let c = entries.compactMap(\.carbs).reduce(0, +)
        let f = entries.compactMap(\.fat).reduce(0, +)
        self.protein = p > 0 ? p : nil
        self.carbs = c > 0 ? c : nil
        self.fat = f > 0 ? f : nil
    }

    var body: some View {
        if hasMacroData {
            HStack(spacing: 8) {
                if let p = protein, p > 0 { macroChip("P", grams: p, color: .cwMacroProtein) }
                if let c = carbs, c > 0 { macroChip("C", grams: c, color: .cwMacroCarbs) }
                if let f = fat, f > 0 { macroChip("F", grams: f, color: .cwMacroFat) }
            }
        }
    }

    private func macroChip(_ label: String, grams: Double, color: Color) -> some View {
        HStack(spacing: 2) {
            Circle()
                .fill(color)
                .frame(width: 5, height: 5)
            Text("\(label): \(Int(grams))g")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}

struct StatRow: View {
    let label: String
    let value: String
    let icon: String
    let color: Color
    var accessibilityID: String? = nil

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
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier(accessibilityID ?? label)
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
                        .foregroundStyle(Color.cwTextSecondary)

                    Text("•")
                        .font(.caption2)
                        .foregroundStyle(Color.cwTextSecondary)

                    Text(SettingsStore.shared.unitSystem.convertQuantity(entry.quantity))
                        .font(.caption2)
                        .foregroundStyle(Color.cwTextSecondary)
                }
            }
            
            Spacer()
            
            Text("\(Int(entry.calories))")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundStyle(Color.cwPrimary)
        }
        .padding(10)
        .cwSurface(role: .row)
        .padding(.horizontal)
        .onAppear {
            if let id = entry.imageID {
                self.thumbnail = ImageStorage.shared.load(id: id)
            }
        }
    }
}

// MARK: - Group Card Concept

struct FoodEntryGroupEdit: Identifiable {
    let id = UUID()
    let items: [FoodEntry]
}

struct FoodEntryGroup {
    let id = UUID()
    var items: [FoodEntry]
    
    var representativeImageID: UUID? {
        items.first(where: { $0.imageID != nil })?.imageID
    }
    
    var representativeName: String? {
        items.first(where: { $0.mealName != nil })?.mealName
    }
    
    var totalCalories: Double {
        items.reduce(0) { $0 + $1.calories }
    }

    var totalProtein: Double { items.compactMap(\.protein).reduce(0, +) }
    var totalCarbs: Double { items.compactMap(\.carbs).reduce(0, +) }
    var totalFat: Double { items.compactMap(\.fat).reduce(0, +) }
    var hasMacroData: Bool { totalProtein > 0 || totalCarbs > 0 || totalFat > 0 }
}

struct FoodEntryGroupCard: View {
    let group: FoodEntryGroup
    var onThumbnailTap: ((UIImage) -> Void)? = nil
    var onEdit: ((FoodEntry) -> Void)? = nil
    var onEditGroup: (([FoodEntry]) -> Void)? = nil
    var onView: ((FoodEntry) -> Void)? = nil
    var onViewGroup: (([FoodEntry]) -> Void)? = nil
    var onDelete: ((FoodEntry) -> Void)? = nil
    var onDeleteGroup: (([FoodEntry]) -> Void)? = nil

    @State private var thumbnail: UIImage?
    @State private var isExpanded = false
    
    var displayedTitle: String {
        if let userDefined = group.representativeName, !userDefined.isEmpty {
            return userDefined
        }
        
        // Fallback styling if no user-defined or Gemini name
        if group.items.count == 1 {
            return group.items[0].name
        } else if let firstItem = group.items.first {
            return "\(firstItem.name) + \(group.items.count - 1) more"
        } else {
            return "Meal"
        }
    }
    
    private var summaryRow: some View {
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
                } else if let first = displayedTitle.first {
                    Text(String(first))
                        .font(.headline)
                        .foregroundStyle(Color.cwPrimary)
                }
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(displayedTitle)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.cwTextPrimary)
                    .lineLimit(1)

                if let firstTimestamp = group.items.first?.timestamp {
                    Text(firstTimestamp, style: .time)
                        .font(.caption2)
                        .foregroundStyle(Color.cwTextSecondary)
                }

                if group.hasMacroData {
                    MacroProportionalBar(
                        proteinCals: group.totalProtein * 4,
                        carbsCals: group.totalCarbs * 4,
                        fatCals: group.totalFat * 9,
                        height: 4
                    )
                }
            }

            Spacer()

            HStack(spacing: 6) {
                Text("\(Int(group.totalCalories)) kcal")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.cwPrimary)

                if group.items.count > 1 {
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(Color.gray)
                        .rotationEffect(.degrees(isExpanded ? 90 : 0))
                }
            }
        }
        .padding()
        .contentShape(Rectangle())
    }

    var body: some View {
        VStack(spacing: 0) {
            // Main Summary Row
            if group.items.count > 1 {
                summaryRow
                    .onTapGesture { withAnimation(.snappy) { isExpanded.toggle() } }
                    .contextMenu {
                        Button {
                            onViewGroup?(group.items)
                        } label: {
                            Label("View", systemImage: "eye")
                        }
                        Button {
                            onEditGroup?(group.items)
                        } label: {
                            Label("Edit", systemImage: "pencil")
                        }
                        Button(role: .destructive) {
                            onDeleteGroup?(group.items)
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
            } else {
                summaryRow
                    .contextMenu {
                        if let item = group.items.first {
                            Button {
                                onView?(item)
                            } label: {
                                Label("View", systemImage: "eye")
                            }
                            Button {
                                onEdit?(item)
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            Button(role: .destructive) {
                                onDelete?(item)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
            }

            // Sub-items List
            if isExpanded && group.items.count > 1 {
                VStack(spacing: 0) {
                    Divider()
                        .padding(.leading, 64)
                    
                    ForEach(group.items) { item in
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 8) {
                                Circle()
                                    .fill(Color.gray.opacity(0.3))
                                    .frame(width: 4, height: 4)

                                Text(item.name)
                                    .font(.caption)
                                    .foregroundStyle(Color.cwTextPrimary.opacity(0.8))
                                    .lineLimit(1)

                                Spacer()

                                Text("\(Int(item.calories))")
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .foregroundStyle(Color.cwTextSecondary)
                            }

                            InlineMacroRow(entry: item)
                                .padding(.leading, 12)
                        }
                        .padding(.vertical, 8)
                        .padding(.leading, 64)
                        .padding(.trailing, 16)
                        .contentShape(Rectangle()) // Make row tappable for context menu
                        .contextMenu {
                            Button {
                                onView?(item)
                            } label: {
                                Label("View", systemImage: "eye")
                            }
                            Button {
                                onEdit?(item)
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            Button(role: .destructive) {
                                onDelete?(item)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                        
                        if item.id != group.items.last?.id {
                            Divider()
                                .padding(.leading, 64)
                        }
                    }
                }
                .padding(.bottom, 8)
                .transition(.opacity)
            }
        }
        .cwSurface(role: .card)
        .padding(.horizontal)
        .sensoryFeedback(.selection, trigger: isExpanded)
        .onAppear {
            if let id = group.representativeImageID {
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
    var onEdit: ((FoodEntry) -> Void)? = nil
    var onEditGroup: (([FoodEntry]) -> Void)? = nil
    var onView: ((FoodEntry) -> Void)? = nil
    var onViewGroup: (([FoodEntry]) -> Void)? = nil
    @Environment(\.modelContext) private var modelContext
    
    var totalCalories: Double {
        entries.reduce(0) { $0 + $1.calories }
    }
    
    var groupedEntries: [FoodEntryGroup] {
        var groups: [FoodEntryGroup] = []
        var currentGroupItems: [FoodEntry] = []
        var currentImageID: UUID? = nil
        
        for entry in entries {
            if entry.imageID == nil {
                // Standalone item without an image
                if !currentGroupItems.isEmpty {
                    groups.append(FoodEntryGroup(items: currentGroupItems))
                    currentGroupItems = []
                }
                groups.append(FoodEntryGroup(items: [entry]))
                currentImageID = nil
            } else if entry.imageID == currentImageID {
                // Same contiguous image group
                currentGroupItems.append(entry)
            } else {
                // New image group
                if !currentGroupItems.isEmpty {
                    groups.append(FoodEntryGroup(items: currentGroupItems))
                }
                currentGroupItems = [entry]
                currentImageID = entry.imageID
            }
        }
        
        if !currentGroupItems.isEmpty {
            groups.append(FoodEntryGroup(items: currentGroupItems))
        }
        
        return groups
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
            
            VStack(spacing: 12) {
                ForEach(groupedEntries, id: \.id) { group in
                    FoodEntryGroupCard(
                        group: group,
                        onThumbnailTap: onImageTap,
                        onEdit: onEdit,
                        onEditGroup: onEditGroup,
                        onView: onView,
                        onViewGroup: onViewGroup,
                        onDelete: { item in
                            if let imageID = item.imageID {
                                let otherEntries = entries.filter { $0.imageID == imageID && $0.id != item.id }
                                if otherEntries.isEmpty {
                                    ImageStorage.shared.delete(id: imageID)
                                }
                            }
                            modelContext.delete(item)
                        },
                        onDeleteGroup: { items in
                            let deletingIDs = Set(items.map { $0.id })
                            for item in items {
                                if let imageID = item.imageID {
                                    let otherEntries = entries.filter { $0.imageID == imageID && !deletingIDs.contains($0.id) }
                                    if otherEntries.isEmpty {
                                        ImageStorage.shared.delete(id: imageID)
                                    }
                                }
                                modelContext.delete(item)
                            }
                        }
                    )
                }
            }
        }
        .accessibilityIdentifier(AccessibilityID.Dashboard.mealSection)
    }
}

// MARK: - Edit Food Entry

struct EditFoodEntryView: View {
    @Environment(\.dismiss) private var dismiss
    
    let entry: FoodEntry
    
    @State private var name: String
    @State private var mealName: String
    @State private var caloriesText: String
    @State private var quantity: String
    @State private var mealType: MealType
    @State private var proteinText: String
    @State private var carbsText: String
    @State private var fatText: String
    @State private var showNutrition: Bool

    init(entry: FoodEntry) {
        self.entry = entry
        _name = State(initialValue: entry.name)
        _mealName = State(initialValue: entry.mealName ?? "")
        _caloriesText = State(initialValue: String(format: "%g", entry.calories))
        _quantity = State(initialValue: entry.quantity)
        _mealType = State(initialValue: entry.mealType)
        _proteinText = State(initialValue: entry.protein.map { String(format: "%g", $0) } ?? "")
        _carbsText = State(initialValue: entry.carbs.map { String(format: "%g", $0) } ?? "")
        _fatText = State(initialValue: entry.fat.map { String(format: "%g", $0) } ?? "")
        _showNutrition = State(initialValue: entry.protein != nil || entry.carbs != nil || entry.fat != nil)
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
        && (Double(caloriesText) ?? -1) > 0
        && !quantity.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Food info section
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Food Details")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            VStack(spacing: 12) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Group Title (Meal Name)")
                                        .font(.caption)
                                        .foregroundStyle(Color.cwTextSecondary)
                                    TextField("Meal Name (e.g., Chicken & Rice)", text: $mealName)
                                        .textFieldStyle(.roundedBorder)
                                }
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Item Name")
                                        .font(.caption)
                                        .foregroundStyle(Color.cwTextSecondary)
                                    TextField("Food name", text: $name)
                                        .textFieldStyle(.roundedBorder)
                                }

                                TextField("Calories", text: $caloriesText)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.decimalPad)

                                TextField(SettingsStore.shared.unitSystem == .metric ? "Quantity (e.g. 200 g, 250 ml)" : "Quantity (e.g. 1 cup, 6 oz)", text: $quantity)
                                    .textFieldStyle(.roundedBorder)
                            }
                        }
                        .cwCard()
                        .padding(.horizontal)

                        // Meal type picker
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Meal")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            Picker("Meal", selection: $mealType) {
                                ForEach(MealType.allCases, id: \.self) { type in
                                    Text(type.rawValue).tag(type)
                                }
                            }
                            .pickerStyle(.segmented)
                        }
                        .cwCard()
                        .padding(.horizontal)

                        // Optional nutrition
                        VStack(alignment: .leading, spacing: 12) {
                            DisclosureGroup("Nutrition Details (optional)", isExpanded: $showNutrition) {
                                VStack(spacing: 12) {
                                    HStack(spacing: 12) {
                                        EditNutrientField(label: "Protein (g)", text: $proteinText)
                                        EditNutrientField(label: "Carbs (g)", text: $carbsText)
                                        EditNutrientField(label: "Fat (g)", text: $fatText)
                                    }
                                }
                                .padding(.top, 8)
                            }
                            .font(.headline)
                            .foregroundStyle(Color.cwTextPrimary)
                        }
                        .cwCard()
                        .padding(.horizontal)
                    }
                    .padding(.top)
                }
            }
            .navigationTitle("Edit Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .fontWeight(.semibold)
                        .disabled(!canSave)
                }
            }
        }
    }

    @Environment(\.modelContext) private var modelContext
    
    // We update the save method to also update sibling entries that share the same imageID.
    private func save() {
        entry.name = name.trimmingCharacters(in: .whitespaces)
        
        // Update mealName for this entry and any sibling entries in the same group
        let updatedMealName = mealName.trimmingCharacters(in: .whitespaces)
        let newMealNameValue: String? = updatedMealName.isEmpty ? nil : updatedMealName
        
        entry.mealName = newMealNameValue
        
        if let imageID = entry.imageID {
            // Find other entries with the same imageID and update their mealName too
            let fetchDescriptor = FetchDescriptor<FoodEntry>(
                predicate: #Predicate { $0.imageID == imageID }
            )
            if let siblings = try? modelContext.fetch(fetchDescriptor) {
                for sibling in siblings where sibling.id != entry.id {
                    sibling.mealName = newMealNameValue
                }
            }
        }
        
        entry.calories = Double(caloriesText) ?? 0
        entry.quantity = quantity.trimmingCharacters(in: .whitespaces)
        entry.mealType = mealType
        
        if let p = Double(proteinText) {
            entry.protein = p
        } else if proteinText.isEmpty {
            entry.protein = nil
        }
        
        if let c = Double(carbsText) {
            entry.carbs = c
        } else if carbsText.isEmpty {
            entry.carbs = nil
        }
        
        if let f = Double(fatText) {
            entry.fat = f
        } else if fatText.isEmpty {
            entry.fat = nil
        }

        dismiss()
    }
}

// MARK: - Edit Meal Group

struct EditMealGroupView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    let entries: [FoodEntry]

    @State private var mealName: String
    @State private var mealType: MealType
    @State private var itemStates: [ItemEditState]

    struct ItemEditState: Identifiable {
        let id: PersistentIdentifier
        var name: String
        var caloriesText: String
        var quantity: String
        var proteinText: String
        var carbsText: String
        var fatText: String
        var showNutrition: Bool
    }

    init(entries: [FoodEntry]) {
        self.entries = entries
        _mealName = State(initialValue: entries.first?.mealName ?? "")
        _mealType = State(initialValue: entries.first?.mealType ?? .snack)
        _itemStates = State(initialValue: entries.map { entry in
            ItemEditState(
                id: entry.persistentModelID,
                name: entry.name,
                caloriesText: String(format: "%g", entry.calories),
                quantity: entry.quantity,
                proteinText: entry.protein.map { String(format: "%g", $0) } ?? "",
                carbsText: entry.carbs.map { String(format: "%g", $0) } ?? "",
                fatText: entry.fat.map { String(format: "%g", $0) } ?? "",
                showNutrition: entry.protein != nil || entry.carbs != nil || entry.fat != nil
            )
        })
    }

    private var totalCalories: Double {
        itemStates.reduce(0) { $0 + (Double($1.caloriesText) ?? 0) }
    }

    private var canSave: Bool {
        itemStates.allSatisfy { item in
            !item.name.trimmingCharacters(in: .whitespaces).isEmpty
            && (Double(item.caloriesText) ?? -1) > 0
            && !item.quantity.trimmingCharacters(in: .whitespaces).isEmpty
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Shared meal info
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Meal Info")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            VStack(spacing: 12) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Meal Name")
                                        .font(.caption)
                                        .foregroundStyle(Color.cwTextSecondary)
                                    TextField("Meal Name (e.g., Chicken & Rice)", text: $mealName)
                                        .textFieldStyle(.roundedBorder)
                                }

                                Picker("Meal", selection: $mealType) {
                                    ForEach(MealType.allCases, id: \.self) { type in
                                        Text(type.rawValue).tag(type)
                                    }
                                }
                                .pickerStyle(.segmented)
                            }
                        }
                        .cwCard()
                        .padding(.horizontal)

                        // Per-item sections
                        ForEach($itemStates) { $item in
                            VStack(alignment: .leading, spacing: 16) {
                                Text(item.name.isEmpty ? "Item" : item.name)
                                    .font(.headline)
                                    .foregroundStyle(Color.cwTextPrimary)

                                VStack(spacing: 12) {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("Item Name")
                                            .font(.caption)
                                            .foregroundStyle(Color.cwTextSecondary)
                                        TextField("Food name", text: $item.name)
                                            .textFieldStyle(.roundedBorder)
                                    }

                                    HStack(spacing: 12) {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text("Calories")
                                                .font(.caption)
                                                .foregroundStyle(Color.cwTextSecondary)
                                            TextField("Calories", text: $item.caloriesText)
                                                .textFieldStyle(.roundedBorder)
                                                .keyboardType(.decimalPad)
                                        }

                                        VStack(alignment: .leading, spacing: 4) {
                                            Text("Quantity")
                                                .font(.caption)
                                                .foregroundStyle(Color.cwTextSecondary)
                                            TextField(SettingsStore.shared.unitSystem == .metric ? "e.g. 200 g" : "e.g. 1 cup", text: $item.quantity)
                                                .textFieldStyle(.roundedBorder)
                                        }
                                    }

                                    DisclosureGroup("Nutrition (optional)", isExpanded: $item.showNutrition) {
                                        HStack(spacing: 12) {
                                            EditNutrientField(label: "Protein (g)", text: $item.proteinText)
                                            EditNutrientField(label: "Carbs (g)", text: $item.carbsText)
                                            EditNutrientField(label: "Fat (g)", text: $item.fatText)
                                        }
                                        .padding(.top, 8)
                                    }
                                    .font(.subheadline)
                                    .foregroundStyle(Color.cwTextPrimary)
                                }
                            }
                            .cwCard()
                            .padding(.horizontal)
                        }

                        // Total calories summary
                        HStack {
                            Text("Total Calories")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)
                            Spacer()
                            Text("\(Int(totalCalories)) kcal")
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundStyle(Color.cwPrimary)
                        }
                        .cwCard()
                        .padding(.horizontal)
                    }
                    .padding(.top)
                }
            }
            .navigationTitle("Edit Meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .fontWeight(.semibold)
                        .disabled(!canSave)
                }
            }
        }
    }

    private func save() {
        let updatedMealName = mealName.trimmingCharacters(in: .whitespaces)
        let newMealNameValue: String? = updatedMealName.isEmpty ? nil : updatedMealName

        for (entry, state) in zip(entries, itemStates) {
            entry.mealName = newMealNameValue
            entry.mealType = mealType
            entry.name = state.name.trimmingCharacters(in: .whitespaces)
            entry.calories = Double(state.caloriesText) ?? 0
            entry.quantity = state.quantity.trimmingCharacters(in: .whitespaces)

            if let p = Double(state.proteinText) {
                entry.protein = p
            } else if state.proteinText.isEmpty {
                entry.protein = nil
            }

            if let c = Double(state.carbsText) {
                entry.carbs = c
            } else if state.carbsText.isEmpty {
                entry.carbs = nil
            }

            if let f = Double(state.fatText) {
                entry.fat = f
            } else if state.fatText.isEmpty {
                entry.fat = nil
            }
        }

        dismiss()
    }
}

private struct EditNutrientField: View {
    let label: String
    @Binding var text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Color.cwTextSecondary)
            TextField("—", text: $text)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.decimalPad)
        }
    }
}

// MARK: - View Food Entry

struct ViewFoodEntryView: View {
    @Environment(\.dismiss) private var dismiss
    let entry: FoodEntry

    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Food info section
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Food Details")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            VStack(spacing: 12) {
                                ViewFieldRow(label: "Food name", value: entry.name)
                                ViewFieldRow(label: "Calories", value: "\(Int(entry.calories)) kcal")
                                ViewFieldRow(label: "Quantity", value: SettingsStore.shared.unitSystem.convertQuantity(entry.quantity))
                            }
                        }
                        .cwCard()
                        .padding(.horizontal)

                        // Meal type
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Meal")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            Text(entry.mealType.rawValue)
                                .font(.subheadline)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .cwCard()
                        .padding(.horizontal)

                        // Optional nutrition
                        if entry.protein != nil || entry.carbs != nil || entry.fat != nil {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Nutrition Details")
                                    .font(.headline)
                                    .foregroundStyle(Color.cwTextPrimary)

                                VStack(spacing: 12) {
                                    HStack(spacing: 12) {
                                        ViewNutrientBox(label: "Protein", value: entry.protein, unit: "g")
                                        ViewNutrientBox(label: "Carbs", value: entry.carbs, unit: "g")
                                        ViewNutrientBox(label: "Fat", value: entry.fat, unit: "g")
                                    }
                                }
                            }
                            .cwCard()
                            .padding(.horizontal)
                        }
                    }
                    .padding(.top)
                }
            }
            .navigationTitle("View Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
    }
}

// MARK: - View Meal Group

struct ViewMealGroupView: View {
    @Environment(\.dismiss) private var dismiss
    let entries: [FoodEntry]

    private var displayedTitle: String {
        if let userDefined = entries.first(where: { $0.mealName != nil })?.mealName, !userDefined.isEmpty {
            return userDefined
        }
        if entries.count == 1 {
            return entries[0].name
        } else if let firstItem = entries.first {
            return "\(firstItem.name) + \(entries.count - 1) more"
        }
        return "Meal"
    }

    private var mealType: MealType {
        entries.first?.mealType ?? .snack
    }

    private var totalCalories: Double {
        entries.reduce(0) { $0 + $1.calories }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Meal info
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Meal Info")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)

                            VStack(spacing: 12) {
                                ViewFieldRow(label: "Meal Name", value: displayedTitle)
                                ViewFieldRow(label: "Meal Type", value: mealType.rawValue)
                            }
                        }
                        .cwCard()
                        .padding(.horizontal)

                        // Per-item sections
                        ForEach(entries) { entry in
                            VStack(alignment: .leading, spacing: 16) {
                                Text(entry.name)
                                    .font(.headline)
                                    .foregroundStyle(Color.cwTextPrimary)

                                VStack(spacing: 12) {
                                    ViewFieldRow(label: "Food name", value: entry.name)
                                    ViewFieldRow(label: "Calories", value: "\(Int(entry.calories)) kcal")
                                    ViewFieldRow(label: "Quantity", value: SettingsStore.shared.unitSystem.convertQuantity(entry.quantity))
                                }

                                if entry.protein != nil || entry.carbs != nil || entry.fat != nil {
                                    VStack(spacing: 12) {
                                        HStack(spacing: 12) {
                                            ViewNutrientBox(label: "Protein", value: entry.protein, unit: "g")
                                            ViewNutrientBox(label: "Carbs", value: entry.carbs, unit: "g")
                                            ViewNutrientBox(label: "Fat", value: entry.fat, unit: "g")
                                        }
                                    }
                                }
                            }
                            .cwCard()
                            .padding(.horizontal)
                        }

                        // Total calories
                        HStack {
                            Text("Total Calories")
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)
                            Spacer()
                            Text("\(Int(totalCalories)) kcal")
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundStyle(Color.cwPrimary)
                        }
                        .cwCard()
                        .padding(.horizontal)
                    }
                    .padding(.top)
                }
            }
            .navigationTitle("View Meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
    }
}

private struct ViewFieldRow: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Color.cwTextSecondary)
            Text(value)
                .font(.body)
                .foregroundStyle(Color.cwTextPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 8)
                .padding(.horizontal, 12)
                .background(Color.cwSurface)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.2), lineWidth: 1))
        }
    }
}

private struct ViewNutrientBox: View {
    let label: String
    let value: Double?
    let unit: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Color.cwTextSecondary)
            Text(value.map { "\(String(format: "%g", $0)) \(unit)" } ?? "—")
                .font(.body)
                .foregroundStyle(Color.cwTextPrimary)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.vertical, 8)
                .background(Color.cwSurface)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.2), lineWidth: 1))
        }
    }
}

// MARK: - AI Consent Sheet

struct AIConsentSheet: View {
    var onAccept: () -> Void
    var onDecline: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "photo.badge.arrow.up")
                .font(.system(size: 48))
                .foregroundStyle(Color.cwPrimary)
                .padding(.top, 32)

            Text("AI Food Analysis")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundStyle(Color.cwTextPrimary)

            Text("Your food photos are sent to Google Gemini, a third-party AI service by Google, for calorie estimation. Photos are processed in transit and are not stored by any external service.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal)

            Text("You can change this in Settings.")
                .font(.caption)
                .foregroundStyle(.tertiary)

            Spacer()

            VStack(spacing: 12) {
                Button(action: onAccept) {
                    Text("Allow Photo Analysis")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.cwPrimary)
                .controlSize(.large)

                Button(action: onDecline) {
                    Text("Don't Allow")
                        .fontWeight(.medium)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
            }
            .padding(.horizontal)
            .padding(.bottom)
        }
        .padding()
        .presentationDetents([.medium])
        .interactiveDismissDisabled()
    }
}

struct CalorieDisclaimerSheet: View {
    var onContinue: (_ dontShowAgain: Bool) -> Void
    @State private var dontShowAgain: Bool = false

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "sparkles")
                .font(.system(size: 48))
                .foregroundStyle(Color.cwPrimary)
                .padding(.top, 32)

            Text("Estimates Are Approximate")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundStyle(Color.cwTextPrimary)

            Text("Calorie and nutrition values are generated by AI and may not be perfectly accurate. Results can vary based on portion size, preparation method, and image quality. This is not medical or nutritional advice.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal)

            Text("You can always find this notice in About.")
                .font(.caption)
                .foregroundStyle(.tertiary)

            Spacer()

            Toggle("Don't show this again", isOn: $dontShowAgain)
                .tint(Color.cwPrimary)
                .padding(.horizontal)
                .accessibilityIdentifier(AccessibilityID.Disclaimer.dontShowToggle)

            Button(action: { onContinue(dontShowAgain) }) {
                Text("Continue")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.cwPrimary)
            .controlSize(.large)
            .padding(.horizontal)
            .padding(.bottom)
            .accessibilityIdentifier(AccessibilityID.Disclaimer.continueButton)
        }
        .padding()
        .presentationDetents([.medium])
    }
}

