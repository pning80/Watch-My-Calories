import SwiftUI
import SwiftData
import Charts

struct HistoryView: View {
    var onLogFood: () -> Void

    // Changed order to .forward so items inside the day card are chronological
    @Query(sort: \FoodEntry.timestamp, order: .forward) private var foodEntries: [FoodEntry]
    @Query private var userProfiles: [UserProfile]

    private var target: Double {
        let t = userProfiles.first?.targetCalories ?? 0
        return t > 0 ? t : 2000
    }

    @State private var selectedImage: UIImage?
    @State private var entryToEdit: FoodEntry?
    @State private var entryToView: FoodEntry?
    @State private var groupToEdit: FoodEntryGroupEdit?
    @State private var groupToView: FoodEntryGroupEdit?
    
    var groupedEntries: [Date: [FoodEntry]] {
        let calendar = Calendar.current
        return Dictionary(grouping: foodEntries) { entry in
            calendar.startOfDay(for: entry.timestamp)
        }
    }
    
    var body: some View {
        // Group once per render; the day cards and the trend card all read from this.
        let grouped = groupedEntries
        // Newest day at top; items inside each day stay chronological (.forward query).
        let sortedDates = grouped.keys.sorted(by: >)

        NavigationStack {
            ZStack {
                Color.cwBackground.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 16) {
                        HStack {
                            Text("History")
                                .cwTitle()
                                .accessibilityIdentifier(AccessibilityID.History.title)
                            Spacer()
                            AppMenuButton()
                        }
                        .padding(.horizontal)
                        .padding(.top)

                        BannerAdView()

                        if sortedDates.isEmpty {
                            Button {
                                onLogFood()
                            } label: {
                                EmptyStateCard()
                            }
                            .buttonStyle(.plain)
                            .padding(.top, 40)
                            .accessibilityIdentifier(AccessibilityID.History.emptyState)
                        } else {
                            HistoryTrendCard(
                                dailyTotals: grouped.mapValues { $0.reduce(0) { $0 + $1.calories } },
                                target: target
                            )

                            ForEach(sortedDates, id: \.self) { date in
                                HistoryDayCard(date: date, target: target, entries: grouped[date] ?? [], onImageTap: { image in
                                    self.selectedImage = image
                                }, onEdit: { entry in
                                    self.entryToEdit = entry
                                }, onEditGroup: { items in
                                    self.groupToEdit = FoodEntryGroupEdit(items: items)
                                }, onView: { entry in
                                    self.entryToView = entry
                                }, onViewGroup: { items in
                                    self.groupToView = FoodEntryGroupEdit(items: items)
                                })
                            }
                        }
                    }
                    .padding(.bottom, 100)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
        }
        .fullScreenCover(item: Binding<ImageWrapper?>(
            get: { selectedImage.map { ImageWrapper(image: $0) } },
            set: { selectedImage = $0?.image }
        )) { wrapper in
            FullScreenImageView(image: wrapper.image)
        }
        .sheet(item: $entryToEdit) { entry in
            EditFoodEntryView(entry: entry)
        }
        .sheet(item: $groupToEdit) { group in
            EditMealGroupView(entries: group.items)
        }
        .sheet(item: $entryToView) { entry in
            ViewFoodEntryView(entry: entry)
        }
        .sheet(item: $groupToView) { group in
            ViewMealGroupView(entries: group.items)
        }
    }
}

// HistoryDayCard remains unchanged (it just displays the entries array it is given)
struct HistoryDayCard: View {
    let date: Date
    var target: Double = 0
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

    var isOverTarget: Bool {
        target > 0 && totalCalories > target
    }
    
    @State private var isExpanded = false
    
    var mealGroupedEntries: [MealType: [FoodEntry]] {
        Dictionary(grouping: entries) { entry in
            entry.mealType
        }
    }
    
    // Extracted grouping logic (similar to MealSection)
    func groupEntries(_ mealEntries: [FoodEntry]) -> [FoodEntryGroup] {
        var groups: [FoodEntryGroup] = []
        var currentGroupItems: [FoodEntry] = []
        var currentImageID: UUID? = nil
        
        for entry in mealEntries {
            if entry.imageID == nil {
                if !currentGroupItems.isEmpty {
                    groups.append(FoodEntryGroup(items: currentGroupItems))
                    currentGroupItems = []
                }
                groups.append(FoodEntryGroup(items: [entry]))
                currentImageID = nil
            } else if entry.imageID == currentImageID {
                currentGroupItems.append(entry)
            } else {
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
    
    let mealOrder: [MealType] = [.breakfast, .lunch, .dinner, .snack]
    
    var body: some View {
        VStack(spacing: 0) {
            Button(action: { withAnimation(.snappy) { isExpanded.toggle() } }) {
                VStack(spacing: 0) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(date, format: .dateTime.day().month(.wide))
                                .font(.headline)
                                .foregroundStyle(Color.cwTextPrimary)
                            Text(date, format: .dateTime.weekday(.wide))
                                .font(.caption)
                                .fontWeight(.medium)
                                .foregroundStyle(Color.cwTextSecondary)
                        }

                        Spacer()

                        VStack(alignment: .trailing, spacing: 0) {
                            Text("\(Int(totalCalories))")
                                .cwNumeral(.dayTotal)
                                .foregroundStyle(isOverTarget ? Color.cwOverGoal : Color.cwPrimary)
                            Text("kcal")
                                .font(.caption2)
                                .fontWeight(.bold)
                                .foregroundStyle(Color.cwTextSecondary)
                        }

                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(Color.cwTextTertiary)
                            .rotationEffect(.degrees(isExpanded ? 90 : 0))
                            .padding(.leading, 8)
                    }
                    .padding()

                    CompactMacroRow(entries: entries)
                        .padding(.horizontal)
                        .padding(.bottom, 10)
                }
                .background(Color.cwSurface)
            }

            if isExpanded {
                Divider()
                VStack(spacing: 0) {
                    ForEach(mealOrder, id: \.self) { mealType in
                        if let mealEntries = mealGroupedEntries[mealType], !mealEntries.isEmpty {
                            HStack {
                                Text(mealType.rawValue)
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundStyle(Color.cwPrimary)
                                    .textCase(.uppercase)
                                    .padding(.vertical, 4)
                                Spacer()
                            }
                            .padding(.horizontal)
                            .background(Color.cwBackground)
                            
                            let groups = groupEntries(mealEntries)
                            ForEach(groups, id: \.id) { group in
                                FoodEntryGroupCard(
                                    group: group,
                                    onThumbnailTap: onImageTap,
                                    onEdit: onEdit,
                                    onEditGroup: onEditGroup,
                                    onView: onView,
                                    onViewGroup: onViewGroup,
                                    onDelete: { item in
                                        withAnimation {
                                            if let imageID = item.imageID {
                                                let otherEntries = entries.filter { $0.imageID == imageID && $0.id != item.id }
                                                if otherEntries.isEmpty {
                                                    ImageStorage.shared.delete(id: imageID)
                                                }
                                            }
                                            modelContext.delete(item)
                                        }
                                    },
                                    onDeleteGroup: { items in
                                        withAnimation {
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
                                    }
                                )
                                
                                if group.id != groups.last?.id {
                                    Divider()
                                        .padding(.leading, 56)
                                }
                            }
                        }
                    }
                }
                .padding(.bottom, 8)
                .background(Color.cwSurface)
                .transition(.opacity)
            }
        }
        .cwSurface(role: .card)
        .padding(.horizontal)
        .sensoryFeedback(.selection, trigger: isExpanded)
        .accessibilityIdentifier(AccessibilityID.History.dayCard)
    }
}

// MARK: - Trend chart + stats

struct DailyTotal: Identifiable, Equatable {
    let date: Date
    let total: Double
    var id: Date { date }
}

/// Pure trend computations behind `HistoryTrendCard`, split out so they are unit-testable.
struct HistoryTrendStats {
    /// Calorie total per start-of-day. A key's presence means the day has at least one entry.
    let dailyTotals: [Date: Double]
    let target: Double
    let days: Int
    var today: Date = Date()
    var calendar: Calendar = .current

    /// One point per day for the last `days` days (oldest first, ending today), zero-filled.
    var series: [DailyTotal] {
        let start = calendar.startOfDay(for: today)
        return (0..<days).reversed().compactMap { offset in
            guard let day = calendar.date(byAdding: .day, value: -offset, to: start) else { return nil }
            return DailyTotal(date: day, total: dailyTotals[day] ?? 0)
        }
    }

    var loggedTotals: [Double] { series.map(\.total).filter { $0 > 0 } }

    /// Average over logged (non-zero) days only, so empty days don't drag it down.
    var avgPerDay: Int {
        guard !loggedTotals.isEmpty else { return 0 }
        return Int(loggedTotals.reduce(0, +) / Double(loggedTotals.count))
    }

    var daysOnTarget: Int {
        target > 0 ? series.filter { $0.total > 0 && $0.total <= target }.count : 0
    }

    /// Consecutive days with entries, counting back from today (0 if today has none).
    var streak: Int {
        var count = 0
        var day = calendar.startOfDay(for: today)
        while dailyTotals[day] != nil {
            count += 1
            guard let prev = calendar.date(byAdding: .day, value: -1, to: day) else { break }
            day = prev
        }
        return count
    }
}

struct HistoryTrendCard: View {
    /// Calorie total per start-of-day, only for days that have entries.
    let dailyTotals: [Date: Double]
    let target: Double

    enum TrendRange: String, CaseIterable, Identifiable {
        case week = "7D"
        case month = "30D"
        var id: String { rawValue }
        var days: Int { self == .week ? 7 : 30 }
    }

    @State private var range: TrendRange = .week

    var body: some View {
        // Built once per render, so every chart mark and stat chip shares one "today".
        let stats = HistoryTrendStats(dailyTotals: dailyTotals, target: target, days: range.days)
        let series = stats.series

        VStack(alignment: .leading, spacing: CWSpacing.m) {
            HStack {
                Text("Trends")
                    .font(.headline)
                    .foregroundStyle(Color.cwTextPrimary)
                Spacer()
                Picker("Range", selection: $range) {
                    ForEach(TrendRange.allCases) { r in
                        Text(r.rawValue).tag(r)
                    }
                }
                .pickerStyle(.segmented)
                .fixedSize()
            }

            Chart {
                ForEach(series) { day in
                    BarMark(
                        x: .value("Day", day.date, unit: .day),
                        y: .value("kcal", day.total)
                    )
                    .foregroundStyle((target > 0 && day.total > target) ? Color.cwOverGoal : Color.cwPrimary)
                    .cornerRadius(3)
                }
                if target > 0 {
                    RuleMark(y: .value("Goal", target))
                        .lineStyle(StrokeStyle(lineWidth: 1, dash: [4]))
                        .foregroundStyle(Color.cwTextTertiary)
                        .annotation(position: .top, alignment: .trailing) {
                            Text("Goal")
                                .font(.caption2)
                                .foregroundStyle(Color.cwTextSecondary)
                        }
                }
            }
            .chartYAxis {
                // Gridlines only — numeric labels are intentionally omitted so the chart
                // stays compact and never duplicates a day-total value elsewhere on screen.
                AxisMarks(position: .leading) { _ in
                    AxisGridLine()
                }
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: .day, count: range == .week ? 1 : 7)) { _ in
                    AxisValueLabel(format: .dateTime.day())
                }
            }
            .frame(height: 150)
            // One VoiceOver element with a spoken summary; Audio Graphs (rotor → "Chart Details")
            // gets the full per-day series from the descriptor.
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Daily calories, last \(range.days) days")
            .accessibilityValue(chartSummary(series: series))
            .accessibilityChartDescriptor(TrendChartDescriptor(series: series, target: target))

            HStack(spacing: CWSpacing.m) {
                statChip(value: "\(stats.avgPerDay) kcal", label: "Avg / day")
                statChip(value: target > 0 ? "\(stats.daysOnTarget)/\(stats.loggedTotals.count)" : "—", label: "On target")
                statChip(value: "\(stats.streak)", label: "Day streak")
            }
        }
        .cwCard()
        .padding(.horizontal)
    }

    private func chartSummary(series: [DailyTotal]) -> String {
        let logged = series.filter { $0.total > 0 }
        guard !logged.isEmpty else { return "No meals logged" }
        var parts = ["\(logged.count) of \(series.count) days logged"]
        if target > 0 {
            let over = logged.filter { $0.total > target }.count
            parts.append(over == 0 ? "none over goal" : "\(over) over goal")
        }
        return parts.joined(separator: ", ")
    }

    private func statChip(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundStyle(Color.cwPrimary)
            Text(label)
                .font(.caption2)
                .foregroundStyle(Color.cwTextSecondary)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label), \(value)")
    }
}

/// Audio Graphs data for the trend chart: one bar per day, goal as a gridline.
private struct TrendChartDescriptor: AXChartDescriptorRepresentable {
    let series: [DailyTotal]
    let target: Double

    func makeChartDescriptor() -> AXChartDescriptor {
        let dayFormat = Date.FormatStyle().month(.abbreviated).day()
        let days = series.map { $0.date.formatted(dayFormat) }
        let maxY = max(series.map(\.total).max() ?? 0, target, 1)

        let xAxis = AXCategoricalDataAxisDescriptor(title: "Day", categoryOrder: days)
        let yAxis = AXNumericDataAxisDescriptor(
            title: "Calories",
            range: 0...maxY,
            gridlinePositions: target > 0 ? [target] : []
        ) { "\(Int($0)) kilocalories" }

        let points = zip(days, series).map { AXDataPoint(x: $0, y: $1.total) }
        return AXChartDescriptor(
            title: "Daily calories",
            summary: target > 0 ? "Goal is \(Int(target)) kilocalories per day" : nil,
            xAxis: xAxis,
            yAxis: yAxis,
            additionalAxes: [],
            series: [AXDataSeriesDescriptor(name: "Calories", isContinuous: false, dataPoints: points)]
        )
    }
}
