import SwiftUI
import SwiftData

struct ScannedMenusView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \MenuScan.timestamp, order: .reverse) private var scans: [MenuScan]

    @State private var editMode: EditMode = .inactive

    var body: some View {
        Group {
            if scans.isEmpty {
                emptyState
            } else {
                scanList
            }
        }
        .navigationTitle("Scanned Menus")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !scans.isEmpty {
                ToolbarItem(placement: .topBarLeading) {
                    EditButton()
                }
            }
        }
        .environment(\.editMode, $editMode)
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "menucard")
                .font(.system(size: 64))
                .foregroundStyle(Color.cwPrimary.opacity(0.4))
            Text("No scanned menus yet")
                .font(.headline)
            Text("Use the Scan Menu tab to photograph a restaurant menu and see calorie estimates.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Spacer()
        }
    }

    private var scanList: some View {
        List {
            Section {
                BannerAdView()
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets())
            }

            Section {
                ForEach(scans) { scan in
                    NavigationLink {
                        MenuScanDetailView(scan: scan)
                    } label: {
                        scanRow(scan)
                    }
                }
                .onDelete(perform: deleteScans)
            }
        }
        .listStyle(.plain)
    }

    private func scanRow(_ scan: MenuScan) -> some View {
        HStack(spacing: 12) {
            if let imageID = scan.imageID, let image = ImageStorage.shared.load(id: imageID) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 56, height: 56)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            } else {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color.cwSurface)
                    .frame(width: 56, height: 56)
                    .overlay(
                        Image(systemName: "menucard")
                            .foregroundStyle(.secondary)
                    )
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(scan.restaurantName ?? "Unknown Restaurant")
                    .font(.headline)
                    .lineLimit(1)

                Text(scan.timestamp, format: .dateTime.month().day().year().hour().minute())
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Text("\(scan.items.count) items")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    private func deleteScans(at offsets: IndexSet) {
        for index in offsets {
            let scan = scans[index]
            if let imageID = scan.imageID {
                _ = ImageStorage.shared.delete(id: imageID)
            }
            modelContext.delete(scan)
        }
    }
}

// MARK: - Detail View

struct MenuScanDetailView: View {
    let scan: MenuScan
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @State private var expandedItems: Set<UUID> = []
    @State private var showDeleteConfirmation = false
    @State private var showFullScreenImage = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Header
                HStack(spacing: 8) {
                    Image(systemName: "fork.knife")
                        .font(.title2)
                        .foregroundStyle(Color.cwPrimary)
                    Text("Menu Analysis")
                        .font(.system(.title2, design: .serif, weight: .bold))
                        .foregroundStyle(Color.cwPrimary)
                }
                .padding(.top, 20)

                if let name = scan.restaurantName {
                    Text(name)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Text(scan.timestamp, format: .dateTime.month().day().year().hour().minute())
                    .font(.caption)
                    .foregroundStyle(.tertiary)

                // Photo
                if let imageID = scan.imageID, let image = ImageStorage.shared.load(id: imageID) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 200)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .padding(.horizontal)
                        .onTapGesture { showFullScreenImage = true }
                        .fullScreenCover(isPresented: $showFullScreenImage) {
                            FullScreenImageView(image: image)
                        }
                }

                // Items
                LazyVStack(spacing: 12) {
                    ForEach(scan.items) { item in
                        itemCard(item)
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 30)
            }
        }
        .background(Color.cwBackground)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(role: .destructive) {
                    showDeleteConfirmation = true
                } label: {
                    Image(systemName: "trash")
                }
            }
        }
        .confirmationDialog("Delete this scanned menu?", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
            Button("Delete", role: .destructive) {
                if let imageID = scan.imageID {
                    _ = ImageStorage.shared.delete(id: imageID)
                }
                modelContext.delete(scan)
                dismiss()
            }
        }
    }

    private func itemCard(_ item: MenuItemResult) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(item.name)
                    .font(.headline)
                Spacer()
                Text("~\(Int(item.calories)) cal")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.cwPrimary)
            }

            if let desc = item.description, !desc.isEmpty {
                Text(desc)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            if expandedItems.contains(item.id) {
                HStack(spacing: 16) {
                    if let p = item.protein { macroLabel("Protein", value: p) }
                    if let c = item.carbs { macroLabel("Carbs", value: c) }
                    if let f = item.fat { macroLabel("Fat", value: f) }
                }
                .padding(.top, 4)
            }
        }
        .padding()
        .background(Color.cwSurface)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.2)) {
                if expandedItems.contains(item.id) {
                    expandedItems.remove(item.id)
                } else {
                    expandedItems.insert(item.id)
                }
            }
        }
    }

    private func macroLabel(_ label: String, value: Double) -> some View {
        VStack(spacing: 2) {
            Text("\(Int(value))g")
                .font(.caption)
                .fontWeight(.semibold)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}

